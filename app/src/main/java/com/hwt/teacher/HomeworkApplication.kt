package com.hwt.teacher

import android.app.Application
import com.hwt.teacher.backup.BackupUploader
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class HomeworkApplication : Application() {

    @Inject
    lateinit var uploader: BackupUploader

    override fun onCreate() {
        super.onCreate()
        uploader.startAutoBackupLoop()
    }
}
