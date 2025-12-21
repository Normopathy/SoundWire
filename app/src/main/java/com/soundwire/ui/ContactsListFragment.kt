package com.soundwire.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.soundwire.*
import com.soundwire.databinding.FragmentContactsListBinding
import kotlinx.coroutines.launch

class ContactsListFragment : Fragment(R.layout.fragment_contacts_list) {

    private var _binding: FragmentContactsListBinding? = null
    private val binding get() = _binding!!

    private val contacts = mutableListOf<User>()
    private lateinit var adapter: ContactsListAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentContactsListBinding.bind(view)

        adapter = ContactsListAdapter(
            items = contacts,
            onChat = { user -> startPrivateChat(user) },
            onRemove = { user -> removeContact(user) }
        )

        binding.recyclerContacts.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerContacts.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { loadContacts() }

        PresenceRepository.onlineIds.observe(viewLifecycleOwner) { online ->
            adapter.setOnlineIds(online)
        }

        loadContacts()
    }

    private fun loadContacts() {
        binding.swipeRefresh.isRefreshing = true
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val list = ApiProvider.api(requireContext()).listContacts()
                contacts.clear()
                contacts.addAll(list)
                adapter.notifyDataSetChanged()

                binding.tvEmpty.visibility = if (contacts.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Не удалось загрузить контакты: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun startPrivateChat(other: User) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val chat = ApiProvider.api(requireContext()).createOrGetPrivateChat(CreateChatRequest(otherUserId = other.id))
                val i = Intent(requireContext(), ChatActivity::class.java)
                i.putExtra("chatId", chat.id)
                i.putExtra("title", other.username)
                i.putExtra("chatType", chat.type)
                startActivity(i)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Не удалось создать чат: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun removeContact(user: User) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                ApiProvider.api(requireContext()).removeContact(user.id)
                Toast.makeText(requireContext(), "Удалено из контактов", Toast.LENGTH_SHORT).show()
                loadContacts()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Не удалось удалить: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
