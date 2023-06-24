package dae.aevum.network

import androidx.annotation.Keep
import dae.aevum.network.models.SearchRequestRetro
import dae.aevum.network.models.SearchRetro
import dae.aevum.network.models.WorklogAddRetro
import dae.aevum.network.models.WorklogListRetro
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

@Keep
interface JiraService {
    @POST("search")
    @Headers("Content-Type: application/json")
    fun searchIssues(@Body body: SearchRequestRetro): Call<SearchRetro?>

    @GET("issue/{issueId}/worklog")
    @Headers("Content-Type: application/json")
    fun listWorklogsFor(@Path("issueId") issueId: String): Call<WorklogListRetro?>

    @POST("issue/{issueId}/worklog")
    @Headers("Content-Type: application/json")
    fun addWorklog(
        @Path("issueId") issueId: String,
        @Body body: WorklogAddRetro
    ): Call<ResponseBody>
}