package app.planlist.local.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.planlist.core.SystemZones
import app.planlist.local.MainActivity
import app.planlist.local.R
import app.planlist.local.prefs.SettingsStore
import app.planlist.local.reminder.Permissions
import app.planlist.local.ui.theme.Cinnabar
import app.planlist.local.ui.theme.Ink
import app.planlist.local.ui.theme.InkSoft
import app.planlist.local.ui.theme.Line
import app.planlist.local.ui.theme.PaperCard
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone

@Composable
fun rememberResumeTick(): Int {
    val lifecycleOwner = LocalLifecycleOwner.current
    var tick by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) tick += 1
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return tick
}

@Composable
fun SettingsScreen(
    settings: SettingsStore,
    nowMillis: Long,
    onLanguage: (String) -> Unit,
    onChanged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val tick = rememberResumeTick()
    val zone = remember(tick, settings.timeZoneMode(), settings.timeZoneId()) { settings.zoneId() }
    val ids = remember { TimeZone.getAvailableIDs().toList() }
    val idSet = remember(ids) { ids.toSet() }
    val regions = remember(ids) { SystemZones.regions(ids) }
    val fixed = settings.timeZoneMode() == SettingsStore.MODE_FIXED
    val selectedId = settings.timeZoneId()?.takeIf { it in idSet }
        ?: ZoneId.systemDefault().id.takeIf { it in idSet }
        ?: ids.first()
    val region = SystemZones.regionKey(selectedId)
    val zones = SystemZones.zones(ids, region)
    val languageTags = listOf("system", "zh", "en")
    val languageLabels = listOf(
        stringResource(R.string.locale_system),
        stringResource(R.string.locale_zh),
        stringResource(R.string.locale_en),
    )
    val currentTag = settings.localeTag().takeIf { it in languageTags } ?: "system"

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .padding(bottom = 24.dp),
    ) {
        Text(stringResource(R.string.settings), style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(18.dp))
        SectionTitle(stringResource(R.string.language))
        MenuButton(
            label = stringResource(R.string.language),
            value = languageLabels[languageTags.indexOf(currentTag)],
            options = languageLabels,
            onSelectIndex = { onLanguage(languageTags[it]) },
        )
        Spacer(Modifier.height(22.dp))
        SectionTitle(stringResource(R.string.time_section))
        Text(
            formatClock(nowMillis, zone, locale),
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            stringResource(R.string.system_time) + " · " + zone.id,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Text(
            stringResource(R.string.clock_body),
            style = MaterialTheme.typography.bodyMedium,
            color = InkSoft,
        )
        Spacer(Modifier.height(10.dp))
        MenuButton(
            label = stringResource(R.string.timezone),
            value = if (fixed) stringResource(R.string.tz_fixed) else stringResource(R.string.tz_system),
            options = listOf(stringResource(R.string.tz_system), stringResource(R.string.tz_fixed)),
            onSelectIndex = { index ->
                if (index == 0) {
                    settings.setTimeZoneMode(SettingsStore.MODE_SYSTEM)
                } else {
                    settings.setTimeZoneMode(SettingsStore.MODE_FIXED)
                    settings.setTimeZoneId(selectedId)
                }
                onChanged()
            },
        )
        if (fixed) {
            Spacer(Modifier.height(8.dp))
            MenuButton(
                label = stringResource(R.string.tz_region),
                value = if (region == SystemZones.OTHER) stringResource(R.string.tz_other) else region,
                options = regions.map { if (it == SystemZones.OTHER) stringResource(R.string.tz_other) else it },
                onSelectIndex = { index ->
                    val nextRegion = regions[index]
                    val pick = SystemZones.zones(ids, nextRegion).first()
                    settings.setTimeZoneId(pick)
                    onChanged()
                },
            )
            Spacer(Modifier.height(8.dp))
            MenuButton(
                label = stringResource(R.string.tz_zone),
                value = zoneLabel(selectedId, locale),
                options = zones.map { zoneLabel(it, locale) },
                onSelectIndex = { index ->
                    settings.setTimeZoneId(zones[index])
                    onChanged()
                },
            )
        }
        Spacer(Modifier.height(22.dp))
        SectionTitle(stringResource(R.string.permission_section))
        PermissionCard(
            title = stringResource(R.string.permission_time),
            body = if (tick >= 0 && Permissions.exactAlarmGranted(context) && android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
                stringResource(R.string.permission_time_ready)
            } else {
                stringResource(R.string.permission_time_body)
            },
            granted = Permissions.exactAlarmGranted(context),
            action = stringResource(R.string.permission_open),
            onAction = { Permissions.openExactAlarmSettings(context) },
            showAction = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S,
        )
        PermissionCard(
            title = stringResource(R.string.permission_notifications),
            body = stringResource(R.string.permission_notifications_body),
            granted = Permissions.notificationsGranted(context),
            action = stringResource(R.string.permission_open),
            onAction = { (context as? MainActivity)?.requestNotifications() },
            showAction = android.os.Build.VERSION.SDK_INT >= 33 && !Permissions.notificationsGranted(context),
        )
        PermissionCard(
            title = stringResource(R.string.permission_fullscreen),
            body = stringResource(R.string.permission_fullscreen_body),
            granted = Permissions.fullScreenGranted(context),
            action = stringResource(R.string.permission_open),
            onAction = { Permissions.openFullScreenSettings(context) },
            showAction = android.os.Build.VERSION.SDK_INT >= 34 && !Permissions.fullScreenGranted(context),
        )
        TextButton(onClick = { Permissions.openDateSettings(context) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.open_system_clock), color = Cinnabar)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = Cinnabar,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun PermissionCard(
    title: String,
    body: String,
    granted: Boolean,
    action: String,
    onAction: () -> Unit,
    showAction: Boolean,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .background(PaperCard, RoundedCornerShape(12.dp))
            .border(1.dp, Line, RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        Row {
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(if (granted) R.string.permission_granted else R.string.permission_missing),
                color = if (granted) app.planlist.local.ui.theme.Pine else Cinnabar,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Text(body, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        if (showAction) {
            TextButton(onClick = onAction, modifier = Modifier.padding(top = 2.dp)) {
                Text(action, color = Cinnabar)
            }
        }
    }
}

private fun zoneLabel(id: String, locale: Locale): String {
    val name = TimeZone.getTimeZone(id).getDisplayName(false, TimeZone.LONG, locale)
    return if (name.isBlank() || name.equals(id, ignoreCase = true)) id else "$id · $name"
}
