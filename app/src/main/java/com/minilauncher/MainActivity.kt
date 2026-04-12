package com.minilauncher

import android.Manifest
import android.content.BroadcastReceiver
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.AlarmClock
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val appsRepository by lazy { AppsRepository(packageManager) }
    private val favoritesStore by lazy { FavoritesStore(applicationContext.launcherDataStore) }
    private val launcherStateStore by lazy { LauncherStateStore(appsRepository, favoritesStore) }
    private val languageStore by lazy { LanguageStore(applicationContext.launcherDataStore) }
    private val themeStore by lazy { ThemeStore(applicationContext.launcherDataStore) }
    private val uiState = MutableStateFlow(LauncherUiState())
    private var loadAppsJob: Job? = null
    private var skipReminderResetOnNextResume = false

    override fun attachBaseContext(newBase: Context) {
        val language = LanguageStore(newBase.launcherDataStore).loadLanguageBlocking()
        super.attachBaseContext(newBase.withAppLanguage(language))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        val language = languageStore.loadLanguageBlocking()
        val themeMode = themeStore.loadThemeModeBlocking()
        uiState.value = uiState.value.copy(
            selectedLanguage = language,
            selectedThemeMode = themeMode,
        )
        updateSystemBars(themeMode)
        enterImmersiveMode()
        handleTimeoutIntent(intent)

        loadApps()

        setContent {
            val state by uiState.asStateFlow().collectAsStateWithLifecycle()
            MinimalLauncherTheme(themeMode = state.selectedThemeMode) {
                LauncherApp(
                    state = state,
                    onQueryChange = ::onQueryChange,
                    onHomeQueryChange = ::onHomeQueryChange,
                    onAppClick = ::promptAppLaunch,
                    onClockClick = ::openAlarms,
                    onToggleFavorite = ::toggleFavorite,
                    onPromoteFavorite = ::promoteFavorite,
                    onPhoneClick = ::openPhone,
                    onCameraClick = ::openCamera,
                    onLaunchDismiss = ::dismissLaunchPrompt,
                    onLaunchConfirm = ::confirmLaunch,
                    onTimeoutDismiss = ::dismissTimeoutNotice,
                    onLanguageChange = ::onLanguageChange,
                    onThemeChange = ::onThemeChange,
                    onClearReminder = ::clearActiveReminderFromSettings,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleTimeoutIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (skipReminderResetOnNextResume) {
            skipReminderResetOnNextResume = false
        } else {
            ReminderScheduler.resetActiveReminder(this)
            uiState.value = uiState.value.copy(timeoutNotice = null)
        }
        enterImmersiveMode()
        loadApps()
    }

    private fun enterImmersiveMode() {
        window.insetsController?.let { controller ->
            controller.hide(WindowInsets.Type.statusBars())
            controller.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun onQueryChange(query: String) {
        uiState.value = launcherStateStore.onQueryChange(uiState.value, query)
    }

    private fun onHomeQueryChange(query: String) {
        uiState.value = launcherStateStore.onHomeQueryChange(uiState.value, query)
    }

    private fun onLanguageChange(language: AppLanguage) {
        val currentLanguage = uiState.value.selectedLanguage
        if (currentLanguage == language) return

        lifecycleScope.launch {
            languageStore.saveLanguage(language)
            uiState.value = uiState.value.copy(selectedLanguage = language)
            recreate()
        }
    }

    private fun onThemeChange(themeMode: ThemeMode) {
        val currentTheme = uiState.value.selectedThemeMode
        if (currentTheme == themeMode) return

        lifecycleScope.launch {
            themeStore.saveThemeMode(themeMode)
            uiState.value = uiState.value.copy(selectedThemeMode = themeMode)
            updateSystemBars(themeMode)
        }
    }

    private fun updateSystemBars(themeMode: ThemeMode) {
        val barColor = when (themeMode) {
            ThemeMode.DARK -> Color.Black
            ThemeMode.LIGHT -> Color(0xFFF3F4F6)
        }
        window.statusBarColor = barColor.toArgb()
        window.navigationBarColor = barColor.toArgb()
    }

    private fun loadApps() {
        loadAppsJob?.cancel()
        loadAppsJob = lifecycleScope.launch(Dispatchers.IO) {
            launcherStateStore.loadApps(uiState)
        }
    }

    private fun toggleFavorite(app: LaunchableApp) {
        lifecycleScope.launch {
            uiState.value = launcherStateStore.toggleFavorite(uiState.value, app)
        }
    }

    private fun promoteFavorite(app: LaunchableApp) {
        lifecycleScope.launch {
            uiState.value = launcherStateStore.promoteFavorite(uiState.value, app)
        }
    }

    private fun openApp(app: LaunchableApp) {
        val intent = packageManager.getLaunchIntentForPackage(app.packageName)
        if (intent != null) {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun promptAppLaunch(app: LaunchableApp) {
        uiState.value = uiState.value.copy(pendingLaunchApp = app)
    }

    private fun dismissLaunchPrompt() {
        uiState.value = uiState.value.copy(pendingLaunchApp = null)
    }

    private fun dismissTimeoutNotice() {
        ReminderScheduler.resetActiveReminder(this)
        uiState.value = uiState.value.copy(timeoutNotice = null)
    }

    private fun clearActiveReminderFromSettings() {
        ReminderScheduler.resetActiveReminder(this)
        uiState.value = uiState.value.copy(timeoutNotice = null)
    }

    private fun handleTimeoutIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(ReminderReceiver.EXTRA_TIMEOUT_REACHED, false) != true) return
        skipReminderResetOnNextResume = true

        val appLabel = intent.getStringExtra(ReminderReceiver.EXTRA_APP_LABEL).orEmpty().ifBlank {
            getString(R.string.reminder_default_app)
        }
        val minutes = intent.getIntExtra(ReminderReceiver.EXTRA_MINUTES, 0)
        uiState.value = uiState.value.copy(
            timeoutNotice = TimeoutNotice(
                appLabel = appLabel,
                minutes = minutes,
            ),
        )

        intent.removeExtra(ReminderReceiver.EXTRA_TIMEOUT_REACHED)
    }

    private fun confirmLaunch(app: LaunchableApp, durationMinutes: Int?) {
        if (durationMinutes != null) {
            ensureNotificationPermission()
            ReminderScheduler.scheduleReminder(
                context = this,
                appLabel = app.label,
                delayMinutes = durationMinutes,
            )
        } else {
            ReminderScheduler.resetActiveReminder(this)
        }
        dismissLaunchPrompt()
        openApp(app)
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
    }

    private fun openAlarms() {
        val intents = listOf(
            Intent(AlarmClock.ACTION_SHOW_ALARMS),
            Intent(AlarmClock.ACTION_SET_ALARM),
        )

        val launchIntent = intents.firstOrNull { intent ->
            intent.resolveActivity(packageManager) != null
        }

        if (launchIntent != null) {
            startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun openPhone() {
        val intents = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_VIEW).apply { type = "vnd.android-dir/mms-sms" },
        )

        val launchIntent = intents.firstOrNull { intent ->
            intent.resolveActivity(packageManager) != null
        }

        if (launchIntent != null) {
            startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun openCamera() {
        val intents = listOf(
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
            Intent("android.media.action.STILL_IMAGE_CAMERA"),
            Intent(MediaStore.ACTION_IMAGE_CAPTURE),
        )

        val launchIntent = intents.firstOrNull { intent ->
            intent.resolveActivity(packageManager) != null
        }

        if (launchIntent != null) {
            startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

}

@Composable
private fun LauncherApp(
    state: LauncherUiState,
    onQueryChange: (String) -> Unit,
    onHomeQueryChange: (String) -> Unit,
    onAppClick: (LaunchableApp) -> Unit,
    onClockClick: () -> Unit,
    onToggleFavorite: (LaunchableApp) -> Unit,
    onPromoteFavorite: (LaunchableApp) -> Unit,
    onPhoneClick: () -> Unit,
    onCameraClick: () -> Unit,
    onLaunchDismiss: () -> Unit = {},
    onLaunchConfirm: (LaunchableApp, Int?) -> Unit = { _, _ -> },
    onTimeoutDismiss: () -> Unit = {},
    onLanguageChange: (AppLanguage) -> Unit = {},
    onThemeChange: (ThemeMode) -> Unit = {},
    onClearReminder: () -> Unit = {},
) {
    val palette = launcherPalette()
    val pagerState = rememberPagerState(initialPage = 0) { 3 }
    val scope = rememberCoroutineScope()
    val favoriteApps = remember(state.allApps, state.favoritePackages) {
        val appsByPackage = state.allApps.associateBy { it.packageName }
        state.favoritePackages.mapNotNull { appsByPackage[it] }
    }
    val filteredFavoriteApps = remember(favoriteApps, state.homeQuery) {
        filterApps(favoriteApps, state.homeQuery)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = palette.background,
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (page) {
                0 -> HomeScreen(
                    favoriteApps = filteredFavoriteApps,
                    homeQuery = state.homeQuery,
                    onHomeQueryChange = onHomeQueryChange,
                    onAppClick = onAppClick,
                    onPromoteFavorite = onPromoteFavorite,
                    onClockClick = onClockClick,
                    onAddFavoritesClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    onPhoneClick = onPhoneClick,
                    onCameraClick = onCameraClick,
                )

                1 -> AppsScreen(
                    state = state,
                    onQueryChange = onQueryChange,
                    onAppClick = onAppClick,
                    onToggleFavorite = onToggleFavorite,
                    onOpenSettings = { scope.launch { pagerState.animateScrollToPage(2) } },
                )

                else -> SettingsScreen(
                    selectedLanguage = state.selectedLanguage,
                    selectedThemeMode = state.selectedThemeMode,
                    onLanguageChange = onLanguageChange,
                    onThemeChange = onThemeChange,
                    onClearReminder = onClearReminder,
                    onBackToApps = { scope.launch { pagerState.animateScrollToPage(1) } },
                )
            }
        }

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
                onDismiss = onTimeoutDismiss,
            )
        }
    }
}

@Composable
private fun HomeScreen(
    favoriteApps: List<LaunchableApp>,
    homeQuery: String,
    onHomeQueryChange: (String) -> Unit,
    onAppClick: (LaunchableApp) -> Unit,
    onPromoteFavorite: (LaunchableApp) -> Unit,
    onClockClick: () -> Unit,
    onAddFavoritesClick: () -> Unit,
    onPhoneClick: () -> Unit,
    onCameraClick: () -> Unit,
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
                ClockHeader(onClick = onClockClick)
            }
            item {
                SearchBox(
                    query = homeQuery,
                    onQueryChange = onHomeQueryChange,
                )
            }
            if (favoriteApps.isEmpty()) {
                item {
                    EmptyFavoritesCard(onAddFavoritesClick = onAddFavoritesClick)
                }
            } else {
                items(
                    items = favoriteApps,
                    key = { app -> app.packageName },
                ) { app ->
                    FavoriteRow(
                        app = app,
                        onClick = { onAppClick(app) },
                        onLongClick = { onPromoteFavorite(app) },
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        BottomShortcuts(
            onPhoneClick = onPhoneClick,
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
private fun AppsScreen(
    state: LauncherUiState,
    onQueryChange: (String) -> Unit,
    onAppClick: (LaunchableApp) -> Unit,
    onToggleFavorite: (LaunchableApp) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val palette = launcherPalette()
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
                key = { it.packageName },
            ) { app ->
                AppRow(
                    app = app,
                    isFavorite = app.packageName in state.favoritePackages,
                    onClick = { onAppClick(app) },
                    onToggleFavorite = { onToggleFavorite(app) },
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ClockHeader(onClick: () -> Unit) {
    val palette = launcherPalette()
    var now by remember { mutableStateOf(Date()) }
    val batteryLevel by rememberBatteryLevel()
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

    val timeText = remember(now) {
        SimpleDateFormat("HH:mm", locale).format(now)
    }
    val dateText = remember(now) {
        SimpleDateFormat("EEEE\nd MMMM", locale).format(now)
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
                val sweepAngle = 360f * (batteryLevel / 100f)

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
                    color = palette.textPrimary,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = timeText,
                    color = palette.textPrimary,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateText.replaceFirstChar { it.titlecase(locale) },
                    color = palette.textSecondary,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

@Composable
private fun rememberBatteryLevel(): State<Int> {
    val context = LocalContext.current
    val batteryLevel = remember { mutableStateOf(readBatteryLevel(context)) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                batteryLevel.value = readBatteryLevel(context ?: return)
            }
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val stickyIntent = context.registerReceiver(receiver, filter)
        batteryLevel.value = readBatteryLevel(context, stickyIntent)

        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    return batteryLevel
}

private fun readBatteryLevel(
    context: Context,
    intent: Intent? = null,
): Int {
    val batteryIntent = intent ?: context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    if (level < 0 || scale <= 0) return 0
    return ((level / scale.toFloat()) * 100).toInt().coerceIn(0, 100)
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
    onLanguageChange: (AppLanguage) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onClearReminder: () -> Unit,
    onBackToApps: () -> Unit,
) {
    val palette = launcherPalette()
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
            SettingsCard(title = stringResource(R.string.settings_usage_title)) {
                Text(
                    text = stringResource(R.string.settings_usage_subtitle),
                    color = palette.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
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
            Spacer(modifier = Modifier.height(24.dp))
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
    onCameraClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = launcherPalette()
    Row(
        modifier = modifier
            .background(palette.background)
            .padding(vertical = 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPhoneClick) {
            Icon(
                imageVector = Icons.Rounded.Call,
                contentDescription = null,
                tint = palette.textPrimary,
                modifier = Modifier.size(28.dp),
            )
        }
        IconButton(onClick = onCameraClick) {
            Icon(
                imageVector = Icons.Rounded.PhotoCamera,
                contentDescription = null,
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
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
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
    onLongClick: () -> Unit,
) {
    AppNameRow(
        app = app,
        titleSize = 28.sp,
        showPackage = false,
        onClick = onClick,
        onLongClick = onLongClick,
        trailing = null,
    )
}

@Composable
private fun AppRow(
    app: LaunchableApp,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val palette = launcherPalette()
    AppNameRow(
        app = app,
        titleSize = 26.sp,
        showPackage = true,
        onClick = onClick,
        onLongClick = null,
        trailing = {
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = null,
                    tint = if (isFavorite) palette.textPrimary else palette.iconMuted,
                )
            }
        },
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun AppNameRow(
    app: LaunchableApp,
    titleSize: androidx.compose.ui.unit.TextUnit,
    showPackage: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    trailing: (@Composable (() -> Unit))?,
) {
    val palette = launcherPalette()
    val context = LocalContext.current
    val packageManager = context.packageManager
    val iconBitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null,
        key1 = app.packageName,
    ) {
        AppIconCache.get(app.packageName)?.let {
            value = it.asImageBitmap()
            return@produceState
        }

        val bitmap = withContext(Dispatchers.IO) {
            runCatching {
                packageManager.getApplicationIcon(app.packageName).toBitmap()
            }.getOrNull()
        }

        if (bitmap != null) {
            AppIconCache.put(app.packageName, bitmap)
            value = bitmap.asImageBitmap()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap!!,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
        } else {
            Spacer(
                modifier = Modifier
                    .size(28.dp)
                    .background(palette.inputBorderUnfocused, RoundedCornerShape(8.dp)),
            )
        }
        Spacer(modifier = Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                color = palette.textPrimary,
                fontSize = titleSize,
                maxLines = 1,
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
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
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
                    horizontalArrangement = Arrangement.End,
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text(stringResource(R.string.timeout_acknowledge), color = palette.textPrimary)
                    }
                }
            }
        }
    }
}

private object ReminderScheduler {
    fun scheduleReminder(
        context: Context,
        appLabel: String,
        delayMinutes: Int,
    ) {
        resetActiveReminder(context)

        val triggerAtMillis = System.currentTimeMillis() + delayMinutes * 60_000L
        val requestCode = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val notificationId = requestCode
        val intent = Intent(context, ReminderReceiver::class.java).apply {
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
