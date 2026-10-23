package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CrosshairPreferences
import com.example.data.CrosshairState
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SolarYellow
import com.example.ui.theme.StealthBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividOrange

data class ColorOption(
    val name: String,
    val color: Color
)

val ColorPresets = listOf(
    ColorOption("Verde Neon", NeonGreen),
    ColorOption("Ciano Neon", NeonCyan),
    ColorOption("Vermelho", ElectricRed),
    ColorOption("Amarelo Solar", SolarYellow),
    ColorOption("Magenta", NeonMagenta),
    ColorOption("Branco Puro", PureWhite),
    ColorOption("Laranja", VividOrange),
    ColorOption("Preto Furtivo", StealthBlack)
)

val SizePresets = listOf(
    3 to "3 dp",
    6 to "6 dp",
    10 to "10 dp",
    16 to "16 dp"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DotCrosshairScreen(
    state: CrosshairState,
    hasOverlayPermission: Boolean,
    isBatteryOptimizationIgnored: Boolean,
    previewOffsetX: Float = 0f,
    previewOffsetY: Float = 0f,
    onToggleEnabled: (Boolean) -> Unit,
    onSizeChange: (Int) -> Unit,
    onColorChange: (Int) -> Unit,
    onToggleGyro: (Boolean) -> Unit,
    onGyroSensitivityChange: (Float) -> Unit,
    onGyroSmoothnessChange: (Float) -> Unit,
    onToggleInvertX: (Boolean) -> Unit,
    onToggleInvertY: (Boolean) -> Unit,
    onCalibrateGyro: () -> Unit,
    onRequestPermission: () -> Unit,
    onRequestDisableBatteryOptimization: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header with fully responsive, non-wrapping status pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Mira centralizada ultra leve para jogos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Status Badge - Fixed single-line layout with softWrap = false to prevent line wraps
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (state.isEnabled) NeonGreen.copy(alpha = 0.15f) else DarkSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (state.isEnabled) NeonGreen.copy(alpha = 0.5f) else DarkBorder
                ),
                modifier = Modifier
                    .clickable {
                        if (!hasOverlayPermission && !state.isEnabled) {
                            onRequestPermission()
                        } else {
                            onToggleEnabled(!state.isEnabled)
                        }
                    }
                    .testTag("status_badge_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (state.isEnabled) NeonGreen else TextMuted)
                    )
                    Text(
                        text = if (state.isEnabled) stringResource(R.string.status_active) else stringResource(R.string.status_inactive),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (state.isEnabled) NeonGreen else TextSecondary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // Overlay Permission Notice
        AnimatedVisibility(visible = !hasOverlayPermission) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, VividOrange.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(VividOrange.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = VividOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.permission_required_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = stringResource(R.string.permission_required_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VividOrange,
                            contentColor = DarkBackground
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("grant_permission_button")
                    ) {
                        Text(
                            text = stringResource(R.string.grant_permission),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Main Activation Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (state.isEnabled) NeonCyan.copy(alpha = 0.5f) else DarkBorder
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = if (state.isEnabled) stringResource(R.string.disable_crosshair) else stringResource(R.string.enable_crosshair),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = if (state.isEnabled) "Mira visível no centro sobre todos os apps" else "Ponto oculto. Toque para ativar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                Switch(
                    checked = state.isEnabled,
                    onCheckedChange = { targetState ->
                        if (!hasOverlayPermission && targetState) {
                            onRequestPermission()
                        } else {
                            onToggleEnabled(targetState)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DarkBackground,
                        checkedTrackColor = NeonCyan,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceVariant
                    ),
                    modifier = Modifier.testTag("main_toggle_switch")
                )
            }
        }

        // Real-Time Center Preview Box with live Gyro feedback
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.preview_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (state.isGyroEnabled) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = NeonCyan.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "Giroscópio Ativo",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = NeonCyan,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${state.dotSizeDp} dp",
                            style = MaterialTheme.typography.labelLarge,
                            color = NeonCyan
                        )
                    }
                }

                // Visual Target Display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF090D12))
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Subtle concentric guide rings
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .border(1.dp, Color(0xFF161E28), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .border(1.dp, Color(0xFF1E2836), CircleShape)
                    )
                    // Axis cross indicators
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .border(0.5.dp, Color(0xFF263242), CircleShape)
                    )

                    // Live Dot Render with live preview offset if gyro is active
                    val previewColor = Color(state.dotColorArgb)
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = previewOffsetX.toInt(),
                                    y = previewOffsetY.toInt()
                                )
                            }
                            .size(state.dotSizeDp.dp)
                            .clip(CircleShape)
                            .background(previewColor)
                    )
                }

                Text(
                    text = if (state.isGyroEnabled) {
                        "Incline o aparelho para testar a movimentação da mira em tempo real."
                    } else {
                        stringResource(R.string.preview_tip)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Gyroscope Assist Section (NEW)
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (state.isGyroEnabled) NeonCyan.copy(alpha = 0.5f) else DarkBorder
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Gyro Header and Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f).padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.gyro_enable_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = stringResource(R.string.gyro_enable_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = state.isGyroEnabled,
                        onCheckedChange = { onToggleGyro(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkBackground,
                            checkedTrackColor = NeonCyan,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkSurfaceVariant
                        ),
                        modifier = Modifier.testTag("gyro_toggle_switch")
                    )
                }

                AnimatedVisibility(visible = state.isGyroEnabled) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        // Sensitivity Slider
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.gyro_sensitivity),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${"%.1f".format(state.gyroSensitivity)}x",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = NeonCyan
                                )
                            }
                            Slider(
                                value = state.gyroSensitivity,
                                onValueChange = onGyroSensitivityChange,
                                valueRange = CrosshairPreferences.MIN_GYRO_SENSITIVITY..CrosshairPreferences.MAX_GYRO_SENSITIVITY,
                                colors = SliderDefaults.colors(
                                    thumbColor = NeonCyan,
                                    activeTrackColor = NeonCyan,
                                    inactiveTrackColor = DarkSurfaceVariant
                                ),
                                modifier = Modifier.testTag("gyro_sensitivity_slider")
                            )
                        }

                        // Smoothness / Response Slider
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.gyro_smoothness),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary
                                )
                                val smoothnessLabel = when {
                                    state.gyroSmoothness >= 0.65f -> "Alta (Muito Suave)"
                                    state.gyroSmoothness >= 0.35f -> "Equilibrada"
                                    else -> "Direta (Máx Resposta)"
                                }
                                Text(
                                    text = smoothnessLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextSecondary
                                )
                            }
                            Slider(
                                value = state.gyroSmoothness,
                                onValueChange = onGyroSmoothnessChange,
                                valueRange = 0f..0.85f,
                                colors = SliderDefaults.colors(
                                    thumbColor = NeonCyan,
                                    activeTrackColor = NeonCyan,
                                    inactiveTrackColor = DarkSurfaceVariant
                                ),
                                modifier = Modifier.testTag("gyro_smoothness_slider")
                            )
                        }

                        // Axis Inversion Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            FilterChip(
                                selected = state.gyroInvertX,
                                onClick = { onToggleInvertX(!state.gyroInvertX) },
                                label = { Text(stringResource(R.string.gyro_invert_x), fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = TextSecondary,
                                    selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = NeonCyan
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = state.gyroInvertX,
                                    borderColor = DarkBorder,
                                    selectedBorderColor = NeonCyan
                                )
                            )

                            FilterChip(
                                selected = state.gyroInvertY,
                                onClick = { onToggleInvertY(!state.gyroInvertY) },
                                label = { Text(stringResource(R.string.gyro_invert_y), fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = TextSecondary,
                                    selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = NeonCyan
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = state.gyroInvertY,
                                    borderColor = DarkBorder,
                                    selectedBorderColor = NeonCyan
                                )
                            )
                        }

                        // Calibration Button
                        OutlinedButton(
                            onClick = onCalibrateGyro,
                            modifier = Modifier.fillMaxWidth().testTag("calibrate_gyro_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = NeonCyan
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp).padding(end = 6.dp)
                            )
                            Text(
                                text = stringResource(R.string.gyro_calibrate_btn),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Battery Optimization & Background Execution Protection Card (NEW)
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isBatteryOptimizationIgnored) NeonGreen.copy(alpha = 0.4f) else VividOrange.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isBatteryOptimizationIgnored) NeonGreen.copy(alpha = 0.15f)
                                else VividOrange.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isBatteryOptimizationIgnored) Icons.Default.Check else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isBatteryOptimizationIgnored) NeonGreen else VividOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.battery_section_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isBatteryOptimizationIgnored) {
                                stringResource(R.string.battery_status_protected)
                            } else {
                                stringResource(R.string.battery_status_unprotected)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (isBatteryOptimizationIgnored) NeonGreen else VividOrange
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.battery_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                if (!isBatteryOptimizationIgnored) {
                    Button(
                        onClick = onRequestDisableBatteryOptimization,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VividOrange,
                            contentColor = DarkBackground
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("disable_battery_optimization_btn")
                    ) {
                        Text(
                            text = stringResource(R.string.battery_btn_disable),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Dot Size Customization Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.dot_size),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "${state.dotSizeDp} dp",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                }

                Slider(
                    value = state.dotSizeDp.toFloat(),
                    onValueChange = { onSizeChange(it.toInt()) },
                    valueRange = CrosshairPreferences.MIN_SIZE_DP.toFloat()..CrosshairPreferences.MAX_SIZE_DP.toFloat(),
                    steps = CrosshairPreferences.MAX_SIZE_DP - CrosshairPreferences.MIN_SIZE_DP - 1,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = DarkSurfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("size_slider")
                )

                // Quick presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SizePresets.forEach { (size, label) ->
                        val isSelected = state.dotSizeDp == size
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSizeChange(size) },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextSecondary,
                                selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                selectedLabelColor = NeonCyan
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = DarkBorder,
                                selectedBorderColor = NeonCyan
                            ),
                            modifier = Modifier.testTag("size_preset_$size")
                        )
                    }
                }
            }
        }

        // Dot Color Customization Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.dot_color),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    val currentHex = String.format("#%06X", (0xFFFFFF and state.dotColorArgb))
                    Text(
                        text = currentHex,
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ColorPresets.forEach { option ->
                        val optionArgb = option.color.toArgb()
                        val isSelected = state.dotColorArgb == optionArgb

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(option.color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) PureWhite else DarkBorder,
                                    shape = CircleShape
                                )
                                .clickable { onColorChange(optionArgb) }
                                .testTag("color_preset_${option.name.replace(" ", "_")}"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "${option.name} selecionada",
                                    tint = if (option.color == PureWhite || option.color == SolarYellow) StealthBlack else PureWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Lightweight Performance Specs
        Surface(
            color = DarkSurfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Garantia de Desempenho",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
                Text(
                    text = "• Giroscópio com sensor delay otimizado e zero alocação de GC\n• 0% consumo de CPU em repouso\n• Aceleração direta por hardware (GPU)\n• Toques totalmente livres sem interferência\n• Totalmente livre de anúncios e telemetria",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
