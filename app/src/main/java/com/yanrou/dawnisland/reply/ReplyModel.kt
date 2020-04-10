package com.yanrou.dawnisland.reply

import com.yanrou.dawnisland.util.Server
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.RequestBody

class ReplyModel {
    suspend fun sendReply(requestBody: RequestBody, cookie: String): String =
            withContext(Dispatchers.IO) {
                val call = Server.getService.sendReply(requestBody, "userhash=$cookie")
                call!!.execute().body()!!.string()
            }
}