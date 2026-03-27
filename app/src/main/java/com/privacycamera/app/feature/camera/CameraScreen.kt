package com.privacycamera.app.feature.camera

import android.Manifest
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.privacycamera.app.domain.model.SensitiveType
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    viewModel: CameraViewModel = hiltViewModel(),
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onNavigateToAlbum: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onPhotoCaptured: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(cameraPermissionState.status.isGranted) {
        if (cameraPermissionState.status.isGranted) {
            viewModel.cameraManager.startCamera(lifecycleOwner, previewView, uiState.flashMode)
        }
    }

    LaunchedEffect(uiState.isFrontCamera) {
        if (cameraPermissionState.status.isGranted) {
            viewModel.cameraManager.startCamera(lifecycleOwner, previewView, uiState.flashMode)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.cameraManager.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!cameraPermissionState.status.isGranted) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("需要相机权限才能使用此功能")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text("授予权限")
                }
            }
        } else {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                TopBar(
                    flashMode = uiState.flashMode,
                    hasGpsWarning = uiState.hasGpsInfo,
                    onFlashClick = { viewModel.toggleFlashMode() },
                    onSwitchCameraClick = {
                        viewModel.switchCamera()
                        scope.launch {
                            viewModel.cameraManager.switchCamera(lifecycleOwner, previewView)
                        }
                    }
                )

                Spacer(modifier = Modifier.weight(1f))

                if (uiState.isProcessing) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                BottomBar(
                    onCaptureClick = {
                        scope.launch {
                            val photoFile = viewModel.createTempPhotoFile()
                            val uri = viewModel.cameraManager.takePhoto(photoFile)
                            viewModel.onPhotoCaptured(uri)
                        }
                    },
                    onAlbumClick = onNavigateToAlbum,
                    onSettingsClick = onNavigateToSettings
                )
            }
        }

        if (uiState.showSensitiveDialog) {
            SensitiveInfoDialog(
                sensitiveInfoList = uiState.detectedSensitiveInfo,
                onDismiss = { viewModel.dismissSensitiveDialog() },
                onConfirm = {
                    viewModel.dismissSensitiveDialog()
                    uiState.capturedPhotoUri?.let { uri ->
                        onPhotoCaptured(uri.toString())
                    }
                }
            )
        }

        if (uiState.showPhotoProcessed) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissPhotoProcessed() },
                title = { Text("照片已保存") },
                text = {
                    Text("照片已成功保存，未检测到敏感信息。")
                },
                confirmButton = {
                    Button(onClick = { viewModel.dismissPhotoProcessed() }) {
                        Text("确定")
                    }
                }
            )
        }

        uiState.error?.let { errorMessage ->
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { Text("错误") },
                text = {
                    Text(errorMessage)
                },
                confirmButton = {
                    Button(onClick = { viewModel.clearError() }) {
                        Text("确定")
                    }
                }
            )
        }
    }
}

@Composable
private fun TopBar(
    flashMode: FlashMode,
    hasGpsWarning: Boolean,
    onFlashClick: () -> Unit,
    onSwitchCameraClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onFlashClick,
            modifier = Modifier
                .size(48.dp)
                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
        ) {
            Icon(
                imageVector = when (flashMode) {
                    FlashMode.AUTO -> Icons.Default.FlashAuto
                    FlashMode.ON -> Icons.Default.FlashOn
                    FlashMode.OFF -> Icons.Default.FlashOff
                },
                contentDescription = "闪光灯",
                tint = Color.White
            )
        }

        if (hasGpsWarning) {
            Row(
                modifier = Modifier
                    .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "GPS已开启",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }

        IconButton(
            onClick = onSwitchCameraClick,
            modifier = Modifier
                .size(48.dp)
                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "切换摄像头",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun BottomBar(
    onCaptureClick: () -> Unit,
    onAlbumClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onAlbumClick,
            modifier = Modifier
                .size(48.dp)
                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Photo,
                contentDescription = "相册",
                tint = Color.White
            )
        }

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(onClick = onCaptureClick)
                .border(4.dp, Color.Gray, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Camera,
                contentDescription = "拍照",
                tint = Color.DarkGray,
                modifier = Modifier.size(36.dp)
            )
        }

        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .size(48.dp)
                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "设置",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun SensitiveInfoDialog(
    sensitiveInfoList: List<com.privacycamera.app.domain.model.SensitiveInfo>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("检测到敏感信息") },
        text = {
            Column {
                Text("照片中检测到以下敏感信息：")
                Spacer(modifier = Modifier.height(8.dp))
                sensitiveInfoList.forEach { info ->
                    val typeName = when (info.type) {
                        SensitiveType.GPS_LOCATION -> "GPS位置"
                        SensitiveType.ID_CARD -> "身份证号"
                        SensitiveType.BANK_CARD -> "银行卡号"
                        SensitiveType.PHONE_NUMBER -> "手机号"
                        SensitiveType.EMAIL -> "邮箱"
                        SensitiveType.PASSWORD -> "密码"
                        SensitiveType.ADDRESS -> "地址"
                    }
                    Text("- $typeName: ${info.content.take(20)}...")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("是否要进行模糊处理？")
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("处理")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
