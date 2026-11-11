package app.planlist.local.prefs

import android.content.Context
import java.time.ZoneId
import java.util.TimeZone

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences(Locales.PREFS, Context.MODE_PRIVATE)

    fun localeTag(): String = prefs.getString(Locales.KEY, "system") ?: "system"

    fun timeZoneMode(): String = prefs.getString(KEY_TZ_MODE, MODE_SYSTEM) ?: MODE_SYSTEM

    fun setTimeZoneMode(mode: String) {
        prefs.edit().putString(KEY_TZ_MODE, mode).apply()
    }

    fun timeZoneId(): String? = prefs.getString(KEY_TZ_ID, null)

    fun setTimeZoneId(id: String) {
        prefs.edit().putString(KEY_TZ_ID, id).apply()
    }

    fun wasNotificationAsked(): Boolean = prefs.getBoolean(KEY_NOTIF_ASKED, false)

    fun setNotificationAsked() {
        prefs.edit().putBoolean(KEY_NOTIF_ASKED, true).apply()
    }

    fun zoneId(): ZoneId {
        if (timeZoneMode() != MODE_FIXED) return ZoneId.systemDefault()
        val id = timeZoneId() ?: return ZoneId.systemDefault()
        if (id !in availableIds()) return ZoneId.systemDefault()
        return runCatching { ZoneId.of(id) }.getOrElse { ZoneId.systemDefault() }
    }

    fun ensureFixedZoneId(): String {
        val ids = availableIds()
        val saved = timeZoneId()
        if (saved != null && saved in ids) return saved
        val systemId = ZoneId.systemDefault().id
        val pick = if (systemId in ids) systemId else ids.first()
        setTimeZoneId(pick)
        return pick
    }

    private fun availableIds(): Set<String> = TimeZone.getAvailableIDs().toSet()

    companion object {
        const val MODE_SYSTEM = "system"
        const val MODE_FIXED = "fixed"
        private const val KEY_TZ_MODE = "tz_mode"
        private const val KEY_TZ_ID = "tz_id"
        private const val KEY_NOTIF_ASKED = "notif_asked"
    }
}
