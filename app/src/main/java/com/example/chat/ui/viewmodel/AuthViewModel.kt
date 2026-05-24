package com.example.chat.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chat.data.local.UserManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data object Authenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val userManager = UserManager(application)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isLoggedIn = MutableStateFlow<Boolean?>(null)
    val isLoggedIn: StateFlow<Boolean?> = _isLoggedIn.asStateFlow()

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    init {
        viewModelScope.launch {
            userManager.isLoggedIn.collect { loggedIn ->
                _isLoggedIn.value = loggedIn
            }
        }
        viewModelScope.launch {
            userManager.user.collect { user ->
                _userName.value = user.name
                _userEmail.value = user.email
            }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("请填写邮箱和密码")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // 本地验证：检查 DataStore 中是否有匹配的用户
                val user = userManager.user.first()
                if (user.email == email) {
                    // 简单验证通过（实际项目应验证密码）
                    _authState.value = AuthState.Authenticated
                } else {
                    _authState.value = AuthState.Error("账号不存在，请先注册")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("登录失败: ${e.message}")
            }
        }
    }

    fun register(email: String, name: String, password: String, confirmPassword: String) {
        if (email.isBlank() || name.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("请填写所有字段")
            return
        }
        if (password != confirmPassword) {
            _authState.value = AuthState.Error("两次密码不一致")
            return
        }
        if (password.length < 6) {
            _authState.value = AuthState.Error("密码至少6位")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // 本地注册：保存用户信息到 DataStore
                val token = "local_token_${System.currentTimeMillis()}"
                userManager.saveUser(email, name, token)
                _authState.value = AuthState.Authenticated
            } catch (e: Exception) {
                _authState.value = AuthState.Error("注册失败: ${e.message}")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            userManager.logout()
            _authState.value = AuthState.Idle
        }
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    fun updateUserProfile(name: String, email: String) {
        if (name.isBlank() || email.isBlank()) {
            _authState.value = AuthState.Error("请填写用户名和邮箱")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                userManager.updateUser(email = email.trim(), name = name.trim())
                _authState.value = AuthState.Idle
            } catch (e: Exception) {
                _authState.value = AuthState.Error("保存失败: ${e.message}")
            }
        }
    }
}
