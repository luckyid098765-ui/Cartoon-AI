package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CartoonLanguage
import com.example.data.model.CartoonProject
import com.example.data.model.CartoonStyle
import com.example.data.model.CharacterExpression
import com.example.data.model.GenerationHistoryEntry
import com.example.data.model.SavedCharacter
import com.example.data.model.SceneRecord
import com.example.data.model.UserProfile
import com.example.data.model.VoiceGender
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CartoonRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun <T> wrapFirestoreStream(
        flow: Flow<T>,
        uid: String,
        operationType: OperationType,
        path: String
    ): Flow<T> = flow.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Refusing cross-user read." }
        val path = "users/$uid"
        emitAll(
            wrapFirestoreStream(
                db.collection("users").document(uid)
                    .snapshots()
                    .map { snapshot ->
                        if (snapshot.exists()) {
                            snapshot.toObject(
                                UserProfile::class.java,
                                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                            )
                        } else null
                    },
                uid,
                OperationType.GET,
                path
            )
        )
    }

    fun observeProjects(userId: String): Flow<List<CartoonProject>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Refusing cross-user read." }
        val path = "users/$uid/projects"
        emitAll(
            wrapFirestoreStream(
                db.collection("users").document(uid).collection("projects")
                    .orderBy("updatedAt", Query.Direction.DESCENDING)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(
                            CartoonProject::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    },
                uid,
                OperationType.LIST,
                path
            )
        )
    }

    fun observeScenes(userId: String, projectId: String): Flow<List<SceneRecord>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Refusing cross-user read." }
        require(projectId.isNotBlank()) { "Project ID required" }
        val path = "users/$uid/projects/$projectId/scenes"
        emitAll(
            wrapFirestoreStream(
                db.collection("users").document(uid)
                    .collection("projects").document(projectId)
                    .collection("scenes")
                    .orderBy("sceneIndex", Query.Direction.ASCENDING)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(
                            SceneRecord::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    },
                uid,
                OperationType.LIST,
                path
            )
        )
    }

    fun observeCharacters(userId: String): Flow<List<SavedCharacter>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Refusing cross-user read." }
        val path = "users/$uid/characters"
        emitAll(
            wrapFirestoreStream(
                db.collection("users").document(uid).collection("characters")
                    .orderBy("updatedAt", Query.Direction.DESCENDING)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(
                            SavedCharacter::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    },
                uid,
                OperationType.LIST,
                path
            )
        )
    }

    fun observeHistory(userId: String): Flow<List<GenerationHistoryEntry>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Refusing cross-user read." }
        val path = "users/$uid/generationHistory"
        emitAll(
            wrapFirestoreStream(
                db.collection("users").document(uid).collection("generationHistory")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(50)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(
                            GenerationHistoryEntry::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    },
                uid,
                OperationType.LIST,
                path
            )
        )
    }

    suspend fun ensureUserProfile(user: FirebaseUser): Result<Unit> {
        val path = "users/${user.uid}"
        return try {
            val uid = requireUserId()
            check(uid == user.uid) { "User mismatch" }
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val safeName = (user.displayName?.takeIf { it.isNotBlank() } ?: "Cartoon Creator").take(80)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to user.email?.take(120),
                    "photoUrl" to user.photoUrl?.toString()?.take(500),
                    "creditsRemaining" to 50L,
                    "totalVideosGenerated" to 0L,
                    "totalScenesRendered" to 0L,
                    "preferredStyle" to CartoonStyle.CARTOON_3D.key,
                    "preferredLanguage" to CartoonLanguage.ENGLISH.key,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateUserPreferences(
        style: CartoonStyle,
        language: CartoonLanguage
    ): Result<Unit> {
        val path = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            db.collection("users").document(uid).update(
                mapOf(
                    "preferredStyle" to style.key,
                    "preferredLanguage" to language.key,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deductCreditsAndRecordStats(
        creditsCost: Long,
        videosIncrement: Long,
        scenesIncrement: Long
    ): Result<Unit> {
        val path = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val currentCredits = snap.getLong("creditsRemaining") ?: 50L
                val currentVideos = snap.getLong("totalVideosGenerated") ?: 0L
                val currentScenes = snap.getLong("totalScenesRendered") ?: 0L
                val nextCredits = (currentCredits - creditsCost).coerceAtLeast(0L)
                tx.update(
                    docRef,
                    mapOf(
                        "creditsRemaining" to nextCredits,
                        "totalVideosGenerated" to (currentVideos + videosIncrement),
                        "totalScenesRendered" to (currentScenes + scenesIncrement),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun refillStarterCreditsIfLow(): Result<Unit> {
        val path = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            db.collection("users").document(uid).update(
                mapOf(
                    "creditsRemaining" to 50L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createProject(project: CartoonProject): Result<String> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/projects"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("projects").document()
            path = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "title" to project.title.trim().take(120).ifBlank { "Untitled Cartoon" },
                "prompt" to project.prompt.trim().take(20000),
                "style" to project.style,
                "durationOption" to project.durationOption,
                "language" to project.language,
                "aspectRatio" to project.aspectRatio,
                "resolution" to project.resolution,
                "status" to project.status,
                "progressPercent" to project.progressPercent.coerceIn(0L, 100L),
                "sceneCount" to project.sceneCount.coerceIn(1L, 30L),
                "completedScenes" to project.completedScenes.coerceIn(0L, 30L),
                "durationSeconds" to project.durationSeconds.coerceIn(1L, 1800L),
                "scriptSummary" to project.scriptSummary?.take(8000),
                "localVideoPath" to project.localVideoPath?.take(500),
                "thumbnailBase64" to project.thumbnailBase64?.take(150000),
                "errorMessage" to project.errorMessage?.take(500),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProjectProgress(
        projectId: String,
        title: String? = null,
        prompt: String? = null,
        status: String,
        progressPercent: Int,
        sceneCount: Int? = null,
        completedScenes: Int,
        durationSeconds: Int? = null,
        scriptSummary: String? = null,
        localVideoPath: String? = null,
        thumbnailBase64: String? = null,
        errorMessage: String? = null
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/projects/$projectId"
        return try {
            val uid = requireUserId()
            require(projectId.isNotBlank()) { "Project ID required" }
            val docRef = db.collection("users").document(uid).collection("projects").document(projectId)
            path = docRef.path
            val updates = mutableMapOf<String, Any>(
                "status" to status,
                "progressPercent" to progressPercent.coerceIn(0, 100).toLong(),
                "completedScenes" to completedScenes.coerceIn(0, 30).toLong(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            title?.let { updates["title"] = it.trim().take(120).ifBlank { "Cartoon Movie" } }
            prompt?.let { updates["prompt"] = it.trim().take(20000) }
            sceneCount?.let { updates["sceneCount"] = it.coerceIn(1, 30).toLong() }
            durationSeconds?.let { updates["durationSeconds"] = it.coerceIn(1, 1800).toLong() }
            scriptSummary?.let { updates["scriptSummary"] = it.take(8000) }
            localVideoPath?.let { updates["localVideoPath"] = it.take(500) }
            thumbnailBase64?.let { updates["thumbnailBase64"] = it.take(150000) }
            errorMessage?.let { updates["errorMessage"] = it.take(500) }

            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteProject(projectId: String): Result<Unit> {
        val path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/projects/$projectId"
        return try {
            val uid = requireUserId()
            val projRef = db.collection("users").document(uid).collection("projects").document(projectId)
            val scenes = projRef.collection("scenes").get().await()
            val batch = db.batch()
            scenes.documents.forEach { batch.delete(it.reference) }
            batch.delete(projRef)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveScenesBatch(projectId: String, scenes: List<SceneRecord>): Result<List<String>> {
        val path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/projects/$projectId/scenes"
        return try {
            val uid = requireUserId()
            val scenesCol = db.collection("users").document(uid)
                .collection("projects").document(projectId)
                .collection("scenes")

            // Remove old scenes if replacing during story edit
            val existing = scenesCol.get().await()
            val batch = db.batch()
            existing.documents.forEach { batch.delete(it.reference) }

            val createdIds = mutableListOf<String>()
            scenes.forEach { scene ->
                val docId = "scene_${scene.sceneIndex}"
                createdIds.add(docId)
                val docRef = scenesCol.document(docId)
                val payload = mapOf(
                    "userId" to uid,
                    "projectId" to projectId,
                    "sceneIndex" to scene.sceneIndex.coerceIn(0L, 30L),
                    "title" to scene.title.trim().take(120).ifBlank { "Scene ${scene.sceneIndex + 1}" },
                    "visualPrompt" to scene.visualPrompt.trim().take(2000).ifBlank { "Cartoon scene" },
                    "cameraMotion" to scene.cameraMotion,
                    "moodMusic" to scene.moodMusic,
                    "sfxCue" to scene.sfxCue,
                    "characterNames" to scene.characterNames.take(10),
                    "dialogues" to scene.dialogues.take(12),
                    "status" to scene.status,
                    "retryCount" to scene.retryCount.coerceIn(0L, 10L),
                    "localImagePath" to scene.localImagePath?.take(500),
                    "errorMessage" to scene.errorMessage?.take(500),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                batch.set(docRef, payload)
            }
            batch.commit().await()
            Result.success(createdIds)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateSceneStatus(
        projectId: String,
        sceneId: String,
        status: String,
        retryCount: Long,
        localImagePath: String? = null,
        errorMessage: String? = null,
        visualPrompt: String? = null,
        dialogues: List<Map<String, Any>>? = null
    ): Result<Unit> {
        val path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/projects/$projectId/scenes/$sceneId"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
                .collection("projects").document(projectId)
                .collection("scenes").document(sceneId)

            val updates = mutableMapOf<String, Any>(
                "status" to status,
                "retryCount" to retryCount.coerceIn(0L, 10L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            localImagePath?.let { updates["localImagePath"] = it.take(500) }
            errorMessage?.let { updates["errorMessage"] = it.take(500) }
            visualPrompt?.let { updates["visualPrompt"] = it.trim().take(2000) }
            dialogues?.let { updates["dialogues"] = it.take(12) }

            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveCharacter(character: SavedCharacter): Result<String> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/characters"
        return try {
            val uid = requireUserId()
            val col = db.collection("users").document(uid).collection("characters")
            val isNew = character.id.isBlank()
            val docRef = if (isNew) col.document() else col.document(character.id)
            path = docRef.path
            if (isNew) {
                val payload = mapOf(
                    "userId" to uid,
                    "name" to character.name.trim().take(60),
                    "role" to character.role.trim().take(80).ifBlank { "Lead Character" },
                    "appearance" to character.appearance.trim().take(1000),
                    "outfit" to character.outfit.trim().take(500),
                    "personality" to character.personality?.trim()?.take(300),
                    "voiceGender" to VoiceGender.fromKey(character.voiceGender).key,
                    "voicePitch" to character.voicePitch.coerceIn(0.5, 2.0),
                    "defaultExpression" to CharacterExpression.fromKey(character.defaultExpression).key,
                    "style" to CartoonStyle.fromKey(character.style).key,
                    "localPortraitPath" to character.localPortraitPath?.take(500),
                    "portraitBase64" to character.portraitBase64?.take(150000),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            } else {
                val updates = mapOf(
                    "name" to character.name.trim().take(60),
                    "role" to character.role.trim().take(80).ifBlank { "Lead Character" },
                    "appearance" to character.appearance.trim().take(1000),
                    "outfit" to character.outfit.trim().take(500),
                    "personality" to character.personality?.trim()?.take(300),
                    "voiceGender" to VoiceGender.fromKey(character.voiceGender).key,
                    "voicePitch" to character.voicePitch.coerceIn(0.5, 2.0),
                    "defaultExpression" to CharacterExpression.fromKey(character.defaultExpression).key,
                    "style" to CartoonStyle.fromKey(character.style).key,
                    "localPortraitPath" to character.localPortraitPath?.take(500),
                    "portraitBase64" to character.portraitBase64?.take(150000),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.update(updates).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCharacter(characterId: String): Result<Unit> {
        val path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/characters/$characterId"
        return try {
            val uid = requireUserId()
            db.collection("users").document(uid)
                .collection("characters").document(characterId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun addHistoryEntry(entry: GenerationHistoryEntry): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/generationHistory"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
                .collection("generationHistory").document()
            path = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "projectId" to entry.projectId.take(128).ifBlank { "draft" },
                "projectTitle" to entry.projectTitle.trim().take(120).ifBlank { "Cartoon Movie" },
                "actionType" to entry.actionType,
                "creditsUsed" to entry.creditsUsed.coerceIn(0L, 100L),
                "status" to entry.status,
                "durationLabel" to entry.durationLabel.take(40).ifBlank { "30s" },
                "language" to entry.language.take(30),
                "style" to entry.style.take(40),
                "details" to entry.details?.take(300),
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteHistoryEntry(historyId: String): Result<Unit> {
        val path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/generationHistory/$historyId"
        return try {
            val uid = requireUserId()
            db.collection("users").document(uid)
                .collection("generationHistory").document(historyId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun clearHistory(): Result<Unit> {
        val path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/generationHistory"
        return try {
            val uid = requireUserId()
            val snap = db.collection("users").document(uid)
                .collection("generationHistory")
                .get().await()
            val batch = db.batch()
            snap.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
