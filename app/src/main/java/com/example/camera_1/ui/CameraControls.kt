package com.example.camera_1.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CameraControls(
    flashMode: String,
    onShutterClick: () -> Unit,
    onSwitchClick: () -> Unit,
    onFlashClick: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // Zoom Controls
        Row(
            modifier = Modifier.padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onZoomOut, modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.White)
            }
            IconButton(onClick = onZoomIn, modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White)
            }
        }

        // Main Controls
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flash Toggle
            IconButton(
                onClick = onFlashClick,
                modifier = Modifier
                    .size(50.dp)
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                val icon = when (flashMode) {
                    "on" -> Icons.Default.FlashOn
                    "auto" -> Icons.Default.FlashAuto
                    else -> Icons.Default.FlashOff
                }
                Icon(icon, contentDescription = "Flash", tint = Color.White)
            }

            // Shutter Button
            IconButton(
                onClick = onShutterClick,
                modifier = Modifier
                    .size(72.dp)
                    .background(Color.White, CircleShape)
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "Shutter",
                    modifier = Modifier.size(40.dp),
                    tint = Color.Black
                )
            }

            // Switch Camera
            IconButton(
                onClick = onSwitchClick,
                modifier = Modifier
                    .size(50.dp)
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Switch Camera", tint = Color.White)
            }
        }
        
        Text(
            text = "Flash: ${flashMode.uppercase()}",
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
