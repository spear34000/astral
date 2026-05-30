package pics.spear.astral

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.text.font.FontWeight
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
import pics.spear.astral.engine.ApiServer
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
    private lateinit var apiServer: ApiServer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        botStore = BotStore(this)
        InboxStore(this)
        logStore = LogStore(this)
        scriptEngine = ScriptEngine(this, botStore, logStore, BotApiProvider.getInstance(this))
        apiServer = ApiServer(botStore, scriptEngine, BotApiProvider.getInstance(this))

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
                    AstralNavHost(botStore = botStore, logStore = logStore, apiServer = apiServer)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        apiServer.stop()
        scriptEngine.onDestroy()
    }
}

@Composable
fun AstralNavHost(botStore: BotStore, logStore: LogStore, apiServer: ApiServer? = null) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route in Screen.tabs.map { it.route }

    val bottomBarHeight = 64.dp

    Scaffold(
        containerColor = SpaceBlack,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = SpaceSurface,
                    contentColor = TextPrimary,
                    tonalElevation = 0.dp,
                    modifier = Modifier.height(bottomBarHeight),
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
                                    tint = if (selected) AstralBlue else TextTertiary,
                                    modifier = Modifier.size(22.dp),
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = AstralBlue.copy(alpha = 0.1f)),
                            label = {
                                Text(
                                    screen.title,
                                    color = if (selected) AstralBlue else TextTertiary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                )
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
            enterTransition = { fadeIn(initialAlpha = 0.2f) },
            exitTransition = { fadeOut(targetAlpha = 0.2f) },
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    botStore = botStore,
                    logStore = logStore,
                    apiServer = apiServer,
                    onNavigateToBots = { navController.navigate(Routes.BOTS) },
                    onNavigateToLogs = { navController.navigate(Routes.LOGS) },
                    onNavigateToFlow = { navController.navigate(Routes.FLOW) },
                )
            }
            composable(Routes.FLOW) {
                FlowScreen(onBack = { navController.popBackStack() })
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
                SettingsScreen(apiServer = apiServer)
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
