package com.linnan.hayaocamera.ui.components

import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onSurfaceReady: (LifecycleOwner, PreviewView) -> Unit,
    onTap: (PreviewView, Float, Float) -> Unit,
    onPinchZoom: (Float) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    val latestOnTap = rememberUpdatedState(onTap)
    val latestOnPinchZoom = rememberUpdatedState(onPinchZoom)

    AndroidView(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    previewViewRef?.let { latestOnTap.value(it, offset.x, offset.y) }
                }
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    latestOnPinchZoom.value(zoom)
                }
            },
        factory = { context ->
            PreviewView(context).apply {
                implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                scaleType = PreviewView.ScaleType.FIT_CENTER
                previewViewRef = this
                onSurfaceReady(lifecycleOwner, this)
            }
        }
    )
}
