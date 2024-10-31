import okhttp3.*
import java.io.IOException

object NetworkClient {
    private val client = OkHttpClient()

    fun fetchMarkers(url: String, callback: (String?, Exception?) -> Unit) {
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null, e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let { responseBody ->
                    callback(responseBody.string(), null)
                } ?: callback(null, IOException("Empty Response Body"))
            }
        })
    }
}