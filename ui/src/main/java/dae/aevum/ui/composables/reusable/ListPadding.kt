package dae.aevum.ui.composables.reusable

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Scaffold content padding; provided by AppScaffold so lists clear the bottom bar. */
val LocalScaffoldContentPadding = compositionLocalOf { PaddingValues(0.dp) }

/** Material BottomNavigation height. */
private val BottomNavigationBarHeight = 56.dp

private val ListBottomExtraPadding = 16.dp

/**
 * Bottom padding so list content clears the bottom navigation bar, system/cutout
 * insets, and a small extra gap. Uses Scaffold-measured padding when available;
 * otherwise falls back to bar height + [WindowInsets.safeDrawing].
 */
@Composable
fun listBottomContentPadding(extra: Dp = ListBottomExtraPadding): Dp {
    val scaffoldBottom = LocalScaffoldContentPadding.current.calculateBottomPadding()
    if (scaffoldBottom > 0.dp) {
        return scaffoldBottom + extra
    }

    val systemBottom =
        WindowInsets.safeDrawing
            .only(WindowInsetsSides.Bottom)
            .asPaddingValues()
            .calculateBottomPadding()
    return BottomNavigationBarHeight + systemBottom + extra
}
