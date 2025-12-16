package com.soundwire

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.soundwire.databinding.ActivityContactsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ContactsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityContactsBinding
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private lateinit var adapter: ContactsAdapter
    private val contactsList = mutableListOf<AppUser>()
    private val pendingRequests = mutableListOf<ContactRequest>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerContacts.layoutManager = LinearLayoutManager(this)
        adapter = ContactsAdapter(contactsList, pendingRequests) { userId, action ->
            when (action) {
                "add" -> sendContactRequest(userId)
                "accept" -> acceptContactRequest(userId)
                "reject" -> rejectContactRequest(userId)
            }
        }
        binding.recyclerContacts.adapter = adapter

        binding.btnSearch.setOnClickListener {
            searchUser()
        }

        loadContacts()
        loadPendingRequests()
    }

    private fun loadContacts() {
        val currentUserId = auth.currentUser?.uid ?: return

        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener { document ->
                val user = document.toObject(AppUser::class.java)
                user?.contacts?.let { contactIds ->
                    if (contactIds.isNotEmpty()) {
                        db.collection("users")
                            .whereIn("uid", contactIds)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                contactsList.clear()
                                for (doc in querySnapshot.documents) {
                                    doc.toObject(AppUser::class.java)?.let { contact ->
                                        contactsList.add(contact)
                                    }
                                }
                                adapter.notifyDataSetChanged()
                            }
                    }
                }
            }
    }

    private fun loadPendingRequests() {
        val currentUserId = auth.currentUser?.uid ?: return

        db.collection("contact_requests")
            .whereEqualTo("toUserId", currentUserId)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { querySnapshot ->
                pendingRequests.clear()
                for (doc in querySnapshot.documents) {
                    doc.toObject(ContactRequest::class.java)?.let { request ->
                        pendingRequests.add(request)
                    }
                }
                adapter.notifyDataSetChanged()
            }
    }

    private fun searchUser() {
        val query = binding.etSearch.text.toString().trim()
        if (query.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Ищем по email
                val emailQuery = db.collection("users")
                    .whereEqualTo("email", query)
                    .get()
                    .await()

                // Ищем по имени
                val nameQuery = db.collection("users")
                    .whereGreaterThanOrEqualTo("displayName", query)
                    .whereLessThanOrEqualTo("displayName", query + "\uf8ff")
                    .get()
                    .await()

                val results = mutableListOf<AppUser>()
                val currentUserId = auth.currentUser?.uid

                for (doc in emailQuery.documents) {
                    val user = doc.toObject(AppUser::class.java)
                    if (user != null && user.uid != currentUserId) {
                        results.add(user)
                    }
                }

                for (doc in nameQuery.documents) {
                    val user = doc.toObject(AppUser::class.java)
                    if (user != null && user.uid != currentUserId &&
                        !results.any { it.uid == user.uid }) {
                        results.add(user)
                    }
                }

                runOnUiThread {
                    if (results.isNotEmpty()) {
                        showSearchResults(results)
                    } else {
                        Toast.makeText(this@ContactsActivity, "Пользователь не найден", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@ContactsActivity, "Ошибка поиска", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showSearchResults(users: List<AppUser>) {
        // Показать диалог с результатами поиска
        val currentUserId = auth.currentUser?.uid ?: return

        users.forEach { user ->
            // Проверяем, уже ли это контакт
            val isContact = contactsList.any { it.uid == user.uid }
            val hasPendingRequest = pendingRequests.any { it.fromUserId == user.uid }

            // Показать пользователя с кнопкой "Добавить" или "Запрос отправлен"
        }
    }

    private fun sendContactRequest(toUserId: String) {
        val fromUserId = auth.currentUser?.uid ?: return
        val requestId = "$fromUserId-$toUserId"

        val request = ContactRequest(
            requestId = requestId,
            fromUserId = fromUserId,
            toUserId = toUserId,
            message = "Хочу добавить вас в контакты"
        )

        db.collection("contact_requests").document(requestId).set(request)
            .addOnSuccessListener {
                Toast.makeText(this, "Запрос отправлен", Toast.LENGTH_SHORT).show()
                // Отправить push-уведомление
                sendNotification(toUserId, "Новый запрос в контакты")
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка отправки запроса", Toast.LENGTH_SHORT).show()
            }
    }

    private fun acceptContactRequest(fromUserId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val requestId = "$fromUserId-$currentUserId"

        // Обновляем статус запроса
        db.collection("contact_requests").document(requestId).update("status", "accepted")
            .addOnSuccessListener {
                // Добавляем друг друга в контакты
                addToContacts(currentUserId, fromUserId)
                addToContacts(fromUserId, currentUserId)

                Toast.makeText(this, "Контакт добавлен", Toast.LENGTH_SHORT).show()
                loadContacts()
                loadPendingRequests()
            }
    }

    private fun rejectContactRequest(fromUserId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val requestId = "$fromUserId-$currentUserId"

        db.collection("contact_requests").document(requestId).update("status", "rejected")
            .addOnSuccessListener {
                Toast.makeText(this, "Запрос отклонен", Toast.LENGTH_SHORT).show()
                loadPendingRequests()
            }
    }

    private fun addToContacts(userId: String, contactId: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val user = document.toObject(AppUser::class.java)
                val currentContacts = user?.contacts?.toMutableList() ?: mutableListOf()
                if (!currentContacts.contains(contactId)) {
                    currentContacts.add(contactId)
                    document.reference.update("contacts", currentContacts)
                }
            }
    }

    private fun sendNotification(toUserId: String, message: String) {
        // Получаем FCM токен пользователя
        db.collection("users").document(toUserId).get()
            .addOnSuccessListener { document ->
                val fcmToken = document.getString("fcmToken")
                fcmToken?.let {
                    // Отправляем уведомление через FCM
                    // Здесь можно использовать Cloud Functions или отправлять напрямую
                }
            }
    }
}