package com.example.ui.components

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.example.data.model.MediaItem
import com.example.ui.theme.PurpleEnd
import com.example.ui.theme.PurpleMid
import com.example.ui.theme.PurpleStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val thumbnailCache = android.util.LruCache<String, Bitmap>(300)

@Composable
fun MediaThumbnail(
    item: MediaItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(item.path, item.customCoverPath) { mutableStateOf<Bitmap?>(thumbnailCache.get(item.path)) }
    var loadAttempted by remember(item.path, item.customCoverPath) { mutableStateOf(thumbnailCache.get(item.path) != null) }

    LaunchedEffect(item.path, item.customCoverPath) {
        if (item.customCoverPath == null && bitmap == null) {
            withContext(Dispatchers.IO) {
                try {
                    val doubleCheck = thumbnailCache.get(item.path)
                    if (doubleCheck != null) {
                        bitmap = doubleCheck
                        loadAttempted = true
                        return@withContext
                    }

                    var b: Bitmap? = null
                    if (item.isVideo) {
                        val retriever = MediaMetadataRetriever()
                        try {
                            if (item.path.startsWith("http") || item.path.startsWith("rtsp") || item.path.startsWith("content://")) {
                                retriever.setDataSource(context, Uri.parse(item.path))
                            } else {
                                retriever.setDataSource(item.path)
                            }
                            b = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                ?: retriever.getFrameAtTime(0)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            try {
                                retriever.release()
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            }
                        }
                    } else {
                        // Audio thumbnail loading
                        try {
                            val uri = if (item.path.startsWith("content://")) {
                                Uri.parse(item.path)
                            } else {
                                val baseUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                                android.content.ContentUris.withAppendedId(baseUri, item.id)
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                b = context.contentResolver.loadThumbnail(uri, Size(300, 300), null)
                            }
                        } catch (e: Exception) {
                            // ignore and try retriever
                        }
                        if (b == null) {
                            val retriever = MediaMetadataRetriever()
                            try {
                                if (item.path.startsWith("http") || item.path.startsWith("rtsp") || item.path.startsWith("content://")) {
                                    retriever.setDataSource(context, Uri.parse(item.path))
                                } else {
                                    retriever.setDataSource(item.path)
                                }
                                val pic = retriever.embeddedPicture
                                if (pic != null) {
                                    b = android.graphics.BitmapFactory.decodeByteArray(pic, 0, pic.size)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                try {
                                    retriever.release()
                                } catch (ex: Exception) {
                                    ex.printStackTrace()
                                }
                            }
                        }
                    }

                    if (b != null) {
                        thumbnailCache.put(item.path, b)
                        bitmap = b
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    loadAttempted = true
                }
            }
        } else {
            loadAttempted = true
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (item.customCoverPath != null) {
            AsyncImage(
                model = item.customCoverPath,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (loadAttempted) {
            val iconGradient = if (item.isVideo) {
                Brush.linearGradient(colors = listOf(PurpleStart, PurpleMid))
            } else {
                Brush.linearGradient(colors = listOf(PurpleMid, PurpleEnd))
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(iconGradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.isVideo) Icons.Default.Movie else Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    }
}
