package dae.aevum.domain.repositories

import dae.aevum.utils.ActiveTimerSnapshot

/** Pushes the active timer state to paired Wear OS devices via the Data Layer API. */
interface WearActiveTimerSync {
    suspend fun sync(active: ActiveTimerSnapshot?)
}
