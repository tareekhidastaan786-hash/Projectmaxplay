package com.example



import android.content.res.Configuration

import android.os.Build

import android.os.Bundle

import androidx.activity.ComponentActivity

import androidx.activity.compose.setContent

import androidx.activity.enableEdgeToEdge

import androidx.compose.animation.*

import androidx.compose.foundation.background

import androidx.compose.foundation.layout.*

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.FolderOpen

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.data.model.MediaItem

import com.example.ui.screens.DashboardScreen

import com.example.ui.screens.FullMusicPlayerSheet

import com.example.ui.screens.MusicMiniPlayer

import com.example.ui.screens.VideoPlayerScreen

import com.example.ui.screens.SplashScreen

import com.google.accompanist.permissions.ExperimentalPermissionsApi

import com.google.accompanist.permissions.rememberMultiplePermissionsState

import com.example.ui.theme.MyApplicationTheme

import com.example.ui.theme.NeonGreen



class MainActivity : ComponentActivity() {

    private val isInPictureInPicture = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.BLACK),
            navigationBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.BLACK)
        )

        setContent {
            val viewModel: com.example.ui.viewmodel.MediaViewModel = viewModel()
            
            val themeMode by viewModel.themeMode.collectAsState()
            val useDynamicColors by viewModel.useDynamicColors.collectAsState()
            
            val isDark = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> true // Fallback/System standard
            }

            MyApplicationTheme(
                darkTheme = isDark,
                useDynamicColors = useDynamicColors
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppLayout(
                        viewModel = viewModel,
                        isInPiP = isInPictureInPicture.value
                    )
                }
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPictureInPicture.value = isInPictureInPictureMode
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainAppLayout(
    viewModel: com.example.ui.viewmodel.MediaViewModel,
    isInPiP: Boolean
) {
    var activeVideo by remember { mutableStateOf<MediaItem?>(null) }
    var isSplashFinished by rememberSaveable { mutableStateOf(false) }
    if (!isSplashFinished) {
        SplashScreen(
            viewModel = viewModel,
            onSplashFinished = { isSplashFinished = true }
        )
        return
    }

    var isMusicExpanded by remember { mutableStateOf(false) }

    val currentPlayingSong by viewModel.currentPlayingSong.collectAsState()
    val defaultSpeed by viewModel.defaultPlaybackSpeed.collectAsState()
    val isGesturesEnabled by viewModel.gesturesEnabled.collectAsState()
    val isPipEnabled by viewModel.pipEnabled.collectAsState()
    val subtitleSize by viewModel.subtitleSize.collectAsState()
    val subtitleColor by viewModel.subtitleColor.collectAsState()
    val expandPlayerEvent by viewModel.expandPlayerEvent.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    DisposableEffect(activeVideo) {
        val window = (context as? ComponentActivity)?.window
        if (activeVideo == null && window != null) {
            window.statusBarColor = android.graphics.Color.BLACK
            window.navigationBarColor = android.graphics.Color.BLACK
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.navigationBarDividerColor = android.graphics.Color.BLACK
            }
            val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
            controller.show(
                androidx.core.view.WindowInsetsCompat.Type.statusBars() or
                androidx.core.view.WindowInsetsCompat.Type.navigationBars()
            )
        }
        onDispose { }
    }

    LaunchedEffect(expandPlayerEvent) {
        if (expandPlayerEvent) {
            isMusicExpanded = true
            viewModel.expandPlayerEvent.value = false
        }
    }

    // Permissions declaration
    val permissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                android.Manifest.permission.READ_MEDIA_AUDIO,
                android.Manifest.permission.READ_MEDIA_VIDEO,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            listOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }

    val permissionState = rememberMultiplePermissionsState(permissionsToRequest)

    // Trigger permission request or auto-scan once granted
    LaunchedEffect(permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted) {
            viewModel.refreshMedia()
        } else {
            permissionState.launchMultiplePermissionRequest()
        }
    }

    // --- PiP HUD Render override ---
    if (isInPiP && activeVideo != null) {
        VideoPlayerScreen(
            videoItem = activeVideo!!,
            viewModel = viewModel,
            defaultSpeed = defaultSpeed,
            isGesturesEnabled = isGesturesEnabled,
            isPipEnabled = isPipEnabled,
            subtitleSize = subtitleSize,
            subtitleColor = subtitleColor,
            onBack = { activeVideo = null }
        )
        return
    }

    // --- Standard HUD Layout Render ---
    if (!permissionState.allPermissionsGranted) {
        // Permission Requesting Screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF070B18))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Permission Required",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "MaxPlay needs storage access to scan and display local media files from your device.",
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { permissionState.launchMultiplePermissionRequest() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                ) {
                    Text("Grant Permission", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { viewModel.refreshMedia() }) {
                    Text("Continue with Cloud Demos", color = NeonGreen)
                }
            }
        }
    } else {
        // Main Core HUD
        Box(modifier = Modifier.fillMaxSize()) {
            if (activeVideo != null) {
                VideoPlayerScreen(
                    videoItem = activeVideo!!,
                    viewModel = viewModel,
                    defaultSpeed = defaultSpeed,
                    isGesturesEnabled = isGesturesEnabled,
                    isPipEnabled = isPipEnabled,
                    subtitleSize = subtitleSize,
                    subtitleColor = subtitleColor,
                    onBack = { activeVideo = null }
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        DashboardScreen(
                            viewModel = viewModel,
                            isMusicExpanded = isMusicExpanded,
                            onExpandMiniPlayer = { isMusicExpanded = true },
                            onPlayVideo = { video ->
                                // Pause music if playing
                                if (viewModel.isPlayingMusic.value) {
                                    viewModel.togglePlayPauseMusic()
                                }
                                viewModel.addToRecentlyPlayed(video)
                                activeVideo = video
                            }
                        )
                    }
                }

                // Full screen music player slide-up panel overlay
                AnimatedVisibility(
                    visible = isMusicExpanded,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    FullMusicPlayerSheet(
                        viewModel = viewModel,
                        onCollapse = { isMusicExpanded = false }
                    )
                }
            }
        }
    }
}
