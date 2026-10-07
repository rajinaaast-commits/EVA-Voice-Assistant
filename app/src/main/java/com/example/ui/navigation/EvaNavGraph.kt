package com.example.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.EvaApplication
import com.example.ui.chat.ChatScreen
import com.example.ui.coding.CodingAgentScreen
import com.example.ui.home.HomeScreen
import com.example.ui.onboarding.Step1ApiSetupScreen
import com.example.ui.onboarding.Step2PermissionsScreen
import com.example.ui.onboarding.Step3BackgroundScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.study.StudyScreen
import com.example.ui.tools.ToolsHubScreen
import com.example.ui.website.WebsiteBuilderScreen

sealed class Screen(val route: String) {
    object Step1ApiSetup : Screen("step1_api_setup")
    object Step2Permissions : Screen("step2_permissions")
    object Step3Background : Screen("step3_background")
    object Home : Screen("home")
    object Chat : Screen("chat?prompt={prompt}") {
        fun createRoute(prompt: String? = null): String {
            return if (!prompt.isNullOrBlank()) "chat?prompt=$prompt" else "chat"
        }
    }
    object Study : Screen("study")
    object Coding : Screen("coding")
    object Website : Screen("website")
    object Tools : Screen("tools?tab={tab}") {
        fun createRoute(tab: Int = 0): String = "tools?tab=$tab"
    }
    object Settings : Screen("settings")
}

@Composable
fun EvaNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val app = EvaApplication.instance
    val isOnboarded = app.preferences.isOnboardingCompleted() && app.aiProviderManager.hasAnyConfigured()
    val startDestination = if (isOnboarded) Screen.Home.route else Screen.Step1ApiSetup.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Step1ApiSetup.route) {
            Step1ApiSetupScreen(
                onContinue = {
                    navController.navigate(Screen.Step2Permissions.route)
                }
            )
        }

        composable(Screen.Step2Permissions.route) {
            Step2PermissionsScreen(
                onContinue = {
                    navController.navigate(Screen.Step3Background.route)
                }
            )
        }

        composable(Screen.Step3Background.route) {
            Step3BackgroundScreen(
                onFinish = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Step1ApiSetup.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToChat = { prompt ->
                    navController.navigate(Screen.Chat.createRoute(prompt))
                },
                onNavigateToStudy = {
                    navController.navigate(Screen.Study.route)
                },
                onNavigateToCoding = {
                    navController.navigate(Screen.Coding.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToWebsite = {
                    navController.navigate(Screen.Website.route)
                }
            )
        }

        composable(Screen.Chat.route) { backStackEntry ->
            val prompt = backStackEntry.arguments?.getString("prompt")
            ChatScreen(
                initialPrompt = prompt,
                onBack = { navController.popBackStack() }
            )
        }

        composable("chat") {
            ChatScreen(
                initialPrompt = null,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Study.route) {
            StudyScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Coding.route) {
            CodingAgentScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Website.route) {
            WebsiteBuilderScreen(onBack = { navController.popBackStack() })
        }

        composable("tools?tab={tab}") { backStackEntry ->
            val tabStr = backStackEntry.arguments?.getString("tab")
            val tab = tabStr?.toIntOrNull() ?: 0
            ToolsHubScreen(initialTab = tab, onBack = { navController.popBackStack() })
        }

        composable("tools") {
            ToolsHubScreen(initialTab = 0, onBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
