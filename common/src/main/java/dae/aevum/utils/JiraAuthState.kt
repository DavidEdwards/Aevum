package dae.aevum.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object JiraAuthState {
    private val _tokenExpired = MutableStateFlow(false)
    val tokenExpired: StateFlow<Boolean> = _tokenExpired.asStateFlow()

    fun markTokenExpired() {
        _tokenExpired.value = true
    }

    fun clearTokenExpired() {
        _tokenExpired.value = false
    }
}
