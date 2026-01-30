package dae.aevum.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dae.aevum.domain.repositories.JiraRepository
import dae.aevum.domain.repositories.UserRepository
import dae.aevum.ui.models.UserUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed class ActiveUserState {
    data object Loading : ActiveUserState()

    data class Loaded(
        val user: UserUiModel?,
    ) : ActiveUserState()
}

@HiltViewModel
class UserViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val userRepository: UserRepository,
        private val jiraRepository: JiraRepository,
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

        val users =
            userRepository
                .flowUsers()
                .map { list ->
                    list.map { UserUiModel.fromEntity(entity = it) }
                }

        val activeUser =
            userRepository
                .flowActiveUser()
                .map { entity -> entity?.let { UserUiModel.fromEntity(entity = it) } }

        val activeUserState =
            userRepository
                .flowActiveUser()
                .map { entity ->
                    ActiveUserState.Loaded(entity?.let { UserUiModel.fromEntity(entity = it) })
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = ActiveUserState.Loading,
                )

        var loading by mutableStateOf(false)

        fun addUser(
            instanceUrl: String,
            userName: String,
            token: String,
            after: (Boolean) -> Unit = {},
        ) {
            viewModelScope.launch {
                loading = true
                val url =
                    if (instanceUrl.contains("://")) {
                        instanceUrl
                    } else {
                        "https://$instanceUrl"
                    }

                val tested =
                    try {
                        userRepository.unselectAllUsers()
                        val id =
                            userRepository.addUser(
                                instanceUrl = url,
                                user = userName,
                                jiraAccountId = "",
                                token = token,
                            )

                        val jiraAccountId = jiraRepository.getJiraAccountId(userName)
                        Timber.v("Jira account id: $jiraAccountId")
                        val correctPermissions = jiraRepository.testUser(url, userName, token)
                        Timber.v("Correct permissions: $correctPermissions")

                        userRepository.removeUser(id)

                        (correctPermissions && jiraAccountId.isNotBlank()).let { correctInformation ->
                            if (correctInformation) {
                                Timber.v("Adding user")
                                userRepository.unselectAllUsers()
                                userRepository.addUser(
                                    instanceUrl = url,
                                    user = userName,
                                    jiraAccountId = jiraAccountId,
                                    token = token,
                                )
                            }
                            correctInformation
                        }
                    } catch (e: IllegalArgumentException) {
                        Timber.e(e)
                        false
                    }

                after(tested)
                loading = false
            }
        }

        fun removeUser(
            userId: Int,
            after: () -> Unit = {},
        ) {
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
