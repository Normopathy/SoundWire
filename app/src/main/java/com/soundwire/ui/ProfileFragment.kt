package com.soundwire.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.soundwire.*
import com.soundwire.databinding.FragmentProfileBinding
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var session: SessionManager

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) uploadAvatar(uri)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        session = SessionManager(requireContext())

        val cached = session.cachedUser()
        if (cached != null) {
            binding.tvEmail.text = cached.email
            binding.etUsername.setText(cached.username)
            binding.etStatus.setText(cached.status ?: "")
            if (!cached.avatarUrl.isNullOrBlank()) {
                Glide.with(this).load(cached.avatarUrl).circleCrop().into(binding.ivAvatar)
            }
        } else {
            binding.tvEmail.text = ""
        }

        binding.etServerUrl.setText(ServerConfig.getBaseUrl(requireContext()))

        binding.btnChangeAvatar.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.btnSave.setOnClickListener {
            saveProfileAndSettings()
        }

        binding.btnLogout.setOnClickListener {
            session.clear()
            SocketManager.disconnect()
            startActivity(Intent(requireContext(), AuthActivity::class.java))
            requireActivity().finish()
        }

        // Подтянем актуальные данные с сервера
        loadMe()
    }

    private fun loadMe() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val me = ApiProvider.api(requireContext()).me()
                session.updateCachedUser(me)

                binding.tvEmail.text = me.email
                binding.etUsername.setText(me.username)
                binding.etStatus.setText(me.status ?: "")
                if (!me.avatarUrl.isNullOrBlank()) {
                    Glide.with(this@ProfileFragment).load(me.avatarUrl).circleCrop().into(binding.ivAvatar)
                }
            } catch (_: Exception) {
                // Не критично
            }
        }
    }

    private fun saveProfileAndSettings() {
        val newServer = binding.etServerUrl.text?.toString()?.trim().orEmpty()
        if (newServer.isNotBlank()) {
            val old = ServerConfig.getBaseUrl(requireContext())
            if (old != newServer && old != (newServer + "/")) {
                ServerConfig.setBaseUrl(requireContext(), newServer)
                // Сокет и ретрофит пересоздадутся автоматически, но сокет лучше отключить.
                SocketManager.disconnect()
                Toast.makeText(requireContext(), "Адрес сервера сохранён", Toast.LENGTH_SHORT).show()
            }
        }

        val username = binding.etUsername.text?.toString()?.trim().orEmpty()
        val status = binding.etStatus.text?.toString()?.trim().orEmpty()

        if (username.isBlank()) {
            Toast.makeText(requireContext(), "Имя не может быть пустым", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val me = ApiProvider.api(requireContext()).updateMe(UpdateProfileRequest(username, status))
                session.updateCachedUser(me)
                Toast.makeText(requireContext(), "Профиль сохранён", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка сохранения профиля: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun uploadAvatar(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val part = createMultipartFromUri(uri)
                val resp = ApiProvider.api(requireContext()).uploadAvatar(part)

                // Обновим профиль с сервера
                loadMe()
                Toast.makeText(requireContext(), "Аватар обновлён", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка загрузки аватара: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun createMultipartFromUri(uri: Uri): MultipartBody.Part {
        val contentResolver = requireContext().contentResolver
        val mime = contentResolver.getType(uri) ?: "image/*"

        // Копируем в temp файл, чтобы OkHttp мог прочитать
        val tempFile = File(requireContext().cacheDir, "avatar_${System.currentTimeMillis()}.tmp")
        contentResolver.openInputStream(uri).use { input ->
            if (input == null) throw IllegalStateException("Cannot open input stream")
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }

        val body = tempFile.asRequestBody(mime.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("avatar", tempFile.name, body)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
