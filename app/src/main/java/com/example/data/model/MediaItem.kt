package com.example.data.model

import java.io.File
import java.io.Serializable

data class MediaItem(
    val id: Long,
    val title: String,
    val artist: String?,
    val album: String?,
    val duration: Long,
    val size: Long,
    val path: String,
    val dateAdded: Long,
    val mimeType: String,
    val isVideo: Boolean,
    val isFavorite: Boolean = false,
    val playbackPosition: Long = 0L,
    val lastPlayed: Long = 0L,
    val subtitlesPath: String? = null,
    val customCoverPath: String? = null
) : Serializable {
    val folderPath: String
        get() = try {
            File(path).parent ?: "/"
        } catch (e: Exception) {
            "/"
        }
    
    val folderName: String
        get() = try {
            File(path).parentFile?.name ?: "Root"
        } catch (e: Exception) {
            "Root"
        }

    val displaySize: String
        get() {
            val kb = size / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.2f MB", mb)
                kb >= 1.0 -> String.format("%.2f KB", kb)
                else -> "$size Bytes"
            }
        }

    val displayDuration: String
        get() {
            if (duration <= 0) return "00:00"
            val totalSecs = duration / 1000
            val hours = totalSecs / 3600
            val minutes = (totalSecs % 3600) / 60
            val seconds = totalSecs % 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
}
