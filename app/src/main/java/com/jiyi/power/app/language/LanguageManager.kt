package com.jiyi.power.app.language

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LanguageManager {
    private const val PREFS = "app_language"
    private const val KEY_TAG = "selected_tag"

    private var fallbackTag: String? = null

    /** Keep the newest in-app choice authoritative while the picker defers the framework change. */
    val selectedTag: String?
        get() = fallbackTag
            ?: AppCompatDelegate.getApplicationLocales()[0]?.language?.takeIf(::isSupported)

    @MainThread
    fun initialize(context: Context) {
        val systemLocales = context.resources.configuration.locales
        val savedTag = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TAG, null)
        val platformTag = AppCompatDelegate.getApplicationLocales()[0]?.language
            ?.takeIf(::isSupported)
        val tag = platformTag ?: LanguagePolicy.resolve(
            savedTag,
            systemLocales[0]?.language.orEmpty(),
            AppLanguages.items.map { it.tag },
        )
        fallbackTag = tag
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_TAG, tag).commit()
        applyLocales(tag)
    }

    @MainThread
    fun select(context: Context, tag: String) {
        require(AppLanguages.items.any { it.tag == tag })
        if (selectedTag == tag) return
        fallbackTag = tag
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_TAG, tag).commit()
        applyLocales(tag)
    }

    /** Stages a picker choice without triggering an app-wide configuration dispatch yet. */
    @MainThread
    fun selectWhilePickerIsOpen(tag: String) {
        require(AppLanguages.items.any { it.tag == tag })
        fallbackTag = tag
    }

    @MainThread
    fun applySelectedLocale(context: Context) {
        fallbackTag?.let { tag ->
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_TAG, tag).commit()
            applyLocales(tag)
        }
    }

    fun localizedContext(context: Context, tag: String): Context {
        require(AppLanguages.items.any { it.tag == tag })
        val configuration = Configuration(context.resources.configuration).apply {
            setLocales(android.os.LocaleList.forLanguageTags(tag))
        }
        return context.createConfigurationContext(configuration)
    }

    private fun applyLocales(tag: String) {
        val locales = LocaleListCompat.forLanguageTags(tag)
        if (AppCompatDelegate.getApplicationLocales() != locales) {
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }

    private fun isSupported(tag: String): Boolean = AppLanguages.items.any { it.tag == tag }
}
