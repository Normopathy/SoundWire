package com.soundwire

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.soundwire.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = Firebase.auth.currentUser
        if (user == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        // Обновляем статус пользователя
        updateUserStatus("В сети")

        binding.btnOpenChats.setOnClickListener {
            startActivity(Intent(this, ChatListActivity::class.java))
        }

        binding.btnOpenMusic.setOnClickListener {
            startActivity(Intent(this, MusicSearchActivity::class.java))
        }

        binding.btnOpenContacts.setOnClickListener {
            startActivity(Intent(this, ContactsActivity::class.java))
        }

        binding.btnOpenProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_profile -> {
                startActivity(Intent(this, ProfileActivity::class.java))
                true
            }
            R.id.menu_contacts -> {
                startActivity(Intent(this, ContactsActivity::class.java))
                true
            }
            R.id.menu_settings -> {
                // Открыть настройки
                Toast.makeText(this, "Настройки в разработке", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateUserStatus(status: String) {
        val user = Firebase.auth.currentUser ?: return
        db.collection("users").document(user.uid).update(
            "status", status,
            "lastSeen", com.google.firebase.Timestamp.now()
        ).addOnFailureListener { e ->
            // Игнорируем ошибки при обновлении статуса
        }
    }

    private fun logout() {
        val user = Firebase.auth.currentUser
        if (user != null) {
            db.collection("users").document(user.uid).update(
                "status", "Не в сети",
                "lastSeen", com.google.firebase.Timestamp.now()
            ).addOnCompleteListener {
                Firebase.auth.signOut()
                startActivity(Intent(this, AuthActivity::class.java))
                finish()
            }
        } else {
            Firebase.auth.signOut()
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        updateUserStatus("В сети")
    }

    override fun onPause() {
        super.onPause()
        updateUserStatus("Не в сети")
    }
}