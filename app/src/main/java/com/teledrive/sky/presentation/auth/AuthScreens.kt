package com.teledrive.sky.presentation.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teledrive.sky.ui.theme.*

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.loginState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    // Auto-navigate on success
    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) onLoginSuccess()
    }

    // Animated background gradient
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        SkyBlue.copy(alpha = 0.15f + animatedOffset * 0.05f),
                        SkyBackground,
                        SkyBackground,
                    ),
                    center = Offset(0.3f, 0.2f),
                    radius = 900f,
                )
            )
    ) {
        // Decorative blur circle top-right
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = 120.dp, y = (-80).dp)
                .align(Alignment.TopEnd)
                .background(
                    Brush.radialGradient(
                        colors = listOf(SkyBlue.copy(0.25f), Color.Transparent),
                    ),
                    shape = CircleShape,
                )
                .blur(60.dp)
        )

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .windowInsetsPadding(WindowInsets.systemBars),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.height(40.dp))

            // Logo & Title
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(600)) + slideInVertically(
                    tween(600, easing = EmphasizedDecelerateEasing()),
                    initialOffsetY = { -it / 3 }
                ),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Logo circle
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(SkyShapes.FAB)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(SkyBlue, SkyBlueDark),
                                    start = Offset.Zero,
                                    end = Offset(80f, 80f),
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(44.dp),
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = "TeleDrive Sky",
                        style = MaterialTheme.typography.headlineMedium,
                        color = SkyTextPrimary,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Cloud storage berbasis Telegram",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SkyTextSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(48.dp))

            // Login Card
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(600, delayMillis = 200)) + slideInVertically(
                    tween(600, delayMillis = 200, easing = EmphasizedDecelerateEasing()),
                    initialOffsetY = { it / 3 }
                ),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = SkyShapes.Dialog,
                    color = SkySurfaceElevated,
                    border = BorderStroke(1.dp, SkyBorder),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = "Masuk",
                            style = MaterialTheme.typography.titleLarge,
                            color = SkyTextPrimary,
                        )

                        // Email Field
                        SkyTextField(
                            value = state.email,
                            onValueChange = viewModel::onEmailChange,
                            label = "Email",
                            placeholder = "nama@email.com",
                            leadingIcon = Icons.Default.Email,
                            isError = state.emailError != null,
                            errorMessage = state.emailError,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next,
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                        )

                        // Password Field
                        SkyPasswordField(
                            value = state.password,
                            onValueChange = viewModel::onPasswordChange,
                            label = "Kata Sandi",
                            isError = state.passwordError != null,
                            errorMessage = state.passwordError,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    viewModel.login()
                                }
                            ),
                        )

                        // Error message
                        AnimatedVisibility(visible = state.generalError != null) {
                            state.generalError?.let { error ->
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = SkyErrorContainer,
                                ) {
                                    Text(
                                        text = error,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SkyError,
                                        modifier = Modifier.padding(12.dp),
                                    )
                                }
                            }
                        }

                        // Login Button
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.login()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = SkyShapes.Button,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SkyBlue,
                                contentColor = Color.White,
                            ),
                            enabled = !state.isLoading,
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text(
                                    text = "Masuk",
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Register link
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(400, delayMillis = 400)),
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Belum punya akun? ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SkyTextSecondary,
                    )
                    TextButton(onClick = onNavigateToRegister) {
                        Text(
                            text = "Daftar",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SkyBlueLight,
                        )
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.registerState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) onRegisterSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyBackground)
    ) {
        Box(
            modifier = Modifier
                .size(250.dp)
                .offset(x = (-80).dp, y = 80.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(SkyAccent.copy(0.15f), Color.Transparent),
                    ),
                    shape = CircleShape,
                )
                .blur(50.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .windowInsetsPadding(WindowInsets.systemBars),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.height(40.dp))

            // Back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onNavigateToLogin) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Kembali",
                        tint = SkyTextSecondary,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = "Buat Akun",
                    style = MaterialTheme.typography.headlineMedium,
                    color = SkyTextPrimary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Bergabung dan mulai simpan file Anda",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SkyTextSecondary,
                )
            }

            Spacer(Modifier.height(32.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = SkyShapes.Dialog,
                color = SkySurfaceElevated,
                border = BorderStroke(1.dp, SkyBorder),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Name
                    SkyTextField(
                        value = state.name,
                        onValueChange = viewModel::onNameChange,
                        label = "Nama Lengkap",
                        placeholder = "Masukkan nama Anda",
                        leadingIcon = Icons.Default.Person,
                        isError = state.nameError != null,
                        errorMessage = state.nameError,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                    )

                    // Email
                    SkyTextField(
                        value = state.email,
                        onValueChange = viewModel::onRegisterEmailChange,
                        label = "Email",
                        placeholder = "nama@email.com",
                        leadingIcon = Icons.Default.Email,
                        isError = state.emailError != null,
                        errorMessage = state.emailError,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                    )

                    // Password
                    SkyPasswordField(
                        value = state.password,
                        onValueChange = viewModel::onRegisterPasswordChange,
                        label = "Kata Sandi",
                        isError = state.passwordError != null,
                        errorMessage = state.passwordError,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                viewModel.register()
                            }
                        ),
                    )

                    // Error
                    AnimatedVisibility(visible = state.generalError != null) {
                        state.generalError?.let { error ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = SkyErrorContainer,
                            ) {
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SkyError,
                                    modifier = Modifier.padding(12.dp),
                                )
                            }
                        }
                    }

                    // Register Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.register()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = SkyShapes.Button,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SkyBlue,
                            contentColor = Color.White,
                        ),
                        enabled = !state.isLoading,
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Daftar Sekarang", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Sudah punya akun? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SkyTextSecondary,
                )
                TextButton(onClick = onNavigateToLogin) {
                    Text(
                        text = "Masuk",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SkyBlueLight,
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ─── Reusable Form Components ────────────────────────────────────────────────

@Composable
fun SkyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            placeholder = { Text(placeholder, color = SkyTextTertiary) },
            leadingIcon = if (leadingIcon != null) {
                { Icon(leadingIcon, null, tint = if (isError) SkyError else SkyTextSecondary) }
            } else null,
            isError = isError,
            shape = SkyShapes.TextField,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SkyBorderFocused,
                unfocusedBorderColor = SkyBorder,
                focusedLabelColor = SkyBlueLight,
                unfocusedLabelColor = SkyTextSecondary,
                cursorColor = SkyBlue,
                focusedTextColor = SkyTextPrimary,
                unfocusedTextColor = SkyTextPrimary,
                errorBorderColor = SkyError,
                errorLabelColor = SkyError,
                focusedContainerColor = SkySurface,
                unfocusedContainerColor = SkySurface,
                errorContainerColor = SkyErrorContainer.copy(alpha = 0.3f),
            ),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = true,
        )
        AnimatedVisibility(visible = isError && errorMessage != null) {
            errorMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = SkyError,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                )
            }
        }
    }
}

@Composable
fun SkyPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    modifier: Modifier = Modifier,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            placeholder = { Text("Masukkan kata sandi", color = SkyTextTertiary) },
            leadingIcon = {
                Icon(Icons.Default.Lock, null, tint = if (isError) SkyError else SkyTextSecondary)
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Sembunyikan" else "Tampilkan",
                        tint = SkyTextSecondary,
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            isError = isError,
            shape = SkyShapes.TextField,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SkyBorderFocused,
                unfocusedBorderColor = SkyBorder,
                focusedLabelColor = SkyBlueLight,
                unfocusedLabelColor = SkyTextSecondary,
                cursorColor = SkyBlue,
                focusedTextColor = SkyTextPrimary,
                unfocusedTextColor = SkyTextPrimary,
                errorBorderColor = SkyError,
                errorLabelColor = SkyError,
                focusedContainerColor = SkySurface,
                unfocusedContainerColor = SkySurface,
                errorContainerColor = SkyErrorContainer.copy(alpha = 0.3f),
            ),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = true,
        )
        AnimatedVisibility(visible = isError && errorMessage != null) {
            errorMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = SkyError,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                )
            }
        }
    }
}

// Helper for easing
private fun EmphasizedDecelerateEasing(): Easing =
    CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
