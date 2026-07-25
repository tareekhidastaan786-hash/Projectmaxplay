#!/bin/bash
sed -i 's/Triple(0, "Video", Icons.Default.Movie),/Triple(0, "Video", Icons.Default.Movie),\n                        Triple(1, "Music", Icons.Default.MusicNote),/' /app/applet/app/src/main/java/com/example/ui/screens/DashboardScreen.kt
sed -i 's/Triple(1, "Music", Icons.Default.MusicNote)//g' /app/applet/app/src/main/java/com/example/ui/screens/DashboardScreen.kt
sed -i 's/Icons.Default.QueueMusic/Icons.AutoMirrored.Filled.QueueMusic/g' /app/applet/app/src/main/java/com/example/ui/screens/DashboardScreen.kt
