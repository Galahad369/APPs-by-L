package com.local.localkit.ui.features

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.*
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashlightScreen() {
    val context = LocalContext.current
    val cameraManager = remember { context.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    val cameraId = remember {
        cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var enabled by remember { mutableStateOf(false) }
    var pattern by remember { mutableStateOf("Steady") }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }

    fun torch(on: Boolean) {
        if (cameraId != null && granted) runCatching { cameraManager.setTorchMode(cameraId, on) }
    }

    LaunchedEffect(enabled, pattern, granted) {
        if (!enabled || !granted) { torch(false); return@LaunchedEffect }
        when (pattern) {
            "Steady" -> torch(true)
            "Strobe" -> while (true) { torch(true); delay(180); torch(false); delay(180) }
            else -> {
                // SOS: ... --- ... with a clear gap. Delays are intentionally conservative.
                val units = listOf(1,1,1,1,1,3,3,1,3,1,3,3,1,1,1,1,1,7)
                var on = true
                while (true) {
                    for (unit in units) { torch(on); delay(unit * 180L); on = !on }
                }
            }
        }
    }
    DisposableEffect(Unit) { onDispose { torch(false) } }

    ToolPage("A light that opens immediately", "No ads, notification permission or background behavior.") {
        if (cameraId == null) {
            ResultCard("No camera flash found on this device")
        } else if (!granted) {
            ResultCard("Camera permission is required by Android to control the torch")
            Button(onClick = { permission.launch(Manifest.permission.CAMERA) }, modifier = Modifier.fillMaxWidth()) { Text("Allow while using this tool") }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth().height(220.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                onClick = { enabled = !enabled }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(if (enabled) Icons.Outlined.FlashlightOn else Icons.Outlined.FlashlightOff, null, Modifier.size(72.dp))
                    Text(if (enabled) "ON" else "OFF", style = MaterialTheme.typography.headlineMedium)
                }
            }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("Steady", "Strobe", "SOS").forEachIndexed { index, item ->
                    SegmentedButton(selected = pattern == item, onClick = { pattern = item }, shape = SegmentedButtonDefaults.itemShape(index, 3)) { Text(item) }
                }
            }
            Text("Warning: flashing light may affect people with photosensitive epilepsy.", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun CompassLevelScreen() {
    val context = LocalContext.current
    val sensors = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    var azimuth by remember { mutableFloatStateOf(0f) }
    var pitch by remember { mutableFloatStateOf(0f) }
    var roll by remember { mutableFloatStateOf(0f) }
    var hasCompass by remember { mutableStateOf(true) }

    DisposableEffect(sensors) {
        val gravity = FloatArray(3)
        val magnetic = FloatArray(3)
        var gravityReady = false
        var magneticReady = false
        fun update() {
            if (!gravityReady || !magneticReady) return
            val rotation = FloatArray(9)
            if (SensorManager.getRotationMatrix(rotation, null, gravity, magnetic)) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotation, orientation)
                azimuth = ((Math.toDegrees(orientation[0].toDouble()).toFloat() + 360f) % 360f)
                pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                roll = Math.toDegrees(orientation[2].toDouble()).toFloat()
            }
        }
        val listener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> { lowPass(event.values, gravity); gravityReady = true }
                    Sensor.TYPE_MAGNETIC_FIELD -> { lowPass(event.values, magnetic); magneticReady = true }
                }
                update()
            }
        }
        val accelerometer = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensors.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        hasCompass = accelerometer != null && magnetometer != null
        accelerometer?.let { sensors.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        magnetometer?.let { sensors.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        onDispose { sensors.unregisterListener(listener) }
    }

    ToolPage("Compass & bubble level", "Measurements depend on your phone sensors. Move away from magnets and metal.") {
        if (!hasCompass) ResultCard("Required direction sensors are not available") else {
            Text("${azimuth.roundToInt()}°  ${direction(azimuth)}", style = MaterialTheme.typography.displaySmall)
            CompassDial(azimuth)
            HorizontalDivider()
            Text("Pitch ${numberFormatLocal(pitch)}°  ·  Roll ${numberFormatLocal(roll)}°", style = MaterialTheme.typography.titleMedium)
            BubbleLevel(pitch, roll)
            if (abs(pitch) < 1 && abs(roll) < 1) AssistChip(onClick = {}, label = { Text("LEVEL") })
        }
    }
}

@Composable
private fun CompassDial(azimuth: Float) {
    val primary = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(Modifier.fillMaxWidth().aspectRatio(1f).padding(22.dp)) {
        val radius = size.minDimension / 2
        val center = this.center
        drawCircle(muted, radius, style = Stroke(4f))
        for (degree in 0 until 360 step 15) {
            val angle = Math.toRadians((degree - 90 - azimuth).toDouble())
            val outer = Offset(center.x + cos(angle).toFloat() * radius, center.y + sin(angle).toFloat() * radius)
            val innerRadius = radius - if (degree % 90 == 0) 28f else 14f
            val inner = Offset(center.x + cos(angle).toFloat() * innerRadius, center.y + sin(angle).toFloat() * innerRadius)
            drawLine(if (degree == 0) Color.Red else muted, inner, outer, strokeWidth = if (degree % 90 == 0) 7f else 3f)
        }
        drawLine(primary, Offset(center.x, 20f), Offset(center.x, center.y), strokeWidth = 9f)
        drawCircle(primary, 12f, center)
    }
}

@Composable
private fun BubbleLevel(pitch: Float, roll: Float) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    Canvas(Modifier.fillMaxWidth().height(180.dp).padding(12.dp)) {
        val center = this.center
        val maxX = size.width / 2 - 28f
        val maxY = size.height / 2 - 28f
        drawRoundRect(outline, style = Stroke(4f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f))
        drawLine(outline, Offset(center.x - 20, center.y), Offset(center.x + 20, center.y), 2f)
        drawLine(outline, Offset(center.x, center.y - 20), Offset(center.x, center.y + 20), 2f)
        val x = center.x + (roll / 45f).coerceIn(-1f, 1f) * maxX
        val y = center.y + (pitch / 45f).coerceIn(-1f, 1f) * maxY
        drawCircle(primary, 23f, Offset(x, y))
    }
}

private fun lowPass(input: FloatArray, output: FloatArray) {
    for (i in 0..2) output[i] = output[i] * .85f + input[i] * .15f
}

private fun direction(degrees: Float): String = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")[((degrees + 22.5f) / 45f).toInt() % 8]
private fun numberFormatLocal(value: Float) = "%.1f".format(value)
