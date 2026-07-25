import re

path = '/app/applet/app/src/main/java/com/example/ui/screens/DashboardScreen.kt'
with open(path, 'r') as f:
    content = f.read()

# We need to replace MediaItemRow implementation.
# Let's extract the whole MediaItemRow function first.
start_str = "@OptIn(ExperimentalFoundationApi::class)\n@Composable\nfun MediaItemRow("
if start_str not in content:
    start_str = "@Composable\nfun MediaItemRow("
if start_str not in content:
    start_str = "fun MediaItemRow("

start_idx = content.find(start_str)
# Find the matching closing brace for MediaItemRow
stack = 0
end_idx = -1
in_func = False
for i in range(start_idx, len(content)):
    if content[i] == '{':
        stack += 1
        in_func = True
    elif content[i] == '}':
        stack -= 1
        if in_func and stack == 0:
            end_idx = i + 1
            break

old_func = content[start_idx:end_idx]

new_func = """@OptIn(ExperimentalFoundationApi::class)
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
                else -> Triple("MaxPlay", R.drawable.ic_launcher_foreground, null)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (sourceName == "MaxPlay") {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(Brush.linearGradient(listOf(Color(0xFF6A11CB), Color(0xFFB517FF))), androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = "MaxPlay",
                            modifier = Modifier.size(12.dp)
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
"""

if old_func:
    content = content.replace(old_func, new_func)
    with open(path, 'w') as f:
        f.write(content)
    print("MediaItemRow successfully patched.")
else:
    print("MediaItemRow not found.")
