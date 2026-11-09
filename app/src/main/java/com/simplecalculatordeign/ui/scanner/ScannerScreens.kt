package com.simplecalculatordeign.ui.scanner

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.RotateLeft
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simplecalculatordeign.ui.components.AppScaffold
import com.simplecalculatordeign.ui.components.GradientButton
import com.simplecalculatordeign.ui.components.GradientCard
import com.simplecalculatordeign.ui.components.IconBadge
import com.simplecalculatordeign.ui.components.InfoBanner
import com.simplecalculatordeign.ui.components.LoadingDots
import com.simplecalculatordeign.ui.components.PremiumCard
import com.simplecalculatordeign.ui.components.SectionTitle
import com.simplecalculatordeign.ui.rememberFeedback
import com.simplecalculatordeign.ui.theme.Brand
import com.simplecalculatordeign.ui.theme.CalcTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

// ===================================================================== Scanner home

@Composable
fun ScannerHomeScreen(
    viewModel: ScannerViewModel,
    onOpenCamera: () -> Unit,
    onImageReady: () -> Unit,
    onOpenResult: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var loading by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            loading = true
            scope.launch {
                try {
                    val bitmap = withContext(Dispatchers.IO) { ImageUtils.loadFromUri(context, uri) }
                    viewModel.setImage(bitmap)
                    onImageReady()
                } catch (e: Exception) {
                    viewModel.setImageError("Could not open this image: ${e.localizedMessage ?: "unsupported format"}")
                } catch (e: OutOfMemoryError) {
                    viewModel.setImageError("The image is too large to open")
                } finally {
                    loading = false
                }
            }
        }
    }

    AppScaffold(title = "Smart Scanner") { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            GradientCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.DocumentScanner, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp)) }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Scan & calculate", style = MaterialTheme.typography.titleLarge, color = Color.White)
                        Text(
                            "Receipts, invoices, worksheets or handwritten sums",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WhiteActionButton("Camera", Icons.Rounded.CameraAlt, Modifier.weight(1f), onOpenCamera)
                    WhiteActionButton("Gallery", Icons.Rounded.PhotoLibrary, Modifier.weight(1f)) {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                }
            }

            if (loading) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { LoadingDots() }
            }
            state.error?.let { InfoBanner(it, Icons.Rounded.ErrorOutline, tint = MaterialTheme.colorScheme.error) }

            if (state.stage == ScanStage.Done) {
                PremiumCard(onClick = onOpenResult) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Rounded.Visibility, size = 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Last scan", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${state.items.size} calculation(s) · ${state.numbers.size} other number(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text("Open", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            SectionTitle("How it works")
            PremiumCard {
                HowStep(1, "Capture", "Take a photo or pick one from your gallery.")
                HowStep(2, "Adjust", "Crop to the calculations, rotate, tune brightness.")
                HowStep(3, "Review", "Check recognised expressions and fix anything unclear.")
                HowStep(4, "Solve", "See results with full step-by-step explanations.")
            }
            InfoBanner(
                "Tip: good light and a flat page give the best results. Handwriting is supported but less reliable — uncertain lines are flagged for you to check.",
                Icons.Rounded.Lightbulb,
                tint = CalcTheme.colors.warning,
            )
            InfoBanner(
                "Images are processed on your device and never uploaded.",
                Icons.Rounded.Lock,
                tint = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun WhiteActionButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    val feedback = rememberFeedback()
    Row(
        modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable { feedback.click(); onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Brand.DeepPurple)
        Spacer(Modifier.width(8.dp))
        Text(text, color = Brand.DeepPurple, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun HowStep(number: Int, title: String, body: String) {
    Row(Modifier.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(CalcTheme.colors.accentGradient),
            contentAlignment = Alignment.Center,
        ) { Text("$number", color = Color.White, style = MaterialTheme.typography.labelLarge) }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ===================================================================== Camera

private fun Context.findActivity(): ComponentActivity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is ComponentActivity) return c
        c = c.baseContext
    }
    return null
}

@Composable
fun CameraScreen(viewModel: ScannerViewModel, onBack: () -> Unit, onCaptured: () -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var askedOnce by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        askedOnce = true
    }
    LaunchedEffect(Unit) { if (!granted && !askedOnce) launcher.launch(Manifest.permission.CAMERA) }

    if (granted) {
        CameraContent(viewModel, onBack, onCaptured)
    } else {
        val activity = context.findActivity()
        val permanentlyDenied = askedOnce && activity != null &&
            !activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
        AppScaffold(title = "Camera", onBack = onBack) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                IconBadge(Icons.Rounded.CameraAlt, size = 84.dp)
                Spacer(Modifier.height(20.dp))
                Text("Camera access needed", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(
                    "The scanner uses the camera to photograph documents. Photos are processed on your device only.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                if (permanentlyDenied) {
                    GradientButton("Open app settings", onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Permission was denied. Enable Camera under Permissions, then come back.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    GradientButton("Allow camera", onClick = { launcher.launch(Manifest.permission.CAMERA) }, modifier = Modifier.fillMaxWidth())
                }
                TextButton(onClick = onBack) { Text("Use gallery instead") }
            }
        }
        // Re-check when returning from system settings.
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                    granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
    }
}

@Composable
private fun CameraContent(viewModel: ScannerViewModel, onBack: () -> Unit, onCaptured: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val feedback = rememberFeedback()
    val executor = remember { Executors.newSingleThreadExecutor() }
    val currentOnCaptured by rememberUpdatedState(onCaptured)
    var flashOn by rememberSaveable { mutableStateOf(false) }
    var capturing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
            imageCaptureMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
        }
    }
    DisposableEffect(lifecycleOwner) {
        controller.bindToLifecycle(lifecycleOwner)
        onDispose { controller.unbind() }
    }
    DisposableEffect(Unit) { onDispose { executor.shutdown() } }
    LaunchedEffect(flashOn) { controller.enableTorch(flashOn) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    this.controller = controller
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        // Document frame guide
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width * 0.86f
            val h = size.height * 0.56f
            val left = (size.width - w) / 2
            val top = (size.height - h) / 2.3f
            drawRoundRect(
                color = Color.White.copy(alpha = 0.85f),
                topLeft = Offset(left, top),
                size = Size(w, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(36f, 36f),
                style = Stroke(width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 20f))),
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { flashOn = !flashOn }) {
                Icon(if (flashOn) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff, contentDescription = "Toggle light", tint = Color.White)
            }
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                error ?: "Fit the calculations inside the frame",
                color = if (error != null) Color(0xFFFFB4B4) else Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
            Spacer(Modifier.height(22.dp))
            Box(
                Modifier
                    .size(80.dp)
                    .border(4.dp, Color.White, CircleShape)
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(if (capturing) Color.White.copy(alpha = 0.5f) else Color.White)
                    .clickable(enabled = !capturing) {
                        feedback.heavy()
                        capturing = true
                        error = null
                        controller.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                val result = runCatching { ImageUtils.fromImageProxy(image) }
                                image.close()
                                scope.launch {
                                    capturing = false
                                    result.onSuccess {
                                        viewModel.setImage(it)
                                        currentOnCaptured()
                                    }.onFailure { error = "Could not process the photo. Please try again." }
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                scope.launch {
                                    capturing = false
                                    error = "Capture failed: ${exception.message ?: "camera error"}"
                                }
                            }
                        })
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (capturing) LoadingDots(color = Brand.DeepPurple)
            }
        }
    }
}

