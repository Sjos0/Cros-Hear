package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CrosshairPreferences
import com.example.service.DotOverlayService
import com.example.ui.DotCrosshairScreen
import com.example.ui.theme.DotCrosshairTheme

class MainActivity : ComponentActivity() {

    private lateinit var preferences: CrosshairPreferences
    private var hasOverlayPermission by mutableStateOf(false)

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Notification permission result handled silently
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        preferences = CrosshairPreferences(this)
        checkOverlayPermission()
        checkNotificationPermission()

        setContent {
            DotCrosshairTheme {
                val state by preferences.state.collectAsStateWithLifecycle()

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    DotCrosshairScreen(
                        state = state,
                        hasOverlayPermission = hasOverlayPermission,
                        onToggleEnabled = { enabled ->
                            if (enabled && !hasOverlayPermission) {
                                requestOverlayPermission()
                            } else {
                                preferences.setEnabled(enabled)
                                if (enabled) {
                                    DotOverlayService.start(this@MainActivity)
                                } else {
                                    DotOverlayService.stop(this@MainActivity)
                                }
                            }
                        },
                        onSizeChange = { sizeDp ->
                            preferences.setSizeDp(sizeDp)
                            if (state.isEnabled) {
                                DotOverlayService.update(this@MainActivity)
                            }
                        },
                        onColorChange = { colorArgb ->
                            preferences.setColor(colorArgb)
                            if (state.isEnabled) {
                                DotOverlayService.update(this@MainActivity)
                            }
                        },
                        onRequestPermission = {
                            requestOverlayPermission()
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkOverlayPermission()
        // If the service is running but permission was revoked, ensure state is updated
        if (!hasOverlayPermission && preferences.state.value.isEnabled) {
            preferences.setEnabled(false)
            DotOverlayService.stop(this)
        }
    }

    private fun checkOverlayPermission() {
        hasOverlayPermission = Settings.canDrawOverlays(this)
    }

    private fun requestOverlayPermission() {
        if (!hasOverlayPermission) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
