package com.kaeru.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.kaeru.app.data.utils.GithubRelease
import com.kaeru.app.data.utils.UpdateManager
import com.kaeru.app.tracking.TrackingRepository
import com.kaeru.app.tracking.TrackingViewModel
import com.kaeru.app.tracking.database.AppDatabase
import com.kaeru.app.ui.screens.*
import com.kaeru.app.ui.screens.settings.*
import com.kaeru.app.ui.theme.KaeruTrackTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.content.edit
import coil.compose.AsyncImage
import com.kaeru.app.tracking.utils.isDeliveredStatus
import com.kaeru.app.ui.components.BatteryOptimizationDialog
import com.kaeru.app.ui.components.EditProfileDialog
import com.kaeru.app.ui.components.ProfileDialog

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val userPrefs = com.kaeru.app.data.UserPreferences(newBase)
        val savedLang = userPrefs.getLanguage()

        if (savedLang != "system") {
            val locale = java.util.Locale.forLanguageTag(savedLang)
            java.util.Locale.setDefault(locale)

            val config = android.content.res.Configuration(newBase.resources.configuration)
            config.setLocale(locale)

            val context = newBase.createConfigurationContext(config)
            super.attachBaseContext(context)
        } else {
            super.attachBaseContext(newBase)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean -> }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        askNotificationPermission()
        enableEdgeToEdge()
        installSplashScreen()

        val db = AppDatabase.getDatabase(applicationContext)
        val dao = db.trackingDao()
        val repository = TrackingRepository(applicationContext)
        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(TrackingViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return TrackingViewModel(
                        application = application,
                        repository = repository,
                        dao = dao
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
        val trackingViewModel = ViewModelProvider(this, viewModelFactory)[TrackingViewModel::class.java]

        setContent {
            val themeMode by trackingViewModel.themeMode.collectAsState()
            val isAmoled by trackingViewModel.isAmoled.collectAsState()
            val themeColorInt by trackingViewModel.currentThemeColor.collectAsState()
            val useDarkTheme = when (themeMode) {
                KaeruThemeMode.LIGHT -> false
                KaeruThemeMode.DARK -> true
                KaeruThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            val trackingViewModel: TrackingViewModel = viewModel()
            var updateRelease by remember { mutableStateOf<GithubRelease?>(null) }
            val updateManager = remember { UpdateManager() }
            val checkUpdatesEnabled by trackingViewModel.checkUpdatesOnStart.collectAsState()
            val context = LocalContext.current
            val sharedPreferences = remember { context.getSharedPreferences("kaeru_prefs", Context.MODE_PRIVATE) }
            var showChangelog by rememberSaveable { mutableStateOf(false) }
            var showBatteryDialog by rememberSaveable { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                if (checkUpdatesEnabled) {
                    updateRelease = updateManager.checkForUpdate()
                } else {
                    updateRelease = null
                }
                val lastSeenVersion = sharedPreferences.getString("last_seen_version", "") ?: ""
                val currentVersion = BuildConfig.VERSION_NAME
                if (lastSeenVersion != currentVersion) {
                    showChangelog = true
                }
                val hideBatteryDialog = sharedPreferences.getBoolean("hide_battery_dialog", false)
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val isIgnoringOptimizations = powerManager.isIgnoringBatteryOptimizations(context.packageName)
                if (!hideBatteryDialog && !isIgnoringOptimizations) {
                    showBatteryDialog = true
                }
                sharedPreferences.edit().putString("last_seen_version", currentVersion).apply()
            } // checagem de att/otimização de bateria ao abrir (essencial pra badge, changelogs e notificações)

            KaeruTrackTheme(
                darkTheme = useDarkTheme,
                pureBlack = isAmoled,
                seedColor = Color(themeColorInt)
            ) {
                var showSplash by remember { mutableStateOf(true) }
                if (showSplash) {
                    SplashScreen(
                        onSplashFinished = {
                            showSplash = false
                        }
                    )
                } else {
                    if (showBatteryDialog) {
                        BatteryOptimizationDialog(
                            onDismiss = { showBatteryDialog = false },
                            onNeverShowAgain = {
                                sharedPreferences.edit().putBoolean("hide_battery_dialog", true).apply()
                                showBatteryDialog = false
                            }
                        )
                    }
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        KaeruNavGraph(viewModel = trackingViewModel, updateRelease = updateRelease)
                        if (showChangelog) {
                            ChangelogSheet(onDismiss = { showChangelog = false })
                        }
                    }
                }
            }
        }
    }
}