// ===================================================================== Crop / rotate / brightness

private enum class DragHandle { TopLeft, TopRight, BottomLeft, BottomRight, Move, None }

@Composable
fun CropScreen(viewModel: ScannerViewModel, onBack: () -> Unit, onScan: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val bitmap = state.sourceBitmap
    if (bitmap == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    val matrix = remember(state.brightness, state.contrast, state.grayscale) {
        ColorMatrix(ImageUtils.colorMatrix(state.brightness, state.contrast, state.grayscale))
    }
    val currentCrop by rememberUpdatedState(state.crop)
    val handleRadiusPx = with(LocalDensity.current) { 28.dp.toPx() }

    AppScaffold(
        title = "Adjust image",
        onBack = onBack,
        actions = {
            IconButton(onClick = viewModel::resetEdits) { Icon(Icons.Rounded.RestartAlt, contentDescription = "Reset adjustments") }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val boxW = constraints.maxWidth.toFloat()
                val boxH = constraints.maxHeight.toFloat()
                val imgAspect = bitmap.width.toFloat() / bitmap.height
                val (dispW, dispH) = if (boxW / boxH > imgAspect) (boxH * imgAspect) to boxH else boxW to (boxW / imgAspect)
                val density = LocalDensity.current
                Box(
                    Modifier
                        .size(with(density) { dispW.toDp() }, with(density) { dispH.toDp() })
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = "Captured document",
                        contentScale = ContentScale.FillBounds,
                        colorFilter = ColorFilter.colorMatrix(matrix),
                        modifier = Modifier.fillMaxSize(),
                    )
                    var active by remember { mutableStateOf(DragHandle.None) }
                    Canvas(
                        Modifier
                            .fillMaxSize()
                            .pointerInput(dispW, dispH) {
                                detectDragGestures(
                                    onDragStart = { p ->
                                        val c = currentCrop
                                        val corners = listOf(
                                            DragHandle.TopLeft to Offset(c.left * dispW, c.top * dispH),
                                            DragHandle.TopRight to Offset(c.right * dispW, c.top * dispH),
                                            DragHandle.BottomLeft to Offset(c.left * dispW, c.bottom * dispH),
                                            DragHandle.BottomRight to Offset(c.right * dispW, c.bottom * dispH),
                                        )
                                        val nearest = corners.minByOrNull { (it.second - p).getDistance() }
                                        active = when {
                                            nearest != null && (nearest.second - p).getDistance() < handleRadiusPx * 1.6f -> nearest.first
                                            p.x / dispW in c.left..c.right && p.y / dispH in c.top..c.bottom -> DragHandle.Move
                                            else -> DragHandle.None
                                        }
                                    },
                                    onDragEnd = { active = DragHandle.None },
                                    onDragCancel = { active = DragHandle.None },
                                    onDrag = { change, drag ->
                                        change.consume()
                                        val dx = drag.x / dispW
                                        val dy = drag.y / dispH
                                        val c = currentCrop
                                        val minSize = 0.08f
                                        val next = when (active) {
                                            DragHandle.TopLeft -> c.copy(
                                                left = (c.left + dx).coerceIn(0f, c.right - minSize),
                                                top = (c.top + dy).coerceIn(0f, c.bottom - minSize),
                                            )
                                            DragHandle.TopRight -> c.copy(
                                                right = (c.right + dx).coerceIn(c.left + minSize, 1f),
                                                top = (c.top + dy).coerceIn(0f, c.bottom - minSize),
                                            )
                                            DragHandle.BottomLeft -> c.copy(
                                                left = (c.left + dx).coerceIn(0f, c.right - minSize),
                                                bottom = (c.bottom + dy).coerceIn(c.top + minSize, 1f),
                                            )
                                            DragHandle.BottomRight -> c.copy(
                                                right = (c.right + dx).coerceIn(c.left + minSize, 1f),
                                                bottom = (c.bottom + dy).coerceIn(c.top + minSize, 1f),
                                            )
                                            DragHandle.Move -> {
                                                val mx = dx.coerceIn(-c.left, 1f - c.right)
                                                val my = dy.coerceIn(-c.top, 1f - c.bottom)
                                                CropRect(c.left + mx, c.top + my, c.right + mx, c.bottom + my)
                                            }
                                            DragHandle.None -> c
                                        }
                                        viewModel.setCrop(next)
                                    },
                                )
                            }
                    ) {
                        val c = state.crop
                        val l = c.left * size.width
                        val t = c.top * size.height
                        val r = c.right * size.width
                        val b = c.bottom * size.height
                        val shade = Color.Black.copy(alpha = 0.5f)
                        drawRect(shade, Offset.Zero, Size(size.width, t))
                        drawRect(shade, Offset(0f, b), Size(size.width, size.height - b))
                        drawRect(shade, Offset(0f, t), Size(l, b - t))
                        drawRect(shade, Offset(r, t), Size(size.width - r, b - t))
                        drawRect(Color.White, Offset(l, t), Size(r - l, b - t), style = Stroke(width = 3f))
                        // rule-of-thirds guides
                        for (i in 1..2) {
                            val x = l + (r - l) * i / 3f
                            val y = t + (b - t) * i / 3f
                            drawLine(Color.White.copy(alpha = 0.35f), Offset(x, t), Offset(x, b), 1.5f)
                            drawLine(Color.White.copy(alpha = 0.35f), Offset(l, y), Offset(r, y), 1.5f)
                        }
                        val handle = handleRadiusPx * 0.45f
                        listOf(Offset(l, t), Offset(r, t), Offset(l, b), Offset(r, b)).forEach {
                            drawCircle(Brand.Purple, handle + 3f, it)
                            drawCircle(Color.White, handle, it)
                        }
                    }
                }
                if (state.busy) LoadingDots()
            }

            Spacer(Modifier.height(12.dp))
            PremiumCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.rotate(-90) }) { Icon(Icons.Rounded.RotateLeft, contentDescription = "Rotate left") }
                    IconButton(onClick = { viewModel.rotate(90) }) { Icon(Icons.Rounded.RotateRight, contentDescription = "Rotate right") }
                    Spacer(Modifier.weight(1f))
                    FilterChip(
                        selected = state.grayscale,
                        onClick = { viewModel.setGrayscale(!state.grayscale) },
                        label = { Text("B&W") },
                    )
                }
                AdjustSlider(Icons.Rounded.Brightness6, "Brightness", state.brightness, -100f..100f) { viewModel.setBrightness(it) }
                AdjustSlider(Icons.Rounded.Contrast, "Contrast", state.contrast, 0.5f..2f) { viewModel.setContrast(it) }
            }
            Spacer(Modifier.height(12.dp))
            GradientButton(
                text = "Scan text",
                icon = Icons.Rounded.DocumentScanner,
                onClick = { viewModel.startRecognition(); onScan() },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun AdjustSlider(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(84.dp))
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.weight(1f),
        )
    }
}

