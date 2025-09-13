package com.markrogers.journal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.markrogers.journal.ui.components.AppRoot   // your AppRoot path
import androidx.fragment.app.FragmentActivity
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Draw behind system bars; keep icons non-light so they’re visible on dark UI
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        val appCtx = applicationContext
        setContent {
            AppRoot(appContext = appCtx)   // <-- pass the required parameter
        }
    }
}
