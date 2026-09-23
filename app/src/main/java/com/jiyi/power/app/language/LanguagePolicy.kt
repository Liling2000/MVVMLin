package com.jiyi.power.app.language

object LanguagePolicy {
    fun resolve(saved: String?, systemLanguage: String, supported: List<String>): String {
        require("zh" in supported && "en" in supported)
        if (saved in supported) return requireNotNull(saved)
        return if (systemLanguage.equals("zh", ignoreCase = true)) "zh" else "en"
    }
}
