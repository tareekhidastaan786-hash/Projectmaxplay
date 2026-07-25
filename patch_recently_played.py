import re

path = '/app/applet/app/src/main/java/com/example/ui/screens/DashboardScreen.kt'
with open(path, 'r') as f:
    content = f.read()

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

old_recent = extract_func("RecentlyPlayedSection")

new_recent = """fun RecentlyPlayedSection(
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
}"""

if old_recent:
    content = content.replace(old_recent, new_recent)
    with open(path, 'w') as f:
        f.write(content)
    print("RecentlyPlayedSection patched.")
else:
    print("RecentlyPlayedSection not found.")
