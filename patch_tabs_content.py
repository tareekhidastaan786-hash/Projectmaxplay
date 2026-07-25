import re

path = '/app/applet/app/src/main/java/com/example/ui/screens/DashboardScreen.kt'
with open(path, 'r') as f:
    content = f.read()

# Replace VideosTab
def extract_func(name):
    start_str = f"fun {name}("
    start_idx = content.find(start_str)
    if start_idx == -1:
        return ""
    stack = 0
    in_func = False
    for i in range(start_idx, len(content)):
        if content[i] == '{':
            stack += 1
            in_func = True
        elif content[i] == '}':
            stack -= 1
            if in_func and stack == 0:
                return content[start_idx:i+1]
    return ""

old_videos_tab = extract_func("VideosTab")
old_music_tab = extract_func("MusicTab")

new_videos_tab = """fun VideosTab(
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
        LazyColumn(
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
                    items(monthVideos) { video ->
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
}"""

new_music_tab = """fun MusicTab(
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
        LazyColumn(
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
                    items(monthSongs) { song ->
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
}"""

if old_videos_tab:
    content = content.replace(old_videos_tab, new_videos_tab)
if old_music_tab:
    content = content.replace(old_music_tab, new_music_tab)

with open(path, 'w') as f:
    f.write(content)
print("Tabs patched.")
