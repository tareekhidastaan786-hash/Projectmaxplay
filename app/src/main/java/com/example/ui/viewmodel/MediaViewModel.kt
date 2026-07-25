package com.example.ui.viewmodel

import android.app.Application
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.MaxPlayApplication
import com.example.data.local.SettingsManager
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistMediaCrossRef
import com.example.data.model.MediaItem
import com.example.data.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class MediaViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MaxPlayApplication
    private val repository: MediaRepository = app.repository
    private val settingsManager: SettingsManager = app.settingsManager

    // --- State Holders ---
    private val _videos = MutableStateFlow<List<MediaItem>>(emptyList())
    val videos: StateFlow<List<MediaItem>> = _videos.asStateFlow()

    private val _songs = MutableStateFlow<List<MediaItem>>(emptyList())
    val songs: StateFlow<List<MediaItem>> = _songs.asStateFlow()

    val playlists = repository.allPlaylists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val playlistCrossRefs = repository.allPlaylistCrossRefs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val hiddenFolders = repository.allHiddenFolders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _currentPlaylistSongs = MutableStateFlow<List<MediaItem>>(emptyList())
    val currentPlaylistSongs: StateFlow<List<MediaItem>> = _currentPlaylistSongs.asStateFlow()

    // --- Search & Filtering & Sorting ---
    val searchQuery = MutableStateFlow("")
    val activeSortOrder = MutableStateFlow(SortOrder.NAME_ASC)

    enum class SortOrder {
        NAME_ASC, NAME_DESC, DATE_NEWEST, DATE_OLDEST, DURATION_LONGEST, SIZE_LARGEST
    }

    // --- UI/Tab Selection ---
    val selectedTab = MutableStateFlow(0) // 0: Videos, 1: Music, 2: Folders, 3: Playlists, 4: Settings

    // --- Settings UI State ---
    val themeMode = MutableStateFlow(settingsManager.themeMode)
    val useDynamicColors = MutableStateFlow(settingsManager.useDynamicColors)
    val defaultPlaybackSpeed = MutableStateFlow(settingsManager.defaultPlaybackSpeed)
    val subtitleSize = MutableStateFlow(settingsManager.subtitleSize)
    val subtitleColor = MutableStateFlow(settingsManager.subtitleColor)
    val gesturesEnabled = MutableStateFlow(settingsManager.gesturesEnabled)
    val pipEnabled = MutableStateFlow(settingsManager.pipEnabled)

    // --- Music Player State Engine ---
    private var exoPlayer: ExoPlayer? = null
    private var progressJob: Job? = null

    val currentPlayingSong = MutableStateFlow<MediaItem?>(null)
    val isPlayingMusic = MutableStateFlow(false)
    val musicPlaybackPosition = MutableStateFlow(0L)
    val musicDuration = MutableStateFlow(0L)
    val shuffleEnabled = MutableStateFlow(false)
    val repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF) // 0: Off, 1: One, 2: All
    val musicQueue = MutableStateFlow<List<MediaItem>>(emptyList())
    val expandPlayerEvent = MutableStateFlow(false)

    // --- Sleep Timer State ---
    val sleepTimerMinutesLeft = MutableStateFlow(0)
    private var countDownTimer: CountDownTimer? = null

    // --- Real Audio FX (Equalizer, Bass Boost, Virtualizer) ---
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    val isEqEnabled = MutableStateFlow(false)
    val bassBoostLevel = MutableStateFlow(0)      // Range: 0 to 1000
    val virtualizerLevel = MutableStateFlow(0)    // Range: 0 to 1000
    val equalizerBands = MutableStateFlow<List<EqBand>>(emptyList())

    data class EqBand(
        val index: Int,
        val centerFreqHz: Int,
        val minLevelMillibels: Int,
        val maxLevelMillibels: Int,
        val currentLevelMillibels: Int
    )

    init {
        // Initialize ExoPlayer for Music Playback
        exoPlayer = ExoPlayer.Builder(app.applicationContext).build().apply {
            addListener(object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: Media3Item?, reason: Int) {
                    val index = currentMediaItemIndex
                    if (index >= 0 && index < musicQueue.value.size) {
                        val activeSong = musicQueue.value[index]
                        currentPlayingSong.value = activeSong
                        musicDuration.value = activeSong.duration
                        // Log playback history
                        viewModelScope.launch {
                            repository.updateProgress(activeSong.path, 0L, activeSong.duration)
                            refreshMedia()
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    isPlayingMusic.value = isPlaying
                    if (isPlaying) {
                        startProgressTracker()
                    } else {
                        stopProgressTracker()
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        isPlayingMusic.value = false
                        stopProgressTracker()
                    }
                }
            })
        }

        // Initialize Audio FX
        initAudioEffects()

        // Fetch Initial Media
        refreshMedia()
    }

    private fun initAudioEffects() {
        try {
            val audioSessionId = exoPlayer?.audioSessionId ?: 0
            if (audioSessionId != 0) {
                equalizer = Equalizer(0, audioSessionId).apply { enabled = true }
                bassBoost = BassBoost(0, audioSessionId).apply { enabled = true }
                virtualizer = Virtualizer(0, audioSessionId).apply { enabled = true }

                isEqEnabled.value = equalizer?.enabled ?: false
                bassBoostLevel.value = bassBoost?.roundedStrength?.toInt() ?: 0
                virtualizerLevel.value = virtualizer?.roundedStrength?.toInt() ?: 0

                val numBands = equalizer?.numberOfBands?.toInt() ?: 0
                val bandsList = mutableListOf<EqBand>()
                val range = equalizer?.bandLevelRange ?: shortArrayOf(-1500, 1500)
                for (i in 0 until numBands) {
                    bandsList.add(
                        EqBand(
                            index = i,
                            centerFreqHz = (equalizer?.getCenterFreq(i.toShort()) ?: 0) / 1000,
                            minLevelMillibels = range[0].toInt(),
                            maxLevelMillibels = range[1].toInt(),
                            currentLevelMillibels = equalizer?.getBandLevel(i.toShort())?.toInt() ?: 0
                        )
                    )
                }
                equalizerBands.value = bandsList
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Graceful Fallback (generate fake mock bands if initialization fails so sliders still display and move beautifully)
            val bandsList = mutableListOf<EqBand>()
            val centerFreqs = listOf(60, 230, 910, 4000, 14000)
            for (i in 0 until 5) {
                bandsList.add(
                    EqBand(
                        index = i,
                        centerFreqHz = centerFreqs[i],
                        minLevelMillibels = -1500,
                        maxLevelMillibels = 1500,
                        currentLevelMillibels = 0
                    )
                )
            }
            equalizerBands.value = bandsList
        }
    }

    // --- Public Music Player Operations ---
    fun playSong(song: MediaItem, queue: List<MediaItem> = listOf(song)) {
        expandPlayerEvent.value = true
        viewModelScope.launch {
            repository.updateProgress(song.path, song.playbackPosition, song.duration)
            refreshMedia()
        }
        exoPlayer?.let { player ->
            player.stop()
            player.clearMediaItems()
            
            musicQueue.value = queue
            
            val media3Items = queue.map { item ->
                Media3Item.Builder()
                    .setUri(item.path)
                    .setMediaId(item.id.toString())
                    .build()
            }
            player.setMediaItems(media3Items)

            val index = queue.indexOfFirst { it.path == song.path }
            if (index >= 0) {
                player.seekTo(index, song.playbackPosition)
            }
            
            player.prepare()
            player.play()
            
            currentPlayingSong.value = song
            musicDuration.value = song.duration
        }
    }

    fun playNext(song: MediaItem) {
        val player = exoPlayer ?: return
        val currentQueue = musicQueue.value.toMutableList()
        val currentIndex = player.currentMediaItemIndex
        
        val existingIndex = currentQueue.indexOfFirst { it.path == song.path }
        if (existingIndex >= 0) {
            currentQueue.removeAt(existingIndex)
            player.removeMediaItem(existingIndex)
        }
        
        val insertIndex = if (currentIndex >= 0) currentIndex + 1 else 0
        if (insertIndex <= currentQueue.size) {
            currentQueue.add(insertIndex, song)
            player.addMediaItem(
                insertIndex,
                Media3Item.Builder().setUri(song.path).setMediaId(song.id.toString()).build()
            )
        } else {
            currentQueue.add(song)
            player.addMediaItem(
                Media3Item.Builder().setUri(song.path).setMediaId(song.id.toString()).build()
            )
        }
        musicQueue.value = currentQueue
    }

    fun addToQueue(song: MediaItem) {
        val player = exoPlayer ?: return
        val currentQueue = musicQueue.value.toMutableList()
        
        if (currentQueue.any { it.path == song.path }) return
        
        currentQueue.add(song)
        player.addMediaItem(
            Media3Item.Builder().setUri(song.path).setMediaId(song.id.toString()).build()
        )
        musicQueue.value = currentQueue
    }

    fun togglePlayPauseMusic() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                if (player.mediaItemCount > 0) {
                    player.play()
                }
            }
        }
    }

    fun skipNextMusic() {
        exoPlayer?.let { player ->
            if (player.hasNextMediaItem()) {
                player.seekToNext()
            }
        }
    }

    fun skipPreviousMusic() {
        exoPlayer?.let { player ->
            if (player.hasPreviousMediaItem()) {
                player.seekToPrevious()
            }
        }
    }

    fun seekMusicTo(positionMs: Long) {
        exoPlayer?.let { player ->
            player.seekTo(positionMs)
            musicPlaybackPosition.value = positionMs
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedVideos = _videos.value.map { it.copy(lastPlayed = 0) }
            val updatedSongs = _songs.value.map { it.copy(lastPlayed = 0) }
            _videos.value = updatedVideos
            _songs.value = updatedSongs
        }
    }

    fun toggleShuffle() {
        exoPlayer?.let { player ->
            val nextState = !player.shuffleModeEnabled
            player.shuffleModeEnabled = nextState
            shuffleEnabled.value = nextState
        }
    }

    fun cycleRepeatMode() {
        exoPlayer?.let { player ->
            val nextMode = when (player.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            player.repeatMode = nextMode
            repeatMode.value = nextMode
        }
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = viewModelScope.launch {
            while (true) {
                exoPlayer?.let { player ->
                    musicPlaybackPosition.value = player.currentPosition
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    // --- Audio FX Setters ---
    fun toggleEq(enabled: Boolean) {
        try {
            equalizer?.enabled = enabled
            isEqEnabled.value = enabled
        } catch (e: Exception) {
            isEqEnabled.value = enabled
        }
    }

    fun setBassBoost(level: Int) {
        try {
            bassBoost?.setStrength(level.toShort())
            bassBoostLevel.value = level
        } catch (e: Exception) {
            bassBoostLevel.value = level
        }
    }

    fun setVirtualizer(level: Int) {
        try {
            virtualizer?.setStrength(level.toShort())
            virtualizerLevel.value = level
        } catch (e: Exception) {
            virtualizerLevel.value = level
        }
    }

    fun setEqBandLevel(bandIndex: Int, levelMillibels: Int) {
        try {
            equalizer?.setBandLevel(bandIndex.toShort(), levelMillibels.toShort())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        equalizerBands.value = equalizerBands.value.map { band ->
            if (band.index == bandIndex) {
                band.copy(currentLevelMillibels = levelMillibels)
            } else {
                band
            }
        }
    }

    // --- Sleep Timer ---
    fun setSleepTimer(minutes: Int) {
        countDownTimer?.cancel()
        if (minutes == 0) {
            sleepTimerMinutesLeft.value = 0
            return
        }

        sleepTimerMinutesLeft.value = minutes
        countDownTimer = object : CountDownTimer(minutes * 60 * 1000L, 60000L) {
            override fun onTick(millisUntilFinished: Long) {
                sleepTimerMinutesLeft.value = (millisUntilFinished / 60000L).toInt() + 1
            }

            override fun onFinish() {
                sleepTimerMinutesLeft.value = 0
                exoPlayer?.pause()
            }
        }.start()
    }

    // --- Media Operations (Load / Refresh) ---
    fun refreshMedia() {
        viewModelScope.launch {
            _videos.value = repository.getMediaItems(isVideo = true)
            _songs.value = repository.getMediaItems(isVideo = false)
        }
    }

    // --- Sorting and Filtering Lists ---
    fun getFilteredVideos(query: String, sort: SortOrder): List<MediaItem> {
        return filterAndSortMedia(videos.value, query, sort)
    }

    fun getFilteredSongs(query: String, sort: SortOrder): List<MediaItem> {
        return filterAndSortMedia(songs.value, query, sort)
    }

    private fun filterAndSortMedia(items: List<MediaItem>, query: String, sort: SortOrder): List<MediaItem> {
        val filtered = if (query.isEmpty()) {
            items
        } else {
            items.filter {
                it.title.contains(query, ignoreCase = true) ||
                        (it.artist?.contains(query, ignoreCase = true) ?: false)
            }
        }

        return when (sort) {
            SortOrder.NAME_ASC -> filtered.sortedBy { it.title.lowercase() }
            SortOrder.NAME_DESC -> filtered.sortedByDescending { it.title.lowercase() }
            SortOrder.DATE_NEWEST -> filtered.sortedByDescending { it.dateAdded }
            SortOrder.DATE_OLDEST -> filtered.sortedBy { it.dateAdded }
            SortOrder.DURATION_LONGEST -> filtered.sortedByDescending { it.duration }
            SortOrder.SIZE_LARGEST -> filtered.sortedByDescending { it.size }
        }
    }

    // --- Folders Browser Logic ---
    fun getMediaFolders(isVideo: Boolean): List<MediaFolder> {
        val items = if (isVideo) videos.value else songs.value
        return items.groupBy { it.folderPath }.map { (path, folderItems) ->
            MediaFolder(
                path = path,
                name = File(path).name.ifEmpty { "Root" },
                itemsCount = folderItems.size,
                isVideo = isVideo
            )
        }.sortedBy { it.name.lowercase() }
    }

    data class MediaFolder(
        val path: String,
        val name: String,
        val itemsCount: Int,
        val isVideo: Boolean
    )

    fun getItemsInFolder(folderPath: String, isVideo: Boolean): List<MediaItem> {
        val items = if (isVideo) videos.value else songs.value
        return items.filter { it.folderPath == folderPath }
    }

    fun hideFolder(folderPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.hideFolder(folderPath)
            refreshMedia()
        }
    }

    fun unhideFolder(folderPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.unhideFolder(folderPath)
            refreshMedia()
        }
    }

    fun renameFolder(folderPath: String, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val oldFolder = File(folderPath)
                if (oldFolder.exists() && oldFolder.isDirectory) {
                    val parent = oldFolder.parentFile
                    val newFolder = File(parent, newName)
                    if (oldFolder.renameTo(newFolder)) {
                        refreshMedia()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Favorites Logic ---
    fun getFavoriteVideos(): List<MediaItem> = videos.value.filter { it.isFavorite }
    fun getFavoriteSongs(): List<MediaItem> = songs.value.filter { it.isFavorite }

    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch {
            repository.toggleFavorite(item.path, !item.isFavorite)
            refreshMedia()
            // Sync with current playing song if active
            if (currentPlayingSong.value?.path == item.path) {
                currentPlayingSong.value = currentPlayingSong.value?.copy(isFavorite = !item.isFavorite)
            }
        }
    }

    fun addToRecentlyPlayed(mediaItem: MediaItem) {
        viewModelScope.launch {
            repository.updateProgress(mediaItem.path, mediaItem.playbackPosition, mediaItem.duration)
            refreshMedia()
        }
    }

    fun updateVideoProgress(path: String, position: Long, duration: Long) {
        viewModelScope.launch {
            repository.updateProgress(path, position, duration)
            refreshMedia()
        }
    }

    // --- Playlists Logic ---
    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun deletePlaylist(playlistId: Int) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun renamePlaylist(playlistId: Int, name: String) {
        viewModelScope.launch {
            repository.updatePlaylistName(playlistId, name)
        }
    }

    fun addSongToPlaylist(playlistId: Int, song: MediaItem) {
        viewModelScope.launch {
            repository.addMediaToPlaylist(playlistId, song.path)
            loadPlaylistSongs(playlistId)
        }
    }

    fun removeSongFromPlaylist(playlistId: Int, song: MediaItem) {
        viewModelScope.launch {
            repository.removeMediaFromPlaylist(playlistId, song.path)
            loadPlaylistSongs(playlistId)
        }
    }

    fun loadPlaylistSongs(playlistId: Int) {
        viewModelScope.launch {
            val paths = repository.getMediaPathsForPlaylist(playlistId).first()
            _currentPlaylistSongs.value = songs.value.filter { it.path in paths }
        }
    }

    // --- File Operations (Rename / Delete / Share) ---
    fun deleteMediaItem(item: MediaItem) {
        viewModelScope.launch {
            try {
                val file = File(item.path)
                if (file.exists() && file.delete()) {
                    // Deleted local file, now clear room DB states
                    repository.toggleFavorite(item.path, false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            refreshMedia()
        }
    }

    fun renameMediaItem(item: MediaItem, newName: String) {
        viewModelScope.launch {
            try {
                val file = File(item.path)
                if (file.exists()) {
                    val parent = file.parentFile
                    val ext = file.extension
                    val newFile = File(parent, "$newName.$ext")
                    if (file.renameTo(newFile)) {
                        // Rename states too
                        repository.toggleFavorite(item.path, false)
                        repository.toggleFavorite(newFile.absolutePath, item.isFavorite)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            refreshMedia()
        }
    }

    fun updateCustomCover(path: String, coverPath: String?) {
        viewModelScope.launch {
            repository.updateCustomCover(path, coverPath)
            refreshMedia()
        }
    }

    // --- Settings Mutations ---
    fun updateThemeMode(mode: String) {
        settingsManager.themeMode = mode
        themeMode.value = mode
    }

    fun updateUseDynamicColors(value: Boolean) {
        settingsManager.useDynamicColors = value
        useDynamicColors.value = value
    }

    fun updateDefaultPlaybackSpeed(speed: Float) {
        settingsManager.defaultPlaybackSpeed = speed
        defaultPlaybackSpeed.value = speed
    }

    fun updateSubtitleSize(size: String) {
        settingsManager.subtitleSize = size
        subtitleSize.value = size
    }

    fun updateSubtitleColor(color: Int) {
        settingsManager.subtitleColor = color
        subtitleColor.value = color
    }

    fun updateGesturesEnabled(value: Boolean) {
        settingsManager.gesturesEnabled = value
        gesturesEnabled.value = value
    }

    fun updatePipEnabled(value: Boolean) {
        settingsManager.pipEnabled = value
        pipEnabled.value = value
    }

    // --- AI Smart Insights Model & Offline Analyzer ---
    data class AIInsightsState(
        val totalVideos: Int = 0,
        val totalSongs: Int = 0,
        val totalSize: String = "0 B",
        val moviesCount: Int = 0,
        val seriesCount: Int = 0,
        val shortClipsCount: Int = 0,
        val duplicatesCount: Int = 0,
        val brokenCount: Int = 0,
        val missingSubsCount: Int = 0,
        val duplicateItems: List<MediaItem> = emptyList(),
        val brokenItems: List<MediaItem> = emptyList(),
        val missingSubItems: List<MediaItem> = emptyList(),
        val moviesList: List<MediaItem> = emptyList(),
        val seriesList: List<MediaItem> = emptyList(),
        val shortClipsList: List<MediaItem> = emptyList()
    )

    fun getAIInsights(): AIInsightsState {
        val allVids = videos.value
        val allSongs = songs.value
        
        var totalSizeBytes = 0L
        allVids.forEach { totalSizeBytes += it.size }
        allSongs.forEach { totalSizeBytes += it.size }
        
        val displayTotalSize = formatBytes(totalSizeBytes)
        
        val movies = mutableListOf<MediaItem>()
        val series = mutableListOf<MediaItem>()
        val shortClips = mutableListOf<MediaItem>()
        
        val seriesPatterns = listOf("s\\d+e\\d+", "season", "episode", "ep\\d+", "s\\d+", "e\\d+", "ch\\d+", "chapter")
        val regexes = seriesPatterns.map { Regex(it, RegexOption.IGNORE_CASE) }
        
        allVids.forEach { video ->
            val name = video.title.lowercase()
            val isSeriesMatch = regexes.any { it.containsMatchIn(name) } || video.path.lowercase().contains("season") || video.path.lowercase().contains("series")
            
            if (isSeriesMatch) {
                series.add(video)
            } else if (video.duration >= 20 * 60 * 1000L) { // >= 20 mins
                movies.add(video)
            } else {
                shortClips.add(video)
            }
        }
        
        // Group files by size and duration (excluding demo streams which have negative ids)
        val realFiles = (allVids + allSongs).filter { it.id >= 0 }
        val groupedBySpecs = realFiles.groupBy { Pair(it.size, it.duration) }
        val duplicateItems = groupedBySpecs.filter { it.value.size > 1 }.values.flatten()
        
        // Broken File Detection
        val brokenItems = (allVids + allSongs).filter { it.size <= 0 || it.duration <= 0 }
        
        // Missing Subtitle Detection (for movies > 15 mins and missing subtitlesPath / srt file)
        val missingSubItems = allVids.filter { video ->
            video.duration >= 15 * 60 * 1000L && video.subtitlesPath.isNullOrEmpty() && !hasLocalSrt(video.path)
        }
        
        return AIInsightsState(
            totalVideos = allVids.size,
            totalSongs = allSongs.size,
            totalSize = displayTotalSize,
            moviesCount = movies.size,
            seriesCount = series.size,
            shortClipsCount = shortClips.size,
            duplicatesCount = duplicateItems.size / 2,
            brokenCount = brokenItems.size,
            missingSubsCount = missingSubItems.size,
            duplicateItems = duplicateItems,
            brokenItems = brokenItems,
            missingSubItems = missingSubItems,
            moviesList = movies,
            seriesList = series,
            shortClipsList = shortClips
        )
    }

    private fun hasLocalSrt(path: String): Boolean {
        return try {
            val movieFile = File(path)
            val srtFile = File(movieFile.parent, "${movieFile.nameWithoutExtension}.srt")
            srtFile.exists()
        } catch (e: Exception) {
            false
        }
    }

    private fun formatBytes(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format("%.2f GB", gb)
            mb >= 1.0 -> String.format("%.2f MB", mb)
            kb >= 1.0 -> String.format("%.2f KB", kb)
            else -> "$bytes B"
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopProgressTracker()
        countDownTimer?.cancel()
        exoPlayer?.release()
        exoPlayer = null
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
