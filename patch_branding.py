import re

path = '/app/applet/app/src/main/java/com/example/ui/screens/DashboardScreen.kt'
with open(path, 'r') as f:
    content = f.read()

old_brand = """                    // Branding: "MaxPlay"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "MaxPl",
                            color = Color.White,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 1.dp)
                                .padding(top = 1.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFF6A11CB), Color(0xFFB517FF))))
                        )
                        Text(
                            text = "ay",
                            color = Color.White,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        )
                    }"""

new_brand = """                    // Branding: "MaxPlay"
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
                    }"""

content = content.replace(old_brand, new_brand)
with open(path, 'w') as f:
    f.write(content)
print("Branding patched.")
