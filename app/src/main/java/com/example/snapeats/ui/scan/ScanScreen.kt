package com.example.snapeats.ui.scan

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.snapeats.domain.model.Food
import com.example.snapeats.ui.components.FoodCard
import com.example.snapeats.util.BitmapUtils
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors

// ---------------------------------------------------------------------------
// ScanScreen entry point
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    viewModel: ScanViewModel,
    onNavigateBack: () -> Unit,
    onMealLogged: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // -----------------------------------------------------------------------
    // Permission state
    // -----------------------------------------------------------------------
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    var showRationaleDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            hasCameraPermission = true
        } else {
            showRationaleDialog = true
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // -----------------------------------------------------------------------
    // CameraX objects
    // -----------------------------------------------------------------------
    val imageCaptureUseCase = remember { ImageCapture.Builder().build() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // -----------------------------------------------------------------------
    // Scan state
    // -----------------------------------------------------------------------
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()

    val displayedFoods = remember { mutableStateListOf<Food>() }
    var showBottomSheet by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(scanState) {
        when (val state = scanState) {
            is ScanState.Results -> {
                displayedFoods.clear()
                displayedFoods.addAll(state.foods)
                showBottomSheet = true
            }
            is ScanState.Error -> {
                scope.launch { snackbarHostState.showSnackbar(state.message) }
            }
            else -> Unit
        }
    }

    if (showRationaleDialog) {
        CameraRationaleDialog(
            isPermanentlyDenied = false,
            onDismiss = { showRationaleDialog = false; onNavigateBack() },
            onGoToSettings = {
                showRationaleDialog = false
                context.openAppSettings()
            },
            onRetry = {
                showRationaleDialog = false
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (hasCameraPermission) {
                CameraPreview(
                    imageCaptureUseCase = imageCaptureUseCase,
                    modifier = Modifier.fillMaxSize()
                )

                ScanOverlay(modifier = Modifier.fillMaxSize())

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                }

                if (scanState is ScanState.Scanning) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Black.copy(alpha = 0.65f)
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "Identifying food…",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                if (scanState is ScanState.Idle || scanState is ScanState.Error) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(bottom = 32.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        CaptureButton(
                            onClick = {
                                captureAndProcess(
                                    context = context,
                                    imageCapture = imageCaptureUseCase,
                                    executor = cameraExecutor,
                                    onBitmapReady = { bitmap ->
                                        viewModel.processImage(bitmap)
                                    },
                                    onError = { message ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(message)
                                        }
                                    }
                                )
                            }
                        )
                    }
                }

            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Camera permission is required to scan food.",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
                viewModel.resetState()
            },
            sheetState = bottomSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            ScanResultsSheet(
                foods = displayedFoods,
                onRemoveFood = { food -> displayedFoods.remove(food) },
                onAddToLog = {
                    scope.launch { bottomSheetState.hide() }.invokeOnCompletion {
                        showBottomSheet = false
                        viewModel.addFoodsToLog(displayedFoods.toList())
                        onMealLogged()
                    }
                },
                onDismiss = {
                    scope.launch { bottomSheetState.hide() }.invokeOnCompletion {
                        showBottomSheet = false
                        viewModel.resetState()
                    }
                }
            )
        }
    }
}

@Composable
private fun CameraPreview(
    imageCaptureUseCase: ImageCapture,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCaptureUseCase
                    )
                } catch (e: Exception) {
                    android.util.Log.e("CameraPreview", "Camera binding failed: ${e.message}", e)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier
    )
}

