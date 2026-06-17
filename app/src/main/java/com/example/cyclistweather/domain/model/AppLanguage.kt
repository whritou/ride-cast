package com.example.cyclistweather.domain.model

/**
 * User-selectable in-app language. [tag] is a BCP-47 language tag used to build the localized
 * resource configuration; `null` means "follow the system language".
 */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    FRENCH("fr"),
    GERMAN("de"),
    SPANISH("es")
}
