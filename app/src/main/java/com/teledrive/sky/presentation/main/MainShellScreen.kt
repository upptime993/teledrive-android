package com.teledrive.sky.presentation.main

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.*
import com.teledrive.sky.presentation.files.FileBrowserScreen
import com.teledrive.sky.presentation.search.SearchScreen
import com.teledrive.sky.presentation.settings.SettingsScreen
import com.teledrive.sky.presentation.starred.StarredScreen
import com.teledrive.sky.presentation.trash.TrashScreen
import com.teledrive.sky.ui.theme.*

// ─── Bottom Navigation Items ─────────────────────────────────────────────────

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

val bottomNavItems = listOf(
    BottomNavItem("home", "Beranda", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem("search", "Cari", Icons.Filled.Search, Icons.Outlined.Search),
    BottomNavItem("starred", "Favorit", Icons.Filled.Star, Icons.Outlined.StarOutline),
    BottomNavItem("trash", "Sampah", Icons.Filled.Delete, Icons.Outlined.Delete),
    BottomNavItem("settings", "Pengaturan", Icons.Filled.Settings, Icons.Outlined.Settings),
)

@Composable
fun MainShellScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        containerColor = SkyBackground,
        bottomBar = {
            SkyBottomNavigationBar(
                items = bottomNavItems,
                currentRoute = currentDestination?.route,
                onItemClick = { item ->
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(tween(220)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(220)) },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(180)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(220)) },
        ) {
            composable("home") {
                FileBrowserScreen(folderId = null, navController = navController)
            }
            composable("search") {
                SearchScreen(navController = navController)
            }
            composable("starred") {
                StarredScreen(navController = navController)
            }
            composable("trash") {
                TrashScreen(navController = navController)
            }
            composable("settings") {
                SettingsScreen(onLogout = onLogout)
            }

            // Nested folder navigation
            composable("folder/{folderId}") { backStackEntry ->
                val folderId = backStackEntry.arguments?.getString("folderId")
                FileBrowserScreen(folderId = folderId, navController = navController)
            }
        }
    }
}

@Composable
fun SkyBottomNavigationBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onItemClick: (BottomNavItem) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, SkyBackground),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY,
                )
            )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            color = SkySurfaceElevated,
            shadowElevation = 0.dp,
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = Brush.horizontalGradient(
                    colors = listOf(SkyBorder, SkyBorder.copy(0.5f), SkyBorder)
                )
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route ||
                        (currentRoute?.startsWith("folder/") == true && item.route == "home")

                    SkyBottomNavItem(
                        item = item,
                        isSelected = isSelected,
                        onClick = { onItemClick(item) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
fun SkyBottomNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animatedBackground by animateColorAsState(
        targetValue = if (isSelected) SkyBlueContainer else Color.Transparent,
        animationSpec = tween(250),
        label = "navBg"
    )
    val animatedIconColor by animateColorAsState(
        targetValue = if (isSelected) SkyBlue else SkyTextTertiary,
        animationSpec = tween(250),
        label = "navIconColor"
    )
    val animatedTextColor by animateColorAsState(
        targetValue = if (isSelected) SkyBlueLight else SkyTextTertiary,
        animationSpec = tween(250),
        label = "navTextColor"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.0f else 0.95f,
        animationSpec = SkyMotion.springSnappy(),
        label = "navScale"
    )

    Column(
        modifier = modifier
            .clip(SkyShapes.NavItem)
            .background(animatedBackground)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
            contentDescription = item.label,
            tint = animatedIconColor,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(2.dp))
        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn(tween(200)) + expandVertically(tween(200)),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(150)),
        ) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall,
                color = animatedTextColor,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}
