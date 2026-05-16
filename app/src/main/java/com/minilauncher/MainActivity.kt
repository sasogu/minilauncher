package es.sasogu.minilauncher

import android.Manifest
import android.content.BroadcastReceiver
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Process
import android.provider.MediaStore
import android.provider.AlarmClock
import android.provider.Settings
import android.widget.Toast
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import androidx.annotation.StringRes

/**
 * Devuelve una representacion compacta de la iluminacion lunar para una fecha dada.
 * Algoritmo local sin red: usa luna nueva de referencia (6 ene 2000) y periodo sinodico.
 * Muestra el icono de fase con orientacion correcta (creciente/menguante).
 */
internal fun lunarPhaseText(
    date: Date = Date(),
    showPercentage: Boolean,
): String {
    val knownNewMoonMs = 947_182_440_000L // 6 ene 2000 18:14 UTC en milisegundos
    val synodicMs = 29.53059 * 24 * 60 * 60 * 1000
    val elapsed = (date.time - knownNewMoonMs).toDouble()
    val phase = ((elapsed % synodicMs) / synodicMs + 1.0) % 1.0
    val illumination = ((1 - cos(2 * PI * phase)) / 2.0 * 100).roundToInt().coerceIn(0, 100)
    val moonEmoji = when {
        illumination == 0 -> "\uD83C\uDF11" // 🌑 Luna nueva
        illumination == 100 -> "\uD83C\uDF15" // 🌕 Luna llena
        else -> when ((phase * 8).toInt()) {
            0 -> "\uD83C\uDF11" // 🌑 Luna nueva
            1 -> "\uD83C\uDF12" // 🌒 Creciente
            2 -> "\uD83C\uDF13" // 🌓 Cuarto creciente
            3 -> "\uD83C\uDF14" // 🌔 Gibosa creciente
            4 -> "\uD83C\uDF15" // 🌕 Luna llena
            5 -> "\uD83C\uDF16" // 🌖 Gibosa menguante
            6 -> "\uD83C\uDF17" // 🌗 Cuarto menguante
            else -> "\uD83C\uDF18" // 🌘 Menguante
        }
    }
    return if (showPercentage) {
        "$moonEmoji $illumination%"
    } else {
        moonEmoji
    }
}

private data class BatteryStatus(
    val level: Int,
    val isCharging: Boolean,
)

