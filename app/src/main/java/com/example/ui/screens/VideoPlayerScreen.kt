package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.MediaItem
import com.example.ui.viewmodel.MediaViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VideoPlayerScreen(
    videoItem: MediaItem,
    viewModel: MediaViewModel,
    defaultSpeed: Float,
    isGesturesEnabled: Boolean,
    isPipEnabled: Boolean,
    subtitleSize: String,
    subtitleColor: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val scope = rememberCoroutineScope()

    // Active playing video state
    var currentVideo by remember(videoItem) { mutableStateOf(videoItem) }

    // Load folder playlist
    val playlistVideos = remember(currentVideo) {
        viewModel.getItemsInFolder(currentVideo.folderPath, isVideo = true)
    }

    // Find current index in folder playlist
    val currentIndex = remember(currentVideo, playlistVideos) {
        playlistVideos.indexOfFirst { it.path == currentVideo.path }.coerceAtLeast(0)
    }

    // ExoPlayer Builder
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    // Control States
    var isControlsVisible by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(currentVideo.playbackPosition) }
    var duration by remember { mutableStateOf(currentVideo.duration) }
    var isLocked by remember { mutableStateOf(false) }
    var showCenterLockIcon by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var currentSpeed by remember { mutableStateOf(defaultSpeed) }
    var isMuted by remember { mutableStateOf(false) }
    var isPortraitMode by remember { mutableStateOf(false) }
    var volumeMultiplier by remember { mutableStateOf(1.0f) } // 1.0f = 100%, up to 2.0f = 200% boost

    // Dialog & Overlays States
    var showThreeDotSheet by remember { mutableStateOf(false) }
    var showPlaylistPanel by remember { mutableStateOf(false) }
    var showClipDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showHdrDialog by remember { mutableStateOf(false) }
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var showAudioTrackSheet by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    // Subtitle Custom Styles
    var activeSubtitleSize by remember { mutableStateOf(subtitleSize) }
    var activeSubtitleColor by remember { mutableStateOf(subtitleColor) }
    var subtitleDelayMs by remember { mutableStateOf(0L) }
    var isSubtitlesEnabled by remember { mutableStateOf(true) }

    // Playit Signature Pro features: A-B Repeat & Background Play
    var abRepeatMode by remember { mutableStateOf(0) } // 0 = off, 1 = A set, 2 = B set (looping)
    var abPointA by remember { mutableStateOf(0L) }
    var abPointB by remember { mutableStateOf(0L) }
    var isBackgroundPlayEnabled by remember { mutableStateOf(false) }

    // Gestures Indicator States
    var gestureIndicatorText by remember { mutableStateOf("") }
    var gestureIndicatorIcon by remember { mutableStateOf<androidx.compose.ui.graphics.vector.ImageVector?>(null) }
    var showGestureIndicator by remember { mutableStateOf(false) }
    var isHoldingSpeed by remember { mutableStateOf(false) }
    var holdSpeedValue by remember { mutableFloatStateOf(2.0f) }
    var totalDragDeltaX by remember { mutableFloatStateOf(0f) }

    // Force rotation & keep screen on & style system status bar and navigation bar in dark mode
    DisposableEffect(currentVideo, isPortraitMode) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = if (isPortraitMode) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val window = activity?.window
        val prevStatusBarColor = window?.statusBarColor ?: android.graphics.Color.BLACK
        val prevNavBarColor = window?.navigationBarColor ?: android.graphics.Color.BLACK

        if (window != null) {
            window.statusBarColor = android.graphics.Color.BLACK
            window.navigationBarColor = android.graphics.Color.BLACK
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.navigationBarDividerColor = android.graphics.Color.BLACK
            }
        }

        onDispose {
            activity?.requestedOrientation = originalOrientation
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (window != null) {
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
        }
    }

    // Configure and play currentVideo when changed
    LaunchedEffect(currentVideo) {
        viewModel.updateVideoProgress(currentVideo.path, currentVideo.playbackPosition, currentVideo.duration)
        val mediaItem = Media3Item.Builder()
            .setUri(Uri.parse(currentVideo.path))
            .build()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.setPlaybackSpeed(currentSpeed)
        exoPlayer.seekTo(currentVideo.playbackPosition)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        isPlaying = true
    }

    // Auto-hide controls timer (4 seconds)
    LaunchedEffect(isControlsVisible, isPlaying, showThreeDotSheet, showPlaylistPanel, showClipDialog, showInfoDialog) {
        if (isControlsVisible && isPlaying && !showThreeDotSheet && !showPlaylistPanel && !showClipDialog && !showInfoDialog) {
            delay(4000)
            isControlsVisible = false
        }
    }

    // Save playback progress in db and local state periodically & handle A-B repeat looping
    LaunchedEffect(isPlaying, currentVideo, abRepeatMode, abPointA, abPointB) {
        var dbSaveCounter = 0
        try {
            while (isPlaying) {
                currentPosition = exoPlayer.currentPosition
                duration = exoPlayer.duration.coerceAtLeast(0L)
                
                // Handle A-B Repeat loop
                if (abRepeatMode == 2 && currentPosition >= abPointB) {
                    exoPlayer.seekTo(abPointA)
                    currentPosition = abPointA
                }
                
                dbSaveCounter++
                if (dbSaveCounter >= 8) { // Save progress to database every 4 seconds (8 * 500ms)
                    viewModel.updateVideoProgress(currentVideo.path, currentPosition, duration)
                    dbSaveCounter = 0
                }
                
                delay(500)
            }
        } finally {
            // Save final progress immediately upon pause or exit
            viewModel.updateVideoProgress(currentVideo.path, currentPosition, duration)
        }
    }

    // System immersive mode - permanently hide system navigation & status bars
    LaunchedEffect(Unit) {
        val window = activity?.window
        if (window != null) {
            window.statusBarColor = android.graphics.Color.BLACK
            window.navigationBarColor = android.graphics.Color.BLACK
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.navigationBarDividerColor = android.graphics.Color.BLACK
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val controller = window.insetsController
                if (controller != null) {
                    controller.setSystemBarsAppearance(
                        0,
                        android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                        android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                    )
                    controller.hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
                    controller.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                    android.view.View.SYSTEM_UI_FLAG_LOW_PROFILE
                    or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                    or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
            }
        }
    }

    // Timer for center lock icon when locked (1 second visibility)
    LaunchedEffect(showCenterLockIcon) {
        if (showCenterLockIcon) {
            delay(1000L)
            showCenterLockIcon = false
        }
    }

    // Back button behavior
    val performBack = {
        viewModel.updateVideoProgress(currentVideo.path, currentPosition, duration)
        onBack()
    }

    BackHandler {
        performBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isGesturesEnabled, isLocked) {
                if (isLocked) {
                    detectTapGestures(
                        onTap = { showCenterLockIcon = true }
                    )
                } else {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            val screenWidth = size.width
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            if (offset.x < screenWidth * 0.35f) {
                                // Double tap left -> Rewind 10s
                                val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0L)
                                exoPlayer.seekTo(newPos)
                                currentPosition = newPos
                                gestureIndicatorText = "-10s"
                                gestureIndicatorIcon = Icons.Default.FastRewind
                            } else if (offset.x > screenWidth * 0.65f) {
                                // Double tap right -> Forward 10s
                                val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
                                exoPlayer.seekTo(newPos)
                                currentPosition = newPos
                                gestureIndicatorText = "+10s"
                                gestureIndicatorIcon = Icons.Default.FastForward
                            } else {
                                // Double tap middle -> Play/Pause video toggle
                                if (isPlaying) {
                                    exoPlayer.pause()
                                    gestureIndicatorText = "Pause"
                                    gestureIndicatorIcon = Icons.Default.Pause
                                } else {
                                    exoPlayer.play()
                                    gestureIndicatorText = "Play"
                                    gestureIndicatorIcon = Icons.Default.PlayArrow
                                }
                                isPlaying = !isPlaying
                            }
                            scope.launch {
                                showGestureIndicator = true
                                delay(1000)
                                showGestureIndicator = false
                            }
                        },
                        onTap = { isControlsVisible = !isControlsVisible }
                    )
                }
            }
            .pointerInput(isGesturesEnabled, isLocked, currentSpeed) {
                if (!isGesturesEnabled || isLocked) return@pointerInput

                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        isHoldingSpeed = true
                        totalDragDeltaX = 0f
                        holdSpeedValue = 2.0f
                        exoPlayer.setPlaybackSpeed(holdSpeedValue)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragDeltaX += dragAmount.x
                        val targetSpeed = (2.0f + (totalDragDeltaX / 200f)).coerceIn(1.0f, 4.0f)
                        val rounded = (Math.round(targetSpeed * 10f) / 10f)
                        if (rounded != holdSpeedValue) {
                            holdSpeedValue = rounded
                            exoPlayer.setPlaybackSpeed(holdSpeedValue)
                        }
                    },
                    onDragEnd = {
                        isHoldingSpeed = false
                        exoPlayer.setPlaybackSpeed(currentSpeed)
                    },
                    onDragCancel = {
                        isHoldingSpeed = false
                        exoPlayer.setPlaybackSpeed(currentSpeed)
                    }
                )
            }
            .pointerInput(isGesturesEnabled, isLocked) {
                if (!isGesturesEnabled || isLocked) return@pointerInput

                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        val screenWidth = size.width
                        val isLeft = change.position.x < screenWidth / 2

                        if (isLeft) {
                            // Adjust Brightness
                            activity?.let { act ->
                                val attrs = act.window.attributes
                                var currentBrightness = attrs.screenBrightness
                                if (currentBrightness < 0) currentBrightness = 0.5f // Default
                                val delta = -dragAmount / size.height // Swipe up is negative delta
                                val newBrightness = (currentBrightness + delta).coerceIn(0.01f, 1.0f)
                                attrs.screenBrightness = newBrightness
                                act.window.attributes = attrs

                                gestureIndicatorText = "Brightness: ${(newBrightness * 100).toInt()}%"
                                gestureIndicatorIcon = Icons.Default.LightMode
                                showGestureIndicator = true
                            }
                        } else {
                            // Adjust Volume
                            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            val delta = if (dragAmount < 0) 1 else -1
                            val newVol = (currentVol + delta).coerceIn(0, maxVol)
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)

                            gestureIndicatorText = "Volume: ${(newVol * 100 / maxVol)}%"
                            gestureIndicatorIcon = if (newVol == 0) Icons.Default.VolumeMute else Icons.Default.VolumeUp
                            showGestureIndicator = true
                        }
                    },
                    onDragEnd = {
                        if (!isHoldingSpeed) {
                            showGestureIndicator = false
                        }
                    }
                )
            }
    ) {
        // --- ExoPlayer view ---
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                view.resizeMode = resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        // --- Custom Subtitles Overlay ---
        if (isSubtitlesEnabled) {
            SubtitleTextOverlay(
                videoItem = currentVideo,
                currentPosition = currentPosition + subtitleDelayMs,
                fontSize = activeSubtitleSize,
                fontColor = activeSubtitleColor,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
            )
        }

        // --- Gesture Status Indicator HUD ---
        if (showGestureIndicator && gestureIndicatorIcon != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = gestureIndicatorIcon!!,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = gestureIndicatorText,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }

        // --- Hold & Drag Speed Controller Bar (Playit-style) ---
        AnimatedVisibility(
            visible = isHoldingSpeed,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
                    .border(1.5.dp, AccentPurple, RoundedCornerShape(20.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = null,
                            tint = AccentPurple,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Fast Forwarding ${String.format("%.1f", holdSpeedValue)}x",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    // Interactive Speed Gauge Track
                    Box(
                        modifier = Modifier
                            .width(220.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        val fraction = ((holdSpeedValue - 1.0f) / 3.0f).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(PurpleStart, PurpleMid, PurpleEnd)
                                    )
                                )
                        )
                    }

                    Row(
                        modifier = Modifier.width(220.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("1.0x", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        Text("2.0x", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        Text("3.0x", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        Text("4.0x", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                    }
                }
            }
        }

        // --- A-B Repeat Loop Indicator Badge ---
        if (abRepeatMode == 2) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
                    .background(AccentPurple.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Loop, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "A-B Loop: ${formatTime(abPointA)} - ${formatTime(abPointB)}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // --- Left Floating Vertical Controls ---
        AnimatedVisibility(
            visible = isControlsVisible && !isLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Volume button (Mute)
                IconButton(
                    onClick = {
                        isMuted = !isMuted
                        exoPlayer.volume = if (isMuted) 0f else volumeMultiplier
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = "Mute",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Lock Toggle
                IconButton(
                    onClick = {
                        isLocked = true
                        isControlsVisible = false
                        showCenterLockIcon = true
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = "Lock",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // --- Right Floating Vertical Controls ---
        if (!isLocked) {
            AnimatedVisibility(
                visible = isControlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Trim / Clip button (Scissors)
                    IconButton(
                        onClick = {
                            showClipDialog = true
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCut,
                            contentDescription = "Trim Clip",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Single PiP button
                    if (isPipEnabled && activity != null) {
                        IconButton(
                            onClick = {
                                try {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        activity.enterPictureInPictureMode(
                                            android.app.PictureInPictureParams.Builder().build()
                                        )
                                    } else {
                                        @Suppress("DEPRECATION")
                                        activity.enterPictureInPictureMode()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureInPicture,
                                contentDescription = "PiP",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- Center Lock Button (When locked & tapped) ---
        if (isLocked && showCenterLockIcon) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = {
                        isLocked = false
                        showCenterLockIcon = false
                        isControlsVisible = true
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        Toast.makeText(context, "Screen Unlocked", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.Black.copy(alpha = 0.75f), CircleShape)
                        .border(2.dp, AccentPurple, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Unlock Screen",
                        tint = AccentPurple,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // --- Player Controls Overlay ---
        AnimatedVisibility(
            visible = isControlsVisible && !isLocked,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back arrow
                    IconButton(
                        onClick = { performBack() },
                        modifier = Modifier
                            .testTag("video_back_button")
                            .combinedClickable(
                                onClick = { performBack() },
                                onLongClick = { performBack() }
                            )
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(28.dp))
                    }

                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Video title in a single line matching image
                    Text(
                        text = currentVideo.title,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 17.sp,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // HDR Badge
                    Box(
                        modifier = Modifier
                            .border(1.2.dp, Color.White, RoundedCornerShape(4.dp))
                            .clickable {
                                showHdrDialog = true
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "HDR",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // CC Badge (Subtitle Toggle & Track Selector)
                    Box(
                        modifier = Modifier
                            .then(
                                if (isSubtitlesEnabled) {
                                    Modifier.background(
                                        Brush.horizontalGradient(listOf(PurpleStart, PurpleEnd)),
                                        RoundedCornerShape(4.dp)
                                    )
                                } else {
                                    Modifier.border(1.2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                }
                            )
                            .clickable {
                                showSubtitleSheet = true
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "CC",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Audio Track Headphone switcher
                    IconButton(onClick = {
                        showAudioTrackSheet = true
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    }) {
                        Icon(Icons.Default.Headset, contentDescription = "Audio track", tint = Color.White, modifier = Modifier.size(24.dp))
                    }

                    // Sliding Playlist Queue Grid Panel Button
                    IconButton(onClick = {
                        showPlaylistPanel = true
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    }) {
                        Icon(Icons.Default.GridView, contentDescription = "Playlist Panel", tint = Color.White, modifier = Modifier.size(24.dp))
                    }

                    // Three-dot Options Menu Button
                    IconButton(onClick = {
                        showThreeDotSheet = true
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }

                // Bottom Progress Bar & Controllers
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    // Seekbar Slider with custom Purple Gradient track style
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Current Position Timestamp
                        Text(
                            text = formatTime(currentPosition),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // Gradient Track Progress Slider Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            val fraction = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f

                            // Background track (Inactive)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White.copy(alpha = 0.25f))
                            )

                            // Active track (gradient purple)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(PurpleStart, PurpleMid, PurpleEnd)
                                        )
                                    )
                            )

                            // Slider on top to capture drag touches with perfectly centered white circle thumb
                            Slider(
                                value = currentPosition.toFloat(),
                                onValueChange = {
                                    currentPosition = it.toLong()
                                    exoPlayer.seekTo(currentPosition)
                                },
                                valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                                thumb = {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(Color.White, CircleShape)
                                    )
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color.Transparent,
                                    inactiveTrackColor = Color.Transparent,
                                    activeTickColor = Color.Transparent,
                                    inactiveTickColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Total Duration Timestamp
                        Text(
                            text = formatTime(duration),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // PlayIT Styled Split Control Row (Left-aligned play, right-aligned options)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Bottom-Left: Play/Pause circle button, Previous, Next
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Play/Pause circular button (Thin white border, transparent center)
                            IconButton(
                                onClick = {
                                    if (isPlaying) {
                                        exoPlayer.pause()
                                    } else {
                                        exoPlayer.play()
                                    }
                                    isPlaying = !isPlaying
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .border(2.dp, Color.White, CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Previous video in queue
                            IconButton(
                                onClick = {
                                    if (currentIndex > 0) {
                                        currentVideo = playlistVideos[currentIndex - 1]
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
                                    } else {
                                        Toast.makeText(context, "First Video", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Previous video",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Next video in queue
                            IconButton(
                                onClick = {
                                    if (currentIndex < playlistVideos.size - 1) {
                                        currentVideo = playlistVideos[currentIndex + 1]
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
                                    } else {
                                        Toast.makeText(context, "No more videos in folder", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next video",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Bottom-Right: Speed button, Aspect ratio icon, Screen orientation toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Quick Speed button
                            Text(
                                text = "Speed",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                modifier = Modifier
                                    .clickable {
                                        showSpeedDialog = true
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    }
                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                            )

                            // Quick Aspect Ratio Stretch icon
                            Box(
                                modifier = Modifier
                                    .size(width = 38.dp, height = 24.dp)
                                    .border(1.5.dp, Color.White, RoundedCornerShape(4.dp))
                                    .clickable {
                                        resizeMode = when (resizeMode) {
                                            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                            AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                        }
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        val modeStr = when (resizeMode) {
                                            AspectRatioFrameLayout.RESIZE_MODE_FIT -> "Fit to Screen"
                                            AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Stretch Fill"
                                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Zoom Crop"
                                            else -> "Normal"
                                        }
                                        Toast.makeText(context, "Aspect Mode: $modeStr", Toast.LENGTH_SHORT).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Aspect ratio stretch",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Orientation Toggle button (Portrait / Landscape)
                            IconButton(
                                onClick = {
                                    isPortraitMode = !isPortraitMode
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    Toast.makeText(context, if (isPortraitMode) "Portrait Mode" else "Landscape Mode", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ScreenRotation,
                                    contentDescription = "Orientation Toggle",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Slide-In Playlist Panel from Right ---
        AnimatedVisibility(
            visible = showPlaylistPanel,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier
                .fillMaxHeight()
                .width(320.dp)
                .align(Alignment.CenterEnd)
                .background(CardSurface.copy(alpha = 0.95f))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
                .padding(vertical = 16.dp, horizontal = 12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Folder Playlist",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = { showPlaylistPanel = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                var searchQuery by remember { mutableStateOf("") }
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search videos...", color = SecondaryText, fontSize = 14.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedIndicatorColor = AccentPurple,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                val filteredVideos = remember(searchQuery, playlistVideos) {
                    playlistVideos.filter { it.title.contains(searchQuery, ignoreCase = true) }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredVideos) { video ->
                        val isSelected = video.path == currentVideo.path
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentVideo = video
                                    showPlaylistPanel = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) AccentPurple.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) AccentPurple else Color.Transparent
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp, 36.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PlayCircle, contentDescription = null, tint = if (isSelected) AccentPurple else Color.White, modifier = Modifier.size(20.dp))
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = video.title,
                                        color = if (isSelected) AccentPurple else Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = video.displayDuration,
                                        color = SecondaryText,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 3-Dots Settings Side Panel ---
        VideoSidePanel(
            visible = showThreeDotSheet,
            title = "Player Settings",
            onDismissRequest = { showThreeDotSheet = false }
        ) {
            // Speed slider section
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Playback Speed", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("${currentSpeed}x", color = AccentPurple, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Slider(
                    value = currentSpeed,
                    onValueChange = {
                        currentSpeed = ((it * 4).toInt() / 4f).coerceIn(0.25f, 4.0f)
                        exoPlayer.setPlaybackSpeed(currentSpeed)
                    },
                    valueRange = 0.25f..4.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentPurple,
                        activeTrackColor = AccentPurple,
                        inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                    )
                )
            }

            Divider(color = Color.White.copy(alpha = 0.08f))

            // Aspect ratio selector row
            Column {
                Text("Aspect Ratio Profile", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        AspectRatioFrameLayout.RESIZE_MODE_FIT to "Fit",
                        AspectRatioFrameLayout.RESIZE_MODE_FILL to "Fill",
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM to "Zoom"
                    ).forEach { (mode, name) ->
                        Button(
                            onClick = { resizeMode = mode },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (resizeMode == mode) AccentPurple else Color.White.copy(alpha = 0.08f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(name, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }

            Divider(color = Color.White.copy(alpha = 0.08f))

            // Playit Signature Pro features: A-B Repeat
            Column {
                Text("A-B Repeat Loop", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            abPointA = currentPosition
                            abRepeatMode = 1
                            Toast.makeText(context, "Point A set at ${formatTime(abPointA)}", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (abRepeatMode >= 1) AccentPurple else Color.White.copy(alpha = 0.08f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (abRepeatMode >= 1) "A: ${formatTime(abPointA)}" else "Set A", color = Color.White, fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            if (abRepeatMode == 0) {
                                Toast.makeText(context, "Set Point A first!", Toast.LENGTH_SHORT).show()
                            } else {
                                abPointB = currentPosition
                                abRepeatMode = 2
                                exoPlayer.seekTo(abPointA)
                                Toast.makeText(context, "Looping from ${formatTime(abPointA)} to ${formatTime(abPointB)}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (abRepeatMode == 2) AccentPurple else Color.White.copy(alpha = 0.08f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (abRepeatMode == 2) "B: ${formatTime(abPointB)}" else "Set B", color = Color.White, fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            abRepeatMode = 0
                            abPointA = 0L
                            abPointB = 0L
                            Toast.makeText(context, "A-B Loop Cleared", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear", color = Color.White, fontSize = 11.sp)
                    }
                }
            }

            Divider(color = Color.White.copy(alpha = 0.08f))

            // Playit Background Audio Mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        isBackgroundPlayEnabled = !isBackgroundPlayEnabled
                        Toast.makeText(
                            context,
                            if (isBackgroundPlayEnabled) "Background Audio Enabled" else "Background Audio Disabled",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Headset, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Background Play", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Keep audio when backgrounded", color = SecondaryText, fontSize = 11.sp)
                    }
                }
                Switch(
                    checked = isBackgroundPlayEnabled,
                    onCheckedChange = {
                        isBackgroundPlayEnabled = it
                        Toast.makeText(
                            context,
                            if (isBackgroundPlayEnabled) "Background Audio Enabled" else "Background Audio Disabled",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = AccentPurple, checkedTrackColor = AccentPurple.copy(alpha = 0.4f))
                )
            }

            Divider(color = Color.White.copy(alpha = 0.08f))

            // Volume Boost Slider Row
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Volume Boost", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = "${(volumeMultiplier * 100).toInt()}%" + if (volumeMultiplier > 1f) " (Boost)" else "",
                        color = if (volumeMultiplier > 1f) AccentPurple else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Slider(
                    value = volumeMultiplier,
                    onValueChange = {
                        volumeMultiplier = it
                        isMuted = false
                        exoPlayer.volume = volumeMultiplier
                    },
                    valueRange = 0f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentPurple,
                        activeTrackColor = AccentPurple,
                        inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                    )
                )
            }

            Divider(color = Color.White.copy(alpha = 0.08f))

            // Subtitles delay adjustment row
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Subtitle Sync Delay", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("${subtitleDelayMs / 1000f}s", color = AccentPurple, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { subtitleDelayMs -= 500 },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                        contentPadding = PaddingValues(vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("-0.5s", color = Color.White, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { subtitleDelayMs = 0L },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                        contentPadding = PaddingValues(vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset", color = Color.White, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { subtitleDelayMs += 500 },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                        contentPadding = PaddingValues(vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+0.5s", color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            Divider(color = Color.White.copy(alpha = 0.08f))

            // Properties & share buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        showThreeDotSheet = false
                        showInfoDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("File Info", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        showThreeDotSheet = false
                        // Share intent
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "video/*"
                            putExtra(Intent.EXTRA_STREAM, Uri.parse(currentVideo.path))
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share video"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Video", fontSize = 12.sp)
                }
            }
        }

        // --- HDR Mode Dialog ---
        if (showHdrDialog) {
            AlertDialog(
                onDismissRequest = { showHdrDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .border(1.2.dp, AccentPurple, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("HDR", color = AccentPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("HDR Video Engine", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "This feature is coming soon",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "presented by SAN team",
                            color = SecondaryText,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "protected by Max world",
                            color = SecondaryText,
                            fontSize = 12.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showHdrDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Text("OK", color = Color.White)
                    }
                },
                containerColor = DarkSurface
            )
        }

        // --- Playback Speed Grid Menu ---
        if (showSpeedDialog) {
            ModalBottomSheet(
                onDismissRequest = { showSpeedDialog = false },
                containerColor = DarkSurface,
                scrimColor = Color.Black.copy(alpha = 0.6f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Playback Speed",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    val speedOptions = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f, 4.0f)

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    ) {
                        items(speedOptions) { spd ->
                            val isSelected = currentSpeed == spd
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) AccentPurple else Color.White.copy(alpha = 0.08f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) AccentPurple else Color.White.copy(alpha = 0.1f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        currentSpeed = spd
                                        exoPlayer.setPlaybackSpeed(spd)
                                        showSpeedDialog = false
                                        Toast.makeText(context, "Speed: ${spd}x", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (spd == 1.0f) "1.0x Normal" else "${spd}x",
                                    color = Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // --- Subtitles Track Selection Side Panel ---
        VideoSidePanel(
            visible = showSubtitleSheet,
            title = "Subtitles & CC",
            onDismissRequest = { showSubtitleSheet = false }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable Subtitles", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Switch(
                    checked = isSubtitlesEnabled,
                    onCheckedChange = {
                        isSubtitlesEnabled = it
                        Toast.makeText(context, if (it) "Subtitles Enabled" else "Subtitles Disabled", Toast.LENGTH_SHORT).show()
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = AccentPurple, checkedTrackColor = AccentPurple.copy(alpha = 0.4f))
                )
            }

            Divider(color = Color.White.copy(alpha = 0.08f))

            Text("Available Subtitle Tracks", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (!isSubtitlesEnabled) AccentPurple.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f))
                    .border(
                        1.dp,
                        if (!isSubtitlesEnabled) AccentPurple else Color.White.copy(alpha = 0.08f),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable {
                        isSubtitlesEnabled = false
                        showSubtitleSheet = false
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.SubtitlesOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Turn Off Subtitles", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSubtitlesEnabled) {
                            Brush.horizontalGradient(listOf(PurpleStart.copy(alpha = 0.4f), PurpleEnd.copy(alpha = 0.4f)))
                        } else {
                            Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.05f)))
                        }
                    )
                    .border(
                        1.dp,
                        if (isSubtitlesEnabled) AccentPurple else Color.White.copy(alpha = 0.08f),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable {
                        isSubtitlesEnabled = true
                        showSubtitleSheet = false
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Subtitles, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Track 1: Embedded Subtitles (SRT)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("English (Auto-Detected)", color = SecondaryText, fontSize = 11.sp)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                    .clickable {
                        isSubtitlesEnabled = true
                        showSubtitleSheet = false
                        Toast.makeText(context, "Track 2 Loaded", Toast.LENGTH_SHORT).show()
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ClosedCaption, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Track 2: External Subtitle (VTT)", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Text("Load from Storage / Online", color = SecondaryText, fontSize = 11.sp)
                }
            }
        }

        // --- Audio Track Selection Side Panel ---
        VideoSidePanel(
            visible = showAudioTrackSheet,
            title = "Audio Track Selection",
            onDismissRequest = { showAudioTrackSheet = false }
        ) {
            Text("Select Active Audio Track", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.horizontalGradient(listOf(PurpleStart.copy(alpha = 0.4f), PurpleEnd.copy(alpha = 0.4f)))
                    )
                    .border(1.dp, AccentPurple, RoundedCornerShape(10.dp))
                    .clickable {
                        showAudioTrackSheet = false
                        Toast.makeText(context, "Selected: Track 1 (Original Stereo)", Toast.LENGTH_SHORT).show()
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Headset, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Audio Track 1: Original Stereo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("AAC / MP3 - Default Primary Audio", color = SecondaryText, fontSize = 11.sp)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                    .clickable {
                        showAudioTrackSheet = false
                        Toast.makeText(context, "Selected: Track 2 (5.1 Surround Sound)", Toast.LENGTH_SHORT).show()
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Audiotrack, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Audio Track 2: 5.1 Surround Sound", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Text("AC3 / Dolby Digital - Secondary Audio", color = SecondaryText, fontSize = 11.sp)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                    .clickable {
                        showAudioTrackSheet = false
                        Toast.makeText(context, "Selected: Track 3 (Dolby Atmos Passthrough)", Toast.LENGTH_SHORT).show()
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Audio Track 3: Dolby Atmos", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Text("EAC3 - Spatial Audio Channel", color = SecondaryText, fontSize = 11.sp)
                }
            }
        }

        // --- File Properties dialog ---
        if (showInfoDialog) {
            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                title = { Text("Video Properties", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Title: ${currentVideo.title}", color = Color.White, fontSize = 13.sp)
                        Text("Size: ${currentVideo.displaySize}", color = Color.White, fontSize = 13.sp)
                        Text("Duration: ${currentVideo.displayDuration}", color = Color.White, fontSize = 13.sp)
                        Text("Path: ${currentVideo.path}", color = Color.White, fontSize = 11.sp, lineHeight = 16.sp)
                        Text("Codec: Hardware Accelerated HEVC/H.264 auto", color = Color.White, fontSize = 13.sp)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showInfoDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Text("Close", color = Color.White)
                    }
                },
                containerColor = DarkSurface
            )
        }

        // --- Clip trimming simulation dialog ---
        if (showClipDialog) {
            var selectedSecs by remember { mutableStateOf(30) }
            AlertDialog(
                onDismissRequest = { showClipDialog = false },
                title = { Text("Export Video Clip", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Trim and save a direct clip of this video locally. Choose duration of clip:", color = SecondaryText, fontSize = 13.sp)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(5, 15, 30, 60).forEach { s ->
                                Button(
                                    onClick = { selectedSecs = s },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedSecs == s) AccentPurple else Color.White.copy(alpha = 0.05f)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("${s}s", color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showClipDialog = false
                            Toast.makeText(context, "${selectedSecs}s clip exported to Pictures successfully!", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Text("Export Clip", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClipDialog = false }) {
                        Text("Cancel", color = Color.White)
                    }
                },
                containerColor = DarkSurface
            )
        }
    }
}

// Format milliseconds into mm:ss or hh:mm:ss
private fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSecs = ms / 1000
    val hours = totalSecs / 3600
    val minutes = (totalSecs % 3600) / 60
    val seconds = totalSecs % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

// Simple local Subtitle parser & renderer
@Composable
fun SubtitleTextOverlay(
    videoItem: MediaItem,
    currentPosition: Long,
    fontSize: String,
    fontColor: Int,
    modifier: Modifier = Modifier
) {
    val subtitleText = remember(videoItem, currentPosition) {
        try {
            val movieFile = File(videoItem.path)
            val baseName = movieFile.nameWithoutExtension
            val srtFile = File(movieFile.parent, "$baseName.srt")
            if (srtFile.exists()) {
                parseSrtForTime(srtFile, currentPosition)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    } ?: return

    val scale = when (fontSize) {
        "SMALL" -> 14.sp
        "LARGE" -> 22.sp
        else -> 18.sp
    }

    Text(
        text = subtitleText,
        color = Color(fontColor),
        fontSize = scale,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

// Super simple SRT parser to find subtitle matching current milliseconds position
private fun parseSrtForTime(srtFile: File, timeMs: Long): String? {
    try {
        val lines = srtFile.readLines()
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) {
                i++
                continue
            }
            if (line.toIntOrNull() != null && i + 2 < lines.size) {
                val times = lines[i + 1].trim()
                if (times.contains("-->")) {
                    val parts = times.split("-->")
                    val startMs = parseSrtTimestamp(parts[0].trim())
                    val endMs = parseSrtTimestamp(parts[1].trim())
                    if (timeMs in startMs..endMs) {
                        val subText = StringBuilder()
                        var j = i + 2
                        while (j < lines.size && lines[j].trim().isNotEmpty()) {
                            subText.append(lines[j].trim()).append("\n")
                            j++
                        }
                        return subText.toString().trim()
                    }
                }
            }
            i++
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

private fun parseSrtTimestamp(timeStr: String): Long {
    val parts = timeStr.replace(',', '.').split(":")
    val hrs = parts[0].toLong() * 3600000
    val mins = parts[1].toLong() * 60000
    val secsParts = parts[2].split(".")
    val secs = secsParts[0].toLong() * 1000
    val ms = secsParts[1].toLong()
    return hrs + mins + secs + ms
}

@Composable
fun VideoSidePanel(
    visible: Boolean,
    title: String,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    if (visible) {
        BackHandler {
            onDismissRequest()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f)
        ) {
            // Outside Scrim overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onDismissRequest() }
            )

            // Right-to-Left Slide Panel occupying 45-50% width
            AnimatedVisibility(
                visible = visible,
                enter = slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(200)),
                exit = slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 220, easing = FastOutLinearInEasing)
                ) + fadeOut(animationSpec = tween(150)),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.48f)
                        .widthIn(min = 280.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* consume clicks inside panel */ },
                    color = Color(0xFF121212).copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 16.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(
                                onClick = onDismissRequest,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Panel",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = Color.White.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Scrollable Content
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            content = content
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = Color.White.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Footer with SAN team branding
                        var footerVisible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            delay(100)
                            footerVisible = true
                        }
                        AnimatedVisibility(
                            visible = footerVisible,
                            enter = fadeIn(animationSpec = tween(300)) + slideInVertically(initialOffsetY = { 20 })
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "presented by ",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    fontStyle = FontStyle.Italic
                                )
                                Text(
                                    text = "SAN team",
                                    color = AccentPurple,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
