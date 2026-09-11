package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DeviceDiagnostics
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DiagnosticsCard(
    diagnostics: DeviceDiagnostics,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ObsidianSurfaceElevated,
        border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.3f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .testTag("device_diagnostics_card")
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "SYSTEM TELEMETRY HUD",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 10.sp
                    ),
                    color = CyanNeon
                )
                Text(
                    text = diagnostics.deviceModel,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Battery Metric
            val batteryFraction = (diagnostics.batteryPct / 100f).coerceIn(0f, 1f)
            DiagnosticMetricRow(
                icon = Icons.Default.BatteryChargingFull,
                label = "Battery",
                value = "${diagnostics.batteryPct}% ${if (diagnostics.isCharging) "(Charging)" else ""}",
                fraction = batteryFraction,
                barColor = if (diagnostics.batteryPct > 20) MatrixGreen else Color(0xFFFF5252)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // RAM Metric
            val ramFraction = if (diagnostics.ramTotalMb > 0) {
                (diagnostics.ramUsedMb.toFloat() / diagnostics.ramTotalMb.toFloat()).coerceIn(0f, 1f)
            } else 0f
            DiagnosticMetricRow(
                icon = Icons.Default.Memory,
                label = "RAM Memory",
                value = "${diagnostics.ramUsedMb} MB / ${diagnostics.ramTotalMb} MB",
                fraction = ramFraction,
                barColor = CyanNeon
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Storage Metric
            val storageUsedGb = (diagnostics.storageTotalGb - diagnostics.storageFreeGb).coerceAtLeast(0.0)
            val storageFraction = if (diagnostics.storageTotalGb > 0) {
                (storageUsedGb / diagnostics.storageTotalGb).toFloat().coerceIn(0f, 1f)
            } else 0f
            DiagnosticMetricRow(
                icon = Icons.Default.SdStorage,
                label = "Internal Storage",
                value = "${diagnostics.storageFreeGb} GB free of ${diagnostics.storageTotalGb} GB",
                fraction = storageFraction,
                barColor = ElectricBlue
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Network & OS row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = MatrixGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = diagnostics.networkType,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = TextPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = diagnostics.androidVersion,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticMetricRow(
    icon: ImageVector,
    label: String,
    value: String,
    fraction: Float,
    barColor: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = barColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = TextSecondary
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                ),
                color = TextPrimary
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = barColor,
            trackColor = ObsidianBorder
        )
    }
}
