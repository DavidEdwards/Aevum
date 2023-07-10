package dae.aevum.network

import android.util.Base64
import dae.aevum.domain.repositories.UserRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import java.net.HttpURLConnection

class AuthInterceptor(
    private val userRepository: UserRepository
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val activeUser = runBlocking {
            userRepository.getActiveUser()
        } ?: return run {
            Response.Builder()
                .code(HttpURLConnection.HTTP_UNAUTHORIZED)
                .message("User required")
                .build()
        }

        val token = Base64.encodeToString(
            "${activeUser.user}:${activeUser.token}".toByteArray(),
            Base64.NO_WRAP
        )
        val url = chain.request().url.toString()
            .replace("https://baseurl", activeUser.instanceUrl.removeSuffix("/"))

        val newRequest = chain.request().newBuilder()
            .addHeader(
                "Authorization",
                "Basic $token"
            )
            .url(url)
            .build()

        return chain.proceed(newRequest)
    }
}