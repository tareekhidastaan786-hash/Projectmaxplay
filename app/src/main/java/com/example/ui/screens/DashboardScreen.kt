package com.example.ui.screens

import androidx.compose.runtime.saveable.rememberSaveable



import android.content.Context

import android.content.Intent

import android.graphics.Bitmap

import android.graphics.BitmapFactory

import android.graphics.Canvas

import android.graphics.Paint

import android.net.Uri

import androidx.activity.compose.rememberLauncherForActivityResult

import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.animation.*

import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.ExperimentalFoundationApi

import androidx.compose.foundation.Image

import androidx.compose.foundation.background

import androidx.compose.foundation.border

import androidx.compose.foundation.clickable

import androidx.compose.foundation.combinedClickable

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.rememberLazyListState


import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.grid.GridCells

import androidx.compose.foundation.lazy.grid.LazyVerticalGrid

import androidx.compose.foundation.lazy.grid.items

import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.*

import androidx.compose.material.icons.outlined.Folder

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.asImageBitmap

import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.platform.testTag

import androidx.compose.ui.res.painterResource

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import com.example.R

import com.example.data.model.MediaItem

import com.example.ui.components.MediaThumbnail

import com.example.ui.theme.*

import com.example.ui.viewmodel.MediaViewModel

import java.io.File

import java.io.FileOutputStream

import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MediaViewModel,
    isMusicExpanded: Boolean,
    onExpandMiniPlayer: () -> Unit,
    onPlayVideo: (MediaItem) -> Unit,
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeSortOrder by viewModel.activeSortOrder.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAISmartHubDialog by remember { mutableStateOf(false) }
    var showSearchOverlay by remember { mutableStateOf(false) }
    var showDownloadsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(DarkSurface)) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Branding: "MaxPlay"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Max",
                            color = Color.White,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFF6A11CB), Color(0xFFB517FF))))
                        )
                        Text(
                            text = "Play",
                            color = Color.White,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        )
                    }

                    // Right-aligned Action Icons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // AI Smart Insights Icon
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { showAISmartHubDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Smart Insights",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "1.43GB",
                                color = SecondaryText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Search (Magnifying Glass) Icon -> Triggers Search Overlay
                        IconButton(onClick = { showSearchOverlay = true }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Download (Down Arrow) Icon -> Triggers Downloads View
                        IconButton(onClick = { showDownloadsDialog = true }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Downloads",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .navigationBarsPadding()
            ) {
                val currentPlayingSong by viewModel.currentPlayingSong.collectAsState()
                if (currentPlayingSong != null && !isMusicExpanded) {
                    MusicMiniPlayer(
                        viewModel = viewModel,
                        onExpand = onExpandMiniPlayer
                    )
                }
                NavigationBar(
                    containerColor = DarkSurface,
                    modifier = Modifier
                ) {
                    val tabs = listOf(
                        Triple(0, "Video", Icons.Default.Movie),
                        Triple(1, "Music", Icons.Default.MusicNote),
                        Triple(2, "Folder", Icons.Outlined.Folder),
                        Triple(3, "Playlist", Icons.Default.QueueMusic),
                        Triple(4, "Favorite", Icons.Default.Favorite)
                    )

                    tabs.forEach { (index, label, icon) ->
                        val isSelected = selectedTab == index
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.selectedTab.value = index },
                            icon = { 
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier.background(Color.Transparent)
                                    ) {
                                        Icon(
                                            icon, 
                                            contentDescription = label,
                                            tint = Color(0xFFB517FF) // Using a purple tint directly as proxy for gradient on default icon
                                        )
                                    }
                                } else {
                                    Icon(icon, contentDescription = label, tint = SecondaryText) 
                                }
                            },
                            label = { 
                                Text(
                                    label, 
                                    fontSize = 10.sp,
                                    color = if (isSelected) Color(0xFFB517FF) else SecondaryText
                                ) 
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        },
        containerColor = DeepNavyBlack
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> VideosTab(viewModel, onPlayVideo, showAISmartHubDialog = { showAISmartHubDialog = true })
                1 -> MusicTab(viewModel, onPlayVideo, showAISmartHubDialog = { showAISmartHubDialog = true })
                2 -> FoldersTab(viewModel, onPlayVideo)
                3 -> PlaylistsTab(viewModel)
                4 -> FavoritesTab(viewModel, onPlayVideo)
            }
        }
    }

    // Settings Configuration Dialog
    if (showSettingsDialog) {
        SettingsDialog(viewModel = viewModel, onDismiss = { showSettingsDialog = false })
    }

    // AI Smart Insights Hub Dialog
    if (showAISmartHubDialog) {
        AISmartHubDialog(
            viewModel = viewModel,
            onDismiss = { showAISmartHubDialog = false },
            onPlayMedia = { item ->
                if (item.isVideo) {
                    onPlayVideo(item)
                } else {
                    viewModel.playSong(item, listOf(item))
                }
            }
        )
    }

    // Search Overlay Dialog
    if (showSearchOverlay) {
        SearchOverlayDialog(
            viewModel = viewModel,
            onDismiss = { showSearchOverlay = false },
            onPlayMedia = { item ->
                showSearchOverlay = false
                if (item.isVideo) {
                    onPlayVideo(item)
                } else {
                    viewModel.playSong(item, listOf(item))
                }
            }
        )
    }

    // Offline Downloads Overlay Dialog
    if (showDownloadsDialog) {
        DownloadsOverlayDialog(
            viewModel = viewModel,
            onDismiss = { showDownloadsDialog = false },
            onPlayMedia = { item ->
                showDownloadsDialog = false
                if (item.isVideo) {
                    onPlayVideo(item)
                } else {
                    viewModel.playSong(item, listOf(item))
                }
            }
        )
    }
}

