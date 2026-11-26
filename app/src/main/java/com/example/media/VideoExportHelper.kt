package com.example.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream

data class VideoDownloadResult(
    val fileName: String,
    val displayLocation: String,
    val bytesCopied: Long
)

object VideoExportHelper {

    private const val MOVIE_PREFIX = "Cartoon_AI_Movie_"
    private val MOVIE_REGEX = Regex("""^Cartoon_AI_Movie_(\d+)\.mp4$""", RegexOption.IGNORE_CASE)

    fun formatMovieFilename(sequenceNumber: Int): String {
        val safeNumber = sequenceNumber.coerceAtLeast(1)
        return "$MOVIE_PREFIX${"%03d".format(safeNumber)}.mp4"
    }

    fun nextMovieFilename(context: Context, fallbackNumber: Int = 1): String {
        var maxNumber = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                val projection = arrayOf(MediaStore.Video.Media.DISPLAY_NAME)
                val selection = "${MediaStore.Video.Media.DISPLAY_NAME} LIKE ?"
                val selectionArgs = arrayOf("${MOVIE_PREFIX}%.mp4")
                context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    null
                )?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                    while (nameIdx >= 0 && cursor.moveToNext()) {
                        val name = cursor.getString(nameIdx) ?: continue
                        val num = MOVIE_REGEX.matchEntire(name)?.groupValues?.getOrNull(1)?.toIntOrNull()
                        if (num != null && num > maxNumber) {
                            maxNumber = num
                        }
                    }
                }
            }
        }
        val nextNum = if (maxNumber > 0) maxNumber + 1 else fallbackNumber.coerceAtLeast(1)
        return formatMovieFilename(nextNum)
    }

    fun validateGeneratedMp4(sourceFile: File): File {
        require(sourceFile.exists() && sourceFile.isFile) {
            "Generated cartoon video file is not available yet."
        }
        require(sourceFile.extension.equals("mp4", ignoreCase = true)) {
            "Only valid MP4 video files can be downloaded."
        }
        require(sourceFile.length() > 1024L) {
            "Generated MP4 video is incomplete or empty."
        }
        return sourceFile
    }

    suspend fun saveMp4ToGallery(
        context: Context,
        sourceFile: File,
        fileName: String,
        onProgress: suspend (progress: Float, bytesCopied: Long, totalBytes: Long) -> Unit = { _, _, _ -> }
    ): Result<VideoDownloadResult> = withContext(Dispatchers.IO) {
        runCatching {
            val validFile = validateGeneratedMp4(sourceFile)
            val totalBytes = validFile.length()

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                throw IllegalStateException("Use the system Save As dialog on Android 9 and below.")
            }

            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/CartoonAI")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Failed to create MediaStore entry for $fileName.")

            try {
                val copied = resolver.openOutputStream(uri)?.use { out ->
                    validFile.inputStream().use { input ->
                        streamWithProgress(input, out, totalBytes, onProgress)
                    }
                } ?: throw IllegalStateException("Unable to open output stream for $fileName.")

                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)

                VideoDownloadResult(
                    fileName = fileName,
                    displayLocation = "Movies/CartoonAI/$fileName",
                    bytesCopied = copied
                )
            } catch (t: Throwable) {
                runCatching { resolver.delete(uri, null, null) }
                throw t
            }
        }
    }

    suspend fun copyMp4ToUri(
        context: Context,
        sourceFile: File,
        destinationUri: Uri,
        fileName: String,
        onProgress: suspend (progress: Float, bytesCopied: Long, totalBytes: Long) -> Unit = { _, _, _ -> }
    ): Result<VideoDownloadResult> = withContext(Dispatchers.IO) {
        runCatching {
            val validFile = validateGeneratedMp4(sourceFile)
            val totalBytes = validFile.length()
            val copied = context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                validFile.inputStream().use { input ->
                    streamWithProgress(input, out, totalBytes, onProgress)
                }
            } ?: throw IllegalStateException("Unable to open destination URI for download.")

            VideoDownloadResult(
                fileName = fileName,
                displayLocation = fileName,
                bytesCopied = copied
            )
        }
    }

    private suspend fun streamWithProgress(
        input: InputStream,
        output: OutputStream,
        totalBytes: Long,
        onProgress: suspend (progress: Float, bytesCopied: Long, totalBytes: Long) -> Unit
    ): Long {
        val buffer = ByteArray(16 * 1024)
        var bytesCopied = 0L
        var lastReportedAt = 0L
        onProgress(0f, 0L, totalBytes)
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            output.write(buffer, 0, read)
            bytesCopied += read
            val now = System.currentTimeMillis()
            if (now - lastReportedAt >= 40L || bytesCopied >= totalBytes) {
                lastReportedAt = now
                val frac = (bytesCopied.toFloat() / totalBytes.coerceAtLeast(1L)).coerceIn(0f, 1f)
                onProgress(frac, bytesCopied, totalBytes)
            }
        }
        output.flush()
        onProgress(1f, bytesCopied, totalBytes)
        return bytesCopied
    }

    fun formatByteSize(bytes: Long): String {
        if (bytes < 1024L) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024.0) return "%.0f KB".format(kb)
        val mb = kb / 1024.0
        return "%.1f MB".format(mb)
    }
}
