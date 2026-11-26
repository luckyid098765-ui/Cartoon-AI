package com.example

import com.example.data.model.CartoonLanguage
import com.example.data.model.CartoonStyle
import com.example.data.model.DialogueLine
import com.example.data.model.VideoAspectRatio
import com.example.data.model.VideoDurationOption
import com.example.media.VideoExportHelper
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun cartoon_enums_and_dialogue_serialization_work() {
        assertEquals(CartoonStyle.CARTOON_3D, CartoonStyle.fromKey("UNKNOWN_STYLE"))
        assertEquals(CartoonLanguage.HINDI, CartoonLanguage.fromKey("HINDI"))
        assertEquals(CartoonLanguage.BENGALI, CartoonLanguage.fromKey("BENGALI"))
        assertEquals(VideoDurationOption.MIN_5, VideoDurationOption.fromKey("MIN_5"))
        assertEquals(10, VideoDurationOption.MIN_5.sceneCount)
        assertEquals(VideoAspectRatio.RATIO_9_16, VideoAspectRatio.fromKey("RATIO_9_16"))

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
}
