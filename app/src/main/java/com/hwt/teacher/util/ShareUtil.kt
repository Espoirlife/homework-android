package com.hwt.teacher.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * 通过系统分享把导出文件交给其他应用（金山文档 / WPS、微信、邮件等）。
 * 文件先写入 cacheDir/exports，再以 FileProvider 内容 URI 授权给目标应用读取。
 */
object ShareUtil {

    private const val XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    /** 返回 null 表示成功，否则返回错误信息。 */
    fun shareXlsx(context: Context, fileName: String, bytes: ByteArray): String? = runCatching {
        val dir = File(context.cacheDir, "exports").apply {
            if (!exists()) mkdirs()
        }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, sanitize(fileName))
        file.writeBytes(bytes)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = XLSX_MIME
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "发送到").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        })
        null
    }.getOrElse { it.message ?: "分享失败" }

    /** 去掉文件名中不能用于文件系统的字符。 */
    private fun sanitize(name: String): String =
        name.replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "export.xlsx" }
}
