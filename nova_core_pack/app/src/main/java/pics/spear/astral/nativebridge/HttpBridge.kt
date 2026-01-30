package pics.spear.astral.nativebridge

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class HttpBridge {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun get(url: String, headers: JSONObject?): JSONObject {
        val requestBuilder = Request.Builder().url(url)
        headers?.keys()?.forEach { key ->
            requestBuilder.addHeader(key, headers.getString(key))
        }
        
        return try {
            val response = client.newCall(requestBuilder.build()).execute()
            formatResponse(response)
        } catch (e: Exception) {
            formatError(e)
        }
    }

    fun post(url: String, body: String, contentType: String, headers: JSONObject?): JSONObject {
        val mediaType = contentType.toMediaType()
        val requestBody = body.toRequestBody(mediaType)
        val requestBuilder = Request.Builder().url(url).post(requestBody)
        
        headers?.keys()?.forEach { key ->
            requestBuilder.addHeader(key, headers.getString(key))
        }

        return try {
            val response = client.newCall(requestBuilder.build()).execute()
            formatResponse(response)
        } catch (e: Exception) {
            formatError(e)
        }
    }

    private fun formatResponse(response: okhttp3.Response): JSONObject {
        val headersArr = JSONObject()
        response.headers.forEach { (name, value) ->
            headersArr.put(name, value)
        }

        return JSONObject()
            .put("status", response.code)
            .put("body", response.body?.string() ?: "")
            .put("headers", headersArr)
    }

    private fun formatError(e: Exception): JSONObject {
        return JSONObject()
            .put("status", -1)
            .put("error", e.message ?: e.toString())
    }
}


