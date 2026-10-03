package org.ahmetyusufoguz.simplevolleyballstats

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Android 15 ve öncesi için yatay kilit
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES. UPSIDE_DOWN_CAKE) { // API 35 = Android 16
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }

        enableEdgeToEdge()

        setContent {
            App()
        }
    }
}