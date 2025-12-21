package com.soundwire.ui

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.soundwire.PlayerActivity
import com.soundwire.R
import com.soundwire.Track
import com.soundwire.TrackSource
import com.soundwire.TracksAdapter
import com.soundwire.databinding.FragmentMusicLocalBinding
import java.io.File
import java.io.FileOutputStream

class LocalMusicFragment : Fragment(R.layout.fragment_music_local) {

    private var _binding: FragmentMusicLocalBinding? = null
    private val binding get() = _binding!!

    private val tracks = mutableListOf<Track>()
    private lateinit var adapter: TracksAdapter

    private val pickAudio = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            importAudio(uri)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMusicLocalBinding.bind(view)

        adapter = TracksAdapter(tracks) { index ->
            PlayerActivity.start(requireContext(), tracks, index)
        }

        binding.recyclerTracks.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTracks.adapter = adapter

        binding.btnAddTrack.setOnClickListener {
            pickAudio.launch("audio/*")
        }

        loadLocalTracks()
    }

    private fun loadLocalTracks() {
        val dir = File(requireContext().filesDir, "local_music")
        if (!dir.exists()) dir.mkdirs()

        val files = dir.listFiles()?.filter { it.isFile }?.sortedBy { it.name } ?: emptyList()

        tracks.clear()
        files.forEach { f ->
            tracks.add(
                Track(
                    id = f.name,
                    title = f.nameWithoutExtension,
                    artist = null,
                    uri = Uri.fromFile(f).toString(),
                    source = TrackSource.LOCAL
                )
            )
        }

        adapter.notifyDataSetChanged()
    }

    private fun importAudio(uri: Uri) {
        try {
            val fileName = queryDisplayName(uri) ?: "track_${System.currentTimeMillis()}.mp3"
            val dir = File(requireContext().filesDir, "local_music")
            if (!dir.exists()) dir.mkdirs()
            val outFile = File(dir, fileName)

            requireContext().contentResolver.openInputStream(uri).use { input ->
                if (input == null) {
                    Toast.makeText(requireContext(), "Не удалось открыть файл", Toast.LENGTH_LONG).show()
                    return
                }
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }

            Toast.makeText(requireContext(), "Добавлено: $fileName", Toast.LENGTH_SHORT).show()
            loadLocalTracks()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Ошибка импорта: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null) ?: return null
        cursor.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst() && nameIndex >= 0) {
                return it.getString(nameIndex)
            }
        }
        return null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
