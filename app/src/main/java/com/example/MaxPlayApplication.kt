package com.example

import android.app.Application
import androidx.room.Room
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.example.data.local.MediaDatabase
import com.example.data.local.SettingsManager
import com.example.data.repository.MediaRepository

class MaxPlayApplication : Application(), ImageLoaderFactory {
    lateinit var database: MediaDatabase
        private set
    lateinit var repository: MediaRepository
        private set
    lateinit var settingsManager: SettingsManager
        private set

    override fun onCreate() {
        super.onCreate()
        
        database = Room.databaseBuilder(
            applicationContext,
            MediaDatabase::class.java,
            "maxplay_database"
        ).fallbackToDestructiveMigration(true).build()
        
        repository = MediaRepository(applicationContext, database.mediaDao())
        settingsManager = SettingsManager(applicationContext)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }
}
