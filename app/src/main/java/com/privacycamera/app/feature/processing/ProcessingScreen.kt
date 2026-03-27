package com.privacycamera.app.feature.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.privacycamera.app.domain.model.BoundingBox
import com.privacycamera.app.domain.model.SensitiveInfo
import com.privacycamera.app.domain.model.SensitiveType

@Composable
fun ProcessingScreen(
    photoUri: String,
    onNavigateBack: () -> Unit,
    viewModel: ProcessingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var imageSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(photoUri) {
        viewModel.loadPhoto(photoUri)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            uiState.processedBitmap != null -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        var currentBitmap = uiState.originalBitmap
                        
                        if (currentBitmap != null) {
                            val sensitiveInfo = uiState.detectedSensitiveInfo
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .onSizeChanged { imageSize = it }
                            ) {
                                androidx.compose.foundation.Image(
                                    bitmap = currentBitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                                
                                if (sensitiveInfo.isNotEmpty()) {
                                    SensitiveOverlay(
                                        sensitiveInfoList = sensitiveInfo,
                                        imageSize = imageSize,
                                        originalBitmap = currentBitmap,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                        
                        if (uiState.showProcessed) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .background(Color.Black.copy(alpha = 0.7f))
                                    .padding(16.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.Green,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                    Text(
                                        text = "处理完成",
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("取消")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = {
                                uiState.processedBitmap?.let { bitmap ->
                                    viewModel.saveProcessedPhoto(context, bitmap)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isSaving
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(24.dp).width(24.dp),
                                    color = Color.White
                                )
                            } else {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("保存")
                            }
                        }
                    }

                    if (uiState.saveSuccess) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Green.copy(alpha = 0.9f))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "照片已保存到隐私相册",
                                color = Color.White,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }

                    if (uiState.error != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Red.copy(alpha = 0.9f))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = uiState.error!!,
                                color = Color.White,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }
            }
            else -> {
                Text(
                    text = "无法加载照片",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun SensitiveOverlay(
    sensitiveInfoList: List<SensitiveInfo>,
    imageSize: IntSize,
    originalBitmap: Bitmap,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    
    Canvas(modifier = modifier) {
        if (imageSize.width <= 0 || imageSize.height <= 0) return@Canvas
        
        val bitmapWidth = originalBitmap.width.toFloat()
        val bitmapHeight = originalBitmap.height.toFloat()
        
        val scaleX = size.width / bitmapWidth
        val scaleY = size.height / bitmapHeight
        val scale = minOf(scaleX, scaleY)
        
        val offsetX = (size.width - bitmapWidth * scale) / 2
        val offsetY = (size.height - bitmapHeight * scale) / 2
        
        for (info in sensitiveInfoList) {
            val box = info.boundingBox
            
            val left = box.left * scale + offsetX
            val top = box.top * scale + offsetY
            val right = box.right * scale + offsetX
            val bottom = box.bottom * scale + offsetY
            
            drawRect(
                color = Color.Red.copy(alpha = 0.3f),
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(right - left, bottom - top)
            )
            
            drawRect(
                color = Color.Red,
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                style = Stroke(width = 3f)
            )
        }
    }
}

@Composable
fun SensitiveLabel(
    info: SensitiveInfo,
    imageSize: IntSize,
    originalBitmap: Bitmap,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    
    val bitmapWidth = originalBitmap.width.toFloat()
    val bitmapHeight = originalBitmap.height.toFloat()
    
    val scaleX = imageSize.width / bitmapWidth
    val scaleY = imageSize.height / bitmapHeight
    val scale = minOf(scaleX, scaleY)
    
    val offsetX = (imageSize.width - bitmapWidth * scale) / 2
    val offsetY = (imageSize.height - bitmapHeight * scale) / 2
    
    val left = info.boundingBox.left * scale + offsetX
    val top = info.boundingBox.top * scale + offsetY
    
    val typeName = when (info.type) {
        SensitiveType.GPS_LOCATION -> "GPS位置"
        SensitiveType.ID_CARD -> "身份证"
        SensitiveType.BANK_CARD -> "银行卡"
        SensitiveType.PHONE_NUMBER -> "手机号"
        SensitiveType.EMAIL -> "邮箱"
        SensitiveType.PASSWORD -> "密码"
        SensitiveType.ADDRESS -> "地址"
    }
    
    Box(
        modifier = modifier
            .padding(
                start = with(density) { left.toDp() },
                top = with(density) { top.toDp() }
            )
            .background(Color.Red, shape = MaterialTheme.shapes.small)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = typeName,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}
