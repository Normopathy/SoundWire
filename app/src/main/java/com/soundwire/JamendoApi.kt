package com.soundwire

import retrofit2.http.GET
import retrofit2.http.Query

data class JamendoResponse<T>(val headers: Any?, val results: List<T>)
data class JamendoTrack(
    val id: String,
    val name: String,
    val duration: Int,
    val audio: String, // streaming URL (provided by Jamendo)
    val artist_name: String,
    val album_image: String
)

interface JamendoApi {
    @GET("tracks/")
    suspend fun searchTracks(
        @Query("client_id") client_id: String,
        @Query("search") search: String,
        @Query("limit") limit: Int = 10,
        @Query("format") fmt: String = "json"
    ): JamendoResponse<JamendoTrack>
}
