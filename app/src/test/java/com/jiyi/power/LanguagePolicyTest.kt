package com.jiyi.power

import com.jiyi.power.app.language.LanguagePolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class LanguagePolicyTest {
    private val languages = listOf("zh", "en")

    @Test fun firstLaunchUsesChineseForChineseSystem() {
        assertEquals("zh", LanguagePolicy.resolve(null, "zh", languages))
    }

    @Test fun firstLaunchUsesEnglishForOtherSystems() {
        listOf("en", "ja", "fr", "ar", "").forEach {
            assertEquals("en", LanguagePolicy.resolve(null, it, languages))
        }
    }

    @Test fun savedChoiceWinsOverSystemChanges() {
        assertEquals("en", LanguagePolicy.resolve("en", "zh", languages))
        assertEquals("zh", LanguagePolicy.resolve("zh", "en", languages))
    }

    @Test fun removedLanguageFallsBackButNewConfiguredLanguageIsPreserved() {
        assertEquals("en", LanguagePolicy.resolve("ja", "ja", languages))
        assertEquals("ja", LanguagePolicy.resolve("ja", "zh", languages + "ja"))
    }
}
