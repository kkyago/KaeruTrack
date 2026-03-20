package com.kaeru.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
                sharedPreferences.edit().putString("last_seen_version", currentVersion).apply()
            } //checagem de att ao abrir (essencial pra badge e changelogs)

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

    Scaffold(
        bottomBar = {
            NavigationBar(modifier = Modifier.height(bottomBarHeight)) {
                AppDestinations.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = destination == currentTab,
                        onClick = { currentTab = destination },
                        label = if (isSlimNav) null else { { Text(stringResource(destination.label)) }},
                        alwaysShowLabel = !isSlimNav,
                        icon = {
                            BadgedBox(
                                badge = {
                                        if (destination == AppDestinations.PROFILE && updateRelease != null && checkUpdatesEnabled) {
                                            Badge(containerColor = MaterialTheme.colorScheme.error)
                                        }
                                }
                            ) {
                                Icon(destination.icon, contentDescription = null)
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
                            onNavigateToResult = { code -> onNavigateToResult(code, "Auto") }
                        )
                    }
                    AppDestinations.SEARCH -> {
                        SearchScreen(
                            onNavigateToResult = onNavigateToResult
                        )
                    }
                    AppDestinations.PROFILE -> {
                        ProfileScreen(
                            viewModel = viewModel,
                            updateRelease = updateRelease,
                            onSettingsClick = onNavigateToSettings,
                        )
                    }
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: Int,
    val icon: ImageVector,
) {
    HISTORY(R.string.home_history, Icons.Outlined.History),
    SEARCH(R.string.home_search, Icons.Default.Search),
    PROFILE(R.string.home_profile, Icons.Default.Person),
}
