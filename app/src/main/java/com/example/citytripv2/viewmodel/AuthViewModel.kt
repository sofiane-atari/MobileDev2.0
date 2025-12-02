package com.example.citytripv2.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun login(email: String, pass: String) {
        val cleanEmail = email.trim()
        val cleanPass = pass.trim()

        if (cleanEmail.isEmpty() || cleanPass.isEmpty()) {
            _authState.value = AuthState.Error("Vul alle velden in")
            return
        }

        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(cleanEmail, cleanPass)
            .addOnSuccessListener {
                _authState.value = AuthState.Success
            }
            .addOnFailureListener { exception ->
                _authState.value = AuthState.Error(exception.message ?: "Login mislukt")
            }
    }

    fun register(email: String, pass: String) {
        val cleanEmail = email.trim()
        val cleanPass = pass.trim()

        // Validatie
        if (cleanEmail.isEmpty() || cleanPass.isEmpty()) {
            _authState.value = AuthState.Error("Vul alsjeblieft alle velden in.")
            return
        }
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            _authState.value = AuthState.Error("E-mail moet een @ en een . bevatten")
            return
        }
        if (cleanPass.length < 6) {
            _authState.value = AuthState.Error("Wachtwoord moet minimaal 6 tekens lang zijn.")
            return
        }

        _authState.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(cleanEmail, cleanPass)
            .addOnSuccessListener {
                _authState.value = AuthState.Success
            }
            .addOnFailureListener { exception ->
                val errorMessage = when (exception) {
                    is FirebaseAuthWeakPasswordException -> "Het wachtwoord is te zwak."
                    is FirebaseAuthInvalidCredentialsException -> "Het e-mailadres is ongeldig."
                    is FirebaseAuthUserCollisionException -> "Dit e-mailadres is al in gebruik."
                    else -> exception.localizedMessage ?: "Registratie mislukt."
                }
                _authState.value = AuthState.Error(errorMessage)
            }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}


sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}