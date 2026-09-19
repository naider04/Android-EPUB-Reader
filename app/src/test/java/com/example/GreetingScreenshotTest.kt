package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.getJustifiedHorizontal
import com.example.ui.components.getJustifiedOffsetForPosition
import com.example.ui.components.getSelectionStartRect
import com.example.ui.components.getSelectionEndRect
import com.example.ui.theme.EpubReaderTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun reader_app_screenshot() {
        composeTestRule.setContent {
            EpubReaderTheme {
                EpubReaderApp(modifier = Modifier.fillMaxSize())
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/reader_app.png")
    }

    @Test
    fun test_text_layout_selection_coordinates() {
        var layoutResultJustify: TextLayoutResult? = null

        val sampleText = "Sun Tzu said: The art of war is of vital importance to the State. It is a matter of life and death, a road either to safety or to ruin."

        composeTestRule.setContent {
            Column(
                modifier = Modifier.width(360.dp)
            ) {
                Text(
                    text = sampleText,
                    style = TextStyle(
                        fontSize = 16.sp,
                        textAlign = TextAlign.Justify
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    onTextLayout = { layoutResultJustify = it }
                )
            }
        }

        composeTestRule.waitForIdle()

        val lJustify = layoutResultJustify!!

        val word = "importance"
        val wordOffset = sampleText.indexOf(word)

        val unjustifiedX = lJustify.getHorizontalPosition(wordOffset, true)
        val justifiedX = getJustifiedHorizontal(lJustify, sampleText, wordOffset, isJustified = true)

        // For justified text, the word should be shifted to the right of its unjustified position
        assertTrue("Justified position ($justifiedX) must be >= unjustified position ($unjustifiedX)", justifiedX >= unjustifiedX)

        // Tapping visually inside 'importance'
        val userTap = Offset(justifiedX + 10f, lJustify.getLineTop(lJustify.getLineForOffset(wordOffset)) + 10f)

        // Without correction, getOffsetForPosition resolves to an offset shifted to the right (past 'importance')
        val uncorrectedOffset = lJustify.getOffsetForPosition(userTap)
        assertTrue("Uncorrected offset ($uncorrectedOffset) is shifted to the right of wordOffset ($wordOffset)", uncorrectedOffset > wordOffset)

        // With our fix, getJustifiedOffsetForPosition resolves accurately to 'importance'
        val correctedOffset = getJustifiedOffsetForPosition(lJustify, sampleText, userTap, isJustified = true)
        assertTrue("Corrected offset ($correctedOffset) must be within word bounds [$wordOffset, ${wordOffset + word.length}]",
            correctedOffset in wordOffset until (wordOffset + word.length))

        // Check selection handles
        val startRect = getSelectionStartRect(lJustify, sampleText, wordOffset, isJustified = true)
        val endRect = getSelectionEndRect(lJustify, sampleText, wordOffset + word.length, isJustified = true)

        assertEquals("Start handle x should match justified start position", justifiedX, startRect.left, 0.5f)
        val expectedEndJustifiedX = getJustifiedHorizontal(lJustify, sampleText, wordOffset + word.length, isJustified = true)
        assertEquals("End handle x should match justified end position", expectedEndJustifiedX, endRect.left, 0.5f)
    }
}


