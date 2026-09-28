package com.linnan.hayaophoto.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.linnan.hayaophoto.AppGraph
import com.linnan.hayaophoto.viewmodel.PersonViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors

private enum class CaptureMode { PHOTO, VIDEO }

@Composable
fun CameraScreen(preselectedPersonId: Long?, onDone: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val personViewModel: PersonViewModel = viewModel()
    val persons by personViewModel.persons.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var hasAudioPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasCameraPermission = result[Manifest.permission.CAMERA] ?: hasCameraPermission
        hasAudioPermission = result[Manifest.permission.RECORD_AUDIO] ?: hasAudioPermission
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission || !hasAudioPermission) {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    var mode by remember { mutableStateOf(CaptureMode.PHOTO) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var isRecording by remember { mutableStateOf(false) }
    var pendingCaptureFile by remember { mutableStateOf<File?>(null) }
    var pendingIsVideo by remember { mutableStateOf(false) }
    var selectedPersonId by remember(preselectedPersonId, persons) {
        mutableStateOf(preselectedPersonId ?: persons.firstOrNull()?.id)
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val mainExecutor: Executor = remember { ContextCompat.getMainExecutor(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val recorder = remember {
        Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD)).build()
    }
    val videoCapture = remember { VideoCapture.withOutput(recorder) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    if (hasCameraPermission) {
        val previewView = remember {
            PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
        }

        // Rebinding tears down any in-progress use case, so this only reruns when the mode or
        // lens actually changes, never on unrelated state like the recording indicator.
        LaunchedEffect(mode, lensFacing) {
            val cameraProvider = withContext(Dispatchers.IO) {
                ProcessCameraProvider.getInstance(context).get()
            }
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            cameraProvider.unbindAll()
            try {
                if (mode == CaptureMode.PHOTO) {
                    cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
                } else {
                    cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, videoCapture)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "カメラを起動できませんでした", Toast.LENGTH_SHORT).show()
            }
        }

        Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black)) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDone) {
                    Icon(Icons.Filled.Close, contentDescription = "閉じる", tint = androidx.compose.ui.graphics.Color.White)
                }
                IconButton(onClick = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                        CameraSelector.LENS_FACING_FRONT
                    } else {
                        CameraSelector.LENS_FACING_BACK
                    }
                }) {
                    Icon(Icons.Filled.Cameraswitch, contentDescription = "カメラ切替", tint = androidx.compose.ui.graphics.Color.White)
                }
            }

            // Bottom controls
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    listOf(CaptureMode.PHOTO to "写真", CaptureMode.VIDEO to "動画").forEach { (m, label) ->
                        Text(
                            label,
                            color = if (mode == m) androidx.compose.ui.graphics.Color(0xFFFFD54F) else androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.clickable(enabled = !isRecording) { mode = m }.padding(8.dp)
                        )
                    }
                }
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 16.dp))
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(if (isRecording) androidx.compose.ui.graphics.Color.Red else androidx.compose.ui.graphics.Color.White)
                        .clickable {
                            when (mode) {
                                CaptureMode.PHOTO -> {
                                    val file = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
                                    val options = ImageCapture.OutputFileOptions.Builder(file).build()
                                    imageCapture.takePicture(options, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                            pendingCaptureFile = file
                                            pendingIsVideo = false
                                        }
                                        override fun onError(exception: ImageCaptureException) {
                                            mainExecutor.execute {
                                                Toast.makeText(context, "撮影に失敗しました", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    })
                                }
                                CaptureMode.VIDEO -> {
                                    if (!isRecording) {
                                        val file = File(context.cacheDir, "capture_${System.currentTimeMillis()}.mp4")
                                        val outputOptions = FileOutputOptions.Builder(file).build()
                                        val pending = recorder.prepareRecording(context, outputOptions)
                                            .apply { if (hasAudioPermission) withAudioEnabled() }
                                        activeRecording = pending.start(mainExecutor) { event ->
                                            if (event is VideoRecordEvent.Finalize && !event.hasError()) {
                                                pendingCaptureFile = file
                                                pendingIsVideo = true
                                            }
                                        }
                                        isRecording = true
                                    } else {
                                        activeRecording?.stop()
                                        activeRecording = null
                                        isRecording = false
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (mode == CaptureMode.VIDEO) {
                        Icon(
                            imageVector = Icons.Filled.FiberManualRecord,
                            contentDescription = "録画",
                            tint = if (isRecording) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Red,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("カメラを使用するには、権限を許可してください")
        }
    }

    val captureFile = pendingCaptureFile
    if (captureFile != null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("保存先の人物を選択") },
            text = {
                Column {
                    if (persons.isEmpty()) {
                        Text("先に「人物」タブで人物を登録してください")
                    }
                    persons.forEach { person ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPersonId = person.id }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = selectedPersonId == person.id, onClick = { selectedPersonId = person.id })
                            Text(person.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val targetId = selectedPersonId
                        if (targetId != null) {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    val mime = if (pendingIsVideo) "video/mp4" else "image/jpeg"
                                    runCatching {
                                        AppGraph.repository.addMediaFromFile(targetId, captureFile, pendingIsVideo, mime)
                                    }
                                    captureFile.delete()
                                }
                                pendingCaptureFile = null
                                onDone()
                            }
                        }
                    },
                    enabled = selectedPersonId != null
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = {
                    captureFile.delete()
                    pendingCaptureFile = null
                }) { Text("破棄") }
            }
        )
    }
}
