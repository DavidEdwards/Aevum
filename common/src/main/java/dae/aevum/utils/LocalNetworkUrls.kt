package dae.aevum.utils

import java.net.URI

/**
 * Helpers for Android 17+ [ACCESS_LOCAL_NETWORK] — LAN destinations need a runtime grant
 * when the app targets SDK 37+.
 */
object LocalNetworkUrls {
    /** API 37 = Android 17, where local-network permission is enforced for targetSdk 37+. */
    const val ACCESS_LOCAL_NETWORK_API = 37

    const val ACCESS_LOCAL_NETWORK_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"

    fun looksLikeLocalNetworkUrl(url: String): Boolean {
        val host =
            runCatching {
                val withScheme = if ("://" in url) url else "http://$url"
                URI(withScheme).host
            }.getOrNull()
                ?.lowercase()
                ?.trim()
                .orEmpty()
        if (host.isEmpty()) return false
        if (host == "localhost" || host == "127.0.0.1" || host == "::1" || host.endsWith(".local")) {
            return true
        }
        // IPv4 private ranges: 10/8, 172.16–31/12, 192.168/16, link-local 169.254/16
        val parts = host.split('.')
        if (parts.size == 4 && parts.all { it.toIntOrNull() != null }) {
            val a = parts[0].toInt()
            val b = parts[1].toInt()
            return when (a) {
                10 -> true
                172 -> b in 16..31
                192 -> b == 168
                169 -> b == 254
                else -> false
            }
        }
        return false
    }
}