// ===================================================================== OCR processing

@Composable
fun OcrProcessingScreen(viewModel: ScannerViewModel, onBack: () -> Unit, onFinished: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.stage) { if (state.stage == ScanStage.Done) onFinished() }

    val transition = rememberInfiniteTransition(label = "scan")
    val sweep by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
        label = "sweep",
    )

    AppScaffold(title = "Recognising", onBack = onBack) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val preview = state.processedBitmap ?: state.sourceBitmap
            if (preview != null) {
                val img = remember(preview) { preview.asImageBitmap() }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(max(0.5f, min(2f, preview.width.toFloat() / preview.height)))
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    Image(img, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    if (state.stage == ScanStage.Processing) {
                        Canvas(Modifier.fillMaxSize()) {
                            val y = size.height * sweep
                            drawRect(Brand.Purple.copy(alpha = 0.18f), Offset(0f, 0f), Size(size.width, y))
                            drawLine(Brand.Cyan, Offset(0f, y), Offset(size.width, y), strokeWidth = 6f)
                        }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            if (state.stage == ScanStage.Failed) {
                Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(8.dp))
                Text(state.error ?: "Something went wrong", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onBack) { Text("Back") }
                    GradientButton("Retry", onClick = viewModel::startRecognition, icon = Icons.Rounded.Refresh)
                }
            } else {
                LoadingDots()
                Spacer(Modifier.height(14.dp))
                Text(state.statusText.ifEmpty { "Working…" }, style = MaterialTheme.typography.titleMedium)
                Text(
                    "On-device recognition · nothing is uploaded",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

