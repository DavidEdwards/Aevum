package dae.aevum.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dae.aevum.domain.repositories.UserRepository
import dae.aevum.ui.models.UserUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class UserViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository
) : ViewModel() {

    var instanceUrl: String = savedStateHandle[SAVED_INSTANCE_URL] ?: ""
        set(value) {
            field = value
            savedStateHandle[SAVED_INSTANCE_URL] = field
        }
    var user: String = savedStateHandle[SAVED_USER] ?: ""
        set(value) {
            field = value
            savedStateHandle[SAVED_USER] = field
        }
    var token: String = savedStateHandle[SAVED_TOKEN] ?: ""
        set(value) {
            field = value
            savedStateHandle[SAVED_TOKEN] = field
        }

    val users = userRepository.flowUsers()
        .map { list ->
            list.map { UserUiModel.fromEntity(entity = it) }
        }

    val activeUser = userRepository.flowActiveUser()
        .map { entity -> entity?.let { UserUiModel.fromEntity(entity = it) } }

    var loading by mutableStateOf(false)

    fun addUser(
        instanceUrl: String,
        user: String,
        token: String, after: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            loading = true
            val url = if (instanceUrl.contains("://")) {
                instanceUrl
            } else {
                "https://$instanceUrl"
            }
            val tested = userRepository.testUser(url, user, token)
            if (tested) {
                userRepository.unselectAllUsers()
                userRepository.addUser(url, user, token)
            }
            after(tested)
            loading = false
        }
    }

    fun removeUser(userId: Int, after: () -> Unit = {}) {
        viewModelScope.launch {
            userRepository.removeUser(userId)
            after()
        }
    }

    fun selectUser(userId: Int) {
        viewModelScope.launch {
            userRepository.selectUser(userId)
        }
    }

    companion object {
        private const val SAVED_INSTANCE_URL = "instanceUrl"
        private const val SAVED_USER = "user"
        private const val SAVED_TOKEN = "token"
    }

}