package dae.aevum.ui.viewmodels

import androidx.lifecycle.ViewModel
import dae.aevum.domain.preferences.LocalNetworkPermissionGate
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LocalNetworkPermissionViewModel
    @Inject
    constructor(
        gate: LocalNetworkPermissionGate,
    ) : ViewModel() {
        val accessRequired = gate.accessRequired
    }
