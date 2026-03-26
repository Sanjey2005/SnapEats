package com.example.snapeats.data.remote.api

import com.example.snapeats.data.remote.dto.FoodSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for the FatSecret Platform REST API.
 *
 * Base URL: https://platform.fatsecret.com/
 *
 * Authentication is handled by [OAuthInterceptor] which signs every request
 * using OAuth 1.0a / HMAC-SHA1 before it hits the wire.
 */
interface FatSecretApi {

    /**
     * Search for foods by keyword.
     *
     * FatSecret uses a single endpoint with a `method` query parameter to
     * route to different API functions (RPC-style over REST).
     *
     * @param method     Always "foods.search" for this call.
     * @param query      The food name or keyword to search for.
     * @param format     Always "json" — tells FatSecret to return JSON.
     * @param maxResults Number of results to return (1–50). Default 5.
     * @return           Parsed [FoodSearchResponse] containing a list of food items.
     */
    @GET("rest/server.api")
    suspend fun searchFoods(
        @Query("method") method: String = "foods.search",
        @Query("search_expression") query: String,
        @Query("format") format: String = "json",
        @Query("max_results") maxResults: Int = 5
    ): FoodSearchResponse
}
