package com.chatnova.ai.ui.navigation

sealed class Screen(val route: String) {
    object Chat : Screen("chat?conversationId={conversationId}") {
        fun createRoute(conversationId: String? = null) = if (conversationId != null) "chat?conversationId=$conversationId" else "chat"
    }
    object History : Screen("history")
    object Models : Screen("models")
    object ApiKey : Screen("api_key")
    object CustomInstructions : Screen("custom_instructions")
    object Settings : Screen("settings")
    object Welcome : Screen("welcome")
}
