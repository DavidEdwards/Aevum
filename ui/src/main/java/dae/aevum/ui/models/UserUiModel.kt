package dae.aevum.ui.models

import dae.aevum.database.entities.UserEntity

data class UserUiModel(
    val id: Int,
    val user: String,
    val active: Boolean
) {
    companion object {
        fun fromEntity(entity: UserEntity): UserUiModel {
            return UserUiModel(
                id = entity.id,
                user = entity.user,
                active = entity.active
            )
        }
    }
}