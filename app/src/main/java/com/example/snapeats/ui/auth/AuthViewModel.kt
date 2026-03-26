package com.example.snapeats.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.snapeats.data.local.dao.AppUserDao
import com.example.snapeats.data.local.entity.AppUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.MessageDigest

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successUserId: Int? = null
)

class AuthViewModel(
    private val appUserDao: AppUserDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Email and password are required.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val hash = sha256(password)
                val user = appUserDao.login(email.trim().lowercase(), hash)
                if (user != null) {
                    _uiState.update { it.copy(isLoading = false, successUserId = user.id) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Invalid email or password.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Login failed: ${e.localizedMessage}") }
            }
        }
    }

    fun register(username: String, email: String, password: String) {
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "All fields are required.") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(error = "Password must be at least 6 characters.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val emailNorm = email.trim().lowercase()
                val existing = appUserDao.findByEmail(emailNorm)
                if (existing != null) {
                    _uiState.update { it.copy(isLoading = false, error = "Email already registered.") }
                    return@launch
                }
                val hash = sha256(password)
                val appUser = AppUser(
                    username = username.trim(),
                    email = emailNorm,
                    passwordHash = hash
                )
                val newId = appUserDao.insertUser(appUser).toInt()
                _uiState.update { it.copy(isLoading = false, successUserId = newId) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Registration failed: ${e.localizedMessage}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    companion object {
        fun factory(appUserDao: AppUserDao): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AuthViewModel(appUserDao) as T
                }
            }

        private fun sha256(input: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
