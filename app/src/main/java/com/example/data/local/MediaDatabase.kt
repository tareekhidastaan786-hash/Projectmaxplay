package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// Entities
@Entity(tableName = "media_playback_state")
data class MediaPlaybackStateEntity(
    @PrimaryKey val path: String,
    val isFavorite: Boolean = false,
    val playbackPosition: Long = 0L,
    val lastPlayed: Long = 0L,
    val subtitlesPath: String? = null,
    val customCoverPath: String? = null
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_media_cross_ref",
    primaryKeys = ["playlistId", "mediaPath"]
)
data class PlaylistMediaCrossRef(
    val playlistId: Int,
    val mediaPath: String
)

@Entity(tableName = "hidden_folders")
data class HiddenFolderEntity(
    @PrimaryKey val folderPath: String
)

// DAO Interface
@Dao
interface MediaDao {
    // Media Playback State Queries
    @Query("SELECT * FROM media_playback_state")
    fun getAllPlaybackStates(): Flow<List<MediaPlaybackStateEntity>>

    @Query("SELECT * FROM media_playback_state WHERE path = :path LIMIT 1")
    suspend fun getPlaybackState(path: String): MediaPlaybackStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePlaybackState(state: MediaPlaybackStateEntity)

    @Query("UPDATE media_playback_state SET isFavorite = :isFav WHERE path = :path")
    suspend fun updateFavorite(path: String, isFav: Boolean)

    @Query("UPDATE media_playback_state SET playbackPosition = :pos, lastPlayed = :lastPlayed WHERE path = :path")
    suspend fun updatePlaybackProgress(path: String, pos: Long, lastPlayed: Long)

    @Query("UPDATE media_playback_state SET subtitlesPath = :subPath WHERE path = :path")
    suspend fun updateSubtitlesPath(path: String, subPath: String?)

    @Query("UPDATE media_playback_state SET customCoverPath = :coverPath WHERE path = :path")
    suspend fun updateCustomCoverPath(path: String, coverPath: String?)

    @Query("DELETE FROM media_playback_state WHERE path = :path")
    suspend fun deletePlaybackState(path: String)

    // Playlist Queries
    @Query("SELECT * FROM playlists ORDER BY name ASC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Int)

    @Query("UPDATE playlists SET name = :newName WHERE id = :playlistId")
    suspend fun updatePlaylistName(playlistId: Int, newName: String)

    // Playlist Media Association Queries
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistMedia(crossRef: PlaylistMediaCrossRef)

    @Query("DELETE FROM playlist_media_cross_ref WHERE playlistId = :playlistId AND mediaPath = :mediaPath")
    suspend fun removeMediaFromPlaylist(playlistId: Int, mediaPath: String)

    @Query("DELETE FROM playlist_media_cross_ref WHERE playlistId = :playlistId")
    suspend fun clearPlaylistMedia(playlistId: Int)

    @Query("SELECT mediaPath FROM playlist_media_cross_ref WHERE playlistId = :playlistId")
    fun getMediaPathsForPlaylist(playlistId: Int): Flow<List<String>>

    @Query("SELECT * FROM playlist_media_cross_ref")
    fun getAllPlaylistCrossRefs(): Flow<List<PlaylistMediaCrossRef>>

    // Hidden Folders
    @Query("SELECT * FROM hidden_folders")
    fun getAllHiddenFolders(): Flow<List<HiddenFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun hideFolder(folder: HiddenFolderEntity)

    @Query("DELETE FROM hidden_folders WHERE folderPath = :folderPath")
    suspend fun unhideFolder(folderPath: String)
}

// Database Class
@Database(
    entities = [
        MediaPlaybackStateEntity::class,
        PlaylistEntity::class,
        PlaylistMediaCrossRef::class,
        HiddenFolderEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MediaDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
}
