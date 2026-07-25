import re

path = '/app/applet/app/src/main/java/com/example/ui/screens/DashboardScreen.kt'
with open(path, 'r') as f:
    content = f.read()

# Add a simple Scrollbar indicator modifier to VideosTab and MusicTab
# Or just wrap the LazyColumn in a Box and draw a scrollbar.

# I will just add `val listState = rememberLazyListState()` and pass it to LazyColumn.
# But it's easier to skip the complex scrollbar logic since Compose doesn't have a drag-to-scroll thumb out of the box in standard Foundation. I will add a visual scrollbar.

scroll_import = "import androidx.compose.foundation.lazy.rememberLazyListState\n"
if scroll_import not in content:
    content = content.replace("import androidx.compose.foundation.layout.*", "import androidx.compose.foundation.layout.*\n" + scroll_import)

videos_tab = "fun VideosTab("
videos_tab_idx = content.find(videos_tab)
if videos_tab_idx != -1:
    videos_tab_state = "    val listState = rememberLazyListState()\n"
    content = content.replace("Column(modifier = Modifier.fillMaxSize()) {", "Column(modifier = Modifier.fillMaxSize()) {\n" + videos_tab_state)
    content = content.replace("LazyColumn(\n            modifier = Modifier.fillMaxSize(),", "LazyColumn(\n            state = listState,\n            modifier = Modifier.fillMaxSize(),")

music_tab = "fun MusicTab("
music_tab_idx = content.find(music_tab)
if music_tab_idx != -1:
    music_tab_state = "    val listState = rememberLazyListState()\n"
    # Find the next Column
    content = content.replace("Column(modifier = Modifier.fillMaxSize()) {\n        LazyColumn(", "Column(modifier = Modifier.fillMaxSize()) {\n" + music_tab_state + "        LazyColumn(\n            state = listState,")
    
with open(path, 'w') as f:
    f.write(content)
print("Scrollbar state added.")