class MainActivity : ComponentActivity() {
    private val launcherViewModel: LauncherViewModel by viewModels()
    private val homeReturnSignal = mutableStateOf(0)
    private var skipReminderResetOnNextResume = false
    private var notificationListenerGranted by mutableStateOf(false)
    private var storagePermissionGranted by mutableStateOf(false)
    private val exportBackupLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri == null) return@registerForActivityResult
            lifecycleScope.launch {
                runCatching {
                    BackupManager.exportToUri(this@MainActivity, uri)
                }.onSuccess {
                    toast(R.string.backup_export_success)
                }.onFailure {
                    toast(R.string.backup_export_error)
                }
            }
        }
    private val requestStoragePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            storagePermissionGranted = granted
        }
    private val importBackupLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@registerForActivityResult
            lifecycleScope.launch {
                runCatching {
                    BackupManager.importFromUri(this@MainActivity, uri)
                }.onSuccess {
                    launcherViewModel.dispatch(LauncherUiAction.ReloadPreferences)
                    launcherViewModel.dispatch(LauncherUiAction.RefreshApps)
                    toast(R.string.backup_import_success)
                    recreate()
                }.onFailure {
                    toast(R.string.backup_import_error)
                }
            }
        }

    override fun attachBaseContext(newBase: Context) {
        val language = LanguageStore(newBase.launcherDataStore).loadLanguageBlocking()
        super.attachBaseContext(newBase.withAppLanguage(language))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        updateSystemBars(launcherViewModel.uiState.value.selectedThemeMode)
        enterImmersiveMode()
        handleTimeoutIntent(intent)
        checkFinancesPermissions()

        setContent {
            val state by launcherViewModel.uiState.collectAsStateWithLifecycle()
            MinimalLauncherTheme(themeMode = state.selectedThemeMode) {
                LauncherApp(
                    state = state,
                    homeReturnSignal = homeReturnSignal.value,
                    onQueryChange = ::onQueryChange,
                    onHomeQueryChange = ::onHomeQueryChange,
                    onAppClick = ::promptAppLaunch,
                    onClockClick = ::openAlarms,
                    onToggleFavorite = ::toggleFavorite,
                    onHideApp = ::hideApp,
                    onSaveAppTags = ::saveAppTags,
                    onUndoHideApp = ::restoreHiddenApp,
                    onHiddenAppNoticeConsumed = ::onHiddenAppNoticeConsumed,
                    onPromoteFavorite = ::promoteFavorite,
                    onDismissHomeReorderHint = ::dismissHomeReorderHint,
                    onPhoneClick = ::openPhone,
                    onCameraClick = ::openCamera,
                    onLaunchDismiss = ::dismissLaunchPrompt,
                    onLaunchConfirm = ::confirmLaunch,
                    onTimeoutDismiss = ::dismissTimeoutNotice,
                    onTimeoutAddFiveMinutes = ::extendBlockedAppByFiveMinutes,
                    onLanguageChange = ::onLanguageChange,
                    onThemeChange = ::onThemeChange,
                    onClearReminder = ::clearActiveReminderFromSettings,
                    onExportBackup = ::exportBackup,
                    onImportBackup = ::importBackup,
                    onUsagePromptToggle = ::onUsagePromptToggle,
                    onMoonIlluminationPercentageToggle = ::onMoonIlluminationPercentageToggle,
                    onHomeWeekdayToggle = ::onHomeWeekdayToggle,
                    onHomeDateToggle = ::onHomeDateToggle,
                    onUse24HourTimeToggle = ::onUse24HourTimeToggle,
                    onRestoreHiddenApp = ::restoreHiddenApp,
                    onRestoreAllHiddenApps = ::restoreAllHiddenApps,
                    onOpenWebSearch = ::onOpenWebSearch,
                    onOpenAppInfo = ::openAppInfo,
                    onWebSearch = ::openWebSearch,
                    onWebSearchQueryChange = ::onWebSearchQueryChange,
                    onWebSearchDismiss = ::dismissWebSearch,
                    onTransientMessageConsumed = ::onTransientMessageConsumed,
                    notificationListenerGranted = notificationListenerGranted,
                    storagePermissionGranted = storagePermissionGranted,
                    onOpenNotificationSettings = ::openNotificationListenerSettings,
                    onRequestStoragePermission = ::requestStoragePermission,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (isHomeIntent(intent)) {
            homeReturnSignal.value += 1
        }
        handleTimeoutIntent(intent)
    }

    private fun isHomeIntent(intent: Intent?): Boolean {
        if (intent?.action != Intent.ACTION_MAIN) return false
        return intent.hasCategory(Intent.CATEGORY_HOME)
    }

    override fun onResume() {
        super.onResume()
        if (skipReminderResetOnNextResume) {
            skipReminderResetOnNextResume = false
        } else {
            ReminderScheduler.resetActiveReminder(this)
            launcherViewModel.dispatch(LauncherUiAction.TimeoutNoticeChanged(null))
        }
        enterImmersiveMode()
        launcherViewModel.dispatch(LauncherUiAction.RefreshApps)
        checkFinancesPermissions()
    }

    private fun enterImmersiveMode() {
        window.insetsController?.let { controller ->
            controller.hide(WindowInsets.Type.statusBars())
            controller.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun onQueryChange(query: String) {
        launcherViewModel.dispatch(LauncherUiAction.QueryChanged(query))
    }

    private fun onHomeQueryChange(query: String) {
        launcherViewModel.dispatch(LauncherUiAction.HomeQueryChanged(query))
    }

    private fun onLanguageChange(language: AppLanguage) {
        val currentLanguage = launcherViewModel.uiState.value.selectedLanguage
        if (currentLanguage == language) return

        launcherViewModel.dispatch(LauncherUiAction.LanguageChanged(language))
        recreate()
    }

    private fun onOpenWebSearch() {
        launcherViewModel.dispatch(LauncherUiAction.OpenWebSearch)
    }

    private fun onWebSearchQueryChange(query: String) {
        launcherViewModel.dispatch(LauncherUiAction.WebSearchQueryChanged(query))
    }

    private fun openWebSearch(query: String) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) {
            dismissWebSearch()
            return
        }

        launcherViewModel.dispatch(LauncherUiAction.WebSearchQueryChanged(normalizedQuery))

        val uri = Uri.parse("https://duckduckgo.com/?q=${Uri.encode(normalizedQuery)}")
        val browserIntent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (browserIntent.resolveActivity(packageManager) != null) {
            startActivity(browserIntent)
        } else {
            showTransientMessage(R.string.error_no_browser)
        }
        dismissWebSearch()
    }

    private fun openAppInfo(app: LaunchableApp) {
        val appInfoIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${app.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (appInfoIntent.resolveActivity(packageManager) != null) {
            startActivity(appInfoIntent)
        } else {
            showTransientMessage(R.string.error_no_app_info)
        }
    }

    private fun dismissWebSearch() {
        launcherViewModel.dispatch(LauncherUiAction.DismissWebSearch)
    }

    private fun onUsagePromptToggle(enabled: Boolean) {
        launcherViewModel.dispatch(LauncherUiAction.UsagePromptToggled(enabled))
    }

    private fun onMoonIlluminationPercentageToggle(visible: Boolean) {
        launcherViewModel.dispatch(LauncherUiAction.MoonIlluminationPercentageToggled(visible))
    }

    private fun onHomeWeekdayToggle(visible: Boolean) {
        launcherViewModel.dispatch(LauncherUiAction.HomeWeekdayToggled(visible))
    }

    private fun onHomeDateToggle(visible: Boolean) {
        launcherViewModel.dispatch(LauncherUiAction.HomeDateToggled(visible))
    }

    private fun onUse24HourTimeToggle(enabled: Boolean) {
        launcherViewModel.dispatch(LauncherUiAction.HomeUse24HourTimeToggled(enabled))
    }

    private fun exportBackup() {
        exportBackupLauncher.launch("minilauncher-backup.json")
    }

    private fun importBackup() {
        importBackupLauncher.launch(arrayOf("application/json", "text/plain"))
    }

    private fun onThemeChange(themeMode: ThemeMode) {
        val currentTheme = launcherViewModel.uiState.value.selectedThemeMode
        if (currentTheme == themeMode) return

        launcherViewModel.dispatch(LauncherUiAction.ThemeChanged(themeMode))
        updateSystemBars(themeMode)
    }

    private fun updateSystemBars(themeMode: ThemeMode) {
        val navigationBarScrim = when (themeMode) {
            ThemeMode.DARK -> Color.Black.toArgb()
            ThemeMode.LIGHT -> Color(0xFFF3F4F6).toArgb()
        }
        val statusBarStyle = when (themeMode) {
            ThemeMode.DARK -> SystemBarStyle.dark(Color.Transparent.toArgb())
            ThemeMode.LIGHT -> SystemBarStyle.light(Color.Transparent.toArgb(), navigationBarScrim)
        }
        val navigationBarStyle = when (themeMode) {
            ThemeMode.DARK -> SystemBarStyle.dark(navigationBarScrim)
            ThemeMode.LIGHT -> SystemBarStyle.light(navigationBarScrim, navigationBarScrim)
        }
        enableEdgeToEdge(statusBarStyle, navigationBarStyle)
    }

    private fun toggleFavorite(app: LaunchableApp) {
        launcherViewModel.dispatch(LauncherUiAction.ToggleFavorite(app))
    }

    private fun promoteFavorite(app: LaunchableApp) {
        launcherViewModel.dispatch(LauncherUiAction.PromoteFavorite(app))
    }

    private fun dismissHomeReorderHint() {
        launcherViewModel.dispatch(LauncherUiAction.DismissHomeReorderHint)
    }

    private fun hideApp(app: LaunchableApp) {
        launcherViewModel.dispatch(LauncherUiAction.HideApp(app))
    }

    private fun saveAppTags(app: LaunchableApp, tags: List<String>) {
        launcherViewModel.dispatch(LauncherUiAction.SaveAppTags(app, tags))
    }

    private fun restoreHiddenApp(app: LaunchableApp) {
        launcherViewModel.dispatch(LauncherUiAction.RestoreHiddenApp(app))
    }

    private fun restoreAllHiddenApps() {
        launcherViewModel.dispatch(LauncherUiAction.RestoreAllHiddenApps)
    }

    private fun onHiddenAppNoticeConsumed() {
        launcherViewModel.dispatch(LauncherUiAction.HiddenAppNoticeConsumed)
    }

    private fun onTransientMessageConsumed() {
        launcherViewModel.dispatch(LauncherUiAction.TransientMessageChanged(null))
    }

    private fun openApp(app: LaunchableApp) {
        if (app.shortcutId != null) {
            runCatching {
                getSystemService(LauncherApps::class.java)
                    .startShortcut(app.packageName, app.shortcutId, null, null, Process.myUserHandle())
            }
            return
        }
        openApp(app.packageName)
    }

    private fun openApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun promptAppLaunch(app: LaunchableApp) {
        val blocked = UsageBlockStore.loadBlocked(this)
        if (blocked?.packageName == app.packageName) {
            launcherViewModel.dispatch(
                LauncherUiAction.TimeoutNoticeChanged(
                    TimeoutNotice(
                        appLabel = blocked.appLabel,
                        minutes = blocked.minutes,
                        packageName = blocked.packageName,
                    ),
                ),
            )
            return
        }
        if (!launcherViewModel.uiState.value.usagePromptEnabled) {
            openApp(app)
            return
        }
        if (UnlimitedDayStore.isUnlimitedToday(this, app.packageName)) {
            openApp(app)
            return
        }
        launcherViewModel.dispatch(LauncherUiAction.PendingLaunchChanged(app))
    }

    private fun dismissLaunchPrompt() {
        launcherViewModel.dispatch(LauncherUiAction.PendingLaunchChanged(null))
    }

    private fun dismissTimeoutNotice() {
        ReminderScheduler.resetActiveReminder(this)
        launcherViewModel.dispatch(LauncherUiAction.TimeoutNoticeChanged(null))
    }

    private fun clearActiveReminderFromSettings() {
        ReminderScheduler.resetActiveReminder(this)
        UsageBlockStore.clear(this)
        launcherViewModel.dispatch(LauncherUiAction.TimeoutNoticeChanged(null))
    }

    private fun extendBlockedAppByFiveMinutes() {
        val blocked = UsageBlockStore.loadBlocked(this) ?: return
        ensureNotificationPermission()
        UsageBlockStore.clear(this)
        ReminderScheduler.scheduleReminder(
            context = this,
            packageName = blocked.packageName,
            appLabel = blocked.appLabel,
            delayMinutes = 5,
        )
        launcherViewModel.dispatch(LauncherUiAction.TimeoutNoticeChanged(null))
        openApp(blocked.packageName)
    }

    private fun handleTimeoutIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(ReminderReceiver.EXTRA_TIMEOUT_REACHED, false) != true) return
        skipReminderResetOnNextResume = true

        val appLabel = intent.getStringExtra(ReminderReceiver.EXTRA_APP_LABEL).orEmpty().ifBlank {
            getString(R.string.reminder_default_app)
        }
        val minutes = intent.getIntExtra(ReminderReceiver.EXTRA_MINUTES, 0)
        launcherViewModel.dispatch(
            LauncherUiAction.TimeoutNoticeChanged(
                TimeoutNotice(
                    appLabel = appLabel,
                    minutes = minutes,
                    packageName = intent.getStringExtra(ReminderReceiver.EXTRA_PACKAGE_NAME),
                ),
            ),
        )

        intent.removeExtra(ReminderReceiver.EXTRA_TIMEOUT_REACHED)
    }

    private fun confirmLaunch(app: LaunchableApp, durationMinutes: Int?) {
        if (durationMinutes != null) {
            ensureNotificationPermission()
            ReminderScheduler.scheduleReminder(
                context = this,
                packageName = app.packageName,
                appLabel = app.label,
                delayMinutes = durationMinutes,
            )
        } else {
            ReminderScheduler.resetActiveReminder(this)
            UnlimitedDayStore.markUnlimitedToday(this, app.packageName)
        }
        dismissLaunchPrompt()
        openApp(app)
    }

    private fun checkFinancesPermissions() {
        notificationListenerGranted = NotificationManagerCompat
            .getEnabledListenerPackages(this).contains(packageName)
        storagePermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    private fun openNotificationListenerSettings() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName"),
                )
            )
        } else {
            requestStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
    }

    private fun openAlarms() {
        val opened = openFirstAvailableIntent(
            Intent(AlarmClock.ACTION_SHOW_ALARMS),
            Intent(AlarmClock.ACTION_SET_ALARM),
        ) || launchAppByPackageKeyword("deskclock", "alarmclock", "clockpackage", "clock", "reloj", "alarm")

        if (!opened) {
            showTransientMessage(R.string.error_no_clock_app)
        }
    }

    private fun openFirstAvailableIntent(vararg intents: Intent): Boolean {
        val resolved = intents.firstOrNull { it.resolveActivity(packageManager) != null } ?: return false
        return runCatching {
            startActivity(resolved.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrDefault(false)
    }

    private fun launchAppByPackageKeyword(vararg keywords: String): Boolean {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = packageManager.queryIntentActivities(launcherIntent, 0)
        val match = apps.firstOrNull { resolveInfo ->
            val pkg = resolveInfo.activityInfo.packageName.lowercase()
            keywords.any { pkg.contains(it) }
        } ?: return false
        val launchIntent = packageManager.getLaunchIntentForPackage(match.activityInfo.packageName) ?: return false
        return runCatching {
            startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrDefault(false)
    }

    private fun openPhone() {
        val opened = openFirstAvailableIntent(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_VIEW).apply { type = "vnd.android-dir/mms-sms" },
        ) || launchAppByPackageKeyword("dialer", "phone", "telefon", "contacts", "contactos")

        if (!opened) {
            showTransientMessage(R.string.error_no_phone_app)
        }
    }

    private fun openCamera() {
        val opened = openFirstAvailableIntent(
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
            Intent("android.media.action.STILL_IMAGE_CAMERA"),
            Intent(MediaStore.ACTION_IMAGE_CAPTURE),
        ) || launchAppByPackageKeyword("camera", "camara", "cam")

        if (!opened) {
            showTransientMessage(R.string.error_no_camera_app)
        }
    }

    private fun showTransientMessage(@StringRes messageRes: Int) {
        launcherViewModel.dispatch(LauncherUiAction.TransientMessageChanged(getString(messageRes)))
    }

    private fun toast(@StringRes messageRes: Int) {
        Toast.makeText(this, getString(messageRes), Toast.LENGTH_SHORT).show()
    }

}

@Composable
private fun LauncherApp(
    state: LauncherUiState,
    homeReturnSignal: Int,
    onQueryChange: (String) -> Unit,
    onHomeQueryChange: (String) -> Unit,
    onAppClick: (LaunchableApp) -> Unit,
    onClockClick: () -> Unit,
    onToggleFavorite: (LaunchableApp) -> Unit,
    onHideApp: (LaunchableApp) -> Unit,
    onSaveAppTags: (LaunchableApp, List<String>) -> Unit,
    onUndoHideApp: (LaunchableApp) -> Unit,
    onHiddenAppNoticeConsumed: () -> Unit,
    onPromoteFavorite: (LaunchableApp) -> Unit,
    onDismissHomeReorderHint: () -> Unit,
    onPhoneClick: () -> Unit,
    onCameraClick: () -> Unit,
    onLaunchDismiss: () -> Unit = {},
    onLaunchConfirm: (LaunchableApp, Int?) -> Unit = { _, _ -> },
    onTimeoutDismiss: () -> Unit = {},
    onTimeoutAddFiveMinutes: () -> Unit = {},
    onLanguageChange: (AppLanguage) -> Unit = {},
    onThemeChange: (ThemeMode) -> Unit = {},
    onClearReminder: () -> Unit = {},
    onExportBackup: () -> Unit = {},
    onImportBackup: () -> Unit = {},
    onUsagePromptToggle: (Boolean) -> Unit = {},
    onMoonIlluminationPercentageToggle: (Boolean) -> Unit = {},
    onHomeWeekdayToggle: (Boolean) -> Unit = {},
    onHomeDateToggle: (Boolean) -> Unit = {},
    onUse24HourTimeToggle: (Boolean) -> Unit = {},
    onRestoreHiddenApp: (LaunchableApp) -> Unit = {},
    onRestoreAllHiddenApps: () -> Unit = {},
    onOpenWebSearch: () -> Unit = {},
    onOpenAppInfo: (LaunchableApp) -> Unit = {},
    onWebSearch: (String) -> Unit = {},
    onWebSearchQueryChange: (String) -> Unit = {},
    onWebSearchDismiss: () -> Unit = {},
    onTransientMessageConsumed: () -> Unit = {},
    notificationListenerGranted: Boolean = false,
    storagePermissionGranted: Boolean = false,
    onOpenNotificationSettings: () -> Unit = {},
    onRequestStoragePermission: () -> Unit = {},
) {
    val palette = launcherPalette()
    val pagerState = rememberPagerState(initialPage = 1) { 4 }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var appTagEditor by remember { mutableStateOf<LaunchableApp?>(null) }
    val visibleApps = remember(state.allApps, state.hiddenPackages) {
        if (state.hiddenPackages.isEmpty()) {
            state.allApps
        } else {
            val hiddenSet = state.hiddenPackages.toSet()
            state.allApps.filterNot { app -> app.appId in hiddenSet }
        }
    }
    val favoriteApps = remember(state.allApps, state.favoritePackages) {
        val appsByAppId = state.allApps.associateBy { it.appId }
        state.favoritePackages.mapNotNull { appsByAppId[it] }
    }
    val homeApps = remember(favoriteApps, visibleApps, state.homeQuery) {
        if (state.homeQuery.isBlank()) {
            favoriteApps
        } else {
            filterApps(visibleApps, state.homeQuery)
        }
    }

    LaunchedEffect(homeReturnSignal) {
        if (homeReturnSignal > 0) {
            pagerState.scrollToPage(1)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = palette.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                when (page) {
                    0 -> TagsScreen(
                        state = state,
                        isCurrentPage = pagerState.currentPage == 0,
                        onAppClick = onAppClick,
                        onToggleFavorite = onToggleFavorite,
                        onHideApp = onHideApp,
                        onEditTags = { appTagEditor = it },
                        onOpenAppInfo = onOpenAppInfo,
                    )

                    1 -> HomeScreen(
                        homeApps = homeApps,
                        showMoonIlluminationPercentage = state.showMoonIlluminationPercentage,
                        showWeekday = state.showHomeWeekday,
                        showDate = state.showHomeDate,
                        use24HourTime = state.use24HourTime,
                        isSearching = state.homeQuery.isNotBlank(),
                        showFavoritesReorderHint = state.showHomeReorderHint,
                        homeQuery = state.homeQuery,
                        onHomeQueryChange = onHomeQueryChange,
                        onAppClick = onAppClick,
                        onPromoteFavorite = onPromoteFavorite,
                        onDismissFavoritesReorderHint = onDismissHomeReorderHint,
                        onClockClick = onClockClick,
                        onAddFavoritesClick = { scope.launch { pagerState.animateScrollToPage(2) } },
                        onPhoneClick = onPhoneClick,
                        onCameraClick = onCameraClick,
                        onOpenWebSearch = onOpenWebSearch,
                    )

                    2 -> AppsScreen(
                        state = state,
                        onQueryChange = onQueryChange,
                        onAppClick = onAppClick,
                        onToggleFavorite = onToggleFavorite,
                        onHideApp = onHideApp,
                        onEditTags = { appTagEditor = it },
                        onOpenAppInfo = onOpenAppInfo,
                        onOpenSettings = { scope.launch { pagerState.animateScrollToPage(3) } },
                    )

                    else -> SettingsScreen(
                        selectedLanguage = state.selectedLanguage,
                        selectedThemeMode = state.selectedThemeMode,
                        usagePromptEnabled = state.usagePromptEnabled,
                        showMoonIlluminationPercentage = state.showMoonIlluminationPercentage,
                        showWeekday = state.showHomeWeekday,
                        showDate = state.showHomeDate,
                        use24HourTime = state.use24HourTime,
                        onLanguageChange = onLanguageChange,
                        onThemeChange = onThemeChange,
                        onClearReminder = onClearReminder,
                        onExportBackup = onExportBackup,
                        onImportBackup = onImportBackup,
                        onUsagePromptToggle = onUsagePromptToggle,
                        onMoonIlluminationPercentageToggle = onMoonIlluminationPercentageToggle,
                        onWeekdayToggle = onHomeWeekdayToggle,
                        onDateToggle = onHomeDateToggle,
                        onUse24HourTimeToggle = onUse24HourTimeToggle,
                        hiddenApps = state.hiddenApps,
                        onRestoreHiddenApp = onRestoreHiddenApp,
                        onRestoreAllHiddenApps = onRestoreAllHiddenApps,
                        onBackToApps = { scope.launch { pagerState.animateScrollToPage(2) } },
                        notificationListenerGranted = notificationListenerGranted,
                        storagePermissionGranted = storagePermissionGranted,
                        onOpenNotificationSettings = onOpenNotificationSettings,
                        onRequestStoragePermission = onRequestStoragePermission,
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            )

            state.pendingLaunchApp?.let { app ->
                LaunchIntentDialog(
                    app = app,
                    onDismiss = onLaunchDismiss,
                    onLaunchNow = { minutes -> onLaunchConfirm(app, minutes) },
                )
            }

            state.timeoutNotice?.let { notice ->
                TimeoutReachedDialog(
                    appLabel = notice.appLabel,
                    minutes = notice.minutes,
                    showAddFiveMinutes = !notice.packageName.isNullOrBlank(),
                    onAddFiveMinutes = onTimeoutAddFiveMinutes,
                    onDismiss = onTimeoutDismiss,
                )
            }
            if (state.showWebSearch) {
                WebSearchDialog(
                    query = state.webSearchQuery,
                    onQueryChange = onWebSearchQueryChange,
                    onSearch = onWebSearch,
                    onDismiss = onWebSearchDismiss,
                )
            }

            appTagEditor?.let { app ->
                AppTagsDialog(
                    app = app,
                    onDismiss = { appTagEditor = null },
                    onSave = { tags ->
                        onSaveAppTags(app, tags)
                        appTagEditor = null
                    },
                )
            }

            state.lastHiddenApp?.let { hiddenApp ->
                val hiddenAppUndoLabel = stringResource(R.string.hidden_app_snackbar_undo)
                val hiddenAppMessage = "${hiddenApp.label} ${stringResource(R.string.hidden_app_snackbar_message_suffix)}"
                LaunchedEffect(hiddenApp.packageName) {
                    val result = snackbarHostState.showSnackbar(
                        message = hiddenAppMessage,
                        actionLabel = hiddenAppUndoLabel,
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        onUndoHideApp(hiddenApp)
                    }
                    onHiddenAppNoticeConsumed()
                }
            }

            state.transientMessage?.let { message ->
                LaunchedEffect(message) {
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = SnackbarDuration.Short,
                    )
                    onTransientMessageConsumed()
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    homeApps: List<LaunchableApp>,
    showMoonIlluminationPercentage: Boolean,
    showWeekday: Boolean,
    showDate: Boolean,
    use24HourTime: Boolean,
    isSearching: Boolean,
    showFavoritesReorderHint: Boolean,
    homeQuery: String,
    onHomeQueryChange: (String) -> Unit,
    onAppClick: (LaunchableApp) -> Unit,
    onPromoteFavorite: (LaunchableApp) -> Unit,
    onDismissFavoritesReorderHint: () -> Unit,
    onClockClick: () -> Unit,
    onAddFavoritesClick: () -> Unit,
    onPhoneClick: () -> Unit,
    onCameraClick: () -> Unit,
    onOpenWebSearch: () -> Unit = {},
) {
    val palette = launcherPalette()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                ClockHeader(
                    onClick = onClockClick,
                    showPercentage = showMoonIlluminationPercentage,
                    showWeekday = showWeekday,
                    showDate = showDate,
                    use24HourTime = use24HourTime,
                )
            }
            item {
                SearchBox(
                    query = homeQuery,
                    onQueryChange = onHomeQueryChange,
                )
            }
            if (!isSearching && homeApps.size > 1 && showFavoritesReorderHint) {
                item {
                    HomeFavoritesHintCard(onDismiss = onDismissFavoritesReorderHint)
                }
            }
            if (homeApps.isEmpty()) {
                if (isSearching) {
                    item {
                        EmptyState(query = homeQuery)
                    }
                } else {
                    item {
                        EmptyFavoritesCard(onAddFavoritesClick = onAddFavoritesClick)
                    }
                }
            } else {
                items(
                    items = homeApps,
                    key = { app -> app.appId },
                    contentType = { "favorite_row" },
                ) { app ->
                    FavoriteRow(
                        app = app,
                        onClick = { onAppClick(app) },
                        onLongClick = if (isSearching) null else { { onPromoteFavorite(app) } },
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        BottomShortcuts(
            onPhoneClick = onPhoneClick,
            onWebSearchClick = onOpenWebSearch,
            onCameraClick = onCameraClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 0.dp),
        )
    }
}

@Composable
private fun TagsScreen(
    state: LauncherUiState,
    isCurrentPage: Boolean,
    onAppClick: (LaunchableApp) -> Unit,
    onToggleFavorite: (LaunchableApp) -> Unit,
    onHideApp: (LaunchableApp) -> Unit,
    onEditTags: (LaunchableApp) -> Unit,
    onOpenAppInfo: (LaunchableApp) -> Unit,
) {
    val palette = launcherPalette()
    var selectedTag by rememberSaveable { mutableStateOf<String?>(null) }
    val hiddenSet = remember(state.hiddenPackages) { state.hiddenPackages.toSet() }
    val visibleApps = remember(state.allApps, hiddenSet) {
        state.allApps.filterNot { it.appId in hiddenSet }
    }
    val tagCounts = remember(visibleApps) {
        val counts = mutableMapOf<String, Int>()
        visibleApps.forEach { app -> app.tags.forEach { tag -> counts[tag] = (counts[tag] ?: 0) + 1 } }
        counts.entries.map { it.key to it.value }.sortedBy { (tag, _) -> normalize(tag) }
    }
    val favoriteAppIds = remember(state.favoritePackages) { state.favoritePackages.toSet() }

    BackHandler(enabled = isCurrentPage && selectedTag != null) {
        selectedTag = null
    }

    if (selectedTag != null) {
        val tag = selectedTag!!
        val appsForTag = remember(visibleApps, tag) {
            visibleApps.filter { app -> app.tags.contains(tag) }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "#$tag",
                        color = palette.textPrimary,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    TextButton(onClick = { selectedTag = null }) {
                        Text(
                            text = stringResource(R.string.tags_title),
                            color = palette.textSecondary,
                        )
                    }
                }
            }
            if (appsForTag.isEmpty()) {
                item {
                    EmptyState(query = "")
                }
            } else {
                items(appsForTag, key = { it.appId }, contentType = { "app_row" }) { app ->
                    AppRow(
                        app = app,
                        isFavorite = app.appId in favoriteAppIds,
                        onClick = { onAppClick(app) },
                        onOpenAppInfo = { onOpenAppInfo(app) },
                        onToggleFavorite = { onToggleFavorite(app) },
                        onHideApp = { onHideApp(app) },
                        onEditTags = { onEditTags(app) },
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.tags_title),
                    color = palette.textPrimary,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                )
            }
            if (tagCounts.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = palette.surface),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.tags_empty_title),
                                color = palette.textPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(R.string.tags_empty_subtitle),
                                color = palette.textSecondary,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            } else {
                items(tagCounts, key = { (tag, _) -> tag }, contentType = { "tag_row" }) { (tag, count) ->
                    TagRow(
                        tag = tag,
                        count = count,
                        onClick = { selectedTag = tag },
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun TagRow(
    tag: String,
    count: Int,
    onClick: () -> Unit,
) {
    val palette = launcherPalette()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 56.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "#$tag",
            color = palette.textPrimary,
            fontSize = 23.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.tags_apps_count, count),
            color = palette.textMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun AppsScreen(
    state: LauncherUiState,
    onQueryChange: (String) -> Unit,
    onAppClick: (LaunchableApp) -> Unit,
    onToggleFavorite: (LaunchableApp) -> Unit,
    onHideApp: (LaunchableApp) -> Unit,
    onEditTags: (LaunchableApp) -> Unit,
    onOpenAppInfo: (LaunchableApp) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val palette = launcherPalette()
    val favoriteAppIds = remember(state.favoritePackages) { state.favoritePackages.toSet() }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            AppsHeader(
                onOpenSettings = onOpenSettings,
            )
        }
        item {
            SearchBox(
                query = state.query,
                onQueryChange = onQueryChange,
            )
        }
        item {
            AppsTipCard(appCount = state.allApps.size)
        }
        if (state.filteredApps.isEmpty()) {
            item {
                EmptyState(query = state.query)
            }
        } else {
            items(
                items = state.filteredApps,
                key = { it.appId },
                contentType = { "app_row" },
            ) { app ->
                AppRow(
                    app = app,
                    isFavorite = app.appId in favoriteAppIds,
                    onClick = { onAppClick(app) },
                    onOpenAppInfo = { onOpenAppInfo(app) },
                    onToggleFavorite = { onToggleFavorite(app) },
                    onHideApp = { onHideApp(app) },
                    onEditTags = { onEditTags(app) },
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HomeFavoritesHintCard(
    onDismiss: () -> Unit,
) {
    val palette = launcherPalette()
    Card(
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = palette.textMuted,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.home_favorites_reorder_hint),
                color = palette.textSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.timeout_acknowledge),
                    color = palette.textPrimary,
                )
            }
        }
    }
}

@Composable
private fun ClockHeader(
    onClick: () -> Unit,
    showPercentage: Boolean,
    showWeekday: Boolean,
    showDate: Boolean,
    use24HourTime: Boolean,
) {
    val palette = launcherPalette()
    var now by remember { mutableStateOf(Date()) }
    val batteryStatus by rememberBatteryStatus()
    val context = LocalContext.current
    val locale = remember(context.resources.configuration.locales) {
        context.resources.configuration.locales[0] ?: Locale.getDefault()
    }

    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(1_000)
        }
    }

    val timeText = remember(now, use24HourTime, locale) {
        SimpleDateFormat(if (use24HourTime) "HH:mm" else "h:mm a", locale).format(now)
    }
    val (mainTimeText, meridiemText) = remember(timeText, use24HourTime, locale) {
        if (use24HourTime) {
            timeText to null
        } else {
            val separatorIndex = timeText.lastIndexOf(' ')
            if (separatorIndex > 0 && separatorIndex < timeText.length - 1) {
                val main = timeText.substring(0, separatorIndex)
                val meridiem = timeText.substring(separatorIndex + 1).uppercase(locale)
                main to meridiem
            } else {
                timeText to null
            }
        }
    }
    val weekdayText = remember(now) {
        SimpleDateFormat("EEEE", locale).format(now)
    }
    val dateText = remember(now) {
        SimpleDateFormat("d MMMM", locale).format(now)
    }
    val lunarPhaseDayKey = remember(now) {
        Calendar.getInstance().apply { time = now }.run {
            get(Calendar.YEAR) to get(Calendar.DAY_OF_YEAR)
        }
    }
    val lunarPhase = remember(lunarPhaseDayKey, showPercentage) {
        lunarPhaseText(now, showPercentage)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(184.dp)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val strokeWidth = 8.dp.toPx()
                val inset = strokeWidth / 2f + 4.dp.toPx()
                val arcSize = size.minDimension - inset * 2
                val topLeft = Offset(inset, inset)
                val sweepAngle = 360f * (batteryStatus.level / 100f)
                val progressColor = if (batteryStatus.isCharging) palette.batteryCharging else palette.textPrimary

                drawArc(
                    color = palette.inputBorderUnfocused,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )

                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (meridiemText == null) {
                    Text(
                        text = mainTimeText,
                        color = palette.textPrimary,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = mainTimeText,
                            color = palette.textPrimary,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = meridiemText,
                            color = palette.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (showWeekday) {
                    Text(
                        text = weekdayText.replaceFirstChar { it.titlecase(locale) },
                        color = palette.textSecondary,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                    )
                }
                if (showWeekday && showDate) {
                    Spacer(modifier = Modifier.height(2.dp))
                }
                if (showDate) {
                    Text(
                        text = dateText,
                        color = palette.textSecondary,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = lunarPhase,
                    color = palette.textPrimary,
                    fontSize = 20.sp,
                )
            }
        }
    }
}

@Composable
private fun rememberBatteryStatus(): State<BatteryStatus> {
    val context = LocalContext.current
    val batteryStatus = remember { mutableStateOf(readBatteryStatus(context)) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                batteryStatus.value = readBatteryStatus(context ?: return, intent)
            }
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val stickyIntent = context.registerReceiver(receiver, filter)
        batteryStatus.value = readBatteryStatus(context, stickyIntent)

        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    return batteryStatus
}

private fun readBatteryStatus(
    context: Context,
    intent: Intent? = null,
): BatteryStatus {
    val batteryIntent = intent ?: context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
    if (level < 0 || scale <= 0) {
        return BatteryStatus(level = 0, isCharging = isCharging)
    }
    return BatteryStatus(
        level = ((level / scale.toFloat()) * 100).toInt().coerceIn(0, 100),
        isCharging = isCharging,
    )
}

@Composable
private fun EmptyFavoritesCard(
    onAddFavoritesClick: () -> Unit,
) {
    val palette = launcherPalette()
    Card(
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.home_empty_title),
                color = palette.textPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.home_empty_subtitle),
                color = palette.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(onClick = onAddFavoritesClick) {
                Text(
                    text = stringResource(R.string.home_empty_cta),
                    color = palette.textPrimary,
                )
            }
        }
    }
}

@Composable
private fun AppsHeader(
    onOpenSettings: () -> Unit,
) {
    val palette = launcherPalette()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.apps_title),
                color = palette.textPrimary,
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = null,
                    tint = palette.textPrimary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.settings_title),
                    color = palette.textPrimary,
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    selectedLanguage: AppLanguage,
    selectedThemeMode: ThemeMode,
    usagePromptEnabled: Boolean,
    showMoonIlluminationPercentage: Boolean,
    showWeekday: Boolean,
    showDate: Boolean,
    use24HourTime: Boolean,
    hiddenApps: List<LaunchableApp>,
    onLanguageChange: (AppLanguage) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onClearReminder: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onUsagePromptToggle: (Boolean) -> Unit,
    onMoonIlluminationPercentageToggle: (Boolean) -> Unit,
    onWeekdayToggle: (Boolean) -> Unit,
    onDateToggle: (Boolean) -> Unit,
    onUse24HourTimeToggle: (Boolean) -> Unit,
    onRestoreHiddenApp: (LaunchableApp) -> Unit,
    onRestoreAllHiddenApps: () -> Unit,
    onBackToApps: () -> Unit,
    notificationListenerGranted: Boolean = false,
    storagePermissionGranted: Boolean = false,
    onOpenNotificationSettings: () -> Unit = {},
    onRequestStoragePermission: () -> Unit = {},
) {
    val palette = launcherPalette()
    val haptic = LocalHapticFeedback.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.settings_title),
                    color = palette.textPrimary,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(onClick = onBackToApps) {
                    Text(
                        text = stringResource(R.string.settings_back_to_apps),
                        color = palette.textSecondary,
                    )
                }
            }
        }

        item {
            SettingsCard(title = stringResource(R.string.settings_theme_title)) {
                Text(
                    text = stringResource(R.string.settings_theme_subtitle),
                    color = palette.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                ThemeMode.entries.forEach { mode ->
                    val selected = selectedThemeMode == mode
                    OutlinedButton(
                        onClick = { onThemeChange(mode) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = mode.fromStorageLabel(LocalContext.current),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start,
                            color = palette.textPrimary,
                        )
                        if (selected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = palette.textPrimary,
                            )
                        }
                    }
                }
            }
        }

        item {
            SettingsCard(title = stringResource(R.string.settings_language_title)) {
                Text(
                    text = stringResource(R.string.settings_language_subtitle),
                    color = palette.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                AppLanguage.entries.forEach { language ->
                    val selected = selectedLanguage == language
                    OutlinedButton(
                        onClick = { onLanguageChange(language) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.language_picker, language.shortLabel),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start,
                            color = palette.textPrimary,
                        )
                        if (selected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = palette.textPrimary,
                            )
                        }
                    }
                }
            }
        }

        item {
            SettingsCard(title = stringResource(R.string.settings_home_title)) {
                Text(
                    text = stringResource(R.string.settings_home_subtitle),
                    color = palette.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.settings_moon_percentage_label),
                        color = palette.textPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = showMoonIlluminationPercentage,
                        onCheckedChange = onMoonIlluminationPercentageToggle,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.settings_home_weekday_label),
                        color = palette.textPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = showWeekday,
                        onCheckedChange = onWeekdayToggle,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.settings_home_date_label),
                        color = palette.textPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = showDate,
                        onCheckedChange = onDateToggle,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.settings_home_24h_label),
                        color = palette.textPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = use24HourTime,
                        onCheckedChange = onUse24HourTimeToggle,
                    )
                }
            }
        }
        item {
            SettingsCard(title = stringResource(R.string.settings_usage_title)) {
                Text(
                    text = stringResource(R.string.settings_usage_subtitle),
                    color = palette.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.settings_usage_prompt_label),
                        color = palette.textPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = usagePromptEnabled,
                        onCheckedChange = onUsagePromptToggle,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onClearReminder,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.settings_clear_reminder),
                        color = palette.textPrimary,
                    )
                }
            }
        }

        item {
            SettingsCard(title = stringResource(R.string.settings_backup_title)) {
                Text(
                    text = stringResource(R.string.settings_backup_subtitle),
                    color = palette.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onExportBackup,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.settings_backup_export),
                        color = palette.textPrimary,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onImportBackup,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.settings_backup_import),
                        color = palette.textPrimary,
                    )
                }
            }
        }

        item {
            SettingsCard(title = stringResource(R.string.settings_finances_title)) {
                Text(
                    text = stringResource(R.string.settings_finances_subtitle),
                    color = palette.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                FinancesPermissionRow(
                    label = stringResource(R.string.settings_finances_notification_label),
                    granted = notificationListenerGranted,
                    onGrant = onOpenNotificationSettings,
                )
                Spacer(modifier = Modifier.height(8.dp))
                FinancesPermissionRow(
                    label = stringResource(R.string.settings_finances_storage_label),
                    granted = storagePermissionGranted,
                    onGrant = onRequestStoragePermission,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.settings_finances_nextcloud_hint),
                    color = palette.textMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        item {
            SettingsCard(title = stringResource(R.string.settings_hidden_apps_title)) {
                Text(
                    text = stringResource(R.string.settings_hidden_apps_subtitle),
                    color = palette.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (hiddenApps.isEmpty()) {
                    Text(
                        text = stringResource(R.string.settings_hidden_apps_empty),
                        color = palette.textMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    hiddenApps.forEach { app ->
                        AppNameRow(
                            app = app,
                            titleSize = 20.sp,
                            showPackage = true,
                            onClick = {},
                            trailing = {
                                IconButton(onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onRestoreHiddenApp(app)
                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Visibility,
                                        contentDescription = null,
                                        tint = palette.textPrimary,
                                    )
                                }
                            },
                            bottomContent = null,
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onRestoreAllHiddenApps()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.settings_hidden_apps_restore_all),
                            color = palette.textPrimary,
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FinancesPermissionRow(
    label: String,
    granted: Boolean,
    onGrant: () -> Unit,
) {
    val palette = launcherPalette()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = palette.textPrimary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        if (granted) {
            Text(
                text = stringResource(R.string.settings_finances_granted),
                color = palette.textSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            TextButton(onClick = onGrant) {
                Text(
                    text = stringResource(R.string.settings_finances_grant),
                    color = palette.textPrimary,
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit,
) {
    val palette = launcherPalette()
    Card(
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                color = palette.textPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

@Composable
private fun AppsTipCard(appCount: Int) {
    val palette = launcherPalette()
    Card(
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = stringResource(R.string.apps_card_title),
                color = palette.textPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.apps_count, appCount),
                color = palette.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun BottomShortcuts(
    onPhoneClick: () -> Unit,
    onWebSearchClick: () -> Unit,
    onCameraClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = launcherPalette()
    val maxBarWidth = 420.dp
    Row(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = maxBarWidth)
            .background(palette.surface, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onPhoneClick,
            modifier = Modifier.sizeIn(minWidth = 56.dp, minHeight = 56.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Call,
                contentDescription = stringResource(R.string.shortcut_phone),
                tint = palette.textPrimary,
                modifier = Modifier.size(28.dp),
            )
        }
        IconButton(
            onClick = onWebSearchClick,
            modifier = Modifier.sizeIn(minWidth = 56.dp, minHeight = 56.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = stringResource(R.string.shortcut_web_search),
                tint = palette.textPrimary,
                modifier = Modifier.size(28.dp),
            )
        }
        IconButton(
            onClick = onCameraClick,
            modifier = Modifier.sizeIn(minWidth = 56.dp, minHeight = 56.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.PhotoCamera,
                contentDescription = stringResource(R.string.shortcut_camera),
                tint = palette.textPrimary,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun SearchBox(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val palette = launcherPalette()
    val interactionSource = remember { MutableInteractionSource() }
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        interactionSource = interactionSource,
        singleLine = true,
        placeholder = {
            Text(stringResource(R.string.search_placeholder), color = palette.textMuted)
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = palette.textPrimary,
            )
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                ClearTextButton(
                    onClear = { onQueryChange("") },
                    interactionSource = interactionSource,
                    tint = palette.textPrimary,
                )
            }
        },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = palette.inputBackground,
            unfocusedContainerColor = palette.inputBackground,
            focusedBorderColor = palette.inputBorderFocused,
            unfocusedBorderColor = palette.inputBorderUnfocused,
            focusedTextColor = palette.textPrimary,
            unfocusedTextColor = palette.textPrimary,
            cursorColor = palette.textPrimary,
        ),
        shape = RoundedCornerShape(16.dp),
    )
}

@Composable
private fun ClearTextButton(
    onClear: () -> Unit,
    interactionSource: MutableInteractionSource,
    tint: Color,
) {
    val isFocused by interactionSource.collectIsFocusedAsState()
    val iconTint = if (isFocused) tint else tint.copy(alpha = 0.6f)
    IconButton(onClick = onClear) {
        Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = stringResource(R.string.clear_search),
            tint = iconTint,
        )
    }
}

@Composable
private fun EmptyState(query: String) {
    val palette = launcherPalette()
    Card(
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = if (query.isBlank()) {
                stringResource(R.string.no_apps_found)
            } else {
                stringResource(R.string.no_results_for, query)
            },
            color = palette.textSecondary,
            modifier = Modifier.padding(18.dp),
        )
    }
}

@Composable
private fun FavoriteRow(
    app: LaunchableApp,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
) {
    AppNameRow(
        app = app,
        titleSize = 28.sp,
        titleMaxLines = 1,
        showPackage = false,
        onClick = onClick,
        onLongClick = onLongClick,
        trailing = null,
        bottomContent = null,
    )
}

@Composable
private fun AppRow(
    app: LaunchableApp,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onToggleFavorite: () -> Unit,
    onHideApp: () -> Unit,
    onEditTags: () -> Unit,
) {
    val palette = launcherPalette()
    val haptic = LocalHapticFeedback.current
    AppNameRow(
        app = app,
        titleSize = 23.sp,
        titleMaxLines = 2,
        iconSize = 24.dp,
        iconToTextSpacing = 10.dp,
        showPackage = true,
        onClick = onClick,
        onLongClick = null,
        trailing = null,
        bottomContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleFavorite()
                }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = stringResource(R.string.app_action_toggle_favorite),
                        tint = if (isFavorite) palette.textPrimary else palette.iconMuted,
                    )
                }
                IconButton(onClick = onOpenAppInfo) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = stringResource(R.string.app_actions_app_info),
                        tint = palette.iconMuted,
                    )
                }
                IconButton(onClick = onEditTags) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.app_action_edit_tags),
                        tint = palette.iconMuted,
                    )
                }
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onHideApp()
                }) {
                    Icon(
                        imageVector = Icons.Outlined.VisibilityOff,
                        contentDescription = stringResource(R.string.app_action_hide_app),
                        tint = palette.iconMuted,
                    )
                }
            }
        },
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun AppNameRow(
    app: LaunchableApp,
    titleSize: androidx.compose.ui.unit.TextUnit,
    titleMaxLines: Int = 1,
    iconSize: androidx.compose.ui.unit.Dp = 28.dp,
    iconToTextSpacing: androidx.compose.ui.unit.Dp = 14.dp,
    showPackage: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    trailing: (@Composable (() -> Unit))?,
    bottomContent: (@Composable (() -> Unit))? = null,
) {
    val palette = launcherPalette()
    val context = LocalContext.current
    val packageManager = context.packageManager
    val iconBitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null,
        key1 = app.appId,
    ) {
        AppIconCache.get(app.appId)?.let {
            value = it.asImageBitmap()
            return@produceState
        }

        val bitmap = withContext(Dispatchers.IO) {
            runCatching {
                packageManager.getApplicationIcon(app.packageName).toBitmap()
            }.getOrNull()
        }

        if (bitmap != null) {
            AppIconCache.put(app.appId, bitmap)
            value = bitmap.asImageBitmap()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 56.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap!!,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
            )
        } else {
            Spacer(
                modifier = Modifier
                    .size(iconSize)
                    .background(palette.inputBorderUnfocused, RoundedCornerShape(8.dp)),
            )
        }
        Spacer(modifier = Modifier.size(iconToTextSpacing))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                color = palette.textPrimary,
                fontSize = titleSize,
                maxLines = titleMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
            if (showPackage) {
                Text(
                    text = app.packageName,
                    color = palette.textMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (app.tags.isNotEmpty()) {
                    Text(
                        text = app.tags.joinToString("  ") { tag -> "#$tag" },
                        color = palette.textSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (bottomContent != null) {
                Spacer(modifier = Modifier.height(4.dp))
                bottomContent()
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}

@Composable
private fun AppTagsDialog(
    app: LaunchableApp,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit,
) {
    var text by rememberSaveable(app.appId) { mutableStateOf(app.tags.joinToString(", ")) }
    val interactionSource = remember { MutableInteractionSource() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tag_editor_title, app.label)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                interactionSource = interactionSource,
                trailingIcon = {
                    if (text.isNotBlank()) {
                        ClearTextButton(
                            onClear = { text = "" },
                            interactionSource = interactionSource,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                placeholder = {
                    Text(stringResource(R.string.tag_editor_hint))
                },
                supportingText = {
                    Text(stringResource(R.string.tag_editor_support))
                },
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(AppTagsStore.parseTagsInput(text)) }) {
                Text(stringResource(R.string.tag_editor_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun LaunchIntentDialog(
    app: LaunchableApp,
    onDismiss: () -> Unit,
    onLaunchNow: (Int?) -> Unit,
) {
    val palette = launcherPalette()
    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB3000000))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clickable(enabled = false) {},
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = app.label,
                    color = palette.textPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.launch_dialog_question),
                    color = palette.textSecondary,
                    fontSize = 16.sp,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickTimeButton(stringResource(R.string.duration_5m)) { onLaunchNow(5) }
                    QuickTimeButton(stringResource(R.string.duration_10m)) { onLaunchNow(10) }
                    QuickTimeButton(stringResource(R.string.duration_15m)) { onLaunchNow(15) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickTimeButton(stringResource(R.string.duration_30m)) { onLaunchNow(30) }
                    QuickTimeButton(stringResource(R.string.duration_60m)) { onLaunchNow(60) }
                    QuickTimeButton(stringResource(R.string.duration_unlimited)) { onLaunchNow(null) }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel), color = palette.textMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickTimeButton(
    label: String,
    onClick: () -> Unit,
) {
    val palette = launcherPalette()
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.width(92.dp),
    ) {
        Text(
            text = label,
            textAlign = TextAlign.Center,
            color = palette.textPrimary,
        )
    }
}

@Composable
private fun TimeoutReachedDialog(
    appLabel: String,
    minutes: Int,
    showAddFiveMinutes: Boolean,
    onAddFiveMinutes: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = launcherPalette()
    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6000000)),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1212)),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.reminder_title),
                    color = Color(0xFFFFB4AB),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.timeout_blocking_message, minutes, appLabel),
                    color = Color(0xFFE4D6D2),
                    fontSize = 17.sp,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                ) {
                    if (showAddFiveMinutes) {
                        OutlinedButton(onClick = onAddFiveMinutes) {
                            Text(stringResource(R.string.timeout_extend_5m), color = palette.textPrimary)
                        }
                    }
                    OutlinedButton(onClick = onDismiss) {
                        Text(stringResource(R.string.timeout_acknowledge), color = palette.textPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun WebSearchDialog(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = launcherPalette()
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    BackHandler(onBack = onDismiss)
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB3000000))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = false) {},
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    interactionSource = interactionSource,
                    singleLine = true,
                    placeholder = {
                        Text(stringResource(R.string.web_search_hint), color = palette.textMuted)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = palette.textPrimary,
                        )
                    },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            ClearTextButton(
                                onClear = { onQueryChange("") },
                                interactionSource = interactionSource,
                                tint = palette.textPrimary,
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Search,
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = { if (query.isNotBlank()) onSearch(query) },
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = palette.inputBackground,
                        unfocusedContainerColor = palette.inputBackground,
                        focusedBorderColor = palette.inputBorderFocused,
                        unfocusedBorderColor = palette.inputBorderUnfocused,
                        focusedTextColor = palette.textPrimary,
                        unfocusedTextColor = palette.textPrimary,
                        cursorColor = palette.textPrimary,
                    ),
                    shape = RoundedCornerShape(16.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel), color = palette.textMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { if (query.isNotBlank()) onSearch(query) },
                        enabled = query.isNotBlank(),
                    ) {
                        Text(stringResource(R.string.web_search_button), color = palette.textPrimary)
                    }
                }
            }
        }
    }
}

