package pics.spear.astral

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import pics.spear.astral.engine.ScriptEngine
import pics.spear.astral.service.AstralNotificationListener
import pics.spear.astral.service.BotApiProvider
import pics.spear.astral.store.BotStore
import pics.spear.astral.store.InboxStore
import pics.spear.astral.store.LogStore
import pics.spear.astral.ui.navigation.Routes
import pics.spear.astral.ui.navigation.Screen
import pics.spear.astral.ui.screens.*
import pics.spear.astral.ui.theme.*

class MainActivity : ComponentActivity() {
    private lateinit var botStore: BotStore
    private lateinit var logStore: LogStore
    private lateinit var scriptEngine: ScriptEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        botStore = BotStore(this)
        InboxStore(this)
        logStore = LogStore(this)
        scriptEngine = ScriptEngine(this, botStore, logStore, BotApiProvider.getInstance(this))

        scriptEngine.initialize()
        botStore.bots.value.filter { it.enabled }.forEach { scriptEngine.startBot(it) }

        val mainScope = MainScope()
        mainScope.launch {
            botStore.bots.collect { bots ->
                bots.forEach { bot ->
                    val running = scriptEngine.isRunning(bot.id)
                    if (bot.enabled && !running) scriptEngine.startBot(bot)
                    else if (!bot.enabled && running) scriptEngine.stopBot(bot.id)
                }
            }
        }

        scriptEngine.startListening(mainScope)
        startService(Intent(this, AstralNotificationListener::class.java))

        setContent {
            AstralTheme {
                var showSplash by remember { mutableStateOf(true) }
                if (showSplash) {
                    SplashScreen(onFinished = { showSplash = false })
                } else {
                    AstralNavHost(botStore = botStore, logStore = logStore)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scriptEngine.onDestroy()
    }
}

@Composable
fun AstralNavHost(botStore: BotStore, logStore: LogStore) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route in Screen.tabs.map { it.route }

    Scaffold(
        containerColor = SpaceBlack,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = SpaceSurface,
                    contentColor = TextPrimary,
                    tonalElevation = 0.dp,
                ) {
                    Screen.tabs.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title,
                                    tint = if (selected) Blue60 else TextTertiary,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = Blue60.copy(alpha = 0.12f)),
                            label = {
                                Text(screen.title, color = if (selected) Blue60 else TextTertiary, style = MaterialTheme.typography.labelSmall)
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(initialAlpha = 0.3f) },
            exitTransition = { fadeOut(targetAlpha = 0.3f) },
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    botStore = botStore,
                    onNavigateToBots = { navController.navigate(Routes.BOTS) },
                    onNavigateToLogs = { navController.navigate(Routes.LOGS) },
                )
            }
            composable(Routes.BOTS) {
                BotsScreen(
                    botStore = botStore,
                    onEditBot = { botId -> navController.navigate(Routes.editor(botId)) },
                )
            }
            composable(Routes.LOGS) {
                LogsScreen(logStore = logStore)
            }
            composable(Routes.SETTINGS) {
                SettingsScreen()
            }
            composable(
                route = Routes.EDITOR,
                arguments = listOf(navArgument("botId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val botId = backStackEntry.arguments?.getString("botId") ?: return@composable
                EditorScreen(botId = botId, botStore = botStore, onBack = { navController.popBackStack() })
            }
        }
    }
}
