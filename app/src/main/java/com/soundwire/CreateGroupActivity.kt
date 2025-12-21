package com.soundwire

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.soundwire.databinding.ActivityCreateGroupBinding
import kotlinx.coroutines.launch

class CreateGroupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateGroupBinding
    private lateinit var adapter: SelectableUsersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = SelectableUsersAdapter(mutableListOf())
        binding.recyclerUsers.layoutManager = LinearLayoutManager(this)
        binding.recyclerUsers.adapter = adapter

        binding.btnCreate.setOnClickListener { createGroup() }

        loadContacts()
    }

    private fun loadContacts() {
        lifecycleScope.launch {
            try {
                val contacts = ApiProvider.api(this@CreateGroupActivity).listContacts()
                adapter.setData(contacts)
            } catch (e: Exception) {
                Toast.makeText(this@CreateGroupActivity, "Не удалось загрузить контакты: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun createGroup() {
        val title = binding.etTitle.text?.toString()?.trim().orEmpty()
        val ids = adapter.selectedIds()

        if (title.isBlank()) {
            Toast.makeText(this, "Введите название группы", Toast.LENGTH_SHORT).show()
            return
        }

        if (ids.isEmpty()) {
            Toast.makeText(this, "Выберите хотя бы одного участника", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val chat = ApiProvider.api(this@CreateGroupActivity)
                    .createGroupChat(CreateGroupChatRequest(title = title, participantIds = ids))

                val i = Intent(this@CreateGroupActivity, ChatActivity::class.java)
                i.putExtra("chatId", chat.id)
                i.putExtra("title", title)
                i.putExtra("chatType", chat.type)
                startActivity(i)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@CreateGroupActivity, "Не удалось создать группу: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
