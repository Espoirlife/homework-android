package com.hwt.teacher.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hwt.teacher.backup.BackupUploader
import com.hwt.teacher.data.HomeworkRepository
import com.hwt.teacher.data.SettingsEntity
import com.hwt.teacher.ui.components.ToastBus
import com.hwt.teacher.util.CloudFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: HomeworkRepository,
    private val uploader: BackupUploader
) : ViewModel() {

    val settings: StateFlow<SettingsEntity> =
        repo.settingsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsEntity())

    fun updateDefaults(completion: String, correction: String) {
        viewModelScope.launch { repo.updateDefaults(completion, correction) }
    }

    fun updateQrParams(perRow: Int, margin: Int, level: String, withNo: Boolean) {
        viewModelScope.launch { repo.updateQrParams(perRow, margin, level, withNo) }
    }

    fun updateAutoBackup(on: Boolean) {
        viewModelScope.launch { repo.updateAutoBackup(on) }
    }

    fun testConnection(url: String, username: String, password: String) {
        viewModelScope.launch {
            repo.saveWebdavConfig(true, url, username, password)
            val err = uploader.testConnection()
            ToastBus.show(err ?: "连接正常")
        }
    }

    fun saveWebdav(enabled: Boolean, url: String, username: String, password: String) {
        viewModelScope.launch {
            repo.saveWebdavConfig(enabled, url, username, password)
            ToastBus.show("已保存 WebDAV 配置")
        }
    }

    fun toggleWebdav(enabled: Boolean) {
        viewModelScope.launch {
            repo.saveWebdavConfig(
                enabled, settings.value.webdavUrl, settings.value.webdavUsername,
                repo.webdavPassword()
            )
        }
    }

    fun uploadNow() {
        viewModelScope.launch {
            val err = uploader.uploadNow()
            ToastBus.show(err ?: "已上传一份备份")
        }
    }

    fun listCloud(onDone: (List<CloudFile>) -> Unit) {
        viewModelScope.launch {
            try {
                onDone(uploader.listCloud())
            } catch (e: Exception) {
                ToastBus.show(e.message ?: "读取云端备份失败")
                onDone(emptyList())
            }
        }
    }

    fun restoreFromCloud(name: String) {
        viewModelScope.launch {
            val err = uploader.restoreFromCloud(name)
            ToastBus.show(err ?: "已恢复")
        }
    }

    fun exportBackup(): String = com.hwt.teacher.util.DateUtil.backupName()

    suspend fun webdavPassword(): String = repo.webdavPassword()

    suspend fun buildBackupJson(): String = repo.exportBackup()

    fun importBackup(json: String) {
        viewModelScope.launch {
            val ok = repo.importBackup(json)
            ToastBus.show(if (ok) "已恢复" else "备份文件格式不正确")
        }
    }
}
