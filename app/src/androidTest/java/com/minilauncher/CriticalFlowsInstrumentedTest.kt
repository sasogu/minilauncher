package com.minilauncher

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CriticalFlowsInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun opens_and_closes_web_search_dialog_from_home() {
        val shortcutWebSearch = composeRule.activity.getString(R.string.shortcut_web_search)
        val webSearchHint = composeRule.activity.getString(R.string.web_search_hint)
        val cancel = composeRule.activity.getString(R.string.cancel)

        composeRule.onNodeWithContentDescription(shortcutWebSearch).performClick()
        composeRule.onNodeWithText(webSearchHint).assertIsDisplayed()
        composeRule.onNodeWithText(cancel).performClick()
    }
}
