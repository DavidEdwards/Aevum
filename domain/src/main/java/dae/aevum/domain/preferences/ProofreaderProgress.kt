package dae.aevum.domain.preferences

import androidx.annotation.StringRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class ProofreaderLogLine(
    @param:StringRes val messageRes: Int,
    val args: List<String> = emptyList(),
)

/**
 * Live progress lines for the proofreader spinner UI.
 */
@Singleton
class ProofreaderProgress
    @Inject
    constructor() {
        private val _lines = MutableStateFlow<List<ProofreaderLogLine>>(emptyList())
        val lines: StateFlow<List<ProofreaderLogLine>> = _lines.asStateFlow()

        fun clear() {
            _lines.value = emptyList()
        }

        fun log(
            @StringRes messageRes: Int,
            vararg args: Any,
        ) {
            _lines.update { current ->
                current +
                    ProofreaderLogLine(
                        messageRes = messageRes,
                        args = args.map { it.toString() },
                    )
            }
        }
    }
