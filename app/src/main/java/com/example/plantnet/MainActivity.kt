package com.example.plantnet

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.plantnet.data.model.PlantResult
import com.example.plantnet.ui.theme.PlantNetTheme
import com.example.plantnet.ui.viewmodel.IdentificationState
import com.example.plantnet.ui.viewmodel.PlantViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlantNetTheme {
                MainScreen()
            }
        }
    }
}

/**
 * Pantallas de navegación principales
 */
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Identify : Screen("identify", "Identificar", Icons.Default.Search)
    object History : Screen("history", "Historial", Icons.Default.List)
    object Settings : Screen("settings", "Ajustes", Icons.Default.Settings)
}

/**
 * Pantalla principal con navegación
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val screens = listOf(Screen.Identify, Screen.History, Screen.Settings)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Identify.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Identify.route) {
                IdentifyScreen()
            }
            composable(Screen.History.route) {
                HistoryScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}

/**
 * Pantalla para identificar plantas
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun IdentifyScreen(viewModel: PlantViewModel = viewModel()) {
    val identificationState by viewModel.identificationState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var capturedImageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var showCamera by remember { mutableStateOf(false) }
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Identificación de Plantas",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Verificación de permisos de cámara
        if (!cameraPermissionState.status.isGranted) {
            PermissionRequest(
                shouldShowRationale = cameraPermissionState.status.shouldShowRationale,
                onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
            )
        } else {
            if (showCamera) {
                // Mostrar cámara para capturar imagen
                CameraCapture(
                    onImageCaptured = { bitmap ->
                        capturedImageBitmap = bitmap
                        showCamera = false
                    },
                    onError = { error ->
                        Log.e("Camera", "Error: $error")
                        showCamera = false
                    }
                )
            } else {
                // Área para mostrar la imagen capturada
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (capturedImageBitmap != null) {
                        Image(
                            bitmap = capturedImageBitmap!!,
                            contentDescription = "Imagen capturada",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    } else {
                        Text("No hay imagen capturada")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Botón para capturar foto
                Button(
                    onClick = { showCamera = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Cámara"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tomar Foto")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Botón para identificar planta
                Button(
                    onClick = {
                        capturedImageBitmap?.let { bitmap ->
                            viewModel.identifyPlant(context, bitmap)
                        }
                    },
                    enabled = capturedImageBitmap != null && identificationState !is IdentificationState.Loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Identificar Planta")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Mostrar resultados o estado de identificación
                when (val state = identificationState) {
                    is IdentificationState.Initial -> {
                        // Mostrar instrucciones iniciales
                        Text("Toma una foto de una planta y presiona 'Identificar'")
                    }
                    is IdentificationState.Loading -> {
                        // Mostrar indicador de carga
                        CircularProgressIndicator()
                        Text("Identificando planta...", modifier = Modifier.padding(top = 8.dp))
                    }
                    is IdentificationState.Success -> {
                        // Mostrar resultados
                        ResultsList(state.results)
                    }
                    is IdentificationState.Error -> {
                        // Mostrar error
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Button(
                            onClick = {
                                capturedImageBitmap?.let { bitmap ->
                                    viewModel.identifyPlant(context, bitmap)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Reintentar")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Pantalla de historial (placeholder)
 */
@Composable
fun HistoryScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.List,
                contentDescription = "Historial",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Historial de Identificaciones",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Próximamente: aquí podrás ver tus identificaciones anteriores",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

/**
 * Pantalla de configuración (placeholder)
 */
@Composable
fun SettingsScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Ajustes",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Configuración",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Próximamente: aquí podrás ajustar las configuraciones de la aplicación",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

/**
 * Lista de resultados de identificación
 */
@Composable
fun ResultsList(results: List<PlantResult>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Resultados de la identificación:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        results.forEachIndexed { index, result ->
            val species = result.species
            val confidence = (result.score * 100).toInt()
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = species.scientificNameWithoutAuthor,
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    if (species.commonNames.isNotEmpty()) {
                        Text(
                            text = species.commonNames.joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    Text(
                        text = "Familia: ${species.family.scientificNameWithoutAuthor}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    Text(
                        text = "Confianza: $confidence%",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

/**
 * Solicitud de permisos de cámara
 */
@Composable
fun PermissionRequest(
    shouldShowRationale: Boolean,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (shouldShowRationale) {
                "El permiso de cámara es necesario para poder identificar plantas"
            } else {
                "Se necesita permiso para acceder a la cámara"
            },
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = onRequestPermission) {
            Text("Solicitar permiso")
        }
    }
}

/**
 * Componente para capturar imágenes con la cámara
 */
@Composable
fun CameraCapture(
    onImageCaptured: (ImageBitmap) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var cameraExecutor: ExecutorService? by remember { mutableStateOf(null) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    
    DisposableEffect(lifecycleOwner) {
        cameraExecutor = Executors.newSingleThreadExecutor()
        onDispose {
            cameraExecutor?.shutdown()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Previsualización de la cámara
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    this.scaleType = PreviewView.ScaleType.FILL_CENTER
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
                
                // Configuración de la cámara
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener(
                    {
                        val cameraProvider = cameraProviderFuture.get()
                        
                        // Configuración de preview
                        val preview = Preview.Builder().build()
                        preview.setSurfaceProvider(previewView.surfaceProvider)
                        
                        // Configuración de captura de imágenes
                        imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        
                        try {
                            // Unbind para reusar la cámara
                            cameraProvider.unbindAll()
                            
                            // Bind a lifecycle
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture
                            )
                        } catch (e: Exception) {
                            onError("Error al configurar la cámara: ${e.message}")
                        }
                    },
                    ContextCompat.getMainExecutor(ctx)
                )
                
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Botón de captura
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Button(
                onClick = {
                    imageCapture?.let { capture ->
                        capture.takePicture(
                            cameraExecutor!!,
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val bitmap = image.toBitmap()
                                    val rotatedBitmap = bitmap.rotateBitmap(image.imageInfo.rotationDegrees.toFloat())
                                    onImageCaptured(rotatedBitmap.asImageBitmap())
                                    image.close()
                                }
                                
                                override fun onError(exception: ImageCaptureException) {
                                    onError("Error al capturar la imagen: ${exception.message}")
                                }
                            }
                        )
                    } ?: run {
                        onError("No se pudo inicializar la captura de imágenes")
                    }
                },
                shape = CircleShape,
                modifier = Modifier
                    .size(80.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                // Contenido vacío para el botón circular
            }
        }
    }
}

/**
 * Extensión para convertir un ImageProxy a Bitmap
 */
fun ImageProxy.toBitmap(): Bitmap {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    
    return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

/**
 * Extensión para rotar un Bitmap según un ángulo específico
 */
fun Bitmap.rotateBitmap(angle: Float): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(angle)
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

/**
 * Función para obtener un proveedor de cámara de forma suspendida
 */
suspend fun getCameraProvider(context: Context): ProcessCameraProvider = suspendCoroutine { continuation ->
    ProcessCameraProvider.getInstance(context).also { future ->
        future.addListener(
            {
                continuation.resume(future.get())
            },
            ContextCompat.getMainExecutor(context)
        )
    }
}

@ComposePreview(showBackground = true)
@Composable
fun DefaultPreview() {
    PlantNetTheme {
        MainScreen()
    }
}