package com.jiyi.power.app.language

import android.content.Context
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LanguageManager {
    private const val PREFS = "app_language"
    private const val KEY_TAG = "selected_tag"

    var selectedTag: String? = null
        private set

    @MainThread
    fun initialize(context: Context) {
        val systemLocales = context.resources.configuration.locales
        val savedTag = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TAG, null)
        val tag = LanguagePolicy.resolve(
            savedTag,
            systemLocales[0]?.language.orEmpty(), AppLanguages.items.map { it.tag },
        )
        selectedTag = tag
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_TAG, tag).apply()
        applyLocales(tag)
    }

    @MainThread
    fun select(context: Context, tag: String) {
        require(AppLanguages.items.any { it.tag == tag })
        if (selectedTag == tag) return
        selectedTag = tag
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_TAG, tag).apply()
        applyLocales(tag)
    }

    private fun applyLocales(tag: String) {
        val locales = LocaleListCompat.forLanguageTags(tag)
        if (AppCompatDelegate.getApplicationLocales() != locales) {
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }
}
