package com.hwt.teacher.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String = "app",
    @ColumnInfo(name = "currentClassId") val currentClassId: String? = null,
    @ColumnInfo(name = "onboarded") val onboarded: Boolean = false,
    @ColumnInfo(name = "defaultCompletion") val defaultCompletion: String = Completion.MISS,
    @ColumnInfo(name = "defaultCorrection") val defaultCorrection: String = Correction.PENDING,
    @ColumnInfo(name = "qrPerRow") val qrPerRow: Int = 3,
    @ColumnInfo(name = "qrMargin") val qrMargin: Int = 8,
    @ColumnInfo(name = "qrLevel") val qrLevel: String = "M",
    @ColumnInfo(name = "qrWithNo") val qrWithNo: Boolean = true,
    @ColumnInfo(name = "autoBackup") val autoBackup: Boolean = false,
    @ColumnInfo(name = "lastBackupAt") val lastBackupAt: Long? = null,
    @ColumnInfo(name = "webdavEnabled") val webdavEnabled: Boolean = false,
    @ColumnInfo(name = "webdavUrl") val webdavUrl: String = "",
    @ColumnInfo(name = "webdavUsername") val webdavUsername: String = "",
    @ColumnInfo(name = "webdavKeep") val webdavKeep: Int = 5,
    @ColumnInfo(name = "webdavTested") val webdavTested: String? = null,
    @ColumnInfo(name = "webdavLastError") val webdavLastError: String? = null
)

@Entity(tableName = "classes")
data class ClassEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "note") val note: String = "",
    @ColumnInfo(name = "prefix") val prefix: String = "",
    @ColumnInfo(name = "digits") val digits: Int = 2,
    @ColumnInfo(name = "recycle") val recycle: Boolean = false,
    @ColumnInfo(name = "createdAt") val createdAt: Long = 0L,
    @ColumnInfo(name = "updatedAt") val updatedAt: Long = 0L
)

@Entity(
    tableName = "students",
    indices = [Index(value = ["classId"])]
)
data class StudentEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "classId") val classId: String,
    @ColumnInfo(name = "seq") val seq: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "note") val note: String = "",
    @ColumnInfo(name = "createdAt") val createdAt: Long = 0L,
    @ColumnInfo(name = "updatedAt") val updatedAt: Long = 0L
)

@Entity(
    tableName = "assignments",
    indices = [Index(value = ["classId"])]
)
data class AssignmentEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "classId") val classId: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "assignedDate") val assignedDate: String,
    @ColumnInfo(name = "note") val note: String = "",
    @ColumnInfo(name = "createdAt") val createdAt: Long = 0L,
    @ColumnInfo(name = "updatedAt") val updatedAt: Long = 0L
)

@Entity(
    tableName = "records",
    indices = [
        Index(value = ["assignmentId", "studentId"], unique = true),
        Index(value = ["studentId"]),
        Index(value = ["classId"])
    ]
)
data class RecordEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "assignmentId") val assignmentId: String,
    @ColumnInfo(name = "studentId") val studentId: String,
    @ColumnInfo(name = "classId") val classId: String,
    @ColumnInfo(name = "completion") val completion: String = Completion.MISS,
    @ColumnInfo(name = "correction") val correction: String = Correction.PENDING,
    @ColumnInfo(name = "grade") val grade: String? = null,
    @ColumnInfo(name = "comment") val comment: String? = null,
    @ColumnInfo(name = "updatedAt") val updatedAt: Long = 0L
)
