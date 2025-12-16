package com.soundwire

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.soundwire.databinding.ActivityPlaylistsBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class PlaylistsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlaylistsBinding
    val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaylistsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCreatePlaylist.setOnClickListener {
            val name = binding.etPlaylistName.text.toString()
            if (name.isBlank()) return@setOnClickListener
            val map = hashMapOf("name" to name, "createdAt" to com.google.firebase.Timestamp.now())
            db.collection("playlists").add(map)
        }
    }
}
