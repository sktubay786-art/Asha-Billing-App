package com.example.ui.components

import android.graphics.Rect as AndroidRect
import android.os.Build
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import kotlin.math.roundToInt

/**
 * Lifecycle-safe [TextToolbar] that guards Android's [ActionMode.TYPE_FLOATING]
 * against double-finish and post-detach calls, preventing:
 * `DecorView: Destroying unexpected ActionMode instance of TYPE_FLOATING; ... Expected null`.
 */
class SafeAndroidTextToolbar(
    private val view: View
) : TextToolbar {

    private var currentStatus: TextToolbarStatus = TextToolbarStatus.Hidden
    private var activeActionMode: ActionMode? = null
    private var activeCallback: FloatingCallback? = null

    private var rect: Rect = Rect.Zero
    private var onCopyRequested: (() -> Unit)? = null
    private var onPasteRequested: (() -> Unit)? = null
    private var onCutRequested: (() -> Unit)? = null
    private var onSelectAllRequested: (() -> Unit)? = null
    private var onAutofillRequested: (() -> Unit)? = null

    private val attachListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) = Unit

        override fun onViewDetachedFromWindow(v: View) {
            // DecorView tears down its own mFloatingActionMode on detach; clear references
            // without calling finish() again so DecorView never sees a stale ActionMode.
            activeCallback?.destroyed = true
            activeActionMode = null
            activeCallback = null
            currentStatus = TextToolbarStatus.Hidden
        }
    }

    init {
        view.addOnAttachStateChangeListener(attachListener)
    }

    override val status: TextToolbarStatus
        get() = currentStatus

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        showMenu(
            rect = rect,
            onCopyRequested = onCopyRequested,
            onPasteRequested = onPasteRequested,
            onCutRequested = onCutRequested,
            onSelectAllRequested = onSelectAllRequested,
            onAutofillRequested = null
        )
    }

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
        onAutofillRequested: (() -> Unit)?
    ) {
        this.rect = rect
        this.onCopyRequested = onCopyRequested
        this.onPasteRequested = onPasteRequested
        this.onCutRequested = onCutRequested
        this.onSelectAllRequested = onSelectAllRequested
        this.onAutofillRequested = onAutofillRequested

        if (!view.isAttachedToWindow || view.windowToken == null) {
            currentStatus = TextToolbarStatus.Hidden
            return
        }

        val existingMode = activeActionMode
        val existingCallback = activeCallback
        if (existingMode != null && existingCallback != null && !existingCallback.destroyed) {
            currentStatus = TextToolbarStatus.Shown
            runCatching {
                existingMode.invalidate()
                existingMode.invalidateContentRect()
            }
            return
        }

        val callback = FloatingCallback()
        val started = runCatching {
            view.startActionMode(callback, ActionMode.TYPE_FLOATING)
        }.getOrNull()

        if (started != null) {
            activeActionMode = started
            activeCallback = callback
            currentStatus = TextToolbarStatus.Shown
        } else {
            activeActionMode = null
            activeCallback = null
            currentStatus = TextToolbarStatus.Hidden
        }
    }

    override fun hide() {
        currentStatus = TextToolbarStatus.Hidden
        val mode = activeActionMode
        val callback = activeCallback
        activeActionMode = null
        activeCallback = null

        if (mode != null && callback != null && !callback.destroyed) {
            callback.destroyed = true
            if (view.isAttachedToWindow && view.windowToken != null) {
                runCatching { mode.finish() }
            }
        }
    }

    fun release() {
        hide()
        view.removeOnAttachStateChangeListener(attachListener)
    }

    private inner class FloatingCallback : ActionMode.Callback2() {
        var destroyed: Boolean = false

        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            if (menu == null || mode == null) return false
            populateMenu(menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            if (menu == null || mode == null) return false
            menu.clear()
            populateMenu(menu)
            return true
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            val handled = when (item?.itemId) {
                MENU_ID_COPY -> {
                    onCopyRequested?.invoke()
                    true
                }
                MENU_ID_PASTE -> {
                    onPasteRequested?.invoke()
                    true
                }
                MENU_ID_CUT -> {
                    onCutRequested?.invoke()
                    true
                }
                MENU_ID_SELECT_ALL -> {
                    onSelectAllRequested?.invoke()
                    true
                }
                MENU_ID_AUTOFILL -> {
                    onAutofillRequested?.invoke()
                    true
                }
                else -> false
            }
            if (handled) {
                hide()
            }
            return handled
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            destroyed = true
            if (activeActionMode === mode || activeCallback === this) {
                activeActionMode = null
                activeCallback = null
                currentStatus = TextToolbarStatus.Hidden
            }
        }

        override fun onGetContentRect(mode: ActionMode?, view: View?, outRect: AndroidRect?) {
            outRect?.set(
                rect.left.roundToInt(),
                rect.top.roundToInt(),
                rect.right.roundToInt(),
                rect.bottom.roundToInt()
            )
        }

        private fun populateMenu(menu: Menu) {
            onCopyRequested?.let {
                menu.add(0, MENU_ID_COPY, 0, android.R.string.copy)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            }
            onPasteRequested?.let {
                menu.add(0, MENU_ID_PASTE, 1, android.R.string.paste)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            }
            onCutRequested?.let {
                menu.add(0, MENU_ID_CUT, 2, android.R.string.cut)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            }
            onSelectAllRequested?.let {
                menu.add(0, MENU_ID_SELECT_ALL, 3, android.R.string.selectAll)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && onAutofillRequested != null) {
                menu.add(0, MENU_ID_AUTOFILL, 4, android.R.string.autofill)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            }
        }
    }

    private companion object {
        const val MENU_ID_COPY = 0
        const val MENU_ID_PASTE = 1
        const val MENU_ID_CUT = 2
        const val MENU_ID_SELECT_ALL = 3
        const val MENU_ID_AUTOFILL = 4
    }
}

@Composable
fun ProvideSafeTextToolbar(content: @Composable () -> Unit) {
    val view = LocalView.current
    val safeToolbar = remember(view) { SafeAndroidTextToolbar(view) }
    DisposableEffect(safeToolbar) {
        onDispose { safeToolbar.release() }
    }
    CompositionLocalProvider(LocalTextToolbar provides safeToolbar) {
        content()
    }
}
