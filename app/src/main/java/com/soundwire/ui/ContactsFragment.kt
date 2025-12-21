package com.soundwire.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.soundwire.PresenceRepository
import com.soundwire.R
import com.soundwire.databinding.FragmentContactsTabsBinding

class ContactsFragment : Fragment(R.layout.fragment_contacts_tabs) {

    private var _binding: FragmentContactsTabsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentContactsTabsBinding.bind(view)

        // Presence на вкладке контактов точно пригодится
        PresenceRepository.init(requireContext())

        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 3
            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> ContactsListFragment()
                    1 -> RequestsFragment()
                    else -> SearchUsersFragment()
                }
            }
        }

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Контакты"
                1 -> "Запросы"
                else -> "Поиск"
            }
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
