package org.ahmetyusufoguz.simplevolleyballstats.domain.model

enum class AppLanguage(val code: String, val displayName: String) {
    TURKISH("tr", "Türkçe"),
    ENGLISH("en", "English");

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.find { it.code == code } ?: TURKISH
        }
    }
}


data class AppSettings(
    val theme: org.ahmetyusufoguz.simplevolleyballstats.presentation.theme.AppTheme =
        org.ahmetyusufoguz.simplevolleyballstats.presentation.theme.AppTheme.LIGHT,
    val language: AppLanguage = AppLanguage.TURKISH
)