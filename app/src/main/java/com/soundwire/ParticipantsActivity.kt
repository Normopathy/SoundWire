package com.soundwire

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.soundwire.databinding.ActivityParticipantsBinding
import kotlinx.coroutines.launch

class ParticipantsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityParticipantsBinding
    private val participants = mutableListOf<ChatParticipant>()
    private lateinit var adapter: ParticipantsAdapter

    private var chatId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityParticipantsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        chatId = intent.getIntExtra("chatId", -1)
        val title = intent.getStringExtra("title") ?: "Участники"
        binding.tvTitle.text = "Участники: $title"

        if (chatId <= 0) {
            Toast.makeText(this, "chatId не задан", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        adapter = ParticipantsAdapter(participants)
        binding.recyclerParticipants.layoutManager = LinearLayoutManager(this)
        binding.recyclerParticipants.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { load() }
        binding.btnAdd.setOnClickListener { showAddDialog() }

        load()
    }

    private fun load() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val list = ApiProvider.api(this@ParticipantsActivity).getParticipants(chatId)
                adapter.setData(list)
            } catch (e: Exception) {
                Toast.makeText(this@ParticipantsActivity, "Не удалось загрузить участников: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun showAddDialog() {
        lifecycleScope.launch {
            try {
                val api = ApiProvider.api(this@ParticipantsActivity)
                val contacts = api.listContacts()
                val existing = participants.map { it.user.id }.toSet()
                val candidates = contacts.filter { !existing.contains(it.id) }

                if (candidates.isEmpty()) {
                    Toast.makeText(this@ParticipantsActivity, "Некого добавлять (все контакты уже в группе)", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val names = candidates.map { it.username }.toTypedArray()
                val checked = BooleanArray(candidates.size)

                AlertDialog.Builder(this@ParticipantsActivity)
                    .setTitle("Добавить участников")
                    .setMultiChoiceItems(names, checked) { _, which, isChecked ->
                        checked[which] = isChecked
                    }
                    .setNegativeButton("Отмена", null)
                    .setPositiveButton("Добавить") { _, _ ->
                        val ids = candidates.filterIndexed { idx, _ -> checked[idx] }.map { it.id }
                        if (ids.isEmpty()) {
                            Toast.makeText(this@ParticipantsActivity, "Никто не выбран", Toast.LENGTH_SHORT).show()
                        } else {
                            addParticipants(ids)
                        }
                    }
                    .show()

            } catch (e: Exception) {
                Toast.makeText(this@ParticipantsActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun addParticipants(ids: List<Int>) {
        lifecycleScope.launch {
            try {
                ApiProvider.api(this@ParticipantsActivity).addParticipants(chatId, AddParticipantsRequest(userIds = ids))
                Toast.makeText(this@ParticipantsActivity, "Добавлено: ${ids.size}", Toast.LENGTH_SHORT).show()
                load()
            } catch (e: Exception) {
                Toast.makeText(this@ParticipantsActivity, "Не удалось добавить: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
