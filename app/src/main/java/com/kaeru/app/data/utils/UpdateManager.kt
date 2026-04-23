package com.kaeru.app.data.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.kaeru.app.BuildConfig
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType

class UpdateManager {
    private val jsonParser = Json {
        ignoreUnknownKeys = true
    }

    private val api: GithubApi by lazy {
        val contentType = "application/json".toMediaType()
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(jsonParser.asConverterFactory(contentType))
            .build()
            .create(GithubApi::class.java)
    }

    suspend fun checkForUpdate(): GithubRelease? {
        return try {
            val latestRelease = api.getLatestRelease()
            val currentVersion = BuildConfig.VERSION_NAME.removeSuffix("-release").trim()
            val cleanRemoteVersion = latestRelease.tagName.removePrefix("v")
            if (isNewer(cleanRemoteVersion, currentVersion)) {
                latestRelease
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun isNewer(remote: String, current: String): Boolean {
        val remoteParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }

        val length = maxOf(remoteParts.size, currentParts.size)

        for (i in 0 until length) {
            val r = remoteParts.getOrNull(i) ?: 0
            val c = currentParts.getOrNull(i) ?: 0
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    fun openDownloadPage(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getAllReleases(): Result<List<GithubRelease>> {
        return try {
            val releases = api.getReleases()
            Result.success(releases)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}