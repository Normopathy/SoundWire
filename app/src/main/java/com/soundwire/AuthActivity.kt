package com.soundwire

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.soundwire.databinding.ActivityAuthBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging

class AuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d("AuthActivity", "Активность запущена")

        // Проверяем, авторизован ли пользователь
        if (Firebase.auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        binding.btnSignUp.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass.length < 6) {
                Toast.makeText(this, "Пароль должен быть не менее 6 символов", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d("AuthActivity", "Попытка регистрации: $email")

            Firebase.auth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener { task ->
                    Log.d("AuthActivity", "Регистрация завершена, успех: ${task.isSuccessful}")
                    if (task.isSuccessful) {
                        val user = Firebase.auth.currentUser
                        if (user != null) {
                            // Создаем профиль пользователя в Firestore
                            createUserProfile(user.uid, email)

                            // Обновляем имя пользователя (email без домена)
                            val displayName = email.substringBefore("@")
                            user.updateProfile(
                                com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                    .setDisplayName(displayName)
                                    .build()
                            )
                        }
                        Log.d("AuthActivity", "Переход в MainActivity...")
                        runOnUiThread {
                            try {
                                startActivity(Intent(this@AuthActivity, MainActivity::class.java))
                                finish()
                            } catch (e: Exception) {
                                Log.e("AuthActivity", "Ошибка перехода: ${e.message}", e)
                                Toast.makeText(this@AuthActivity, "Ошибка перехода", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        val error = task.exception?.message ?: "Неизвестная ошибка"
                        Log.e("AuthActivity", "Ошибка регистрации: $error")
                        Toast.makeText(this, "Ошибка: $error", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("AuthActivity", "Критическая ошибка регистрации", e)
                }
        }

        binding.btnSignIn.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d("AuthActivity", "Попытка входа: $email")

            Firebase.auth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener { task ->
                    Log.d("AuthActivity", "Вход завершен, успех: ${task.isSuccessful}")
                    if (task.isSuccessful) {
                        val user = Firebase.auth.currentUser
                        if (user != null) {
                            // Проверяем и создаем профиль если его нет
                            checkAndCreateUserProfile(user.uid, email)
                        }
                        Log.d("AuthActivity", "Переход в MainActivity...")
                        runOnUiThread {
                            try {
                                startActivity(Intent(this@AuthActivity, MainActivity::class.java))
                                finish()
                            } catch (e: Exception) {
                                Log.e("AuthActivity", "Ошибка перехода: ${e.message}", e)
                                Toast.makeText(this@AuthActivity, "Ошибка перехода", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        val error = task.exception?.message ?: "Неизвестная ошибка"
                        Log.e("AuthActivity", "Ошибка входа: $error")
                        Toast.makeText(this, "Ошибка: $error", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("AuthActivity", "Критическая ошибка входа", e)
                }
        }
    }

    private fun createUserProfile(uid: String, email: String) {
        val displayName = email.substringBefore("@")
        val user = AppUser(
            uid = uid,
            email = email,
            displayName = displayName,
            status = "В сети",
            lastSeen = com.google.firebase.Timestamp.now()
        )

        db.collection("users").document(uid).set(user)
            .addOnSuccessListener {
                Log.d("AuthActivity", "Профиль пользователя создан в Firestore")
                // Получаем FCM токен
                getFCMToken(uid)
            }
            .addOnFailureListener { e ->
                Log.e("AuthActivity", "Ошибка при создании профиля в Firestore", e)
            }
    }

    private fun checkAndCreateUserProfile(uid: String, email: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Обновляем статус и время последней активности
                    document.reference.update(
                        "status", "В сети",
                        "lastSeen", com.google.firebase.Timestamp.now()
                    )
                    // Получаем FCM токен
                    getFCMToken(uid)
                } else {
                    createUserProfile(uid, email)
                }
            }
            .addOnFailureListener { e ->
                Log.e("AuthActivity", "Ошибка при проверке профиля в Firestore", e)
            }
    }

    private fun getFCMToken(uid: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                db.collection("users").document(uid).update("fcmToken", token)
                    .addOnSuccessListener {
                        Log.d("AuthActivity", "FCM токен сохранен")
                    }
            }
        }
    }
}