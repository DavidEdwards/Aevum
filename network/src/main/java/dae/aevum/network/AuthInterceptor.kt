package dae.aevum.network

import android.util.Base64
import dae.aevum.database.entities.UserEntity
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.net.HttpURLConnection

class AuthInterceptor(
    private val getActiveUser: suspend () -> UserEntity?,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val newRequest =
            try {
                val activeUser =
                    runBlocking {
                        getActiveUser()
                    } ?: return run {
                        Timber.w("No active user found for request")
                        Response
                            .Builder()
                            .code(HttpURLConnection.HTTP_UNAUTHORIZED)
                            .message("User required")
                            .request(chain.request())
                            .protocol(okhttp3.Protocol.HTTP_1_1)
                            .build()
                    }

                val token =
                    Base64.encodeToString(
                        "${activeUser.user}:${activeUser.token}".toByteArray(),
                        Base64.NO_WRAP,
                    )
                val url =
                    chain
                        .request()
                        .url
                        .toString()
                        .replace("https://baseurl", activeUser.instanceUrl.removeSuffix("/"))

                chain
                    .request()
                    .newBuilder()
                    .addHeader(
                        "Authorization",
                        "Basic $token",
                    ).url(url)
                    .build()
            } catch (e: Exception) {
                Timber.e(e, "Error in AuthInterceptor")
                chain.request()
            }

        return chain.proceed(newRequest)
    }
}
