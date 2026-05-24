package com.example.chat.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.chat.ui.screen.ChatScreen
import com.example.chat.ui.screen.EditProfileScreen
import com.example.chat.ui.screen.LoginScreen
import com.example.chat.ui.screen.RegisterScreen
import com.example.chat.ui.screen.SettingsScreen
import com.example.chat.ui.viewmodel.AuthState
import com.example.chat.ui.viewmodel.AuthViewModel
import com.example.chat.ui.viewmodel.ChatViewModel

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val CHAT = "chat"
    const val SETTINGS = "settings"
    const val EDIT_PROFILE = "edit_profile"
}

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel(),
) {
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val chatViewModel: ChatViewModel = viewModel()

    if (isLoggedIn == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn == true) Routes.CHAT else Routes.LOGIN,
    ) {
        composable(Routes.LOGIN) {
            val authState by authViewModel.authState.collectAsState()

            LoginScreen(
                onLogin = { email, password ->
                    authViewModel.login(email, password)
                },
                onNavigateToRegister = {
                    authViewModel.resetAuthState()
                    navController.navigate(Routes.REGISTER)
                },
                isLoading = authState is AuthState.Loading,
                error = (authState as? AuthState.Error)?.message,
            )

            LaunchedEffect(authState) {
                if (authState is AuthState.Authenticated) {
                    navController.navigate(Routes.CHAT) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }

        composable(Routes.REGISTER) {
            val authState by authViewModel.authState.collectAsState()

            RegisterScreen(
                onRegister = { email, name, password, confirmPassword ->
                    authViewModel.register(email, name, password, confirmPassword)
                },
                onNavigateToLogin = {
                    authViewModel.resetAuthState()
                    navController.popBackStack()
                },
                isLoading = authState is AuthState.Loading,
                error = (authState as? AuthState.Error)?.message,
            )

            LaunchedEffect(authState) {
                if (authState is AuthState.Authenticated) {
                    navController.navigate(Routes.CHAT) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }

        composable(Routes.CHAT) {
            val conversations by chatViewModel.conversations.collectAsState()
            val currentId by chatViewModel.currentConversationId.collectAsState()
            val messages by chatViewModel.messages.collectAsState()
            val inputText by chatViewModel.inputText.collectAsState()
            val isLoading by chatViewModel.isLoading.collectAsState()
            val streamingContent by chatViewModel.streamingContent.collectAsState()
            val selectedImageDataUrl by chatViewModel.selectedImageDataUrl.collectAsState()
            val error by chatViewModel.error.collectAsState()

            ChatScreen(
                conversations = conversations,
                currentConversationId = currentId,
                messages = messages,
                inputText = inputText,
                isLoading = isLoading,
                streamingContent = streamingContent,
                error = error,
                onClearError = { chatViewModel.clearError() },
                onSelectConversation = { chatViewModel.selectConversation(it) },
                onNewConversation = { chatViewModel.newConversation() },
                onDeleteConversation = { chatViewModel.deleteConversation(it) },
                onInputChange = { chatViewModel.onInputChange(it) },
                onSendMessage = { chatViewModel.sendMessage() },
                selectedImageDataUrl = selectedImageDataUrl,
                onImageSelected = { chatViewModel.setSelectedImage(it) },
                onClearSelectedImage = { chatViewModel.clearSelectedImage() },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                onRegenerate = { chatViewModel.regenerateLastResponse() },
            )
        }

        composable(Routes.SETTINGS) {
            val selectedModel by chatViewModel.selectedModel.collectAsState()
            val baseUrl by chatViewModel.baseUrl.collectAsState()
            val apiKey by chatViewModel.apiKey.collectAsState()

            SettingsScreen(
                currentBaseUrl = baseUrl,
                currentApiKey = apiKey,
                currentModelName = selectedModel,
                onSaveConfig = { config ->
                    chatViewModel.updateApiConfig(
                        baseUrl = config.baseUrl,
                        apiKey = config.apiKey,
                        model = config.modelName,
                    )
                },
                onEditProfile = { navController.navigate(Routes.EDIT_PROFILE) },
                onLogout = {
                    authViewModel.logout()
                    authViewModel.resetAuthState()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.EDIT_PROFILE) {
            val authState by authViewModel.authState.collectAsState()
            val userName by authViewModel.userName.collectAsState()
            val userEmail by authViewModel.userEmail.collectAsState()
            var pendingBackAfterSave by remember { mutableStateOf(false) }

            EditProfileScreen(
                currentName = userName,
                currentEmail = userEmail,
                isLoading = authState is AuthState.Loading,
                error = (authState as? AuthState.Error)?.message,
                onSave = { name, email ->
                    pendingBackAfterSave = true
                    authViewModel.updateUserProfile(name, email)
                },
                onBack = { navController.popBackStack() },
            )

            LaunchedEffect(authState, pendingBackAfterSave) {
                if (pendingBackAfterSave && authState is AuthState.Idle) {
                    navController.popBackStack()
                    pendingBackAfterSave = false
                }
                if (authState is AuthState.Error) {
                    pendingBackAfterSave = false
                }
            }
        }
    }
}
