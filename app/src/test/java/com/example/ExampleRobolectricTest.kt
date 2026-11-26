package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CartoonLanguage
import com.example.data.model.CartoonStyle
import com.example.data.model.DialogueLine
import com.example.data.model.VideoDurationOption
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Cartoon AI", appName)
    }

    @Test
    fun `cartoon enum parsing and dialogue map serialization`() {
        assertEquals(CartoonStyle.CARTOON_3D, CartoonStyle.fromKey("UNKNOWN_STYLE"))
        assertEquals(CartoonLanguage.HINDI, CartoonLanguage.fromKey("HINDI"))
        assertEquals(VideoDurationOption.MIN_5, VideoDurationOption.fromKey("MIN_5"))

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
}
