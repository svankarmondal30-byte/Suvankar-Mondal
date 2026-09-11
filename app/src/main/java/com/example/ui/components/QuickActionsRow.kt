package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.RedDanger
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun QuickActionsRow(
    isTorchOn: Boolean,
    isSosActive: Boolean,
    batteryLevel: Int,
    notesCount: Int,
    onToggleTorch: () -> Unit,
    onToggleSos: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenNotes: () -> Unit,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    onCommandSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Hardware quick status & controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flashlight Quick Toggle
            Surface(
                onClick = onToggleTorch,
                shape = RoundedCornerShape(10.dp),
                color = if (isTorchOn) CyanNeon.copy(alpha = 0.2f) else ObsidianSurfaceElevated,
                border = BorderStroke(1.dp, if (isTorchOn) CyanNeon else ObsidianBorder),
                modifier = Modifier.testTag("quick_torch_toggle")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flashlight",
                        tint = if (isTorchOn) CyanNeon else TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isTorchOn) "Torch ON" else "Torch",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        color = if (isTorchOn) CyanNeon else TextSecondary
                    )
                }
            }

            // SOS Strobe Toggle
            Surface(
                onClick = onToggleSos,
                shape = RoundedCornerShape(10.dp),
                color = if (isSosActive) RedDanger.copy(alpha = 0.2f) else ObsidianSurfaceElevated,
                border = BorderStroke(1.dp, if (isSosActive) RedDanger else ObsidianBorder),
                modifier = Modifier.testTag("quick_sos_toggle")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CrisisAlert,
                        contentDescription = "SOS Strobe",
                        tint = if (isSosActive) RedDanger else AmberAlert,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isSosActive) "SOS ACTIVE" else "SOS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        color = if (isSosActive) RedDanger else AmberAlert
                    )
                }
            }

            // Diagnostics HUD trigger
            Surface(
                onClick = onOpenDiagnostics,
                shape = RoundedCornerShape(10.dp),
                color = ObsidianSurfaceElevated,
                border = BorderStroke(1.dp, MatrixGreen.copy(alpha = 0.5f)),
                modifier = Modifier.testTag("quick_diagnostics_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = "Telemetry",
                        tint = MatrixGreen,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Diagnostics",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MatrixGreen
                    )
                }
            }

            // Notes Button
            Surface(
                onClick = onOpenNotes,
                shape = RoundedCornerShape(10.dp),
                color = ObsidianSurfaceElevated,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.testTag("quick_notes_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Note,
                        contentDescription = "Notes",
                        tint = CyanNeon,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (notesCount > 0) "Notes ($notesCount)" else "Notes",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = TextPrimary
                    )
                }
            }

            // Volume Up
            Surface(
                onClick = onVolumeUp,
                shape = RoundedCornerShape(10.dp),
                color = ObsidianSurfaceElevated,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.testTag("quick_volume_up")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Volume Up",
                        tint = TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Vol +", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = TextSecondary)
                }
            }

            // Volume Down
            Surface(
                onClick = onVolumeDown,
                shape = RoundedCornerShape(10.dp),
                color = ObsidianSurfaceElevated,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.testTag("quick_volume_down")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeDown,
                        contentDescription = "Volume Down",
                        tint = TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Vol -", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = TextSecondary)
                }
            }

            // Battery Level Display
            if (batteryLevel >= 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(ObsidianSurfaceElevated, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BatteryChargingFull,
                        contentDescription = "Battery",
                        tint = MatrixGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$batteryLevel%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MatrixGreen
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Multilingual Command Suggestion Pills
        val commandPills = listOf(
            "Phone diagnostics",
            "SOS torch",
            "Silent mode",
            "MAX, YouTube kholo",
            "MAX, torch on karo",
            "Note likho: meeting tomorrow",
            "ফোন স্ট্যাটাস",
            "টর্চ অন করো",
            "নোট লেখো",
            "মাকে ফোন করো",
            "फोन स्थिति",
            "यूट्यूब खोलो",
            "नोट लिखो",
            "Open Camera",
            "Set alarm for 7 AM",
            "Calculate 45 * 12",
            "Translate 'good morning' to Bengali"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            commandPills.forEach { cmd ->
                FilterChip(
                    selected = false,
                    onClick = { onCommandSelected(cmd) },
                    label = {
                        Text(
                            text = cmd,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = TextPrimary
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = ObsidianSurface,
                        labelColor = TextPrimary
                    ),
                    border = BorderStroke(0.8.dp, ObsidianBorder),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("quick_cmd_${cmd.take(8).replace(" ", "_")}")
                )
            }
        }
    }
}
