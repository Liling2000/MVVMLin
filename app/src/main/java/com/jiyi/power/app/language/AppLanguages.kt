package com.jiyi.power.app.language

import androidx.annotation.StringRes
import com.jiyi.power.R

data class LanguageOption(val tag: String, @StringRes val label: Int)

object AppLanguages {
    // Add a resource directory and one item here to support another language.
    val items = listOf(
        LanguageOption("zh", R.string.language_chinese),
        LanguageOption("en", R.string.language_english),
    )
}
