package com.soundwire

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RecommendationManager(val jamendoClientId: String) {
    private val db = Firebase.firestore
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.jamendo.com/v3.0/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val api = retrofit.create(JamendoApi::class.java)

    suspend fun getRecentPlays(limit: Int = 5) = withContext(Dispatchers.IO) {
        val docs = db.collection("recent_plays")
            .orderBy("playedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()
        docs.documents.mapNotNull { it.data }
    }

    // simple recommendation: search Jamendo for tracks by same artist / keywords
    suspend fun recommendForArtist(artist: String): List<JamendoTrack> {
        return api.searchTracks(jamendoClientId, artist, 10).results
    }
}