package com.example.genritv

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class HomeScreenSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun home_screen_renders_primary_branding() {
        composeRule.onNodeWithText("GENRI TV").assertIsDisplayed()
        composeRule.onNodeWithText("Nikmati tayangan TV, Film, dan Series terbaik dalam satu aplikasi.")
            .assertIsDisplayed()
    }
}
