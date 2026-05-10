package com.teledrive.sky.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset

// ─── Custom Animation Specs ─────────────────────────────────────────────────
// Smooth, premium-feeling animation timings

object SkyMotion {

    // ── Easing Curves ──────────────────────────────────────────────────────
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EmphasizedDecelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerateEasing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    val StandardEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val StandardDecelerateEasing = CubicBezierEasing(0.0f, 0.0f, 0.0f, 1.0f)
    val StandardAccelerateEasing = CubicBezierEasing(0.3f, 0.0f, 1.0f, 1.0f)

    // ── Duration ───────────────────────────────────────────────────────────
    const val DurationShort1 = 50
    const val DurationShort2 = 100
    const val DurationShort3 = 150
    const val DurationShort4 = 200
    const val DurationMedium1 = 250
    const val DurationMedium2 = 300
    const val DurationMedium3 = 350
    const val DurationMedium4 = 400
    const val DurationLong1 = 450
    const val DurationLong2 = 500
    const val DurationLong3 = 550
    const val DurationLong4 = 600

    // ── Tween Specs ────────────────────────────────────────────────────────
    fun <T> tweenFast(): TweenSpec<T> = tween(
        durationMillis = DurationShort4,
        easing = EmphasizedEasing
    )

    fun <T> tweenMedium(): TweenSpec<T> = tween(
        durationMillis = DurationMedium2,
        easing = EmphasizedEasing
    )

    fun <T> tweenSlow(): TweenSpec<T> = tween(
        durationMillis = DurationMedium4,
        easing = EmphasizedDecelerateEasing
    )

    fun <T> tweenEnter(): TweenSpec<T> = tween(
        durationMillis = DurationMedium3,
        easing = EmphasizedDecelerateEasing
    )

    fun <T> tweenExit(): TweenSpec<T> = tween(
        durationMillis = DurationShort4,
        easing = EmphasizedAccelerateEasing
    )

    // ── Spring Specs ───────────────────────────────────────────────────────
    fun <T> springBouncy(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    fun <T> springSmooth(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    fun <T> springSnappy(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    // ── Specific Component Animations ──────────────────────────────────────

    // For list items appearing
    fun listItemAppear(): TweenSpec<Float> = tween(
        durationMillis = DurationMedium2,
        easing = EmphasizedDecelerateEasing
    )

    // For FAB
    fun fabScale(): SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    // For page transitions
    fun pageEnter(): TweenSpec<IntOffset> = tween(
        durationMillis = DurationMedium3,
        easing = EmphasizedDecelerateEasing
    )

    fun pageExit(): TweenSpec<IntOffset> = tween(
        durationMillis = DurationShort4,
        easing = EmphasizedAccelerateEasing
    )

    // For bottom sheet
    fun sheetExpand(): TweenSpec<Dp> = tween(
        durationMillis = DurationMedium4,
        easing = EmphasizedDecelerateEasing
    )

    // For checkbox/star toggle
    fun toggleScale(): SpringSpec<Float> = spring(
        dampingRatio = 0.6f,
        stiffness = Spring.StiffnessMedium
    )

    // For progress bars
    fun progressUpdate(): TweenSpec<Float> = tween(
        durationMillis = DurationShort4,
        easing = FastOutSlowInEasing
    )
}
