package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MeetFlowViewModel
import com.example.ui.navigation.MeetFlowAppShell
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.MeetFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MeetFlowViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            val darkTheme = uiState.session.darkModeOverride ?: isSystemInDarkTheme()

            MeetFlowTheme(darkTheme = darkTheme) {
                when {
                    !uiState.isReady -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    uiState.currentUser == null -> {
                        AuthScreen(
                            profiles = uiState.allProfiles,
                            authError = uiState.authError,
                            onClearError = { viewModel.clearAuthError() },
                            onSignIn = { email, pass -> viewModel.signIn(email, pass) },
                            onSignUp = { name, email, pass, role -> viewModel.signUp(name, email, pass, role) },
                            onResetPassword = { email, pass -> viewModel.resetPassword(email, pass) },
                            onQuickDemoUser = { id -> viewModel.quickDemoSignIn(id) }
                        )
                    }
                    else -> {
                        MeetFlowAppShell(
                            uiState = uiState,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}
