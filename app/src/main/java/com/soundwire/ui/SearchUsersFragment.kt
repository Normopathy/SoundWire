package com.soundwire.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.soundwire.*
import com.soundwire.databinding.FragmentUserSearchBinding
import kotlinx.coroutines.launch

class SearchUsersFragment : Fragment(R.layout.fragment_user_search) {

    private var _binding: FragmentUserSearchBinding? = null
    private val binding get() = _binding!!

    private val users = mutableListOf<User>()
    private lateinit var adapter: UserSearchAdapter

    private var contactIds: Set<Int> = emptySet()
    private var pendingOutgoing: Set<Int> = emptySet()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentUserSearchBinding.bind(view)

        adapter = UserSearchAdapter(
            users = users,
            onAdd = { user -> sendRequest(user) },
            onChat = { user -> startPrivateChat(user) }
        )

        binding.recyclerUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerUsers.adapter = adapter

        binding.btnSearch.setOnClickListener { search() }
        binding.swipeRefresh.setOnRefreshListener { search() }

        PresenceRepository.onlineIds.observe(viewLifecycleOwner) { online ->
            adapter.setOnlineIds(online)
        }

        refreshRelations()
    }

    override fun onResume() {
        super.onResume()
        refreshRelations()
    }

    private fun refreshRelations() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val api = ApiProvider.api(requireContext())
                val contacts = api.listContacts()
                val outgoing = api.outgoingRequests()

                contactIds = contacts.map { it.id }.toSet()
                pendingOutgoing = outgoing.mapNotNull { it.toUser?.id }.toSet()

                adapter.setRelations(contactIds, pendingOutgoing)
            } catch (_: Exception) {
                // не критично
            }
        }
    }

    private fun search() {
        val q = binding.etSearch.text?.toString()?.trim().orEmpty()
        if (q.isBlank()) {
            adapter.setData(emptyList())
            binding.tvEmpty.visibility = View.VISIBLE
            binding.tvEmpty.text = "Введите запрос"
            return
        }

        binding.swipeRefresh.isRefreshing = true
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val list = ApiProvider.api(requireContext()).searchUsers(q)
                adapter.setData(list)
                binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                binding.tvEmpty.text = "Ничего не найдено"
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка поиска: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun sendRequest(user: User) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resp = ApiProvider.api(requireContext()).sendContactRequest(SendContactRequest(toUserId = user.id))
                when {
                    resp.already == true -> Toast.makeText(requireContext(), "Уже в контактах", Toast.LENGTH_SHORT).show()
                    resp.accepted == true -> Toast.makeText(requireContext(), "Запрос принят автоматически (у вас был встречный)", Toast.LENGTH_SHORT).show()
                    else -> Toast.makeText(requireContext(), "Запрос отправлен", Toast.LENGTH_SHORT).show()
                }
                refreshRelations()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Не удалось отправить запрос: ${e.message}", Toast.LENGTH_LONG).show()
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
                Toast.makeText(requireContext(), "Не удалось открыть чат: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
