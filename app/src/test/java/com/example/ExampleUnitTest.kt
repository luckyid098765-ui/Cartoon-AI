package com.example

import com.example.data.model.BgmTrackOption
import com.example.data.model.CartoonLanguage
import com.example.data.model.CartoonProject
import com.example.data.model.CartoonStyle
import com.example.data.model.DialogueLine
import com.example.data.model.GenerationHistoryEntry
import com.example.data.model.MoodMusic
import com.example.data.model.ProjectStatus
import com.example.data.model.StoryGenre
import com.example.data.model.VideoAspectRatio
import com.example.data.model.VideoDurationOption
import com.example.data.model.VideoResolution
import com.example.data.repository.LocalStudioStore
import com.example.media.VideoExportHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class ExampleUnitTest {
    @Test
    fun cartoon_enums_and_dialogue_serialization_work() {
        assertEquals(CartoonStyle.CARTOON_3D, CartoonStyle.fromKey("UNKNOWN_STYLE"))
        assertEquals(CartoonLanguage.HINDI, CartoonLanguage.fromKey("HINDI"))
        assertEquals(CartoonLanguage.BENGALI, CartoonLanguage.fromKey("BENGALI"))
        assertEquals(CartoonLanguage.SPANISH, CartoonLanguage.fromKey("SPANISH"))
        assertEquals(CartoonLanguage.JAPANESE, CartoonLanguage.fromKey("JAPANESE"))
        assertEquals(30, VideoDurationOption.MAX_STORY_MINUTES)
        assertEquals(1800, VideoDurationOption.MAX_STORY_SECONDS)
        assertEquals(VideoDurationOption.MIN_30, VideoDurationOption.forMinutes(35))
        assertEquals(VideoDurationOption.MIN_1, VideoDurationOption.forMinutes(0))
        assertEquals(1800, VideoDurationOption.MIN_30.targetSeconds)
        assertEquals(30, VideoDurationOption.sceneCountForMinutes(30))
        assertEquals(4, VideoDurationOption.sceneCountForMinutes(1))
        assertTrue(VideoDurationOption.sceneCountForMinutes(15) in 10..30)
        assertEquals(StoryGenre.HORROR, StoryGenre.fromKey("HORROR"))
        assertEquals(MoodMusic.EMOTIONAL, BgmTrackOption.EMOTIONAL.fixedMood)
        assertEquals(MoodMusic.SPOOKY, BgmTrackOption.MYSTERY.fixedMood)
        assertEquals(VideoDurationOption.SEC_10, VideoDurationOption.fromKey("SEC_10"))
        assertEquals(10, VideoDurationOption.SEC_10.targetSeconds)
        assertEquals(VideoDurationOption.MIN_2, VideoDurationOption.fromKey("MIN_2"))
        assertEquals(120, VideoDurationOption.MIN_2.targetSeconds)
        assertEquals(VideoDurationOption.MIN_5, VideoDurationOption.fromKey("MIN_5"))
        assertEquals(10, VideoDurationOption.MIN_5.sceneCount)
        assertEquals(VideoAspectRatio.RATIO_9_16, VideoAspectRatio.fromKey("RATIO_9_16"))
        assertEquals(VideoResolution.RES_480P, VideoResolution.fromKey("RES_480P"))
        assertEquals(848, VideoAspectRatio.RATIO_16_9.width480)
        assertEquals(480, VideoAspectRatio.RATIO_16_9.height480)

        val dlg = DialogueLine(
            speaker = "Bolt",
            gender = "MALE",
            expression = "EXCITED",
            line = "Let's fly!",
            durationSec = 4
        )
        val map = dlg.toFirestoreMap()
        assertEquals("Bolt", map["speaker"])
        assertEquals("MALE", map["gender"])
        assertEquals(4, map["durationSec"])
    }

    @Test
    fun video_export_filename_and_size_formatting_work() {
        assertEquals("Cartoon_AI_Movie_001.mp4", VideoExportHelper.formatMovieFilename(1))
        assertEquals("Cartoon_AI_Movie_001.mp4", VideoExportHelper.formatMovieFilename(0))
        assertEquals("Cartoon_AI_Movie_012.mp4", VideoExportHelper.formatMovieFilename(12))
        assertEquals("Cartoon_AI_Movie_105.mp4", VideoExportHelper.formatMovieFilename(105))
        assertEquals("512 B", VideoExportHelper.formatByteSize(512L))
        assertEquals("2.0 MB", VideoExportHelper.formatByteSize(2L * 1024L * 1024L))
    }

    @Test
    fun local_studio_store_persists_projects_and_history_deletion() = runBlocking {
        val tempDir = Files.createTempDirectory("cartoon_ai_store_test").toFile()
        try {
            val store = LocalStudioStore(tempDir)
            val createdId = store.createProject(
                CartoonProject(
                    title = "Robo Fox Adventure",
                    prompt = "A little robot fox explores a floating sky island.",
                    style = CartoonStyle.CARTOON_3D.key,
                    status = ProjectStatus.GENERATING.key
                )
            ).getOrThrow()

            store.updateProjectProgress(
                projectId = createdId,
                status = ProjectStatus.COMPLETED.key,
                progressPercent = 100,
                completedScenes = 3
            ).getOrThrow()

            store.addHistoryEntry(
                GenerationHistoryEntry(
                    projectId = createdId,
                    projectTitle = "Robo Fox Adventure",
                    status = "COMPLETED"
                )
            ).getOrThrow()

            // Reload from disk to confirm persistence
            val reloaded = LocalStudioStore(tempDir)
            assertEquals(1, reloaded.projects.value.size)
            assertEquals("COMPLETED", reloaded.projects.value.first().status)
            assertEquals(1, reloaded.history.value.size)

            val histId = reloaded.history.value.first().id
            reloaded.deleteHistoryEntry(histId).getOrThrow()
            assertTrue(reloaded.history.value.isEmpty())

            reloaded.deleteProject(createdId).getOrThrow()
            assertTrue(reloaded.projects.value.isEmpty())
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
