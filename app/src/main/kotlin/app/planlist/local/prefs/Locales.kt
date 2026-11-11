package app.planlist.local.prefs

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object Locales {
    const val PREFS = "planlist_settings"
    const val KEY = "locale"

    fun tag(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "system") ?: "system"

    fun setTag(context: Context, tag: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, tag)
            .apply()
    }

    fun localeOf(tag: String): Locale? = when (tag) {
        "zh" -> Locale.SIMPLIFIED_CHINESE
        "en" -> Locale.ENGLISH
        else -> null
    }

    fun wrap(base: Context): Context {
        val locale = localeOf(tag(base)) ?: return base
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }

    fun apply(app: Application) {
        val locale = localeOf(tag(app)) ?: return
        val config = Configuration(app.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        @Suppress("DEPRECATION")
        app.resources.updateConfiguration(config, app.resources.displayMetrics)
    }
}
