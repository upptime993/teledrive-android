package com.teledrive.sky.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.teledrive.sky.presentation.auth.LoginScreen
import com.teledrive.sky.presentation.auth.RegisterScreen
import com.teledrive.sky.presentation.main.MainShellScreen

// ─── App Routes ──────────────────────────────────────────────────────────────
object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAIN = "main"
    const val FILES = "files?folderId={folderId}"
    const val SEARCH = "search"
    const val STARRED = "starred"
    const val TRASH = "trash"
    const val SETTINGS = "settings"
    const val FILE_DETAIL = "file/{fileId}"
    const val SHARE = "share/{fileId}"
    const val UPLOAD = "upload/{folderId}"
    const val APP_LOCK = "app_lock"

    fun filesRoute(folderId: String? = null) = if (folderId != null) "files?folderId=$folderId" else "files"
    fun fileDetailRoute(fileId: String) = "file/$fileId"
    fun shareRoute(fileId: String) = "share/$fileId"
}

@Composable
fun TeleDriveNavGraph(isLoggedIn: Boolean) {
    val navController = rememberNavController()
    val startDestination = if (isLoggedIn) Routes.MAIN else Routes.LOGIN

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(280)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(220)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(280)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(220)
            )
        },
    ) {
        // ── Auth ─────────────────────────────────────────────────────────────
        composable(
            route = Routes.LOGIN,
            enterTransition = { fadeIn(tween(300)) },
            exitTransition = { fadeOut(tween(200)) },
        ) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                onLoginSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.REGISTER,
        ) {
            RegisterScreen(
                onNavigateToLogin = { navController.navigateUp() },
                onRegisterSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        // ── Main Shell (contains BottomNav + nested graphs) ───────────────────
        composable(
            route = Routes.MAIN,
            enterTransition = { fadeIn(tween(400)) },
            exitTransition = { fadeOut(tween(300)) },
        ) {
            MainShellScreen(
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