private object ReminderScheduler {
    fun scheduleReminder(
        context: Context,
        packageName: String,
        appLabel: String,
        delayMinutes: Int,
    ) {
        resetActiveReminder(context)

        val triggerAtMillis = System.currentTimeMillis() + delayMinutes * 60_000L
        val requestCode = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val notificationId = requestCode
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_PACKAGE_NAME, packageName)
            putExtra(ReminderReceiver.EXTRA_APP_LABEL, appLabel)
            putExtra(ReminderReceiver.EXTRA_MINUTES, delayMinutes)
            putExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent,
        )

        ReminderSessionTracker.saveSession(
            context = context,
            requestCode = requestCode,
            notificationId = notificationId,
        )
    }

    fun resetActiveReminder(context: Context) {
        val session = ReminderSessionTracker.loadSession(context) ?: return

        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            session.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()

        NotificationManagerCompat.from(context).cancel(session.notificationId)
        ReminderSessionTracker.clearSession(context)
    }
}

private object ReminderSessionTracker {
    private const val PREFS_NAME = "reminder_session"
    private const val KEY_REQUEST_CODE = "request_code"
    private const val KEY_NOTIFICATION_ID = "notification_id"

    data class Session(
        val requestCode: Int,
        val notificationId: Int,
    )

    fun saveSession(context: Context, requestCode: Int, notificationId: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_REQUEST_CODE, requestCode)
            .putInt(KEY_NOTIFICATION_ID, notificationId)
            .apply()
    }

    fun loadSession(context: Context): Session? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_REQUEST_CODE) || !prefs.contains(KEY_NOTIFICATION_ID)) return null
        return Session(
            requestCode = prefs.getInt(KEY_REQUEST_CODE, -1),
            notificationId = prefs.getInt(KEY_NOTIFICATION_ID, -1),
        )
    }

    fun clearSession(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_REQUEST_CODE)
            .remove(KEY_NOTIFICATION_ID)
            .apply()
    }
}
