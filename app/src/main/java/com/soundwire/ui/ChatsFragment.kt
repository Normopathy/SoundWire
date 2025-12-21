package com.soundwire.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.soundwire.*
import com.soundwire.databinding.FragmentChatsBinding
import kotlinx.coroutines.launch

class ChatsFragment : Fragment(R.layout.fragment_chats) {

    private var _binding: FragmentChatsBinding? = null
    private val binding get() = _binding!!

    private val chats = mutableListOf<ChatSummary>()
    private lateinit var adapter: ChatListAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentChatsBinding.bind(view)

        PresenceRepository.init(requireContext())

        adapter = ChatListAdapter(chats) { chat ->
            val title = when {
                !chat.title.isNullOrBlank() -> chat.title
                chat.type == "private" && chat.otherUser != null -> chat.otherUser.username
                else -> "Чат #${chat.id}"
            }
            val i = Intent(requireContext(), ChatActivity::class.java)
            i.putExtra("chatId", chat.id)
            i.putExtra("title", title)
            i.putExtra("chatType", chat.type)
            startActivity(i)
        }

        binding.recyclerChats.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerChats.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { load() }

        PresenceRepository.onlineIds.observe(viewLifecycleOwner) { online ->
            adapter.setOnlineIds(online)
        }

        binding.fabNewChat.setOnClickListener {
            showNewChatMenu()
        }

        load()
    }

    private fun showNewChatMenu() {
        val activity = activity as? MainActivity
        val items = arrayOf("Новый чат", "Новая группа")

        AlertDialog.Builder(requireContext())
            .setTitle("Создать")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> activity?.selectTab(MainActivity.Tab.CONTACTS)
                    1 -> startActivity(Intent(requireContext(), CreateGroupActivity::class.java))
                }
            }
            .show()
    }

    private fun load() {
        binding.swipeRefresh.isRefreshing = true
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val list = ApiProvider.api(requireContext()).listChats()
                chats.clear()
                chats.addAll(list)
                adapter.notifyDataSetChanged()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Не удалось загрузить чаты: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
