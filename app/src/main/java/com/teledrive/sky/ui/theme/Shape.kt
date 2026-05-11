package com.teledrive.sky.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val TeleDriveShapes = Shapes(
    // Extra Small — Chips, badges
    extraSmall = RoundedCornerShape(4.dp),
    // Small — Snackbars, text fields
    small = RoundedCornerShape(8.dp),
    // Medium — Cards, dialogs
    medium = RoundedCornerShape(12.dp),
    // Large — Bottom sheets, navigation drawer
    large = RoundedCornerShape(16.dp),
    // Extra Large — Full-screen modals
    extraLarge = RoundedCornerShape(24.dp),
)

// Custom shapes for specific components
object SkyShapes {
    val FileThumbnail = RoundedCornerShape(10.dp)
    val FileCard = RoundedCornerShape(14.dp)
    val BottomSheet = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    val Dialog = RoundedCornerShape(20.dp)
    val Chip = RoundedCornerShape(50.dp)
    val Button = RoundedCornerShape(12.dp)
    val TextField = RoundedCornerShape(12.dp)
    val ProgressBar = RoundedCornerShape(50.dp)
    val Tooltip = RoundedCornerShape(8.dp)
    val NavItem = RoundedCornerShape(12.dp)
    val FAB = RoundedCornerShape(16.dp)
}
