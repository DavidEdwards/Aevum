package dae.aevum.domain.preferences

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Signals that a LAN connection failed (typically Android 17+ without
 * ACCESS_LOCAL_NETWORK). UI collects [accessRequired] and requests permission
 * with user-visible context.
 */
@Singleton
class LocalNetworkPermissionGate
    @Inject
    constructor() {
        private val _accessRequired =
            MutableSharedFlow<Unit>(
                extraBufferCapacity = 1,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )

        val accessRequired: SharedFlow<Unit> = _accessRequired.asSharedFlow()

        fun notifyAccessRequired() {
            _accessRequired.tryEmit(Unit)
        }
    }
