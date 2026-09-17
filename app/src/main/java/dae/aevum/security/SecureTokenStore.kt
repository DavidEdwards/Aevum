@file:Suppress("DEPRECATION")

package dae.aevum.security

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dae.aevum.domain.repositories.TokenStore
import timber.log.Timber

/**
 * Keystore-backed storage for Jira API tokens. Room keeps only non-secret user metadata.
 *
 * Uses Jetpack Security Crypto (deprecated but still the supported Keystore-backed prefs API
 * until a replacement ships). Suppressions keep -Werror builds green.
 */
class SecureTokenStore(
    context: Context,
) : TokenStore {
    private val prefs: SharedPreferences =
        try {
            val masterKey =
                MasterKey
                    .Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (e: Exception) {
            Timber.e(e, "Falling back to private SharedPreferences for tokens")
            context.getSharedPreferences(PREFS_NAME_FALLBACK, Context.MODE_PRIVATE)
        }

    override fun getToken(userId: Int): String? = prefs.getString(key(userId), null)

    override fun setToken(
        userId: Int,
        token: String,
    ) {
        prefs.edit { putString(key(userId), token) }
    }

    override fun removeToken(userId: Int) {
        prefs.edit { remove(key(userId)) }
    }

    private fun key(userId: Int) = "token_$userId"

    companion object {
        private const val PREFS_NAME = "aevum_secure_tokens"
        private const val PREFS_NAME_FALLBACK = "aevum_secure_tokens_fallback"
    }
}