// rotas
object Routes {
    const val HOME = "home_screen"
    const val RESULT = "result_screen/{code}?carrier={carrier}"
    const val SETTINGS = "settings_screen"
    const val APPEARANCE = "appearance_screen"
    const val THEME = "theme_screen"
    const val BACKUP = "backup_screen"
    const val UPDATE = "update_screen"
    const val ABOUT = "about_screen"
}

@Composable
fun KaeruNavGraph(viewModel: TrackingViewModel, updateRelease: GithubRelease?) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    LaunchedEffect(Unit) {
        activity?.intent?.let { intent ->
            val trackingCode = intent.getStringExtra("tracking_code")
            if (!trackingCode.isNullOrBlank()) {
                navController.navigate("result_screen/$trackingCode?carrier=Auto")
                intent.removeExtra("tracking_code")
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(500))
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(500))
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(500))
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(500))
        }
    ) {
        composable(route = Routes.HOME) {
            KaeruTabsScreen(
                viewModel = viewModel,
                updateRelease = updateRelease,
                onNavigateToResult = { code, carrier ->
                    navController.navigate("result_screen/$code?carrier=$carrier")
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }

        composable(route = Routes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                updateRelease = updateRelease,
                onBack = { navController.popBackStack() },
                onAppearanceClick = {
                    navController.navigate(Routes.APPEARANCE)
                },
                onBackupClick = {
                    navController.navigate(Routes.BACKUP)
                },
                onUpdaterClick = {
                    navController.navigate(Routes.UPDATE)
                },
                onAboutClick = {
                    navController.navigate(Routes.ABOUT)
                }
            )
        }

        composable(route = Routes.APPEARANCE) {
            AppearanceScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onEditColorsClick = {
                    navController.navigate(Routes.THEME)
                }
            )
        }

        composable(route = Routes.THEME) {
            ThemeScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.RESULT,
            arguments = listOf(navArgument("code") { type = NavType.StringType },
                navArgument("carrier") {
                    type = NavType.StringType
                    defaultValue = "Auto"
                })
        ) { backStackEntry ->
            val code = backStackEntry.arguments?.getString("code") ?: ""
            val carrier = backStackEntry.arguments?.getString("carrier") ?: "Auto"

            ResultScreen(
                trackingCode = code,
                carrier = carrier,
                viewModel = viewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Routes.BACKUP) {
            BackupAndRestore(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(route = Routes.UPDATE) {
            UpdaterScreen(
                viewModel = viewModel,
                updateRelease = updateRelease,
                onBack = { navController.popBackStack() }
            )
        }

        composable(route = Routes.ABOUT) {
            AboutScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun KaeruTabsScreen(
    viewModel: TrackingViewModel,
    updateRelease: GithubRelease?,
    onNavigateToResult: (String, String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val defaultTab by viewModel.defaultOpenTab.collectAsState()
    var currentTab by rememberSaveable(defaultTab) { mutableStateOf(defaultTab) }
    val checkUpdatesEnabled by viewModel.checkUpdatesOnStart.collectAsState()
    val isSlimNav by viewModel.isSlimNav.collectAsState()
    val bottomBarHeight = if (isSlimNav) 80.dp else 96.dp
    val userAvatar by viewModel.userAvatar.collectAsState(initial = null)
    val userName by viewModel.userName.collectAsState()
    val userBio by viewModel.userBio.collectAsState()
    val historyList by viewModel.historyList.collectAsState()
    val defaultFilter by viewModel.defaultHistoryFilter.collectAsState()
    var currentFilter by rememberSaveable(defaultFilter) { mutableStateOf(defaultFilter) }

    val filteredCount = remember(historyList, currentFilter) {
        when (currentFilter) {
            TrackingFilter.IN_TRANSIT -> historyList.count { !it.lastStatus.isDeliveredStatus() }
            TrackingFilter.DELIVERED -> historyList.count { it.lastStatus.isDeliveredStatus() }
            TrackingFilter.ALL -> historyList.size
        }
    }
    val name by viewModel.userName.collectAsState()
    val bio by viewModel.userBio.collectAsState()
    var showProfileDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    if (showEditDialog) {
        EditProfileDialog(
            currentName = name,
            currentBio = bio,
            onDismiss = { showEditDialog = false },
            onSave = { newName, newBio ->
                viewModel.updateProfile(newName, newBio)
                showEditDialog = false
            }
        )
    }
    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.updateAvatar(it.toString()) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                currentTab = currentTab,
                packageCount = filteredCount,
                userAvatar = userAvatar,
                onAvatarClick = { showProfileDialog = true },
                onSettingsClick = onNavigateToSettings
            )
        },
        bottomBar = {
            NavigationBar(modifier = Modifier.height(bottomBarHeight)) {
                AppDestinations.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = destination == currentTab,
                        onClick = { currentTab = destination },
                        label = if (isSlimNav) null else { { Text(stringResource(destination.label)) }},
                        alwaysShowLabel = !isSlimNav,
                        icon = {
                            val icon = destination.icon
                            when (icon) {
                                is ImageVector -> {
                                    Icon(imageVector = icon, contentDescription = null)
                                }
                                is Int -> {
                                    Icon(painter = painterResource(id = icon), contentDescription = null)
                                }
                            }
                        }
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    val duration = 400
                    if (targetState.ordinal > initialState.ordinal) {
                        slideInHorizontally(animationSpec = tween(duration)) { it } + fadeIn(tween(duration)) togetherWith
                                slideOutHorizontally(animationSpec = tween(duration)) { -it } + fadeOut(tween(duration))
                    } else {
                        slideInHorizontally(animationSpec = tween(duration)) { -it } + fadeIn(tween(duration)) togetherWith
                                slideOutHorizontally(animationSpec = tween(duration)) { it } + fadeOut(tween(duration))
                    }
                },
                label = "TabTransition"
            ) { targetTab ->
                when (targetTab) {
                    AppDestinations.HISTORY -> {
                        HistoryScreen(
                            viewModel = viewModel,
                            currentFilter = currentFilter,
                            onFilterChange = { newFilter -> currentFilter = newFilter },
                            onNavigateToResult = { code -> onNavigateToResult(code, "Auto") }
                        )
                    }
                    AppDestinations.SEARCH -> {
                        SearchScreen(
                            onNavigateToResult = onNavigateToResult,

                            )
                    }
                    AppDestinations.CHARTS -> {
                        StatisticsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
    if (showProfileDialog) {
        ProfileDialog(
            userName = userName,
            userBio = userBio,
            userAvatar = userAvatar,
            onDismiss = { showProfileDialog = false },
            onSettingsClick = {
                showProfileDialog = false
            },
            onMakeBackup = {
                showProfileDialog = false
                // Lógica para chamar o backup (ou navegar pra tela de backup)
                // Ex: navController.navigate(Routes.BACKUP)
            },
            onRestoreBackup = {
                showProfileDialog = false
                // Lógica para restaurar
            },
            onViewHistory = {
                showProfileDialog = false
                // Lógica para ver histórico
            },
            onPhotoClick = {
                photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onEditProfileClick = {
                showEditDialog = true
            }
        )
    }
}

enum class AppDestinations(
    val label: Int,
    val icon: Any,
) {
    HISTORY(R.string.home_history, Icons.Outlined.History),
    SEARCH(R.string.home_search, Icons.Default.Search),
    CHARTS(R.string.home_stats, R.drawable.ic_charts),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(
    currentTab: AppDestinations,
    packageCount: Int,
    userAvatar: String?,
    onAvatarClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val title = when (currentTab) {
        AppDestinations.SEARCH -> stringResource(R.string.search_tab_label)
        AppDestinations.HISTORY -> stringResource(R.string.packages)
        AppDestinations.CHARTS -> "Estatísticas"
        else -> "Kaeru"
    }

    TopAppBar(
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (currentTab == AppDestinations.HISTORY) {
                if (packageCount > 0) {
                    IconButton(
                        onClick = onAvatarClick,
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(26.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    if (packageCount > 99) "99+" else packageCount.toString(),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (packageCount > 99) 9.sp else 12.sp
                                )
                            }
                        }
                    }
                }
            } else {
                IconButton(
                    onClick = onAvatarClick,
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(26.dp)
                    ) {
                        if (!userAvatar.isNullOrEmpty()) {
                            AsyncImage(
                                model = userAvatar,
                                contentDescription = "Perfil",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Perfil",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }
                }
            }
        },
    )
}
