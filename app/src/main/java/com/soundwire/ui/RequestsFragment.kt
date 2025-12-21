package com.soundwire.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.soundwire.ApiProvider
import com.soundwire.R
import com.soundwire.RequestItem
import com.soundwire.RequestsAdapter
import com.soundwire.databinding.FragmentRequestsBinding
import kotlinx.coroutines.launch

class RequestsFragment : Fragment(R.layout.fragment_requests) {

    private var _binding: FragmentRequestsBinding? = null
    private val binding get() = _binding!!

    private val items = mutableListOf<RequestItem>()
    private lateinit var adapter: RequestsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRequestsBinding.bind(view)

        adapter = RequestsAdapter(
            items = items,
            onAccept = { id -> accept(id) },
            onDecline = { id -> decline(id) }
        )

        binding.recyclerRequests.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerRequests.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { load() }

        load()
    }

    private fun load() {
        binding.swipeRefresh.isRefreshing = true
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val api = ApiProvider.api(requireContext())
                val incoming = api.incomingRequests()
                val outgoing = api.outgoingRequests()

                val out = mutableListOf<RequestItem>()
                if (incoming.isNotEmpty()) {
                    out.add(RequestItem.Header("Входящие"))
                    incoming.forEach { out.add(RequestItem.Incoming(it)) }
                }
                if (outgoing.isNotEmpty()) {
                    out.add(RequestItem.Header("Исходящие"))
                    outgoing.forEach { out.add(RequestItem.Outgoing(it)) }
                }

                adapter.setData(out)
                binding.tvEmpty.visibility = if (out.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Не удалось загрузить запросы: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun accept(id: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                ApiProvider.api(requireContext()).acceptRequest(id)
                Toast.makeText(requireContext(), "Запрос принят", Toast.LENGTH_SHORT).show()
                load()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun decline(id: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                ApiProvider.api(requireContext()).declineRequest(id)
                Toast.makeText(requireContext(), "Запрос отклонён", Toast.LENGTH_SHORT).show()
                load()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
