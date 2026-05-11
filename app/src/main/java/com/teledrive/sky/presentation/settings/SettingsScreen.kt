package com.teledrive.sky.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.teledrive.sky.domain.repository.AuthRepository
import com.teledrive.sky.domain.model.UserProfile
import com.teledrive.sky.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val authRepository: AuthRepository) : ViewModel() {
    val userProfile = authRepository.getUserProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onDone()
        }
    }
}

@Composable
fun SettingsScreen(onLogout: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    var showLogoutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().background(SkyBackground)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Text(
            "Pengaturan",
            style = MaterialTheme.typography.headlineSmall,
            color = SkyTextPrimary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )

        HorizontalDivider(color = SkyDivider, thickness = 0.5.dp)

        // Profile card
        profile?.let { p ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                color = SkySurfaceElevated,
                shape = SkyShapes.FileCard,
                border = BorderStroke(1.dp, SkyBorder),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(
                        shape = SkyShapes.Dialog,
                        color = SkyBlueContainer,
                        modifier = Modifier.size(52.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                p.name.firstOrNull()?.uppercase() ?: "T",
                                style = MaterialTheme.typography.titleLarge,
                                color = SkyBlue,
                            )
                        }
                    }
                    Column {
                        Text(p.name, style = MaterialTheme.typography.titleMedium, color = SkyTextPrimary)
                        Text(p.email, style = MaterialTheme.typography.bodySmall, color = SkyTextSecondary)
                    }
                }
            }
        }

        // Settings items
        SettingsGroup("Akun") {
            SettingsItem(Icons.Default.Security, "Keamanan", "Kunci Biometrik & PIN") {}
            SettingsItem(Icons.Default.Storage, "Penyimpanan", "Kelola cache lokal") {}
            SettingsItem(Icons.Default.Notifications, "Notifikasi", "Transfer & pembaruan") {}
        }

        SettingsGroup("Tampilan") {
            SettingsItem(Icons.Default.DarkMode, "Tema", "Gelap (default)") {}
            SettingsItem(Icons.Default.Language, "Bahasa", "Indonesia") {}
        }

        SettingsGroup("Informasi") {
            SettingsItem(Icons.Default.Info, "Versi", "1.0.0") {}
            SettingsItem(Icons.Default.Help, "Bantuan", "FAQ & dukungan") {}
        }

        Spacer(Modifier.height(16.dp))

        // Logout button
        TextButton(
            onClick = { showLogoutDialog = true },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            Icon(Icons.Default.Logout, null, tint = SkyError)
            Spacer(Modifier.width(8.dp))
            Text("Keluar", color = SkyError, style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(80.dp))
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Keluar?", color = SkyTextPrimary) },
            text = { Text("Anda akan keluar dari akun ini.", color = SkyTextSecondary) },
            confirmButton = {
                TextButton(onClick = { showLogoutDialog = false; viewModel.logout(onLogout) }) {
                    Text("Keluar", color = SkyError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Batal", color = SkyTextSecondary)
                }
            },
            containerColor = SkySurfaceElevated,
            shape = SkyShapes.Dialog,
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = SkyTextTertiary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            color = SkySurfaceElevated,
            shape = SkyShapes.FileCard,
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun SettingsItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = SkySurfaceElevated,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(icon, null, tint = SkyBlue, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = SkyTextPrimary)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = SkyTextTertiary)
            }
            Icon(Icons.Default.ChevronRight, null, tint = SkyTextTertiary, modifier = Modifier.size(18.dp))
        }
    }
    HorizontalDivider(color = SkyDivider, thickness = 0.3.dp, modifier = Modifier.padding(start = 52.dp))
}
