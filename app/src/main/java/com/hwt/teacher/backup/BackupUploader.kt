package com.hwt.teacher.backup

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.hwt.teacher.HomeworkApplication
import com.hwt.teacher.data.HomeworkRepository
import com.hwt.teacher.util.CloudFile
import com.hwt.teacher.util.DateUtil
import com.hwt.teacher.util.WebdavClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 备份上传器：自动备份与手动上传/测试/恢复的统一入口。
 *
 * 自动备份采用真防抖 + 最小间隔节流：
 * 每次数据变更取消上一个待执行任务并重新计时，停止操作 [DEBOUNCE_MS] 后才上传；
 * 距上次成功上传不足 [MIN_INTERVAL_MS] 时顺延，避免密集录入导致频繁写云端。
 */
@Singleton
class BackupUploader @Inject constructor(
    private val repository: HomeworkRepository,
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    private var uploading = false
    private var pendingJob: Job? = null

    fun startAutoBackupLoop() {
        scope.launch {
            repository.dataChanged.collect {
                pendingJob?.cancel()
                pendingJob = scope.launch {
                    delay(DEBOUNCE_MS)
                    autoUpload()
                }
            }
        }
    }

    private suspend fun autoUpload() {
        val settings = repository.settingsFlow.first()
        if (!settings.autoBackup || !settings.webdavEnabled || settings.webdavUrl.isBlank()) return
        val since = System.currentTimeMillis() - (settings.lastBackupAt ?: 0L)
        if (since < MIN_INTERVAL_MS) {
            delay(MIN_INTERVAL_MS - since)
        }
        val err = uploadNow(notifyError = true)
        if (err != null) scheduleRetry()
    }

    private fun scheduleRetry() {
        val request = OneTimeWorkRequestBuilder<BackupRetryWorker>()
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("hwt-backup-retry", ExistingWorkPolicy.REPLACE, request)
    }

    /** 返回 null 表示成功，否则返回错误信息。 */
    suspend fun uploadNow(notifyError: Boolean = true): String? {
        val settings = repository.settingsFlow.first()
        if (!settings.webdavEnabled) return "请先启用 WebDAV"
        if (settings.webdavUrl.isBlank() || settings.webdavUsername.isBlank()) return "WebDAV 配置不完整"
        val password = repository.webdavPassword()
        synchronized(lock) {
            if (uploading) return "上传进行中"
            uploading = true
        }
        return try {
            val client = WebdavClient(settings.webdavUrl, settings.webdavUsername, password)
            val json = repository.exportBackup()
            val name = DateUtil.backupName()
            client.upload(name, json.toByteArray(Charsets.UTF_8))
            repository.markBackupUploaded()
            repository.setWebdavTested("ok")
            null
        } catch (e: Exception) {
            val msg = describe(e)
            if (notifyError) repository.setBackupError(msg)
            msg
        } finally {
            synchronized(lock) { uploading = false }
        }
    }

    /** 返回 null 表示连接正常，否则返回错误信息。 */
    suspend fun testConnection(): String? {
        val settings = repository.settingsFlow.first()
        if (settings.webdavUrl.isBlank() || settings.webdavUsername.isBlank()) return "信息不完整"
        val urlError = validateUrl(settings.webdavUrl)
        if (urlError != null) {
            val msg = "连接失败：$urlError"
            repository.setWebdavTested("fail", msg)
            return msg
        }
        val client = WebdavClient(settings.webdavUrl, settings.webdavUsername, repository.webdavPassword())
        return try {
            val r = client.testConnection()
            if (r == "ok") {
                repository.setWebdavTested("ok")
                null
            } else {
                val msg = "连接失败：${r.removePrefix("fail:")}"
                repository.setWebdavTested("fail", msg)
                msg
            }
        } catch (e: Exception) {
            val msg = "连接失败：${describe(e)}"
            repository.setWebdavTested("fail", msg)
            msg
        }
    }

    private fun validateUrl(raw: String): String? {
        val url = raw.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "地址需以 https:// 开头"
        }
        return null
    }

    private fun describe(e: Exception): String = when (e) {
        is android.os.NetworkOnMainThreadException -> "内部错误：网络请求未切换到后台线程"
        is java.net.UnknownHostException -> "无法解析服务器域名，请检查地址与网络"
        is java.net.SocketTimeoutException -> "连接超时，请检查网络后重试"
        is java.net.ConnectException -> "无法连接到服务器，请检查网络"
        is javax.net.ssl.SSLException -> "HTTPS 握手失败，请确认地址正确"
        is IllegalArgumentException -> "地址格式不正确：${e.message ?: ""}"
        else -> e.message ?: e.javaClass.simpleName
    }

    suspend fun listCloud(): List<CloudFile> {
        val settings = repository.settingsFlow.first()
        val client = WebdavClient(settings.webdavUrl, settings.webdavUsername, repository.webdavPassword())
        return client.list().mapIndexed { i, f -> f.copy(latest = i == 0) }
    }

    suspend fun restoreFromCloud(name: String): String? {
        val settings = repository.settingsFlow.first()
        val client = WebdavClient(settings.webdavUrl, settings.webdavUsername, repository.webdavPassword())
        val bytes = client.download(name) ?: return "下载失败"
        val json = String(bytes, Charsets.UTF_8)
        return if (repository.importBackup(json)) null else "备份文件格式不正确"
    }

    companion object {
        /** 停止操作后延迟上传的防抖窗口。 */
        const val DEBOUNCE_MS = 30_000L

        /** 两次自动上传之间的最小间隔。 */
        const val MIN_INTERVAL_MS = 5 * 60_000L
    }
}

/** WebDAV 上传失败后的静默重试（不产生用户可见通知）。 */
class BackupRetryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? HomeworkApplication ?: return Result.failure()
        return try {
            val err = app.uploader.uploadNow(notifyError = false)
            if (err == null) Result.success() else Result.retry()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
