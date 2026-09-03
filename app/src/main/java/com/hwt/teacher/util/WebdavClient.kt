package com.hwt.teacher.util

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.xmlpull.v1.XmlPullParser
import java.io.IOException
import java.util.concurrent.TimeUnit

data class CloudFile(
    val name: String,
    val size: Long,
    val lastModified: Long,
    val latest: Boolean
)

/** 手写 WebDAV 客户端：PROPFIND / MKCOL / PUT / GET / DELETE。 */
class WebdavClient(
    private val baseUrlRaw: String,
    private val username: String,
    private val password: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val baseUrl: String
        get() = if (baseUrlRaw.endsWith("/")) baseUrlRaw else "$baseUrlRaw/"

    private fun auth(): String = Credentials.basic(username, password)

    private fun exec(
        method: String,
        url: String,
        body: String? = null,
        contentType: String? = null,
        depth: String? = null
    ): okhttp3.Response {
        val rb = body?.toRequestBody(contentType?.toMediaType())
        val req = Request.Builder()
            .url(url)
            .header("Authorization", auth())
            .header("User-Agent", "hwt-teacher/0.1 (Android)")
            .apply { if (depth != null) header("Depth", depth) }
            .method(method, rb)
            .build()
        return client.newCall(req).execute()
    }

    /** 返回 "ok" 或 "fail:原因"。 */
    suspend fun testConnection(): String = withContext(Dispatchers.IO) {
        exec(
            "PROPFIND", baseUrl,
            PROPFIND_RESOURCETYPE,
            "application/xml",
            depth = "0"
        ).use { resp ->
            when {
                resp.code in 200..299 -> "ok"
                resp.code == 401 || resp.code == 403 ->
                    "fail:账号或密码不正确（坚果云需使用「应用密码」，不是登录密码）"
                resp.code == 404 -> "fail:目录不存在，请确认地址路径"
                resp.code == 405 -> "fail:该地址不支持 WebDAV，请检查是否填成了网页地址"
                else -> "fail:服务器返回 HTTP ${resp.code}"
            }
        }
    }

    suspend fun ensureDir() = withContext(Dispatchers.IO) {
        exec("MKCOL", baseUrl).use { }
    }

    suspend fun upload(name: String, bytes: ByteArray) = withContext(Dispatchers.IO) {
        ensureDir()
        val url = baseUrl + name
        exec("PUT", url, String(bytes, Charsets.UTF_8), "application/json").use { resp ->
            if (resp.code !in 200..299) throw IOException("上传失败 HTTP ${resp.code}")
        }
    }

    suspend fun download(name: String): ByteArray? = withContext(Dispatchers.IO) {
        val url = baseUrl + name
        exec("GET", url).use { resp ->
            if (resp.code != 200) null else resp.body?.bytes()
        }
    }

    suspend fun delete(name: String) = withContext(Dispatchers.IO) {
        exec("DELETE", baseUrl + name).use { }
    }

    suspend fun list(): List<CloudFile> = withContext(Dispatchers.IO) {
        exec(
            "PROPFIND", baseUrl,
            PROPFIND_LIST,
            "application/xml",
            depth = "1"
        ).use { resp ->
            if (resp.code !in 200..299) throw IOException("列表失败 HTTP ${resp.code}")
            val body = resp.body?.string() ?: return@withContext emptyList()
            parseMultistatus(body)
        }
    }

    private fun parseMultistatus(xml: String): List<CloudFile> {
        val out = mutableListOf<CloudFile>()
        val parser = Xml.newPullParser()
        parser.setInput(xml.reader())
        var href: String? = null
        var size: Long = 0
        var modified: Long = 0
        var inResponse = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "response" -> { inResponse = true; href = null; size = 0; modified = 0 }
                    "href" -> href = parser.nextText()
                    "getcontentlength" -> size = parser.nextText().toLongOrNull() ?: 0
                    "getlastmodified" -> {
                        val t = parser.nextText()
                        modified = parseHttpDate(t)
                    }
                }
                XmlPullParser.END_TAG -> if (parser.name == "response" && inResponse) {
                    inResponse = false
                    val name = href?.substringAfterLast('/')?.trim() ?: ""
                    if (name.endsWith(".json")) {
                        out.add(CloudFile(name, size, modified, latest = false))
                    }
                }
            }
            event = parser.next()
        }
        out.sortByDescending { it.lastModified }
        return out
    }

    private fun parseHttpDate(s: String): Long {
        // 兼容 RFC1123 / RFC850 / asctime 等常见格式
        val formats = arrayOf(
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            "EEEE, dd-MMM-yy HH:mm:ss zzz",
            "EEE MMM dd HH:mm:ss yyyy",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )
        for (f in formats) {
            try {
                return java.text.SimpleDateFormat(f, java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.parse(s)?.time ?: 0
            } catch (e: Exception) { }
        }
        return 0
    }

    private companion object {
        const val PROPFIND_RESOURCETYPE =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<d:propfind xmlns:d=\"DAV:\"><d:prop><d:resourcetype/></d:prop></d:propfind>"

        const val PROPFIND_LIST =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<d:propfind xmlns:d=\"DAV:\"><d:prop>" +
                "<d:displayname/><d:getcontentlength/><d:getlastmodified/>" +
                "</d:prop></d:propfind>"
    }
}
