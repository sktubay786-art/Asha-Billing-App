package com.example.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView

/**
 * Unified haptic feedback controller driven by the user's Settings preference.
 */
class AshaHaptics(
    private val view: View?,
    val enabled: Boolean
) {
    fun tap() {
        if (!enabled || view == null) return
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun tick() {
        if (!enabled || view == null) return
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    fun confirm() {
        if (!enabled || view == null) return
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.VIRTUAL_KEY
        }
        view.performHapticFeedback(constant)
    }

    fun error() {
        if (!enabled || view == null) return
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.REJECT
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
        view.performHapticFeedback(constant)
    }

    fun heavy() {
        if (!enabled || view == null) return
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
}

val LocalAshaHaptics = staticCompositionLocalOf { AshaHaptics(view = null, enabled = false) }

@Composable
fun ProvideAshaHaptics(
    enabled: Boolean,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val haptics = remember(view, enabled) { AshaHaptics(view, enabled) }
    CompositionLocalProvider(LocalAshaHaptics provides haptics) {
        content()
    }
}
