package com.minilauncher

import android.Manifest
import android.content.BroadcastReceiver
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
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
import androidx.compose.material.icons.rounded.PhotoCamera
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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
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
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val launcherPreferences by lazy { getSharedPreferences("launcher_prefs", MODE_PRIVATE) }
    private val appsRepository by lazy { AppsRepository(packageManager) }
    private val favoritesStore by lazy { FavoritesStore(launcherPreferences) }
    private val languageStore by lazy { LanguageStore(launcherPreferences) }
    private val uiState = MutableStateFlow(LauncherUiState())

    override fun attachBaseContext(newBase: Context) {
        val preferences = newBase.getSharedPreferences("launcher_prefs", MODE_PRIVATE)
        val language = LanguageStore(preferences).loadLanguage()
        super.attachBaseContext(newBase.withAppLanguage(language))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.Black.toArgb()
        window.navigationBarColor = Color.Black.toArgb()
        enterImmersiveMode()

        val language = languageStore.loadLanguage()
        uiState.value = uiState.value.copy(selectedLanguage = language)

        loadApps()

        setContent {
            MinimalLauncherTheme {
                val state by uiState.asStateFlow().collectAsStateWithLifecycle()
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
                    onLanguageChange = ::onLanguageChange,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
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
        val current = uiState.value
        uiState.value = current.copy(
            query = query,
            filteredApps = filterApps(current.allApps, query),
        )
    }

    private fun onHomeQueryChange(query: String) {
        uiState.value = uiState.value.copy(homeQuery = query)
    }

    private fun onLanguageChange(language: AppLanguage) {
        val currentLanguage = uiState.value.selectedLanguage
        if (currentLanguage == language) return

        languageStore.saveLanguage(language)
        uiState.value = uiState.value.copy(selectedLanguage = language)
        recreate()
    }

    private fun loadApps() {
        lifecycleScope.launch(Dispatchers.IO) {
            val apps = appsRepository.loadLaunchableApps()
            val current = uiState.value
            val query = current.query
            uiState.value = LauncherUiState(
                query = query,
                homeQuery = current.homeQuery,
                allApps = apps,
                filteredApps = filterApps(apps, query),
                favoritePackages = favoritesStore.loadFavorites(),
                selectedLanguage = current.selectedLanguage,
            )
        }
    }

    private fun toggleFavorite(app: LaunchableApp) {
        val updatedFavorites = favoritesStore.toggle(app.packageName)
        uiState.value = uiState.value.copy(favoritePackages = updatedFavorites)
    }

    private fun promoteFavorite(app: LaunchableApp) {
        val updatedFavorites = favoritesStore.promote(app.packageName)
        uiState.value = uiState.value.copy(favoritePackages = updatedFavorites)
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

    private fun confirmLaunch(app: LaunchableApp, durationMinutes: Int?) {
        if (durationMinutes != null) {
            ensureNotificationPermission()
            ReminderScheduler.scheduleReminder(
                context = this,
                appLabel = app.label,
                delayMinutes = durationMinutes,
            )
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

data class LauncherUiState(
    val query: String = "",
    val homeQuery: String = "",
    val allApps: List<LaunchableApp> = emptyList(),
    val filteredApps: List<LaunchableApp> = emptyList(),
    val favoritePackages: List<String> = emptyList(),
    val pendingLaunchApp: LaunchableApp? = null,
    val selectedLanguage: AppLanguage = AppLanguage.SPANISH,
)

data class LaunchableApp(
    val label: String,
    val packageName: String,
)

private class AppsRepository(
    private val packageManager: PackageManager,
) {
    fun loadLaunchableApps(): List<LaunchableApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val apps: List<ResolveInfo> = packageManager.queryIntentActivities(intent, 0)

        return apps
            .mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                val label = resolveInfo.loadLabel(packageManager)?.toString()?.trim().orEmpty()
                if (label.isBlank()) return@mapNotNull null

                LaunchableApp(
                    label = label,
                    packageName = packageName,
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { normalize(it.label) }
    }
}

private class FavoritesStore(
    private val preferences: SharedPreferences,
) {
    fun loadFavorites(): List<String> {
        val ordered = preferences.getString(FAVORITES_ORDER_KEY, null)
            ?.split(FAVORITES_SEPARATOR)
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.distinct()
        if (!ordered.isNullOrEmpty()) return ordered

        return preferences.getStringSet(FAVORITES_KEY, emptySet())
            .orEmpty()
            .toList()
            .sorted()
    }

    fun toggle(packageName: String): List<String> {
        val current = loadFavorites().toMutableList()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(0, packageName)
        }
        return save(current)
    }

    fun promote(packageName: String): List<String> {
        val current = loadFavorites().toMutableList()
        val index = current.indexOf(packageName)
        if (index <= 0) return current
        current.removeAt(index)
        current.add(0, packageName)
        return save(current)
    }

    private fun save(values: List<String>): List<String> {
        val cleaned = values.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        preferences.edit()
            .putString(FAVORITES_ORDER_KEY, cleaned.joinToString(FAVORITES_SEPARATOR))
            .putStringSet(FAVORITES_KEY, cleaned.toSet())
            .apply()
        return cleaned
    }

    private companion object {
        const val FAVORITES_KEY = "favorite_packages"
        const val FAVORITES_ORDER_KEY = "favorite_packages_order"
        const val FAVORITES_SEPARATOR = "|"
    }
}

private fun filterApps(
    apps: List<LaunchableApp>,
    query: String,
): List<LaunchableApp> {
    if (query.isBlank()) return apps
    val normalizedQuery = normalize(query)
    return apps.filter { app ->
        normalize(app.label).contains(normalizedQuery) ||
            app.packageName.lowercase(Locale.ROOT).contains(normalizedQuery)
    }
}

private fun normalize(value: String): String {
    return Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .lowercase(Locale.ROOT)
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
    onLanguageChange: (AppLanguage) -> Unit = {},
) {
    val pagerState = rememberPagerState(initialPage = 0) { 2 }
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
        color = Color.Black,
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

                else -> AppsScreen(
                    state = state,
                    onQueryChange = onQueryChange,
                    onAppClick = onAppClick,
                    onToggleFavorite = onToggleFavorite,
                    onLanguageChange = onLanguageChange,
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun AppsScreen(
    state: LauncherUiState,
    onQueryChange: (String) -> Unit,
    onAppClick: (LaunchableApp) -> Unit,
    onToggleFavorite: (LaunchableApp) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            AppsHeader(
                selectedLanguage = state.selectedLanguage,
                onLanguageChange = onLanguageChange,
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
                    color = Color(0xFF242424),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )

                drawArc(
                    color = Color.White,
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
                    color = Color.White,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateText.replaceFirstChar { it.titlecase(locale) },
                    color = Color(0xFFD0D0D0),
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
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.home_empty_title),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.home_empty_subtitle),
                color = Color(0xFFCFCFCF),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(onClick = onAddFavoritesClick) {
                Text(
                    text = stringResource(R.string.home_empty_cta),
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun AppsHeader(
    selectedLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
) {
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
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(onClick = { onLanguageChange(selectedLanguage.next()) }) {
                Text(
                    text = stringResource(R.string.language_picker, selectedLanguage.shortLabel),
                    color = Color(0xFFDADADA),
                )
            }
        }
    }
}

@Composable
private fun AppsTipCard(appCount: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF181818)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = stringResource(R.string.apps_card_title),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.apps_count, appCount),
                color = Color(0xFFCFCFCF),
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
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPhoneClick) {
            Icon(
                imageVector = Icons.Rounded.Call,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
        IconButton(onClick = onCameraClick) {
            Icon(
                imageVector = Icons.Rounded.PhotoCamera,
                contentDescription = null,
                tint = Color.White,
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
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = {
            Text(stringResource(R.string.search_placeholder), color = Color(0xFF8D8D8D))
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = Color.White,
            )
        },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF101010),
            unfocusedContainerColor = Color(0xFF101010),
            focusedBorderColor = Color(0xFFBDBDBD),
            unfocusedBorderColor = Color(0xFF4A4A4A),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color.White,
        ),
        shape = RoundedCornerShape(16.dp),
    )
}

@Composable
private fun EmptyState(query: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = if (query.isBlank()) {
                stringResource(R.string.no_apps_found)
            } else {
                stringResource(R.string.no_results_for, query)
            },
            color = Color(0xFFDADADA),
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
                    tint = if (isFavorite) Color.White else Color(0xFF727272),
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
    val context = LocalContext.current
    val packageManager = context.packageManager
    val iconBitmap = remember(app.packageName) {
        packageManager.getApplicationIcon(app.packageName).toBitmap().asImageBitmap()
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
        Image(
            bitmap = iconBitmap,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
        )
        Spacer(modifier = Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                color = Color.White,
                fontSize = titleSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (showPackage) {
                Text(
                    text = app.packageName,
                    color = Color(0xFF888888),
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
private fun MinimalLauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}

@Composable
private fun LaunchIntentDialog(
    app: LaunchableApp,
    onDismiss: () -> Unit,
    onLaunchNow: (Int?) -> Unit,
) {
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
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.launch_dialog_question),
                    color = Color(0xFFD0D0D0),
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
                        Text(stringResource(R.string.cancel), color = Color(0xFFB5B5B5))
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
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.width(92.dp),
    ) {
        Text(
            text = label,
            textAlign = TextAlign.Center,
            color = Color.White,
        )
    }
}

private object ReminderScheduler {
    fun scheduleReminder(
        context: Context,
        appLabel: String,
        delayMinutes: Int,
    ) {
        val triggerAtMillis = System.currentTimeMillis() + delayMinutes * 60_000L
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_APP_LABEL, appLabel)
            putExtra(ReminderReceiver.EXTRA_MINUTES, delayMinutes)
        }
        val requestCode = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
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
    }
}
