package com.kaeru.app.data.utils

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET

@Serializable
data class GithubRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("body") val body: String,
    @SerialName("published_at") val releaseDate: String
)

interface GithubApi {
    @GET("repos/kkyago/KaeruTrack/releases/latest")
    suspend fun getLatestRelease(): GithubRelease
    @GET("repos/kkyago/KaeruTrack/releases")
    suspend fun getReleases(): List<GithubRelease>
}