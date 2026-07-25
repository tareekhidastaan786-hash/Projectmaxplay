package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaItem
import com.example.ui.components.MediaThumbnail
import com.example.ui.theme.*
import com.example.ui.viewmodel.MediaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun MusicMiniPlayer(
    viewModel: MediaViewModel,
    onExpand: () -> Unit
) {
    val currentSong by viewModel.currentPlayingSong.collectAsState()
    val isPlaying by viewModel.isPlayingMusic.collectAsState()
    val progress by viewModel.musicPlaybackPosition.collectAsState()
    val duration by viewModel.musicDuration.collectAsState()
    val song = currentSong ?: return

    val progressPercent = if (duration > 0) progress.toFloat() / duration else 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .background(CardSurface, RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .clickable { onExpand() }
    ) {
        Column {
            LinearProgressIndicator(
                progress = { progressPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                color = PurpleStart,
                trackColor = Color.Transparent
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MediaThumbnail(
                    item = song,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist ?: "Unknown Artist",
                        color = SecondaryText,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { viewModel.togglePlayPauseMusic() }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = NeonGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }
                IconButton(onClick = { viewModel.skipNextMusic() }) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Skip",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// --- Dynamic Circular Audio Waveform Visualizer ---
@Composable
fun CircularAudioVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val barCount = 80
    val animPhases = remember { FloatArray(barCount) { (it * 0.15f) % 6.28318f } }
    var time by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                time += 0.08f
                delay(16) // Smooth 60 FPS update loop
            }
        }
    }

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = min(size.width, size.height) / 2f
        val innerRadius = maxRadius * 0.68f
        val maxBarLength = maxRadius * 0.28f

        for (i in 0 until barCount) {
            val angleDeg = (i * 360f / barCount)
            val angleRad = Math.toRadians(angleDeg.toDouble())

            val factor = if (isPlaying) {
                val wave1 = sin(time * 3f + animPhases[i])
                val wave2 = cos(time * 5f - i * 0.2f)
                ((wave1 * 0.5f + wave2 * 0.5f) + 1f) / 2f * 0.85f + 0.15f
            } else {
                0.12f
            }

            val barLength = maxBarLength * factor
            val startX = center.x + innerRadius * cos(angleRad).toFloat()
            val startY = center.y + innerRadius * sin(angleRad).toFloat()
            val endX = center.x + (innerRadius + barLength) * cos(angleRad).toFloat()
            val endY = center.y + (innerRadius + barLength) * sin(angleRad).toFloat()

            drawLine(
                color = Color.White.copy(alpha = if (isPlaying) 0.85f else 0.40f),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun FullMusicPlayerSheet(
    viewModel: MediaViewModel,
    onCollapse: () -> Unit
) {
    val currentSong by viewModel.currentPlayingSong.collectAsState()
    val isPlaying by viewModel.isPlayingMusic.collectAsState()
    val progress by viewModel.musicPlaybackPosition.collectAsState()
    val duration by viewModel.musicDuration.collectAsState()
    val shuffle by viewModel.shuffleEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()

    var showEqualizer by remember { mutableStateOf(false) }
    var showTimerDialog by remember { mutableStateOf(false) }
    var showQueueDialog by remember { mutableStateOf(false) }
    var showLyricsPanel by remember { mutableStateOf(false) }

    val song = currentSong ?: return
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val localPath = saveUriToInternalStorage(context, it)
            if (localPath != null) {
                viewModel.updateCustomCover(song.path, localPath)
            }
        }
    }

    // Vinyl rotation angle
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            rotationAngle = (rotationAngle + 1.8f) % 360f
            delay(30)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Dynamic Ambient Blurred Artwork Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.35f)
        ) {
            MediaThumbnail(
                item = song,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(80.dp)
            )
        }

        // Ambient Dark Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.60f),
                            Color(0xFF101014).copy(alpha = 0.85f),
                            Color.Black.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 2. Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = song.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = song.artist ?: "Jashn-e-Adab",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                IconButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Now listening to ${song.title} - ${song.artist ?: "MaxPlay"}")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Track"))
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Center Stage: Circular Vinyl & Audio Visualizer Ring
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (showLyricsPanel) {
                    LrcLyricsView(song = song, progressMs = progress)
                } else {
                    Box(
                        modifier = Modifier.size(290.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Dynamic Radial Frequency Waveform Ring
                        CircularAudioVisualizer(
                            isPlaying = isPlaying,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Circular Vinyl Frame with Grooves
                        Box(
                            modifier = Modifier
                                .size(210.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF2E2E33),
                                            Color(0xFF18181C),
                                            Color(0xFF000000)
                                        )
                                    )
                                )
                                .border(5.dp, Color(0xFF38383F), CircleShape)
                                .rotate(rotationAngle),
                            contentAlignment = Alignment.Center
                        ) {
                            // Concentric Vinyl Grooves
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(color = Color.White.copy(alpha = 0.08f), radius = size.width * 0.46f, style = Stroke(width = 1f))
                                drawCircle(color = Color.White.copy(alpha = 0.10f), radius = size.width * 0.40f, style = Stroke(width = 1f))
                                drawCircle(color = Color.White.copy(alpha = 0.06f), radius = size.width * 0.34f, style = Stroke(width = 1f))
                            }

                            // Center Album Artwork
                            MediaThumbnail(
                                item = song,
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                    .clickable { imagePickerLauncher.launch("image/*") }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Secondary Action Row (5 Icons)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Favorite
                IconButton(onClick = { viewModel.toggleFavorite(song) }) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (song.isFavorite) Color(0xFFFFD700) else Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Equalizer
                IconButton(onClick = { showEqualizer = true }) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Equalizer",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Sleep Timer
                val timerMinutesLeft by viewModel.sleepTimerMinutesLeft.collectAsState()
                IconButton(onClick = { showTimerDialog = true }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Sleep Timer",
                            tint = if (timerMinutesLeft > 0) NeonGreen else Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(26.dp)
                        )
                        if (timerMinutesLeft > 0) {
                            Text(
                                text = " ${timerMinutesLeft}m",
                                color = PurpleStart,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Theme / Skin (T-shirt icon)
                IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                    Icon(
                        imageVector = Icons.Default.Checkroom,
                        contentDescription = "Theme / Skin",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(26.dp)
                    )
                }

                // More Options (Vertical 3 dots)
                var showPlayerMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showPlayerMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Options",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showPlayerMenu,
                        onDismissRequest = { showPlayerMenu = false },
                        modifier = Modifier.background(CardSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Change Cover", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, tint = NeonGreen) },
                            onClick = {
                                imagePickerLauncher.launch("image/*")
                                showPlayerMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (showLyricsPanel) "Show Visualizer" else "Show Lyrics (.lrc)", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.TextSnippet, contentDescription = null, tint = NeonGreen) },
                            onClick = {
                                showLyricsPanel = !showLyricsPanel
                                showPlayerMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Playback Seekbar & Timers
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(progress),
                        color = Color.White.copy(alpha = 0.70f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formatTime(duration),
                        color = Color.White.copy(alpha = 0.70f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Slider(
                    value = progress.toFloat(),
                    onValueChange = { viewModel.seekMusicTo(it.toLong()) },
                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = PurpleStart,
                        inactiveTrackColor = Color.White.copy(alpha = 0.20f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 6. Primary Bottom Controls (Player Bar)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Mode Button
                IconButton(onClick = { viewModel.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffle) NeonGreen else Color.White.copy(alpha = 0.80f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Previous
                IconButton(onClick = { viewModel.skipPreviousMusic() }) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Prominent Play/Pause Button in Solid White Circle
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { viewModel.togglePlayPauseMusic() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.Black,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Next
                IconButton(onClick = { viewModel.skipNextMusic() }) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Playlist Queue Icon
                IconButton(onClick = { showQueueDialog = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                        contentDescription = "Queue",
                        tint = Color.White.copy(alpha = 0.80f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Equalizer Panel Overlay
        if (showEqualizer) {
            EqualizerOverlayPanel(viewModel = viewModel, onClose = { showEqualizer = false })
        }

        // Sleep Timer Selection Dialog
        if (showTimerDialog) {
            SleepTimerDialog(viewModel = viewModel, onDismiss = { showTimerDialog = false })
        }

        // Queue Manager Dialog
        if (showQueueDialog) {
            QueueManagerDialog(viewModel = viewModel, onDismiss = { showQueueDialog = false })
        }
    }
}

@Composable
fun EqualizerOverlayPanel(
    viewModel: MediaViewModel,
    onClose: () -> Unit
) {
    val preset by viewModel.eqPreset.collectAsState()
    val isEqEnabled by viewModel.eqEnabled.collectAsState()
    val bassStrength by viewModel.bassBoostStrength.collectAsState()
    val virtualizerStrength by viewModel.virtualizerStrength.collectAsState()

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Audio Equalizer & FX", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Switch(
                    checked = isEqEnabled,
                    onCheckedChange = { viewModel.toggleEqualizer(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = "Equalizer Presets", color = SecondaryText, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val presets = listOf("Flat", "Bass", "Pop", "Rock", "Vocal")
                    presets.forEach { p ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (preset == p) PurpleStart else CardSurface)
                                .clickable { viewModel.setEqPreset(p) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(text = p, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardSurface)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.SpeakerGroup, contentDescription = "Bass", tint = NeonGreen)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Bass Boost", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Slider(
                            value = bassStrength.toFloat(),
                            onValueChange = { viewModel.setBassBoost(it.toInt()) },
                            valueRange = 0f..1000f,
                            colors = SliderDefaults.colors(
                                thumbColor = PurpleStart,
                                activeTrackColor = PurpleStart
                            )
                        )
                        Text(text = "${(bassStrength / 10)}%", color = PurpleStart, fontSize = 12.sp)
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardSurface)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.SurroundSound, contentDescription = "3D", tint = NeonGreen)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "3D Virtualizer", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Slider(
                            value = virtualizerStrength.toFloat(),
                            onValueChange = { viewModel.setVirtualizer(it.toInt()) },
                            valueRange = 0f..1000f,
                            colors = SliderDefaults.colors(
                                thumbColor = PurpleStart,
                                activeTrackColor = PurpleStart
                            )
                        )
                        Text(text = "${(virtualizerStrength / 10)}%", color = PurpleStart, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) {
                Text("Done", color = NeonGreen)
            }
        },
        containerColor = DarkSurface
    )
}

@Composable
fun SleepTimerDialog(
    viewModel: MediaViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep Timer", color = Color.White) },
        text = {
            Column {
                Text("Select when to automatically turn off playback:", color = SecondaryText)
                Spacer(modifier = Modifier.height(16.dp))
                val options = listOf(
                    0 to "Turn Off Timer",
                    5 to "5 Minutes",
                    15 to "15 Minutes",
                    30 to "30 Minutes",
                    45 to "45 Minutes",
                    60 to "60 Minutes"
                )
                options.forEach { (mins, label) ->
                    TextButton(
                        onClick = {
                            viewModel.setSleepTimer(mins)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(label, color = PurpleStart, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        },
        containerColor = DarkSurface
    )
}

@Composable
fun QueueManagerDialog(
    viewModel: MediaViewModel,
    onDismiss: () -> Unit
) {
    val queue by viewModel.musicQueue.collectAsState()
    val activeSong by viewModel.currentPlayingSong.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Playback Queue", color = Color.White) },
        text = {
            Box(modifier = Modifier.height(300.dp)) {
                if (queue.isEmpty()) {
                    Text("The queue is empty.", color = SecondaryText)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(queue) { song ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.playSong(song, queue) }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = song.title,
                                    color = if (song.path == activeSong?.path) NeonGreen else Color.White,
                                    fontWeight = if (song.path == activeSong?.path) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                if (song.path == activeSong?.path) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "Playing", tint = NeonGreen, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = PurpleStart)
            }
        },
        containerColor = DarkSurface
    )
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSecs = ms / 1000
    val minutes = totalSecs / 60
    val seconds = totalSecs % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
fun LrcLyricsView(
    song: MediaItem,
    progressMs: Long,
    modifier: Modifier = Modifier
) {
    val lyricsLines = remember(song) {
        try {
            val audioFile = File(song.path)
            val baseName = audioFile.nameWithoutExtension
            val lrcFile = File(audioFile.parent, "$baseName.lrc")
            if (lrcFile.exists()) {
                parseLrcFile(lrcFile)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    if (lyricsLines.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No offline LRC lyrics found.\nTo view scrolling lyrics, save a '.lrc' file next to the song with the same file name.",
                color = SecondaryText,
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    val lazyListState = rememberLazyListState()
    val activeIndex = lyricsLines.indexOfLast { progressMs >= it.timeMs }.coerceAtLeast(0)

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            lazyListState.animateScrollToItem(activeIndex)
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(lyricsLines.size) { index ->
            val line = lyricsLines[index]
            val isActive = index == activeIndex
            Text(
                text = line.text,
                color = if (isActive) NeonGreen else Color.White.copy(alpha = 0.5f),
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                fontSize = if (isActive) 18.sp else 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
    }
}

data class LrcLine(val timeMs: Long, val text: String)

private fun parseLrcFile(file: File): List<LrcLine> {
    val lines = mutableListOf<LrcLine>()
    val regex = Regex("\\[(\\d+):(\\d+\\.\\d+)\\](.*)")
    file.forEachLine { lineStr ->
        val match = regex.find(lineStr)
        if (match != null) {
            val mins = match.groupValues[1].toLongOrNull() ?: 0L
            val secs = match.groupValues[2].toDoubleOrNull() ?: 0.0
            val text = match.groupValues[3].trim()
            val timeMs = (mins * 60 * 1000) + (secs * 1000).toLong()
            lines.add(LrcLine(timeMs, text))
        }
    }
    return lines.sortedBy { it.timeMs }
}
