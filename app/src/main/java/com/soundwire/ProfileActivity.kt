package com.soundwire

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.soundwire.databinding.ActivityProfileBinding
import java.util.UUID

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val storage = Firebase.storage
    private var selectedImageUri: Uri? = null
    private var currentUser: AppUser? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadUserProfile()

        binding.btnChangeAvatar.setOnClickListener {
            openImagePicker()
        }

        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }

        binding.btnLogout.setOnClickListener {
            logout()
        }
    }

    private fun loadUserProfile() {
        val user = auth.currentUser
        if (user != null) {
            binding.tvEmail.text = "Email: ${user.email}"
            binding.tvUid.text = "UID: ${user.uid}"

            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        currentUser = document.toObject(AppUser::class.java)
                        currentUser?.let { appUser ->
                            binding.etDisplayName.setText(appUser.displayName)
                            binding.etStatus.setText(appUser.status)
                            binding.etPhone.setText(appUser.phoneNumber ?: "")

                            if (!appUser.photoURL.isNullOrEmpty()) {
                                Glide.with(this)
                                    .load(appUser.photoURL)
                                    .circleCrop()
                                    .into(binding.ivAvatar)
                            }
                        }
                    }
                }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data
            binding.ivAvatar.setImageURI(selectedImageUri)
        }
    }

    private fun saveProfile() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Пользователь не аутентифицирован", Toast.LENGTH_SHORT).show()
            return
        }

        val displayName = binding.etDisplayName.text.toString().trim()
        val status = binding.etStatus.text.toString().trim()
        val phoneNumber = binding.etPhone.text.toString().trim()

        if (displayName.isEmpty()) {
            Toast.makeText(this, "Введите отображаемое имя", Toast.LENGTH_SHORT).show()
            return
        }

        val updates = hashMapOf<String, Any>(
            "displayName" to displayName,
            "status" to status,
            "phoneNumber" to phoneNumber
        )

        // Обновляем в Firebase Auth
        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()

        user.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Если выбрано новое изображение, загружаем его
                    if (selectedImageUri != null) {
                        uploadImageToStorage(user.uid) { photoURL ->
                            updates["photoURL"] = photoURL
                            updateFirestoreUser(user.uid, updates)
                        }
                    } else {
                        updateFirestoreUser(user.uid, updates)
                    }
                }
            }
    }

    private fun uploadImageToStorage(uid: String, onSuccess: (String) -> Unit) {
        val storageRef = storage.reference
        val imageRef = storageRef.child("avatars/${uid}/${UUID.randomUUID()}.jpg")

        selectedImageUri?.let { uri ->
            imageRef.putFile(uri)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        onSuccess(downloadUri.toString())
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateFirestoreUser(uid: String, updates: Map<String, Any>) {
        db.collection("users").document(uid).update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Профиль обновлен", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка обновления", Toast.LENGTH_SHORT).show()
            }
    }

    private fun logout() {
        val user = auth.currentUser
        if (user != null) {
            // Обновляем статус перед выходом
            db.collection("users").document(user.uid).update(
                "status", "Не в сети",
                "lastSeen", com.google.firebase.Timestamp.now()
            )
        }

        Firebase.auth.signOut()
        startActivity(Intent(this, AuthActivity::class.java))
        finish()
    }
}