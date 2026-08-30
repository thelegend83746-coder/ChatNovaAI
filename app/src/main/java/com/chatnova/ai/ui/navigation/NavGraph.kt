package com.chatnova.ai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.chatnova.ai.di.AppContainer
import com.chatnova.ai.ui.apikey.ApiKeyScreen
import com.chatnova.ai.ui.apikey.ApiKeyViewModel
import com.chatnova.ai.ui.chat.ChatScreen
import com.chatnova.ai.ui.chat.ChatViewModel
import com.chatnova.ai.ui.history.HistoryScreen
import com.chatnova.ai.ui.history.HistoryViewModel
import com.chatnova.ai.ui.instructions.CustomInstructionsScreen
import com.chatnova.ai.ui.instructions.CustomInstructionsViewModel
import com.chatnova.ai.ui.models.ModelsScreen
import com.chatnova.ai.ui.models.ModelsViewModel
import com.chatnova.ai.ui.settings.SettingsScreen
import com.chatnova.ai.ui.settings.SettingsViewModel
import com.chatnova.ai.ui.welcome.WelcomeScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    appContainer: AppContainer,
    startDestination: String = Screen.Chat.route,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onGetStarted = {
                    navController.navigate(Screen.Chat.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onConfigureApiKey = {
                    navController.navigate(Screen.ApiKey.route)
                }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("conversationId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId")
            val viewModel: ChatViewModel = viewModel(
                factory = ChatViewModel.provideFactory(
                    appContainer.chatRepository,
                    appContainer.modelRepository,
                    appContainer.settingsRepository
                )
            )

            ChatScreen(
                viewModel = viewModel,
                conversationId = conversationId,
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToModels = { navController.navigate(Screen.Models.route) },
                onNavigateToApiKey = { navController.navigate(Screen.ApiKey.route) },
                onNavigateToInstructions = { navController.navigate(Screen.CustomInstructions.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.History.route) {
            val viewModel: HistoryViewModel = viewModel(
                factory = HistoryViewModel.provideFactory(appContainer.chatRepository)
            )

            HistoryScreen(
                viewModel = viewModel,
                onSelectConversation = { convId ->
                    navController.navigate(Screen.Chat.createRoute(convId)) {
                        popUpTo(Screen.Chat.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Models.route) {
            val viewModel: ModelsViewModel = viewModel(
                factory = ModelsViewModel.provideFactory(
                    appContainer.modelRepository,
                    appContainer.settingsRepository
                )
            )

            ModelsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ApiKey.route) {
            val viewModel: ApiKeyViewModel = viewModel(
                factory = ApiKeyViewModel.provideFactory(appContainer.settingsRepository)
            )

            ApiKeyScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CustomInstructions.route) {
            val viewModel: CustomInstructionsViewModel = viewModel(
                factory = CustomInstructionsViewModel.provideFactory(appContainer.settingsRepository)
            )

            CustomInstructionsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.provideFactory(
                    appContainer.settingsRepository,
                    appContainer.chatRepository
                )
            )

            SettingsScreen(
                viewModel = viewModel,
                onNavigateToApiKey = { navController.navigate(Screen.ApiKey.route) },
                onNavigateToInstructions = { navController.navigate(Screen.CustomInstructions.route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
