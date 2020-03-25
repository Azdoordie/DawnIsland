package com.yanrou.dawnisland.content

import android.util.Log
import com.google.gson.Gson
import com.yanrou.dawnisland.FooterView
import com.yanrou.dawnisland.MyApplication
import com.yanrou.dawnisland.Reference
import com.yanrou.dawnisland.database.SeriesData
import com.yanrou.dawnisland.json2class.ReplysBean
import com.yanrou.dawnisland.json2class.SeriesContentJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import retrofit2.Retrofit

class SeriesContentModel(private val id: String) {
    private var seriesData: SeriesData? = null
    private var po: MutableList<String> = ArrayList()
    private val items: MutableList<Any> = ArrayList()
    private var seriesId: String = id
    /**
     * 标记是否有缓存
     */
    private var hasCache = false
    /**
     * 标记最后一页是否完整
     * 如果为false,则应该用获取到的页面替换当前页
     */
    private var wholePage = true
    private var lastPageCount = 0
    /**
     * 标记最后一页是否有广告
     */
    private var hasAd = false
    /**
     * 控制翻页，如果有缓存就会从缓存中获取上次看到的位置,没有的话从第一页开始
     */
    private val GET_FIRST_PAGE = 1001
    private val GET_FRONT_PAGE = 1002
    private val GET_NEXT_PAGE = 1003
    private val GET_JUMP_PAGE = 1004
    /**
     * 只有state为READY时可以请求新数据
     */
    private val READY = 1000
    private var state = 1000
    private val footerView = FooterView()

    val replyCount: Int
        get() = seriesData!!.lastReplyCount

    /**
     * 用于给ViewModel调用
     * 返回一个ReplysBean的List或者一个String
     * 由于A岛的APi设计问题，不得不很不优雅的写一个Any在这里
     * 当串被删了，就返回一个String对象，如果没有更多数据，就返回一个空list，如果还有数据则返回一个正常的list
     */
    suspend fun getSeriesContent(page: Int): Any {
        val s = withContext(Dispatchers.IO) {
            getSeriesContentFromNet(page)
        }
        /*
             先判断串是否存在
             这里有堵的成分，如果不是第一页就不判断，可能会快一点
             如果为真表示串已经被删了
             TODO 这个地方仅仅考虑了没有缓存的情况，加上缓存功能后还需要改进
             TODO 例如已缓存的串被删除，用户在下拉的时候会触发刷新，这里的就不适用了
            */
        if ("\"\\u8be5\\u4e3b\\u9898\\u4e0d\\u5b58\\u5728\"" == s) {
            return s
        }
        return preFormatJson(page, s)
    }

    private fun getSeriesContentFromNet(page: Int): String {
        val retrofit = Retrofit.Builder()
                .baseUrl("https://nmb.fastmirror.org/")
                .build()
                .create(SeriesContentService::class.java)
        val result = retrofit.getSeriesContent(seriesId, page)
        return result!!.execute().body()!!.string()
    }


    private fun preFormatJson(page: Int, s: String): List<ReplysBean> {
        /*
          解析串
         */
        val seriesContentJson = Gson().fromJson(s, SeriesContentJson::class.java)
        /**
         * 防止翻页翻过，这一句表示已经翻到底了
         */
        Log.d(TAG, "onResponse: " + (page != 1) + page)
        //为真则表示空页
        if (page != 1 && (seriesContentJson.replys.size == 1 && "9999999" == seriesContentJson.replys[0].seriesId || seriesContentJson.replys.size == 0)) {
            return ArrayList()
        }


        if (page == 1) {
            Log.d(TAG, "onResponse: 第一页")
            seriesData!!.lastPage = 1
            seriesData!!.lastReplyCount = seriesContentJson.replyCount
            seriesData!!.po.add(seriesContentJson.userid)
            seriesData!!.save()
            po.add(seriesContentJson.userid)
            seriesContentJson.replys.add(0, ReplysBean(
                    seriesContentJson.seriesId,
                    seriesContentJson.userid,
                    seriesContentJson.admin,
                    seriesContentJson.title,
                    seriesContentJson.email,
                    seriesContentJson.now,
                    seriesContentJson.content,
                    seriesContentJson.img,
                    seriesContentJson.ext,
                    seriesContentJson.name,
                    seriesContentJson.sage
            ))
        }

        for (i in seriesContentJson.replys.indices) {
            val temp = seriesContentJson.replys[i]
            temp.page = page
            temp.parentId = seriesId
            temp.posInPage = i
        }
        /**
         * 保存页面数据用于下次加载
         */
        val replysBeanDao = MyApplication.getDaoSession().replysBeanDao
        replysBeanDao.insertInTx(seriesContentJson.replys)

        return seriesContentJson.replys
        //上面这一步做完我们获得了一个处理好的列表
    }

    /**
     * 如果采用https://adnmb2.com/Api/ref?id=23294468形式的话， 这个function就没有意义了
     * 用来获取引用内容
     * 引用内容分为串内引用和串外引用，这个是用来获取串外引用的跳转原帖链接的
     *
     * @param html
     * @return
     */
    private fun decodeReference(html: String): Reference {
        val reference = Reference()
        val doc = Jsoup.parse(html)
        val elements: List<Element> = doc.allElements
        for (element in elements) {
            val className = element.className()
            if ("h-threads-item-reply h-threads-item-ref" == className) {
                reference.id = element.attr("data-threads-id")
            } else if ("h-threads-img-a" == className) {
                reference.image = element.attr("href")
            } else if ("h-threads-img" == className) {
                reference.thumb = element.attr("src")
            } else if ("h-threads-info-title" == className) {
                reference.title = element.text()
            } else if ("h-threads-info-email" == className) { // TODO email or user ?
                reference.user = element.text()
            } else if ("h-threads-info-createdat" == className) {
                reference.time = element.text()
            } else if ("h-threads-info-uid" == className) {
                val user = element.text()
                if (user.startsWith("ID:")) {
                    reference.userId = user.substring(3)
                } else {
                    reference.userId = user
                }
                reference.admin = element.childNodeSize() > 1
            } else if ("h-threads-info-id" == className) {
                val href = element.attr("href")
                if (href.startsWith("/t/")) {
                    val index = href.indexOf('?')
                    if (index >= 0) {
                        reference.postId = href.substring(3, index)
                    } else {
                        reference.postId = href.substring(3)
                    }
                }
            } else if ("h-threads-content" == className) {
                reference.content = element.html()
            }
        }
        return reference
    }

    companion object {
        private const val TAG = "SeriesContentModel"
    }

}