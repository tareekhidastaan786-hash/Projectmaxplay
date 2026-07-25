import re

path = '/app/applet/app/src/main/java/com/example/ui/screens/DashboardScreen.kt'
with open(path, 'r') as f:
    content = f.read()

old_block = """                    val tabs = listOf(
                        Triple(0, "Video", Icons.Default.Movie),
                        ,
                        Triple(2, "Folder", Icons.Outlined.Folder),
                        Triple(3, "Playlist", Icons.AutoMirrored.Filled.QueueMusic),
                        Triple(4, "Favorite", Icons.Default.Favorite),
                        
                    )"""

new_block = """                    val tabs = listOf(
                        Triple(0, "Video", Icons.Default.Movie),
                        Triple(1, "Music", Icons.Default.MusicNote),
                        Triple(2, "Folder", Icons.Outlined.Folder),
                        Triple(3, "Playlist", Icons.AutoMirrored.Filled.QueueMusic),
                        Triple(4, "Favorite", Icons.Default.Favorite)
                    )"""

content = content.replace(old_block, new_block)
with open(path, 'w') as f:
    f.write(content)
