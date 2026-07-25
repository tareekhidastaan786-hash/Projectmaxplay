package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MediaThumbnail
import androidx.media3.common.Player
import com.example.data.model.MediaItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.MediaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

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
            // Linear Progress bar along top edge
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
                // Disc Artwork Placeholder with sleek gradient
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

    // Sub panels
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

    // Decorative vinyl rotation state
    var rotationAngle by remember { mutableStateOf(0f) }
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            rotationAngle = (rotationAngle + 2f) % 360f
            delay(30)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DarkSurface, DeepNavyBlack)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(Icons.Default.ExpandMore, contentDescription = "Collapse", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Text(
                    text = "NOW PLAYING",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 1.5.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showQueueDialog = true }) {
                        Icon(Icons.Default.QueueMusic, contentDescription = "Queue", tint = Color.White)
                    }
                    
                    var showPlayerMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showPlayerMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = Color.White)
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
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Disc Artwork Panel or Scrolling Lyrics Panel
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                if (showLyricsPanel) {
                    LrcLyricsView(song = song, progressMs = progress)
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        // Vinyl design
                        Box(
                            modifier = Modifier
                                .fillMaxSize(0.80f)
                                .clip(CircleShape)
                                .background(Color.Black)
                                .border(6.dp, CardSurface, CircleShape)
                                .rotate(rotationAngle),
                            contentAlignment = Alignment.Center
                        ) {
                            // Center disc sticker
                            MediaThumbnail(
                                item = song,
                                modifier = Modifier
                                    .fillMaxSize(0.4f)
                                    .clip(CircleShape)
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                    .clickable { imagePickerLauncher.launch("image/*") }
                            )
                        }

                        // Glowing Purple Wave Visualizer overlay at the bottom of the disc box
                        MusicWaveVisualizer(
                            isPlaying = isPlaying,
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(56.dp)
                                .align(Alignment.BottomCenter)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Details and Favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist ?: "Unknown Artist",
                        color = SecondaryText,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = { viewModel.toggleFavorite(song) }) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (song.isFavorite) Color.Red else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrubber
            Slider(
                value = progress.toFloat(),
                onValueChange = { viewModel.seekMusicTo(it.toLong()) },
                valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(
                    thumbColor = PurpleStart,
                    activeTrackColor = PurpleStart,
                    inactiveTrackColor = SlateGray
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = formatTime(progress), color = SecondaryText, fontSize = 12.sp)
                Text(text = formatTime(duration), color = SecondaryText, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Playback Controls Rows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Mode Button
                IconButton(onClick = { viewModel.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffle) NeonGreen else Color.White
                    )
                }

                // Previous
                IconButton(onClick = { viewModel.skipPreviousMusic() }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = Color.White, modifier = Modifier.size(36.dp))
                }

                // Play / Pause FAB
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(NeonGreen)
                        .clickable { viewModel.togglePlayPauseMusic() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.Black,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Next
                IconButton(onClick = { viewModel.skipNextMusic() }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(36.dp))
                }

                // Repeat Mode Button
                IconButton(onClick = { viewModel.cycleRepeatMode() }) {
                    val repeatIcon = when (repeatMode) {
                        Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                        else -> Icons.Default.Repeat
                    }
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = "Repeat",
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) NeonGreen else Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Advanced FX & Timer Utility buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sleep Timer button
                val timerMinutesLeft by viewModel.sleepTimerMinutesLeft.collectAsState()
                IconButton(onClick = { showTimerDialog = true }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = "Sleep Timer",
                            tint = if (timerMinutesLeft > 0) NeonGreen else Color.White
                        )
                        if (timerMinutesLeft > 0) {
                            Text(
                                text = " ${timerMinutesLeft}m",
                                color = PurpleStart,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Equalizer Menu button
                IconButton(onClick = { showEqualizer = true }) {
                    Icon(Icons.Default.Equalizer, contentDescription = "Equalizer", tint = Color.White)
                }

                // Lyrics Panel button
                IconButton(onClick = { showLyricsPanel = !showLyricsPanel }) {
                    Icon(
                        Icons.Default.TextSnippet,
                        contentDescription = "Lyrics",
                        tint = if (showLyricsPanel) NeonGreen else Color.White
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
fun LrcLyricsView(
    song: MediaItem,
    progressMs: Long,
    modifier: Modifier = Modifier
) {
    // Try to load .lrc file next to audio file
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
        if (activeIndex >= 0 && lyricsLines.isNotEmpty()) {
            lazyListState.animateScrollToItem(activeIndex)
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        items(lyricsLines.size) { index ->
            val item = lyricsLines[index]
            val isActive = index == activeIndex
            Text(
                text = item.text,
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

private fun parseLrcFile(lrcFile: File): List<LrcLine> {
    val list = mutableListOf<LrcLine>()
    try {
        lrcFile.readLines().forEach { line ->
            val cleaned = line.trim()
            if (cleaned.startsWith("[") && cleaned.contains("]")) {
                val index = cleaned.indexOf("]")
                val timeTag = cleaned.substring(1, index)
                val lyricText = cleaned.substring(index + 1).trim()
                val ms = parseLrcTimestamp(timeTag)
                if (ms >= 0) {
                    list.add(LrcLine(ms, lyricText))
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list.sortedBy { it.timeMs }
}

private fun parseLrcTimestamp(tag: String): Long {
    return try {
        // format: 01:23.45 or 01:23
        val parts = tag.split(":")
        val mins = parts[0].toLong() * 60000
        val secsParts = parts[1].split(".")
        val secs = secsParts[0].toLong() * 1000
        val ms = if (secsParts.size > 1) secsParts[1].toLong() * 10 else 0L
        mins + secs + ms
    } catch (e: Exception) {
        -1L
    }
}

@Composable
fun EqualizerOverlayPanel(
    viewModel: MediaViewModel,
    onClose: () -> Unit
) {
    val eqEnabled by viewModel.isEqEnabled.collectAsState()
    val bands by viewModel.equalizerBands.collectAsState()
    val bassStrength by viewModel.bassBoostLevel.collectAsState()
    val virtualizerStrength by viewModel.virtualizerLevel.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .clickable { /* Block taps */ }
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "SOUND EQUALIZER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Master On/Off switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardSurface)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Master Equalizer Switch", color = Color.White, fontWeight = FontWeight.SemiBold)
                Switch(
                    checked = eqEnabled,
                    onCheckedChange = { viewModel.toggleEq(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonGreen,
                        checkedTrackColor = NeonGreen.copy(alpha = 0.5f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bands Sliders Row (Standard 5 vertical or horizontal bands)
            Text(text = "Decibel Bands (Hz)", color = SecondaryText, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardSurface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                bands.forEach { band ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${band.centerFreqHz}Hz",
                            color = Color.White,
                            modifier = Modifier.width(60.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Slider(
                            value = band.currentLevelMillibels.toFloat(),
                            onValueChange = { level ->
                                if (eqEnabled) {
                                    viewModel.setEqBandLevel(band.index, level.toInt())
                                }
                            },
                            valueRange = band.minLevelMillibels.toFloat()..band.maxLevelMillibels.toFloat(),
                            enabled = eqEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = PurpleStart,
                                activeTrackColor = PurpleStart,
                                inactiveTrackColor = DarkSurface
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        val db = band.currentLevelMillibels / 100
                        Text(
                            text = "${if (db >= 0) "+" else ""}${db}dB",
                            color = if (eqEnabled) NeonGreen else Color.Gray,
                            modifier = Modifier.width(50.dp),
                            textAlign = TextAlign.End,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // FX sliders (Bass Boost and Virtualizer 3D)
            Text(text = "AUDIO FX EFFECTS", color = SecondaryText, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Bass Boost Box
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

                // 3D Virtualizer Box
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
    }
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

// Simple time formatter for mm:ss
private fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSecs = ms / 1000
    val minutes = totalSecs / 60
    val seconds = totalSecs % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
fun MusicWaveVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val barCount = 16
    val animStates = remember { List(barCount) { androidx.compose.animation.core.Animatable(0.2f) } }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                animStates.forEach { anim ->
                    val target = kotlin.random.Random.nextFloat() * (0.95f - 0.2f) + 0.2f
                    val duration = kotlin.random.Random.nextInt(180, 320)
                    scope.launch {
                        anim.animateTo(
                            targetValue = target,
                            animationSpec = androidx.compose.animation.core.tween(
                                durationMillis = duration,
                                easing = androidx.compose.animation.core.FastOutSlowInEasing
                            )
                        )
                    }
                }
                delay(150)
            }
        } else {
            animStates.forEach { anim ->
                scope.launch {
                    anim.animateTo(
                        0.15f,
                        animationSpec = androidx.compose.animation.core.tween(durationMillis = 500)
                    )
                }
            }
        }
    }

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val barWidth = (width / (barCount * 1.5f))
        val spacing = barWidth * 0.5f

        val totalWidth = barCount * barWidth + (barCount - 1) * spacing
        val startX = (width - totalWidth) / 2f

        for (i in 0 until barCount) {
            val x = startX + i * (barWidth + spacing)
            val barHeight = height * animStates[i].value
            val y = height - barHeight

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(PurpleStart, PurpleEnd)
                ),
                topLeft = androidx.compose.ui.geometry.Offset(x, y),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
