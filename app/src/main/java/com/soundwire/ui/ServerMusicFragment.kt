package com.soundwire.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.soundwire.*
import com.soundwire.databinding.FragmentMusicServerBinding
import kotlinx.coroutines.launch

class ServerMusicFragment : Fragment(R.layout.fragment_music_server) {

    private var _binding: FragmentMusicServerBinding? = null
    private val binding get() = _binding!!

    private val tracks = mutableListOf<Track>()
    private lateinit var adapter: TracksAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMusicServerBinding.bind(view)

        adapter = TracksAdapter(tracks) { index ->
            PlayerActivity.start(requireContext(), tracks, index)
        }

        binding.recyclerTracks.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTracks.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { load() }

        load()
    }

    private fun load() {
        binding.swipeRefresh.isRefreshing = true
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val list = ApiProvider.api(requireContext()).listServerTracks()
                val base = ServerConfig.getBaseUrl(requireContext()).removeSuffix("/")

                tracks.clear()
                list.forEach { dto ->
                    val fullUrl = when {
                        dto.url.startsWith("http") -> dto.url
                        dto.url.startsWith("/") -> base + dto.url
                        else -> "$base/${dto.url}"
                    }
                    tracks.add(
                        Track(
                            id = dto.id,
                            title = dto.title,
                            artist = dto.artist,
                            uri = fullUrl,
                            source = TrackSource.SERVER
                        )
                    )
                }

                adapter.notifyDataSetChanged()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Не удалось получить список песен: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
