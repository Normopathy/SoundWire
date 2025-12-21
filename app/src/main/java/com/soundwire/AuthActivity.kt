package com.soundwire

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.soundwire.databinding.ActivityAuthBinding
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)

        // Если уже залогинен — сразу в главное окно
        if (session.isLoggedIn()) {
            startMain()
            return
        }

        binding.btnSignIn.setOnClickListener { doLogin() }
        binding.btnSignUp.setOnClickListener { doRegister() }
    }

    private fun doLogin() {
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString()?.trim().orEmpty()

        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(this, "Введите email и пароль", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val resp = ApiProvider.api(this@AuthActivity)
                    .login(LoginRequest(email = email, password = password))

                session.saveSession(resp.token, resp.user)
                Toast.makeText(this@AuthActivity, "Вход выполнен", Toast.LENGTH_SHORT).show()
                startMain()
            } catch (e: Exception) {
                Toast.makeText(this@AuthActivity, "Ошибка входа: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun doRegister() {
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString()?.trim().orEmpty()
        val username = binding.etUsername.text?.toString()?.trim().orEmpty()
            .ifBlank { email.substringBefore("@") }

        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(this, "Введите email и пароль", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Пароль должен быть не менее 6 символов", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                ApiProvider.api(this@AuthActivity)
                    .register(RegisterRequest(email = email, password = password, username = username))

                // Сразу логинимся
                val resp = ApiProvider.api(this@AuthActivity)
                    .login(LoginRequest(email = email, password = password))

                session.saveSession(resp.token, resp.user)
                Toast.makeText(this@AuthActivity, "Регистрация успешна", Toast.LENGTH_SHORT).show()
                startMain()
            } catch (e: Exception) {
                Toast.makeText(this@AuthActivity, "Ошибка регистрации: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
