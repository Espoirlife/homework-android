package com.hwt.teacher.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.hwt.teacher.data.AssignmentEntity
import com.hwt.teacher.data.ClassEntity
import com.hwt.teacher.data.RecordEntity
import com.hwt.teacher.data.SettingsEntity
import com.hwt.teacher.data.StudentEntity

/** 附录 D：hwt-backup 自有备份格式。 */
data class BackupFile(
    val format: String = "hwt-backup",
    val version: Int = 1,
    val exportedAt: String? = null,
    val data: BackupData? = null
)

data class BackupData(
    val settings: List<SettingsDto> = emptyList(),
    val classes: List<ClassDto> = emptyList(),
    val students: List<StudentDto> = emptyList(),
    val assignments: List<AssignmentDto> = emptyList(),
    val records: List<RecordDto> = emptyList()
)

data class SettingsDto(
    val id: String = "app",
    val currentClassId: String? = null,
    val onboarded: Boolean = false,
    val defaultCompletion: String = "miss",
    val defaultCorrection: String = "pending",
    val qrPerRow: Int = 3,
    val qrMargin: Int = 8,
    val qrLevel: String = "M",
    val qrWithNo: Boolean = true,
    val autoBackup: Boolean = false,
    val lastBackupAt: String? = null,
    val webdavEnabled: Boolean = false,
    val webdavUrl: String = "",
    val webdavUsername: String = "",
    val webdavKeep: Int = 5
)

data class ClassDto(
    val id: String,
    val name: String,
    val note: String = "",
    val prefix: String = "",
    val digits: Int = 2,
    val recycle: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class StudentDto(
    val id: String,
    val classId: String,
    val seq: Int,
    val name: String,
    val note: String = "",
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class AssignmentDto(
    val id: String,
    val classId: String,
    val title: String,
    val assignedDate: String,
    val note: String = "",
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class RecordDto(
    val id: String,
    val assignmentId: String,
    val studentId: String,
    val classId: String,
    val completion: String = "miss",
    val correction: String = "pending",
    val grade: String? = null,
    val comment: String? = null,
    val updatedAt: String? = null
)

object BackupManager {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun toJson(
        settings: SettingsEntity,
        classes: List<ClassEntity>,
        students: List<StudentEntity>,
        assignments: List<AssignmentEntity>,
        records: List<RecordEntity>
    ): String {
        val file = BackupFile(
            exportedAt = DateUtil.toIso(System.currentTimeMillis()),
            data = BackupData(
                settings = listOf(
                    SettingsDto(
                        currentClassId = settings.currentClassId,
                        onboarded = settings.onboarded,
                        defaultCompletion = settings.defaultCompletion,
                        defaultCorrection = settings.defaultCorrection,
                        qrPerRow = settings.qrPerRow,
                        qrMargin = settings.qrMargin,
                        qrLevel = settings.qrLevel,
                        qrWithNo = settings.qrWithNo,
                        autoBackup = settings.autoBackup,
                        lastBackupAt = DateUtil.toIso(settings.lastBackupAt),
                        webdavEnabled = settings.webdavEnabled,
                        webdavUrl = settings.webdavUrl,
                        webdavUsername = settings.webdavUsername,
                        webdavKeep = settings.webdavKeep
                    )
                ),
                classes = classes.map {
                    ClassDto(it.id, it.name, it.note, it.prefix, it.digits, it.recycle, DateUtil.toIso(it.createdAt), DateUtil.toIso(it.updatedAt))
                },
                students = students.map {
                    StudentDto(it.id, it.classId, it.seq, it.name, it.note, DateUtil.toIso(it.createdAt), DateUtil.toIso(it.updatedAt))
                },
                assignments = assignments.map {
                    AssignmentDto(it.id, it.classId, it.title, it.assignedDate, it.note, DateUtil.toIso(it.createdAt), DateUtil.toIso(it.updatedAt))
                },
                records = records.map {
                    RecordDto(it.id, it.assignmentId, it.studentId, it.classId, it.completion, it.correction, it.grade, it.comment, DateUtil.toIso(it.updatedAt))
                }
            )
        )
        return gson.toJson(file)
    }

    /** 校验并解析备份；格式不正确返回 null。 */
    fun parse(json: String): BackupFile? {
        return try {
            val file = gson.fromJson(json, BackupFile::class.java)
            if (file.format != "hwt-backup") null else file
        } catch (e: Exception) {
            null
        }
    }

    fun toSettings(dto: SettingsDto): SettingsEntity = SettingsEntity(
        id = "app",
        currentClassId = dto.currentClassId,
        onboarded = dto.onboarded,
        defaultCompletion = dto.defaultCompletion,
        defaultCorrection = dto.defaultCorrection,
        qrPerRow = dto.qrPerRow,
        qrMargin = dto.qrMargin,
        qrLevel = dto.qrLevel,
        qrWithNo = dto.qrWithNo,
        autoBackup = dto.autoBackup,
        lastBackupAt = DateUtil.fromIso(dto.lastBackupAt),
        webdavEnabled = dto.webdavEnabled,
        webdavUrl = dto.webdavUrl,
        webdavUsername = dto.webdavUsername,
        webdavKeep = dto.webdavKeep
    )

    fun toClass(dto: ClassDto): ClassEntity = ClassEntity(
        id = dto.id, name = dto.name, note = dto.note, prefix = dto.prefix,
        digits = dto.digits, recycle = dto.recycle,
        createdAt = DateUtil.fromIso(dto.createdAt) ?: System.currentTimeMillis(),
        updatedAt = DateUtil.fromIso(dto.updatedAt) ?: System.currentTimeMillis()
    )

    fun toStudent(dto: StudentDto): StudentEntity = StudentEntity(
        id = dto.id, classId = dto.classId, seq = dto.seq, name = dto.name, note = dto.note,
        createdAt = DateUtil.fromIso(dto.createdAt) ?: System.currentTimeMillis(),
        updatedAt = DateUtil.fromIso(dto.updatedAt) ?: System.currentTimeMillis()
    )

    fun toAssignment(dto: AssignmentDto): AssignmentEntity = AssignmentEntity(
        id = dto.id, classId = dto.classId, title = dto.title, assignedDate = dto.assignedDate,
        note = dto.note,
        createdAt = DateUtil.fromIso(dto.createdAt) ?: System.currentTimeMillis(),
        updatedAt = DateUtil.fromIso(dto.updatedAt) ?: System.currentTimeMillis()
    )

    fun toRecord(dto: RecordDto): RecordEntity = RecordEntity(
        id = dto.id, assignmentId = dto.assignmentId, studentId = dto.studentId,
        classId = dto.classId, completion = dto.completion, correction = dto.correction,
        grade = dto.grade, comment = dto.comment,
        updatedAt = DateUtil.fromIso(dto.updatedAt) ?: System.currentTimeMillis()
    )
}