@Composable
private fun ScanOverlay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanLine")
    val lineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLineY"
    )

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.35f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.35f)
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .offset(y = with(androidx.compose.ui.platform.LocalDensity.current) {
                    (lineY * 600).dp
                })
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF00E5FF).copy(alpha = 0.85f),
                            Color.White,
                            Color(0xFF00E5FF).copy(alpha = 0.85f),
                            Color.Transparent
                        )
                    )
                )
        )

        ScanCorners(modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
private fun ScanCorners(modifier: Modifier = Modifier) {
    val bracketColor = Color.White.copy(alpha = 0.9f)
    val strokeWidth = 3.dp
    val bracketLength = 28.dp
    val boxSize = 240.dp

    Box(
        modifier = modifier.size(boxSize)
    ) {
        CornerBracket(
            modifier = Modifier.align(Alignment.TopStart),
            color = bracketColor,
            strokeWidth = strokeWidth,
            length = bracketLength,
            flipHorizontal = false,
            flipVertical = false
        )
        CornerBracket(
            modifier = Modifier.align(Alignment.TopEnd),
            color = bracketColor,
            strokeWidth = strokeWidth,
            length = bracketLength,
            flipHorizontal = true,
            flipVertical = false
        )
        CornerBracket(
            modifier = Modifier.align(Alignment.BottomStart),
            color = bracketColor,
            strokeWidth = strokeWidth,
            length = bracketLength,
            flipHorizontal = false,
            flipVertical = true
        )
        CornerBracket(
            modifier = Modifier.align(Alignment.BottomEnd),
            color = bracketColor,
            strokeWidth = strokeWidth,
            length = bracketLength,
            flipHorizontal = true,
            flipVertical = true
        )
    }
}

@Composable
private fun CornerBracket(
    modifier: Modifier = Modifier,
    color: Color,
    strokeWidth: androidx.compose.ui.unit.Dp,
    length: androidx.compose.ui.unit.Dp,
    flipHorizontal: Boolean,
    flipVertical: Boolean
) {
    androidx.compose.foundation.Canvas(
        modifier = modifier.size(length)
    ) {
        val sw = strokeWidth.toPx()
        val l = length.toPx()
        val halfSw = sw / 2f

        val startX = if (flipHorizontal) l - halfSw else halfSw
        val startY = if (flipVertical) l - halfSw else halfSw
        val endX = if (flipHorizontal) halfSw else l - halfSw
        val endY = if (flipVertical) halfSw else l - halfSw

        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(startX, startY),
            end = androidx.compose.ui.geometry.Offset(endX, startY),
            strokeWidth = sw,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(startX, startY),
            end = androidx.compose.ui.geometry.Offset(startX, endY),
            strokeWidth = sw,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

@Composable
private fun CaptureButton(onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.15f))
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Camera,
                contentDescription = "Capture",
                tint = Color.Black,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScanResultsSheet(
    foods: List<Food>,
    onRemoveFood: (Food) -> Unit,
    onAddToLog: () -> Unit,
    onDismiss: () -> Unit
) {
    val totalCalories = foods.sumOf { it.calories }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Detected Foods",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }

        if (foods.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No items — swipe to dismiss or cancel.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = foods,
                    key = { food -> food.name + food.calories }
                ) { food ->
                    SwipeableFoodItem(
                        food = food,
                        onDismissed = { onRemoveFood(food) }
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }

        Surface(
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$totalCalories kcal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Button(
                    onClick = onAddToLog,
                    enabled = foods.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add to Today's Log")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableFoodItem(
    food: Food,
    onDismissed: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                onDismissed()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.StartToEnd,
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                    Alignment.CenterEnd else Alignment.CenterStart
            ) {
                if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                    Text(
                        text = "Remove",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    ) {
        FoodCard(food = food)
    }
}

@Composable
private fun CameraRationaleDialog(
    isPermanentlyDenied: Boolean,
    onDismiss: () -> Unit,
    onGoToSettings: () -> Unit,
    onRetry: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Camera Permission Needed") },
        text = {
            Text(
                if (isPermanentlyDenied) {
                    "Camera access has been permanently denied. Please enable it in app settings to scan food."
                } else {
                    "SnapEats needs camera access to photograph your meals and identify foods automatically."
                }
            )
        },
        confirmButton = {
            if (isPermanentlyDenied) {
                TextButton(onClick = onGoToSettings) { Text("Open Settings") }
            } else {
                TextButton(onClick = onRetry) { Text("Allow") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun captureAndProcess(
    context: Context,
    imageCapture: ImageCapture,
    executor: java.util.concurrent.Executor,
    onBitmapReady: (android.graphics.Bitmap) -> Unit,
    onError: (String) -> Unit
) {
    val outputFile = File(context.cacheDir, "snapeats_capture_${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                try {
                    val original = BitmapFactory.decodeFile(outputFile.absolutePath)
                        ?: throw IllegalStateException("Failed to decode captured image.")

                    val scaled = BitmapUtils.scaleBitmap(original)

                    if (scaled !== original) {
                        original.recycle()
                    }

                    outputFile.delete()

                    onBitmapReady(scaled)
                } catch (e: Exception) {
                    android.util.Log.e("ScanScreen", "Bitmap decode failed: ${e.message}", e)
                    onError("Failed to process the captured photo. Please try again.")
                }
            }

            override fun onError(exception: ImageCaptureException) {
                android.util.Log.e("ScanScreen", "Image capture failed: ${exception.message}", exception)
                onError("Could not capture photo. Please try again.")
            }
        }
    )
}

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
}
