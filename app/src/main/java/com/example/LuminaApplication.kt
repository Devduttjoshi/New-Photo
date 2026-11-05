package com.example

import android.app.Application
import android.os.Build
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Custom Application class that enforces strict, bounded disk and memory limits on Coil
 * and manages global app-wide image caching.
 *
 * Without explicit configuration, Coil and OkHttp can default to caching indefinitely
 * across thousands of high-resolution images, exceeding 20 GB!
 * Here we constrain Coil's internal disk cache to a strict 150 MB ceiling
 * with automatic FIFO/LRU eviction.
 */
class LuminaApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        Log.d("LuminaApplication", "Initializing LuminaApplication with bounded cache limits")
    }

    override fun newImageLoader(): ImageLoader {
        val boundedOkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(boundedOkHttpClient)
            .memoryCache {
                MemoryCache.Builder(this)
                    // Maximum 25% of available app memory for bitmap caching
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(cacheDir, "coil_image_cache"))
                    // Strict hard cap of 150 MB for Coil's HTTP cache
                    .maxSizeBytes(150L * 1024 * 1024)
                    .build()
            }
            // Optimize image decoding: use RGB_565 on lower memory or standard ARGB_8888
            .allowRgb565(true)
            .crossfade(false)
            .build()
    }
}
