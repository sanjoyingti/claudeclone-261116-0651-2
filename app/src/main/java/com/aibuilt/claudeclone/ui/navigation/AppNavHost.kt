package com.aibuilt.claudeclone.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aibuilt.claudeclone.AppContainer
import com.aibuilt.claudeclone.ui.artifacts.ArtifactDetailScreen
import com.aibuilt.claudeclone.ui.artifacts.ArtifactsScreen
import com.aibuilt.claudeclone.ui.chat.ChatScreen
import com.aibuilt.claudeclone.ui.home.HomeScreen
import com.aibuilt.claudeclone.ui.projects.ProjectDetailScreen
import com.aibuilt.claudeclone.ui.projects.ProjectsScreen
import com.aibuilt.claudeclone.ui.settings.SettingsScreen
import com.aibuilt.claudeclone.ui.splash.SplashScreen

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Destinations.HOME, "Chats", Icons.Filled.ChatBubbleOutline),
    Tab(Destinations.ARTIFACTS, "Artifacts", Icons.Filled.Code),
    Tab(Destinations.PROJECTS, "Projects", Icons.Filled.Folder),
    Tab(Destinations.SETTINGS, "Settings", Icons.Filled.Settings)
)

@Composable
fun AppNavHost(container: AppContainer, navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in Destinations.topLevel

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                if (currentRoute != tab.route) {
                                    navController.navigate(tab.route) {
                                        popUpTo(Destinations.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destinations.SPLASH,
            modifier = Modifier.padding(padding)
        ) {
            composable(Destinations.SPLASH) {
                SplashScreen(
                    onFinish = {
                        navController.navigate(Destinations.HOME) {
                            popUpTo(Destinations.SPLASH) { inclusive = true }
                        }
                    }
                )
            }

            composable(Destinations.HOME) {
                HomeScreen(
                    container = container,
                    onOpenChat = { id -> navController.navigate(Destinations.chat(id)) },
                    onOpenProjects = { navController.navigate(Destinations.PROJECTS) }
                )
            }

            composable(
                route = Destinations.CHAT,
                arguments = listOf(navArgument(Destinations.ARG_CONVERSATION_ID) { type = NavType.StringType })
            ) { entry ->
                val conversationId = entry.arguments?.getString(Destinations.ARG_CONVERSATION_ID).orEmpty()
                ChatScreen(
                    container = container,
                    conversationId = conversationId,
                    onBack = { navController.popBackStack() },
                    onOpenArtifact = { id -> navController.navigate(Destinations.artifact(id)) }
                )
            }

            composable(Destinations.ARTIFACTS) {
                ArtifactsScreen(
                    container = container,
                    onOpenArtifact = { id -> navController.navigate(Destinations.artifact(id)) }
                )
            }

            composable(
                route = Destinations.ARTIFACT,
                arguments = listOf(navArgument(Destinations.ARG_ARTIFACT_ID) { type = NavType.StringType })
            ) { entry ->
                val artifactId = entry.arguments?.getString(Destinations.ARG_ARTIFACT_ID).orEmpty()
                ArtifactDetailScreen(
                    container = container,
                    artifactId = artifactId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Destinations.PROJECTS) {
                ProjectsScreen(
                    container = container,
                    onOpenProject = { id -> navController.navigate(Destinations.project(id)) },
                    onNewProject = { navController.navigate(Destinations.project("new")) }
                )
            }

            composable(
                route = Destinations.PROJECT,
                arguments = listOf(navArgument(Destinations.ARG_PROJECT_ID) { type = NavType.StringType })
            ) { entry ->
                val projectId = entry.arguments?.getString(Destinations.ARG_PROJECT_ID).orEmpty()
                ProjectDetailScreen(
                    container = container,
                    projectId = projectId,
                    onBack = { navController.popBackStack() },
                    onOpenChat = { id -> navController.navigate(Destinations.chat(id)) }
                )
            }

            composable(Destinations.SETTINGS) {
                SettingsScreen(container = container)
            }
        }
    }
}