@Composable
fun CategoryTabsRow(
    selectedCategory: Int,
    onCategorySelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sub-tabs: Video | Folder | Playlist
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val categories = listOf("Video", "Folder", "Playlist")
            categories.forEachIndexed { index, label ->
                val isSelected = selectedCategory == index
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onCategorySelected(index) }
                        .padding(vertical = 4.dp)
                ) {
                    val brush = if (isSelected) Brush.linearGradient(listOf(Color(0xFF6A11CB), Color(0xFFB517FF))) else null
                    
                    Text(
                        text = label,
                        color = if (isSelected) Color(0xFFB517FF) else Color.White,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(brush = brush!!)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                }
            }
        }

        // Far right action icons: Sort (1↓) and Grid/List view
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Sort,
                contentDescription = "Sort",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Icon(
                imageVector = Icons.Default.GridView,
                contentDescription = "Grid View",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun VideosTab(
    viewModel: MediaViewModel,
    onPlayVideo: (MediaItem) -> Unit,
    showAISmartHubDialog: () -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOrder by viewModel.activeSortOrder.collectAsState()
    val allVideos by viewModel.videos.collectAsState()
    val videos = remember(allVideos, searchQuery, sortOrder) {
        viewModel.getFilteredVideos(searchQuery, sortOrder)
    }

    val groupedVideos = remember(videos) {
        videos.groupBy { 
            java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(it.dateAdded * 1000))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
    val listState = rememberLazyListState()

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (searchQuery.isEmpty()) {
                item {
                    // Smart Firebase Banner (Offline-First Caching System)
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SmartFirebaseBannerView(onClick = showAISmartHubDialog)
                    }
                }
                item {
                    // Recently Played Section with Purple Gradient playback progress bar
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        RecentlyPlayedSection(viewModel, onPlayVideo, isVideoTab = true)
                    }
                }
            }

            if (videos.isEmpty()) {
                item {
                    EmptyStateView(icon = Icons.Default.Movie, text = "No local videos found.")
                }
            } else {
                groupedVideos.forEach { (month, monthVideos) ->
                    item {
                        Text(
                            text = month,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(monthVideos, key = { it.path }) { video ->
                        MediaItemRow(
                            item = video,
                            onClick = { onPlayVideo(video) },
                            viewModel = viewModel,
                            isVideo = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MusicTab(
    viewModel: MediaViewModel,
    onPlayVideo: (MediaItem) -> Unit,
    showAISmartHubDialog: () -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOrder by viewModel.activeSortOrder.collectAsState()
    val allSongs by viewModel.songs.collectAsState()
    val songs = remember(allSongs, searchQuery, sortOrder) {
        viewModel.getFilteredSongs(searchQuery, sortOrder)
    }
    
    val groupedSongs = remember(songs) {
        songs.groupBy { 
            java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(it.dateAdded * 1000))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
    val listState = rememberLazyListState()

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (searchQuery.isEmpty()) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SmartFirebaseBannerView(onClick = showAISmartHubDialog)
                    }
                }
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        RecentlyPlayedSection(viewModel, onPlayVideo, isVideoTab = false)
                    }
                }
            }

            if (songs.isEmpty()) {
                item {
                    EmptyStateView(icon = Icons.Default.MusicNote, text = "No local songs found.")
                }
            } else {
                groupedSongs.forEach { (month, monthSongs) ->
                    item {
                        Text(
                            text = month,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(monthSongs, key = { it.path }) { song ->
                        MediaItemRow(
                            item = song,
                            onClick = { viewModel.playSong(song, songs) },
                            viewModel = viewModel,
                            isVideo = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FoldersTab(
    viewModel: MediaViewModel,
    onPlayVideo: (MediaItem) -> Unit,
) {
    val allVideos by viewModel.videos.collectAsState()
    val allSongs by viewModel.songs.collectAsState()
    var selectedFolder by remember { mutableStateOf<MediaViewModel.MediaFolder?>(null) }

    if (selectedFolder != null) {
        // Inner folder view
        val folder = selectedFolder!!
        val files = remember(allVideos, allSongs, selectedFolder) { viewModel.getItemsInFolder(folder.path, folder.isVideo) }

        Column(modifier = Modifier.fillMaxSize()) {
    val listState = rememberLazyListState()

            // Folder Back header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedFolder = null }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = folder.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(files, key = { it.path }) { file ->
                    MediaItemRow(
                        item = file,
                        onClick = {
                            if (folder.isVideo) {
                                onPlayVideo(file)
                            } else {
                                viewModel.playSong(file, files)
                            }
                        },
                        viewModel = viewModel,
                        isVideo = folder.isVideo
                    )
                }
            }
        }
    } else {
        // List directories containing audio or video files
        val videoFolders = remember(allVideos) { viewModel.getMediaFolders(isVideo = true) }
        val audioFolders = remember(allSongs) { viewModel.getMediaFolders(isVideo = false) }
        val allFolders = (videoFolders + audioFolders).distinctBy { it.path }

        if (allFolders.isEmpty()) {
            EmptyStateView(icon = Icons.Outlined.Folder, text = "No media folders scanned.")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(allFolders, key = { it.name }) { folder ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFolder = folder },
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(PurpleStart, PurpleEnd))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (folder.isVideo) Icons.Default.FolderOpen else Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = folder.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${folder.itemsCount} ${if(folder.isVideo) "Videos" else "Songs"}",
                                color = SecondaryText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistsTab(
    viewModel: MediaViewModel,
) {
    val playlists by viewModel.playlists.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val crossRefs by viewModel.playlistCrossRefs.collectAsState()
    val songs by viewModel.songs.collectAsState()

    var selectedPlaylist by remember { mutableStateOf<com.example.data.local.PlaylistEntity?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDetailsSong by remember { mutableStateOf<MediaItem?>(null) }
    var activeSortBy by remember { mutableStateOf<String>("default") } // "default", "title", "duration"

    if (selectedPlaylist != null) {
        val playlist = selectedPlaylist!!
        val playlistSongs by viewModel.currentPlaylistSongs.collectAsState()

        LaunchedEffect(playlist.id, crossRefs) {
            viewModel.loadPlaylistSongs(playlist.id)
        }

        // Filter & Sort Songs
        val filteredAndSortedSongs = remember(playlistSongs, searchQuery, activeSortBy) {
            val filtered = if (searchQuery.isEmpty()) {
                playlistSongs
            } else {
                playlistSongs.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                            (it.artist?.contains(searchQuery, ignoreCase = true) ?: false)
                }
            }
            when (activeSortBy) {
                "title" -> filtered.sortedBy { it.title }
                "duration" -> filtered.sortedByDescending { it.duration }
                else -> filtered
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
    val listState = rememberLazyListState()

                // Immersive Header Background Gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(AccentPurple.copy(alpha = 0.4f), Color.Transparent)
                            )
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = { selectedPlaylist = null }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                            
                            var showHeaderMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { showHeaderMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Playlist Options", tint = Color.White)
                                }
                                DropdownMenu(
                                    expanded = showHeaderMenu,
                                    onDismissRequest = { showHeaderMenu = false },
                                    modifier = Modifier.background(CardSurface)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Rename Playlist", color = Color.White) },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = NeonGreen) },
                                        onClick = {
                                            showRenameDialog = true
                                            showHeaderMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete Playlist", color = Color.Red) },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                                        onClick = {
                                            viewModel.deletePlaylist(playlist.id)
                                            selectedPlaylist = null
                                            showHeaderMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Sort by Title", color = Color.White) },
                                        leadingIcon = { Icon(Icons.Default.SortByAlpha, contentDescription = null, tint = NeonGreen) },
                                        onClick = {
                                            activeSortBy = "title"
                                            showHeaderMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Sort by Duration", color = Color.White) },
                                        leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null, tint = NeonGreen) },
                                        onClick = {
                                            activeSortBy = "duration"
                                            showHeaderMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Share Playlist", color = Color.White) },
                                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = NeonGreen) },
                                        onClick = {
                                            sharePlaylist(viewModel.getApplication(), playlist.name, playlistSongs)
                                            showHeaderMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Large rounded cover + details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // First song cover or gradient cover
                            val firstSong = playlistSongs.firstOrNull()
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(PurpleStart, PurpleEnd)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (firstSong != null) {
                                    MediaThumbnail(item = firstSong, modifier = Modifier.fillMaxSize())
                                } else {
                                    Icon(Icons.Default.QueueMusic, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = playlist.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    lineHeight = 28.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${playlistSongs.size} Songs • ${formatDuration(playlistSongs.sumOf { it.duration })}",
                                    color = SecondaryText,
                                    fontSize = 13.sp
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Play & Shuffle buttons side-by-side
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = {
                                            if (playlistSongs.isNotEmpty()) {
                                                viewModel.playSong(playlistSongs.first(), playlistSongs)
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Play All", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            if (playlistSongs.isNotEmpty()) {
                                                val shuffled = playlistSongs.shuffled()
                                                viewModel.playSong(shuffled.first(), shuffled)
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = CardSurface),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Icon(Icons.Default.Shuffle, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Shuffle", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                if (filteredAndSortedSongs.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        EmptyStateView(icon = Icons.Default.QueueMusic, text = if (searchQuery.isEmpty()) "This playlist is empty." else "No matching songs found.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredAndSortedSongs, key = { it.path }) { song ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.playSong(song, playlistSongs) }
                                    .border(1.dp, Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = CardSurfaceDark),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MediaThumbnail(
                                        item = song,
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = song.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = "${song.artist ?: "Unknown artist"} • ${song.displayDuration}", color = SecondaryText, fontSize = 11.sp, maxLines = 1)
                                    }

                                    var showMenu by remember { mutableStateOf(false) }
                                    Box {
                                        IconButton(onClick = { showMenu = true }) {
                                            Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.White)
                                        }

                                        DropdownMenu(
                                            expanded = showMenu,
                                            onDismissRequest = { showMenu = false },
                                            modifier = Modifier.background(CardSurface)
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Play Next", color = Color.White) },
                                                leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = NeonGreen) },
                                                onClick = {
                                                    viewModel.playNext(song)
                                                    showMenu = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Add to Queue", color = Color.White) },
                                                leadingIcon = { Icon(Icons.Default.QueueMusic, contentDescription = null, tint = NeonGreen) },
                                                onClick = {
                                                    viewModel.addToQueue(song)
                                                    showMenu = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Favorite", color = Color.White) },
                                                leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = NeonGreen) },
                                                onClick = {
                                                    viewModel.toggleFavorite(song)
                                                    showMenu = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Share", color = Color.White) },
                                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = NeonGreen) },
                                                onClick = {
                                                    shareFile(viewModel.getApplication(), song)
                                                    showMenu = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Properties", color = Color.White) },
                                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = NeonGreen) },
                                                onClick = {
                                                    showDetailsSong = song
                                                    showMenu = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Remove From Playlist", color = Color.Red) },
                                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                                                onClick = {
                                                    viewModel.removeSongFromPlaylist(playlist.id, song)
                                                    showMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Rename playlist dialog
            if (showRenameDialog) {
                var nameInput by remember { mutableStateOf(playlist.name) }
                AlertDialog(
                    onDismissRequest = { showRenameDialog = false },
                    title = { Text("Rename Playlist", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = {
                        TextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            placeholder = { Text("Playlist Name", color = SecondaryText) },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (nameInput.trim().isNotEmpty()) {
                                    viewModel.renamePlaylist(playlist.id, nameInput)
                                    selectedPlaylist = playlist.copy(name = nameInput)
                                    showRenameDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                        ) {
                            Text("Rename", color = Color.Black)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRenameDialog = false }) {
                            Text("Cancel", color = Color.White)
                        }
                    },
                    containerColor = DarkSurface
                )
            }

            // Song properties dialog
            if (showDetailsSong != null) {
                val s = showDetailsSong!!
                AlertDialog(
                    onDismissRequest = { showDetailsSong = null },
                    title = { Text("Properties", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Title: ${s.title}", color = Color.White, fontSize = 14.sp)
                            Text("Artist: ${s.artist ?: "Unknown"}", color = Color.White, fontSize = 14.sp)
                            Text("Album: ${s.album ?: "Unknown"}", color = Color.White, fontSize = 14.sp)
                            Text("Duration: ${s.displayDuration}", color = Color.White, fontSize = 14.sp)
                            Text("Size: ${s.displaySize}", color = Color.White, fontSize = 14.sp)
                            Text("Path: ${s.path}", color = Color.White, fontSize = 12.sp)
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showDetailsSong = null },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                        ) {
                            Text("Close", color = Color.White)
                        }
                    },
                    containerColor = DarkSurface
                )
            }
        }
    } else {
        // --- State 1: Playlist Home ---
        val filteredPlaylists = remember(playlists, searchQuery) {
            if (searchQuery.isEmpty()) {
                playlists
            } else {
                playlists.filter { it.name.contains(searchQuery, ignoreCase = true) }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
    val listState = rememberLazyListState()

                // Large Rounded Card header with elegant gradient background
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(PurpleStart, AccentPurple)
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Text(
                                text = "My Music Library",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Organize, queue, and manage your favorite tracks dynamically in personalized playlists.",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                if (filteredPlaylists.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        EmptyStateView(icon = Icons.Default.QueueMusic, text = if (searchQuery.isEmpty()) "No playlists created yet." else "No matching playlists found.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredPlaylists, key = { it.id }) { playlist ->
                            // Calculate playlist songs & sum of duration
                            val playlistSongs = remember(crossRefs, songs, playlist.id) {
                                val paths = crossRefs.filter { it.playlistId == playlist.id }.map { it.mediaPath }
                                songs.filter { it.path in paths }
                            }
                            val songCount = playlistSongs.size
                            val totalDurationMs = playlistSongs.sumOf { it.duration }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPlaylist = playlist }
                                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = CardSurface),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Playlist thumbnail
                                    val firstSong = playlistSongs.firstOrNull()
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(PurpleStart, PurpleMid)
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (firstSong != null) {
                                            MediaThumbnail(item = firstSong, modifier = Modifier.fillMaxSize())
                                        } else {
                                            Icon(Icons.Default.QueueMusic, contentDescription = null, tint = Color.White)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = playlist.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "$songCount Songs • ${formatDuration(totalDurationMs)}",
                                            color = SecondaryText,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Icon(Icons.Default.ChevronRight, contentDescription = "Open Playlist", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Floating FAB to Create Playlist
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = NeonGreen,
                contentColor = Color.Black,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Playlist")
            }

            if (showCreateDialog) {
                var nameInput by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showCreateDialog = false },
                    title = { Text("Create Playlist", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = {
                        TextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            placeholder = { Text("Playlist Name", color = SecondaryText) },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (nameInput.trim().isNotEmpty()) {
                                    viewModel.createPlaylist(nameInput)
                                    showCreateDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                        ) {
                            Text("Create", color = Color.Black)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreateDialog = false }) {
                            Text("Cancel", color = Color.White)
                        }
                    },
                    containerColor = DarkSurface
                )
            }
        }
    }
}

// Share playlist helper
fun sharePlaylist(context: Context, playlistName: String, songs: List<MediaItem>) {
    val shareContent = buildString {
        appendLine("🎵 Playlist: $playlistName")
        appendLine("Tracks count: ${songs.size}")
        appendLine()
        songs.forEachIndexed { i, s ->
            appendLine("${i + 1}. ${s.title} - ${s.artist ?: "Unknown"}")
        }
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareContent)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(intent, "Share Playlist"))
}

@Composable
fun FavoritesTab(
    viewModel: MediaViewModel,
    onPlayVideo: (MediaItem) -> Unit,
) {
    val allVideos by viewModel.videos.collectAsState()
    val allSongs by viewModel.songs.collectAsState()
    var subTab by remember { mutableStateOf(0) } // 0: Videos, 1: Songs

    Column(modifier = Modifier.fillMaxSize()) {
    val listState = rememberLazyListState()

        // Toggle tabs
        TabRow(
            selectedTabIndex = subTab,
            containerColor = DarkSurface,
            contentColor = NeonGreen
        ) {
            Tab(selected = subTab == 0, onClick = { subTab = 0 }) {
                Text("Favorite Videos", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold, color = if (subTab == 0) NeonGreen else Color.Gray)
            }
            Tab(selected = subTab == 1, onClick = { subTab = 1 }) {
                Text("Favorite Songs", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold, color = if (subTab == 1) NeonGreen else Color.Gray)
            }
        }

        if (subTab == 0) {
            val favVideos = remember(allVideos) { viewModel.getFavoriteVideos() }
            if (favVideos.isEmpty()) {
                EmptyStateView(icon = Icons.Default.FavoriteBorder, text = "No favorite videos yet.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(favVideos, key = { it.path }) { video ->
                        MediaItemRow(item = video, onClick = { onPlayVideo(video) }, viewModel = viewModel, isVideo = true)
                    }
                }
            }
        } else {
            val favSongs = remember(allSongs) { viewModel.getFavoriteSongs() }
            if (favSongs.isEmpty()) {
                EmptyStateView(icon = Icons.Default.FavoriteBorder, text = "No favorite songs yet.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(favSongs, key = { it.path }) { song ->
                        MediaItemRow(item = song, onClick = { viewModel.playSong(song, favSongs) }, viewModel = viewModel, isVideo = false)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaItemRow(
    item: MediaItem,
    onClick: () -> Unit,
    viewModel: MediaViewModel,
    isVideo: Boolean
) {
    val context = LocalContext.current
    var showMoreMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMoreMenu = true }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(width = if (isVideo) 144.dp else 81.dp, height = 81.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            MediaThumbnail(
                item = item,
                modifier = Modifier.fillMaxSize()
            )
            
            // Duration overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = item.displayDuration,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = if (isVideo) "480p | ${item.displaySize}" else "${item.artist ?: "Unknown"} | ${item.displaySize}",
                color = SecondaryText,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            // Source Attribution
            val pathLower = item.path.lowercase()
            val (sourceName, sourceIconRes, iconTint) = when {
                pathLower.contains("whatsapp") -> Triple("WhatsApp", null, Color(0xFF25D366)) // Use default chat icon for WA
                pathLower.contains("telegram") -> Triple("Telegram", null, Color(0xFF0088cc))
                pathLower.contains("instagram") -> Triple("Instagram", null, Color(0xFFE1306C))
                else -> Triple("MaxPlay", R.drawable.maxplay_logo, null)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (sourceName == "MaxPlay") {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.maxplay_logo),
                            contentDescription = "MaxPlay",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(iconTint ?: Color.Gray, RoundedCornerShape(2.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val iconVector = when (sourceName) {
                            "WhatsApp" -> Icons.Default.Chat
                            "Telegram" -> Icons.Default.Send
                            "Instagram" -> Icons.Default.CameraAlt
                            else -> Icons.Default.Info
                        }
                        Icon(
                            imageVector = iconVector,
                            contentDescription = sourceName,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = sourceName, color = SecondaryText, fontSize = 11.sp)
            }
        }

        Box {
            IconButton(onClick = { showMoreMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.White)
            }

            DropdownMenu(
                expanded = showMoreMenu,
                onDismissRequest = { showMoreMenu = false },
                modifier = Modifier.background(CardSurface)
            ) {
                DropdownMenuItem(
                    text = { Text("Play", color = Color.White) },
                    onClick = { showMoreMenu = false; onClick() }
                )
            }
        }
    }
}


@Composable
fun EmptyStateView(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = imageVector ?: icon,
                contentDescription = null,
                tint = SecondaryText,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = text,
                color = SecondaryText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SettingsDialog(
    viewModel: MediaViewModel,
    onDismiss: () -> Unit
) {
    val theme by viewModel.themeMode.collectAsState()
    val dynamicCol by viewModel.useDynamicColors.collectAsState()
    val defaultSpeed by viewModel.defaultPlaybackSpeed.collectAsState()
    val subSize by viewModel.subtitleSize.collectAsState()
    val gesturesEnabled by viewModel.gesturesEnabled.collectAsState()
    val pipEnabled by viewModel.pipEnabled.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("MaxPlay Preferences", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Theme selection
                Column {
                    Text("Theme Selection", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("DARK", "LIGHT").forEach { mode ->
                            FilterChip(
                                selected = theme == mode,
                                onClick = { viewModel.updateThemeMode(mode) },
                                label = { Text(mode) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonGreen,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }

                // Gestures Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Volume & Brightness Gestures", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("Swipe vertically on video to adjust", color = SecondaryText, fontSize = 12.sp)
                    }
                    Switch(
                        checked = gesturesEnabled,
                        onCheckedChange = { viewModel.updateGesturesEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen)
                    )
                }

                // PiP Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Picture-in-Picture (PiP)", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("Squeeze video window when backgrounding", color = SecondaryText, fontSize = 12.sp)
                    }
                    Switch(
                        checked = pipEnabled,
                        onCheckedChange = { viewModel.updatePipEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen)
                    )
                }

                // Default Speed Selector
                Column {
                    Text("Default Playback Speed", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(1.0f, 1.25f, 1.5f, 2.0f).forEach { s ->
                            FilterChip(
                                selected = defaultSpeed == s,
                                onClick = { viewModel.updateDefaultPlaybackSpeed(s) },
                                label = { Text("${s}x") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonGreen,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }

                // Subtitle customizer
                Column {
                    Text("Subtitle Size Setting", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("SMALL", "MEDIUM", "LARGE").forEach { size ->
                            FilterChip(
                                selected = subSize == size,
                                onClick = { viewModel.updateSubtitleSize(size) },
                                label = { Text(size) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonGreen,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
            ) {
                Text("Dismiss", color = Color.Black)
            }
        },
        containerColor = DarkSurface
    )
}

// Share file utility using standard intent
private fun shareFile(context: Context, item: MediaItem) {
    try {
        val file = File(item.path)
        if (file.exists()) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType
                putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Media File"))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// --- AI Smart Insights Dialog ---
@Composable
fun AISmartHubDialog(
    viewModel: MediaViewModel,
    onDismiss: () -> Unit,
    onPlayMedia: (MediaItem) -> Unit
) {
    val insights = viewModel.getAIInsights()
    var selectedCategory by remember { mutableStateOf<String?>(null) } // "MOVIES", "SERIES", "CLIPS"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AccentPurple)
                Text("AI Smart Insights", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Purple Gradient Banner with Stats
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(PurpleStart, PurpleEnd)))
                        .padding(16.dp)
                ) {
                    Column {
                        Text("Storage Under Management", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                        Text(insights.totalSize, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Videos: ${insights.totalVideos}", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                            Text("Total Songs: ${insights.totalSongs}", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                        }
                    }
                }

                // Smart Grouping Categories
                Column {
                    Text("AI Smart Grouping", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Movies Card
                    SmartCategoryCard(
                        title = "Movies / Feature Films",
                        count = "${insights.moviesCount} files",
                        description = "Videos longer than 20 minutes",
                        isSelected = selectedCategory == "MOVIES",
                        onClick = { selectedCategory = if (selectedCategory == "MOVIES") null else "MOVIES" }
                    )
                    if (selectedCategory == "MOVIES" && insights.moviesCount > 0) {
                        CategoryMediaList(items = insights.moviesList, onPlay = { onPlayMedia(it); onDismiss() })
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Series Card
                    SmartCategoryCard(
                        title = "TV Shows / Series Episodes",
                        count = "${insights.seriesCount} files",
                        description = "Detected using episodic name patterns",
                        isSelected = selectedCategory == "SERIES",
                        onClick = { selectedCategory = if (selectedCategory == "SERIES") null else "SERIES" }
                    )
                    if (selectedCategory == "SERIES" && insights.seriesCount > 0) {
                        CategoryMediaList(items = insights.seriesList, onPlay = { onPlayMedia(it); onDismiss() })
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Short Clips Card
                    SmartCategoryCard(
                        title = "Short Clips & Videos",
                        count = "${insights.shortClipsCount} files",
                        description = "Short-form video assets",
                        isSelected = selectedCategory == "CLIPS",
                        onClick = { selectedCategory = if (selectedCategory == "CLIPS") null else "CLIPS" }
                    )
                    if (selectedCategory == "CLIPS" && insights.shortClipsCount > 0) {
                        CategoryMediaList(items = insights.shortClipsList, onPlay = { onPlayMedia(it); onDismiss() })
                    }
                }

                // Storage Health & Diagnostics
                Column {
                    Text("Storage Health Diagnostics", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Duplicates Alert
                    HealthDiagnosticRow(
                        title = "Duplicate Files Found",
                        status = if (insights.duplicatesCount > 0) "${insights.duplicatesCount} Duplicates" else "Clean",
                        isWarning = insights.duplicatesCount > 0,
                        description = "Identified by exact matching file size & length"
                    )
                    if (insights.duplicatesCount > 0) {
                        Button(
                            onClick = {
                                insights.duplicateItems.take(insights.duplicatesCount).forEach {
                                    viewModel.deleteMediaItem(it)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                            modifier = Modifier.padding(top = 4.dp).fillMaxWidth()
                        ) {
                            Text("Clean Duplicate Files", color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Broken Files Alert
                    HealthDiagnosticRow(
                        title = "Corrupted / Broken Files",
                        status = if (insights.brokenCount > 0) "${insights.brokenCount} Broken" else "All Healthy",
                        isWarning = insights.brokenCount > 0,
                        description = "Files with zero duration or unreadable content"
                    )
                    if (insights.brokenCount > 0) {
                        Button(
                            onClick = {
                                insights.brokenItems.forEach {
                                    viewModel.deleteMediaItem(it)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                            modifier = Modifier.padding(top = 4.dp).fillMaxWidth()
                        ) {
                            Text("Purge Corrupted Files", color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Missing Subtitles Alert
                    HealthDiagnosticRow(
                        title = "Missing Subtitles Warning",
                        status = if (insights.missingSubsCount > 0) "${insights.missingSubsCount} Movies" else "All Loaded",
                        isWarning = insights.missingSubsCount > 0,
                        description = "Movie-length files missing corresponding subtitle (.srt)"
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
            ) {
                Text("Close Hub", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = SurfaceDark,
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
    )
}

@Composable
fun SmartCategoryCard(
    title: String,
    count: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, if (isSelected) AccentPurple else BorderWhite5, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = CardSurfaceDark),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(description, color = SecondaryTextDark, fontSize = 11.sp)
            }
            Text(count, color = AccentPurple, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        }
    }
}

@Composable
fun CategoryMediaList(
    items: List<MediaItem>,
    onPlay: (MediaItem) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, start = 8.dp, end = 8.dp)
            .border(1.dp, BorderWhite5, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = BackgroundDark.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items.take(4).forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlay(item) }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(text = item.displayDuration, color = AccentPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (items.size > 4) {
                Text(
                    text = "+ ${items.size - 4} more files in main library",
                    color = SecondaryTextDark,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun HealthDiagnosticRow(
    title: String,
    status: String,
    isWarning: Boolean,
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderWhite5, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = CardSurfaceDark),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    status,
                    color = if (isWarning) Color.Red else Color.Green,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            Text(description, color = SecondaryTextDark, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

// --- Recently Played Horizontal Scroll Row ---
@Composable
fun RecentlyPlayedSection(
    viewModel: MediaViewModel,
    onPlayVideo: (MediaItem) -> Unit,
    isVideoTab: Boolean
) {
    val songs by viewModel.songs.collectAsState()
    val videos by viewModel.videos.collectAsState()
    
    val recentlyPlayed = remember(songs, videos, isVideoTab) {
        val items = if (isVideoTab) videos else songs
        items.filter { it.lastPlayed > 0 }
            .sortedByDescending { it.lastPlayed }
            .take(10)
    }

    if (recentlyPlayed.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "History",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                letterSpacing = (-0.3).sp
            )
            IconButton(
                onClick = { viewModel.clearHistory() },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Clear History",
                    tint = SecondaryText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(recentlyPlayed, key = { it.path }) { item ->
                Box(
                    modifier = Modifier
                        .size(if (isVideoTab) 160.dp else 120.dp, if (isVideoTab) 90.dp else 120.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (item.isVideo) {
                                onPlayVideo(item)
                            } else {
                                viewModel.playSong(item, songs)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    MediaThumbnail(
                        item = item,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Duration overlay tag
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(2.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.PlayArrow, 
                                contentDescription = null, 
                                tint = Color.White, 
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = item.displayDuration,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Purple Gradient playback progress bar at the bottom of thumbnail
                    val progressFraction = remember(item) {
                        if (item.playbackPosition > 0 && item.duration > 0) {
                            (item.playbackPosition.toFloat() / item.duration.toFloat()).coerceIn(0.1f, 1.0f)
                        } else {
                            0.55f // Mock progress for showcase
                        }
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth(progressFraction)
                            .height(3.dp)
                            .background(Brush.horizontalGradient(listOf(Color(0xFF6A11CB), Color(0xFFB517FF))))
                    )
                }
            }
        }
    }
}

// --- Smart Firebase Banner View (Offline-First Real-time Caching System) ---
@Composable
fun SmartFirebaseBannerView(
    onClick: () -> Unit
) {
    val defaultBannerUrl = "https://images.unsplash.com/photo-1611162617474-5b21e879e113?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80"
    var activeBannerUrl by rememberSaveable { mutableStateOf(defaultBannerUrl) }
    var bannerTitle by rememberSaveable { mutableStateOf<String?>(null) }
    var isLiveFromFirebase by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current

    DisposableEffect(Unit) {
        var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
        try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            listenerRegistration = firestore.collection("banners")
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null && !snapshot.isEmpty) {
                        val doc = snapshot.documents.firstOrNull()
                        val remoteUrl = doc?.getString("imageUrl")
                            ?: doc?.getString("bannerUrl")
                            ?: doc?.getString("url")
                            ?: doc?.getString("image")
                        if (!remoteUrl.isNullOrBlank()) {
                            activeBannerUrl = remoteUrl
                            bannerTitle = doc?.getString("title")
                            isLiveFromFirebase = true
                        }
                    } else if (error == null && snapshot != null && snapshot.isEmpty) {
                        firestore.collection("app_banners").limit(1).get()
                            .addOnSuccessListener { fallbackSnap ->
                                if (fallbackSnap != null && !fallbackSnap.isEmpty) {
                                    val doc = fallbackSnap.documents.firstOrNull()
                                    val remoteUrl = doc?.getString("imageUrl")
                                        ?: doc?.getString("bannerUrl")
                                        ?: doc?.getString("url")
                                        ?: doc?.getString("image")
                                    if (!remoteUrl.isNullOrBlank()) {
                                        activeBannerUrl = remoteUrl
                                        bannerTitle = doc?.getString("title")
                                        isLiveFromFirebase = true
                                    }
                                }
                            }
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        onDispose {
            listenerRegistration?.remove()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        coil.compose.AsyncImage(
            model = coil.request.ImageRequest.Builder(context)
                .data(activeBannerUrl)
                .crossfade(true)
                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                .build(),
            contentDescription = "Smart Firebase Banner",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (isLiveFromFirebase || bannerTitle != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = bannerTitle ?: "Featured MaxPlay",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF25D366), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "LIVE",
                            color = Color.Black,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchOverlayDialog(
    viewModel: MediaViewModel,
    onDismiss: () -> Unit,
    onPlayMedia: (MediaItem) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val allVideos by viewModel.videos.collectAsState()
    val allSongs by viewModel.songs.collectAsState()

    val searchResults = remember(query, allVideos, allSongs) {
        if (query.isBlank()) emptyList()
        else {
            (allVideos + allSongs).filter {
                it.title.contains(query, ignoreCase = true) ||
                        (it.artist?.contains(query, ignoreCase = true) ?: false)
            }
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepNavyBlack)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
    val listState = rememberLazyListState()

                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search...", color = SecondaryText) },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CardSurface,
                            unfocusedContainerColor = CardSurface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(25.dp),
                        singleLine = true
                    )
                }

                // Results
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(searchResults, key = { it.path }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlayMedia(item) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (item.isVideo) Icons.Default.Movie else Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = AccentPurple,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (item.isVideo) item.displaySize else item.artist ?: "Unknown Artist",
                                    color = SecondaryText,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadsOverlayDialog(
    viewModel: MediaViewModel,
    onDismiss: () -> Unit,
    onPlayMedia: (MediaItem) -> Unit
) {
    val allVideos by viewModel.videos.collectAsState()
    val allSongs by viewModel.songs.collectAsState()

    val downloadedItems = remember(allVideos, allSongs) {
        (allVideos + allSongs).take(8)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, tint = AccentPurple)
                    Text("Offline Downloads", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CardSurfaceDark,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Storage Used", color = SecondaryText, fontSize = 11.sp)
                            Text("1.42 GB Offline Media", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = AccentPurple.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, AccentPurple)
                        ) {
                            Text("OFFLINE MODE", color = AccentPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (downloadedItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No downloaded videos or songs found", color = SecondaryText, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(downloadedItems, key = { it.path }) { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPlayMedia(item) },
                                colors = CardDefaults.cardColors(containerColor = CardSurfaceDark),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MediaThumbnail(
                                        item = item,
                                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${item.displaySize} • Saved to Local Storage", color = SecondaryText, fontSize = 11.sp)
                                    }
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = AccentPurple)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        containerColor = DarkSurface
    )
}

fun saveUriToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val folder = File(context.filesDir, "custom_covers")
        if (!folder.exists()) {
            folder.mkdirs()
        }
        val file = File(folder, "cover_${System.currentTimeMillis()}.png")
        val outputStream = java.io.FileOutputStream(file)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) return "00:00"
    val totalSecs = durationMs / 1000
    val hours = totalSecs / 3600
    val minutes = (totalSecs % 3600) / 60
    val seconds = totalSecs % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
