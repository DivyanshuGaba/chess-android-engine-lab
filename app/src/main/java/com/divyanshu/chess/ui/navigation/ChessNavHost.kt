package com.divyanshu.chess.ui.navigation

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.divyanshu.chess.ui.components.ChessIcons
import com.divyanshu.chess.ui.game.GameScreen
import com.divyanshu.chess.ui.game.GameViewModel
import com.divyanshu.chess.ui.history.HistoryScreen
import com.divyanshu.chess.ui.home.HomeScreen
import com.divyanshu.chess.ui.lab.EngineLabScreen
import com.divyanshu.chess.ui.learn.LearnScreen
import com.divyanshu.chess.ui.spectator.SpectatorScreen

private const val PLAY = "play"
private const val WATCH = "watch"
private const val LEARN = "learn"
private const val LEARN_ROUTE = "learn?topic={topic}"
private const val HISTORY = "history"
private const val LAB = "lab"
private const val GAME = "game/{${GameViewModel.GAME_ID_ARG}}"

private class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(PLAY, "Play", ChessIcons.Play),
    Tab(WATCH, "Watch", ChessIcons.Watch),
    Tab(LEARN, "Learn", ChessIcons.Learn),
    Tab(HISTORY, "History", ChessIcons.History),
    Tab(LAB, "Lab", ChessIcons.Lab),
)

/** The app's root: a bottom bar for the five main areas, and the game itself in full screen. */
@Composable
fun ChessApp() {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val current = entry?.destination?.route?.substringBefore('?')
    val showBar = tabs.any { it.route == current }

    fun openGame(id: Long, replaceCurrent: Boolean = false) {
        navController.navigate("game/$id") {
            if (replaceCurrent) popUpTo(GAME) { inclusive = true }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = current == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                            modifier = Modifier.testTag("tab_${tab.route}"),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = PLAY,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            composable(PLAY) { HomeScreen(onOpenGame = { openGame(it) }) }
            composable(WATCH) { SpectatorScreen() }
            composable(
                route = LEARN_ROUTE,
                arguments = listOf(
                    navArgument("topic") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { backStack -> LearnScreen(topicId = backStack.arguments?.getString("topic")) }
            composable(HISTORY) { HistoryScreen(onOpenGame = { openGame(it) }) }
            composable(LAB) { EngineLabScreen() }
            composable(
                route = GAME,
                arguments = listOf(navArgument(GameViewModel.GAME_ID_ARG) { type = NavType.LongType }),
            ) {
                GameScreen(
                    onBack = { navController.popBackStack() },
                    onLearn = { topic -> navController.navigate("learn?topic=$topic") },
                    onOpenGame = { openGame(it, replaceCurrent = true) },
                )
            }
        }
    }
}
