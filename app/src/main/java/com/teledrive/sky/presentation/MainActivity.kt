package com.teledrive.sky.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.teledrive.sky.domain.repository.AuthRepository
import com.teledrive.sky.presentation.navigation.TeleDriveNavGraph
import com.teledrive.sky.ui.theme.TeleDriveTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep splash screen while loading auth state
        splashScreen.setKeepOnScreenCondition {
            viewModel.isLoading.value
        }

        setContent {
            TeleDriveTheme {
                val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
                val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

                if (!isLoading) {
                    TeleDriveNavGraph(
                        isLoggedIn = isLoggedIn ?: false
                    )
                }
            }
        }
    }
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val isLoggedIn = authRepository.isLoggedIn()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            isLoggedIn.collect { value ->
                if (value != null) {
                    _isLoading.value = false
                }
            }
        }
    }
}
