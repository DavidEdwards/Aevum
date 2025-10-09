package dae.aevum.network

import dae.aevum.network.models.SearchRetro
import dae.aevum.network.models.UserResponseRetro
import dae.aevum.network.models.WorklogAddRetro
import dae.aevum.network.models.WorklogListRetro
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface JiraService {
    @GET("user/picker")
    @Headers("Content-Type: application/json", "Accept: application/json")
    fun getUser(@Query("query") query: String): Call<UserResponseRetro?>

    @GET("search/jql")
    @Headers("Content-Type: application/json", "Accept: application/json")
    fun searchIssues(
        @Query("jql") jql: String,
        @Query("fields") fields: String? = null,
        @Query("maxResults") maxResults: Int = 100,
        @Query("startAt") startAt: Int = 0
    ): Call<SearchRetro?>

    @GET("issue/{issueId}/worklog")
    @Headers("Content-Type: application/json", "Accept: application/json")
    fun listWorklogsFor(
        @Path("issueId") issueId: String,
        @Query("startedAfter") startedAfter: Long
    ): Call<WorklogListRetro?>

    @POST("issue/{issueId}/worklog")
    @Headers("Content-Type: application/json", "Accept: application/json")
    fun addWorklog(
        @Path("issueId") issueId: String,
        @Body body: WorklogAddRetro
    ): Call<ResponseBody>

    @GET("search/jql")
    @Headers("Content-Type: application/json", "Accept: application/json")
    fun testUser(
        @Query("jql") jql: String = "worklogAuthor = currentUser()",
        @Query("maxResults") maxResults: Int = 1,
        @Header("Authorization") token: String
    ): Call<ResponseBody>

    companion object {
        const val ONE_WEEK_IN_MILLIS = 604800000L
        const val ONE_MONTH_IN_MILLIS = ONE_WEEK_IN_MILLIS * 4
    }
}