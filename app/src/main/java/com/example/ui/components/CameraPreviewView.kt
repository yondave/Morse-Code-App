package com.example.ui.components

import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.decoder.CameraMorseDecoder
import com.example.ui.theme.SophisticatedGreen
import com.example.ui.theme.SophisticatedPrimary
import com.example.ui.theme.SophisticatedSecondary
import com.example.ui.theme.SophisticatedSurfaceDark
import java.util.concurrent.Executors

@Composable
fun CameraPreviewView(
    opticalDecoder: CameraMorseDecoder,
    isLightDetected: Boolean,
    roiX: Float,
    roiY: Float,
    isExposureLocked: Boolean,
    zoomLinear: Float,
    onRoiTapped: (Float, Float) -> Unit,
    onResetRoi: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }
    var activeCamera: Camera? by remember { mutableStateOf(null) }
    var previewViewRef: PreviewView? by remember { mutableStateOf(null) }

    // Sync camera zoom level
    LaunchedEffect(zoomLinear, activeCamera) {
        try {
            activeCamera?.cameraControl?.setLinearZoom(zoomLinear.coerceIn(0f, 1f))
        } catch (e: Exception) {
            Log.w("CameraPreviewView", "Zoom adjustment error: ${e.message}")
        }
    }

    val reticleColor by animateColorAsState(
        targetValue = if (isLightDetected) SophisticatedGreen else SophisticatedPrimary,
        animationSpec = tween(durationMillis = 80),
        label = "reticleColor"
    )

    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraProvider?.unbindAll()
                cameraExecutor.shutdown()
            } catch (e: Exception) {
                Log.w("CameraPreviewView", "Shutdown error: ${e.message}")
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, SophisticatedPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
    ) {
        val containerWidth = maxWidth
        val containerHeight = maxHeight

        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .testTag("camera_preview_surface")
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        val normX = (tapOffset.x / size.width.toFloat()).coerceIn(0.12f, 0.88f)
                        val normY = (tapOffset.y / size.height.toFloat()).coerceIn(0.12f, 0.88f)
                        onRoiTapped(normX, normY)

                        // Trigger CameraX tap-to-focus and metering
                        previewViewRef?.let { pView ->
                            try {
                                val factory = SurfaceOrientedMeteringPointFactory(
                                    size.width.toFloat(),
                                    size.height.toFloat()
                                )
                                val point = factory.createPoint(tapOffset.x, tapOffset.y)
                                val action = FocusMeteringAction.Builder(point).build()
                                activeCamera?.cameraControl?.startFocusAndMetering(action)
                            } catch (e: Exception) {
                                Log.w("CameraPreviewView", "FocusMetering failed: ${e.message}")
                            }
                        }
                    }
                },
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
                previewViewRef = previewView

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val provider = cameraProviderFuture.get()
                        cameraProvider = provider

                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val imageAnalyzer = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                            .build()
                            .also {
                                it.setAnalyzer(cameraExecutor, opticalDecoder)
                            }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        provider.unbindAll()
                        val cam = provider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalyzer
                        )
                        activeCamera = cam
                        cam.cameraControl.setLinearZoom(zoomLinear.coerceIn(0f, 1f))
                    } catch (exc: Exception) {
                        Log.e("CameraPreviewView", "Camera binding failed", exc)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        // Animated Position for Tap-to-Aim Reticle
        val reticleSize = 130.dp
        val animatedOffsetX by animateDpAsState(
            targetValue = (containerWidth * roiX) - (reticleSize / 2),
            animationSpec = tween(durationMillis = 150),
            label = "reticleX"
        )
        val animatedOffsetY by animateDpAsState(
            targetValue = (containerHeight * roiY) - (reticleSize / 2),
            animationSpec = tween(durationMillis = 150),
            label = "reticleY"
        )

        // Tactical Reticle Box
        Box(
            modifier = Modifier
                .offset(x = animatedOffsetX, y = animatedOffsetY)
                .size(reticleSize)
                .border(1.5.dp, reticleColor.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                .testTag("optical_aiming_reticle"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val crossHairLen = 14.dp.toPx()

                // Center Crosshair
                drawLine(
                    color = reticleColor,
                    start = Offset(cx - crossHairLen, cy),
                    end = Offset(cx + crossHairLen, cy),
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = reticleColor,
                    start = Offset(cx, cy - crossHairLen),
                    end = Offset(cx, cy + crossHairLen),
                    strokeWidth = 1.5f
                )

                // Corner Brackets
                val cornerLen = 14.dp.toPx()
                val stroke = 3f
                // Top-Left
                drawLine(reticleColor, Offset(0f, 0f), Offset(cornerLen, 0f), stroke)
                drawLine(reticleColor, Offset(0f, 0f), Offset(0f, cornerLen), stroke)
                // Top-Right
                drawLine(reticleColor, Offset(size.width, 0f), Offset(size.width - cornerLen, 0f), stroke)
                drawLine(reticleColor, Offset(size.width, 0f), Offset(size.width, cornerLen), stroke)
                // Bottom-Left
                drawLine(reticleColor, Offset(0f, size.height), Offset(cornerLen, size.height), stroke)
                drawLine(reticleColor, Offset(0f, size.height), Offset(0f, size.height - cornerLen), stroke)
                // Bottom-Right
                drawLine(reticleColor, Offset(size.width, size.height), Offset(size.width - cornerLen, size.height), stroke)
                drawLine(reticleColor, Offset(size.width, size.height), Offset(size.width, size.height - cornerLen), stroke)
            }

            Text(
                text = if (isLightDetected) "LOCKED" else "AIM BEACON",
                color = if (isLightDetected) SophisticatedGreen else SophisticatedPrimary,
                fontSize = 9.sp,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 0.5.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
            )
        }

        // Tactical HUD Top Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Target Coordinate Badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .background(SophisticatedSurfaceDark.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "ROI [X:${(roiX * 100).toInt()}% Y:${(roiY * 100).toInt()}%]",
                    color = SophisticatedPrimary,
                    fontSize = 10.sp,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // Reset Reticle Target Button (if moved)
            if (roiX != 0.5f || roiY != 0.5f) {
                IconButton(
                    onClick = onResetRoi,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(36.dp)
                        .background(SophisticatedSurfaceDark.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                        .testTag("reset_roi_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Center Reticle",
                        tint = SophisticatedPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
