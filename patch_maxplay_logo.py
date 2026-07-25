import re

path = '/app/applet/app/src/main/java/com/example/ui/screens/DashboardScreen.kt'
with open(path, 'r') as f:
    content = f.read()

old_maxplay_source = """                if (sourceName == "MaxPlay") {
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
                } else {"""

new_maxplay_source = """                if (sourceName == "MaxPlay") {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.maxplay_logo),
                            contentDescription = "MaxPlay",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {"""

content = content.replace(old_maxplay_source, new_maxplay_source)

with open(path, 'w') as f:
    f.write(content)
print("MaxPlay logo patched.")
