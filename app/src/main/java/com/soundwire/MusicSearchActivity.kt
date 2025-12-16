package com.soundwire

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.soundwire.databinding.ActivityMusicSearchBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MusicSearchActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMusicSearchBinding
    val jamendoClientId = "38422152" // <<-- developer must set
    lateinit var api: JamendoApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMusicSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.jamendo.com/v3.0/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofit.create(JamendoApi::class.java)

        binding.btnSearch.setOnClickListener {
            val q = binding.etQuery.text.toString()
            if (q.isBlank()) return@setOnClickListener
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val resp = api.searchTracks(jamendoClientId, q, 10)
                    val list = resp.results.map { "${it.name} — ${it.artist_name}" }
                    val adapter = ArrayAdapter(this@MusicSearchActivity, android.R.layout.simple_list_item_1, list)
                    binding.listView.adapter = adapter
                    binding.listView.setOnItemClickListener { _, _, pos, _ ->
                        val track = resp.results[pos]
                        // play track preview via PlayerActivity
                        PlayerActivity.start(this@MusicSearchActivity, track.audio, track.name, track.artist_name, track.album_image)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@MusicSearchActivity, "Search failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
