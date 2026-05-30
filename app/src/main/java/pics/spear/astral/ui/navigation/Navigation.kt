package pics.spear.astral.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    data object Home : Screen("home", "홈", Icons.Filled.Home, Icons.Outlined.Home)
    data object Flow : Screen("flow", "플로우", Icons.Filled.AccountTree, Icons.Outlined.AccountTree)
    data object Bots : Screen("bots", "봇", Icons.Filled.SmartToy, Icons.Outlined.SmartToy)
    data object Logs : Screen("logs", "로그", Icons.Filled.Terminal, Icons.Outlined.Terminal)
    data object Settings : Screen("settings", "설정", Icons.Filled.Settings, Icons.Outlined.Settings)

    companion object {
        val tabs = listOf(Home, Flow, Bots, Logs, Settings)
    }
}

object Routes {
    const val HOME = "home"
    const val FLOW = "flow"
    const val FLOW_EDITOR = "flow/{flowId}"
    const val BOTS = "bots"
    const val LOGS = "logs"
    const val SETTINGS = "settings"
    const val EDITOR = "editor/{botId}"

    fun editor(botId: String) = "editor/$botId"
    fun flowEditor(flowId: String) = "flow/$flowId"
}
