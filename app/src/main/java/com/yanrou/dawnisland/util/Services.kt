package com.yanrou.dawnisland.util

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*


interface SeriesContentService {
    @GET("Api/thread")
    fun getSeriesContent(@Query("id") id: String?, @Query("page") page: Int): Call<ResponseBody>?

    @GET("Api/showf")
    fun getSeriesList(@Query("id") id: Int?, @Query("page") page: Int): Call<ResponseBody>?

    @GET("Api/timeline")
    fun getTimelineList(@Query("page") page: Int): Call<ResponseBody>?

    @POST("Home/Forum/doReplyThread.html")
    fun sendReply(@Body body: RequestBody, @Header("Cookie") cookie: String): Call<ResponseBody>?

    @GET("Api/ref")
    fun getQuote(@Query("id") id: String): Call<ResponseBody>?
}
