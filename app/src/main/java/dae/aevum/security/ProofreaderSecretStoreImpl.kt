@file:Suppress("DEPRECATION")

package dae.aevum.security

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dae.aevum.domain.preferences.ProofreaderSecretStore
import timber.log.Timber

/**
 * Keystore-backed storage for the OpenAI-compatible proofreader API key.
 */
class ProofreaderSecretStoreImpl(
    context: Context,
) : ProofreaderSecretStore {
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
            Timber.e(e, "Falling back to private SharedPreferences for proofreader API key")
            context.getSharedPreferences(PREFS_NAME_FALLBACK, Context.MODE_PRIVATE)
        }

    override fun getApiKey(): String? = prefs.getString(KEY_API_KEY, null)

    override fun setApiKey(apiKey: String) {
        prefs.edit { putString(KEY_API_KEY, apiKey) }
    }

    override fun clearApiKey() {
        prefs.edit { remove(KEY_API_KEY) }
    }

    companion object {
        private const val PREFS_NAME = "aevum_proofreader_secrets"
        private const val PREFS_NAME_FALLBACK = "aevum_proofreader_secrets_fallback"
        private const val KEY_API_KEY = "openai_compatible_api_key"
    }
}
