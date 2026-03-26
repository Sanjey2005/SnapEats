package com.example.snapeats.data.remote.api

import okhttp3.Interceptor
import okhttp3.Response
import java.net.URLEncoder
import java.util.TreeMap
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * OkHttp [Interceptor] that signs every outgoing request with OAuth 1.0a / HMAC-SHA1
 * as required by the FatSecret Platform API.
 *
 * @param consumerKey    OAuth consumer key from BuildConfig.FATSECRET_KEY
 * @param consumerSecret OAuth consumer secret from BuildConfig.FATSECRET_SECRET
 */
class OAuthInterceptor(
    private val consumerKey: String,
    private val consumerSecret: String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url

        // --- 1. Collect base OAuth parameters ---------------------------------
        val timestamp = (System.currentTimeMillis() / 1000L).toString()
        val nonce = UUID.randomUUID().toString().replace("-", "")

        val oauthParams = mapOf(
            "oauth_consumer_key" to consumerKey,
            "oauth_nonce" to nonce,
            "oauth_signature_method" to "HMAC-SHA1",
            "oauth_timestamp" to timestamp,
            "oauth_version" to "1.0"
        )

        // --- 2. Collect all query parameters from the URL ---------------------
        val queryParams: MutableMap<String, String> = mutableMapOf()
        for (i in 0 until originalUrl.querySize) {
            val name = originalUrl.queryParameterName(i)
            val value = originalUrl.queryParameterValue(i) ?: ""
            queryParams[name] = value
        }

        // --- 3. Build the parameter string (sorted, percent-encoded) ----------
        // OAuth spec §9.1.1: collect all params, percent-encode each name and
        // value, sort lexicographically by name (then value), join with "&".
        val allParams = TreeMap<String, String>()
        allParams.putAll(queryParams)
        allParams.putAll(oauthParams)

        val parameterString = allParams.entries.joinToString("&") { (k, v) ->
            "${k.percentEncode()}&${v.percentEncode()}"
                .let { "${k.percentEncode()}=${v.percentEncode()}" }
        }

        // --- 4. Build the signature base string -------------------------------
        // METHOD & BASE_URL & ENCODED_PARAMS
        val baseUrl = "${originalUrl.scheme}://${originalUrl.host}${originalUrl.encodedPath}"
        val signatureBaseString =
            "GET&${baseUrl.percentEncode()}&${parameterString.percentEncode()}"

        // --- 5. Build the signing key -----------------------------------------
        // consumerSecret & tokenSecret (empty for two-legged OAuth)
        val signingKey = "${consumerSecret.percentEncode()}&"

        // --- 6. Compute HMAC-SHA1 signature -----------------------------------
        val signature = hmacSha1(signingKey, signatureBaseString)

        // --- 7. Rebuild the URL with the oauth_signature query parameter ------
        // FatSecret accepts OAuth params in the query string (two-legged flow).
        val newUrl = originalUrl.newBuilder().apply {
            oauthParams.forEach { (k, v) -> addQueryParameter(k, v) }
            addQueryParameter("oauth_signature", signature)
        }.build()

        val newRequest = originalRequest.newBuilder()
            .url(newUrl)
            .build()

        return chain.proceed(newRequest)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** RFC 3986 percent-encoding (space → %20, not +). */
    private fun String.percentEncode(): String =
        URLEncoder.encode(this, "UTF-8")
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("%7E", "~")

    /**
     * Computes HMAC-SHA1 of [data] using [key] and returns the result as a
     * Base64-encoded string (standard alphabet, no line-wrap).
     */
    private fun hmacSha1(key: String, data: String): String {
        val secretKeySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA1")
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(secretKeySpec)
        val rawBytes = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return android.util.Base64.encodeToString(rawBytes, android.util.Base64.NO_WRAP)
    }
}
