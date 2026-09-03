package com.hwt.teacher.data

import androidx.room.withTransaction
import com.hwt.teacher.util.Ids
import com.hwt.teacher.util.PasswordStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

data class StudentView(val student: StudentEntity, val code: String)

data class ClassCardView(
    val classEntity: ClassEntity,
    val studentCount: Int,
    val assignmentCount: Int
)

data class EntryRowView(
    val student: StudentEntity,
    val code: String,
    val completion: String,
    val correction: String,
    val grade: String,
    val saved: Boolean
)

data class AssignmentStats(
    val total: Int,
    val done: Int,
    val miss: Int,
    val partial: Int,
    val pending: Int,
    val counted: Int,
    val rate: Int
)

data class AssignmentRowView(val assignment: AssignmentEntity, val stats: AssignmentStats)

data class Bar(val id: String, val title: String, val date: String, val rate: Int)

data class ClassSummary(val avg: Int, val pending: Int, val missTotal: Int, val bars: List<Bar>, val count: Int)

data class PersonItem(val assignment: AssignmentEntity, val completion: String, val correction: String, val grade: String)

data class PersonReport(val student: StudentView, val rate: Int, val doneCount: Int, val items: List<PersonItem>)

data class MissEntry(val studentId: String, val code: String, val name: String, val assignmentTitle: String)

private val DEFAULT_SETTINGS = SettingsEntity()

@OptIn(ExperimentalCoroutinesApi::class)
class HomeworkRepository(
    private val db: AppDatabase,
    private val passwordStore: PasswordStore
) {
    private val settingsDao = db.settingsDao()
    private val classDao = db.classDao()
    private val studentDao = db.studentDao()
    private val assignmentDao = db.assignmentDao()
    private val recordDao = db.recordDao()

    private val _dataChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val dataChanged: SharedFlow<Unit> = _dataChanged

    private fun notifyChanged() {
        _dataChanged.tryEmit(Unit)
    }

    // ---------- 基础 Flow ----------

    val settingsFlow: Flow<SettingsEntity> =
        settingsDao.observe().map { it ?: DEFAULT_SETTINGS }.distinctUntilChanged()

    val classesFlow: Flow<List<ClassEntity>> = classDao.observeAll()

    fun classById(id: String): Flow<ClassEntity?> = classDao.observeById(id)

    fun assignmentById(id: String): Flow<AssignmentEntity?> = assignmentDao.observeById(id)

    fun classCards(): Flow<List<ClassCardView>> =
        combine(classDao.observeAll(), studentDao.observeAll(), assignmentDao.observeAll()) { classes, students, assignments ->
            classes.map { c ->
                ClassCardView(c, students.count { it.classId == c.id }, assignments.count { it.classId == c.id })
            }
        }

    fun studentsOf(classId: String): Flow<List<StudentView>> =
        combine(classDao.observeById(classId), studentDao.observeByClass(classId)) { cls, students ->
            if (cls == null) emptyList()
            else students.map { StudentView(it, codeOf(cls, it.seq)) }
        }

    fun entryRows(assignmentId: String): Flow<List<EntryRowView>> =
        assignmentDao.observeById(assignmentId).flatMapLatest { a ->
            if (a == null) flowOf(emptyList())
            else combine(
                classDao.observeById(a.classId),
                studentDao.observeByClass(a.classId),
                recordDao.observeByAssignment(assignmentId),
                settingsFlow
            ) { cls, students, records, settings ->
                if (cls == null) emptyList()
                else {
                    val map = records.associateBy { it.studentId }
                    students.map { s ->
                        val r = map[s.id]
                        EntryRowView(
                            student = s,
                            code = codeOf(cls, s.seq),
                            completion = r?.completion ?: settings.defaultCompletion,
                            correction = r?.correction ?: settings.defaultCorrection,
                            grade = r?.grade ?: Grade.NONE,
                            saved = r != null
                        )
                    }
                }
            }
        }

    fun assignmentRows(classId: String): Flow<List<AssignmentRowView>> =
        combine(
            assignmentDao.observeByClass(classId),
            studentDao.observeByClass(classId),
            recordDao.observeByClass(classId),
            settingsFlow
        ) { assignments, students, records, settings ->
            val map = records.associateBy { it.assignmentId to it.studentId }
            assignments.map { a ->
                val stats = computeStats(students, a.id, map, settings)
                AssignmentRowView(a, stats)
            }
        }

    fun classSummary(classId: String): Flow<ClassSummary?> =
        assignmentRows(classId).map { rows ->
            if (rows.isEmpty()) null
            else {
                val recent = rows.take(5).reversed()
                val bars = recent.map { Bar(it.assignment.id, it.assignment.title, it.assignment.assignedDate, it.stats.rate) }
                val avg = (bars.map { it.rate }.average().roundToInt())
                val pending = rows.sumOf { it.stats.pending }
                val missTotal = rows.sumOf { it.stats.miss }
                ClassSummary(avg, pending, missTotal, bars, rows.size)
            }
        }

    fun missListFlow(classId: String): Flow<List<MissEntry>> =
        assignmentRows(classId).map { rows ->
            val recent = rows.take(2)
            val out = mutableListOf<MissEntry>()
            for (a in recent) {
                val entryRows = entryRows(a.assignment.id).first()
                for (r in entryRows) {
                    if (r.completion == Completion.MISS) {
                        out.add(MissEntry(
                            r.student.id, r.code, r.student.name, a.assignment.title
                        ))
                    }
                }
            }
            out
        }

    fun personReport(classId: String, studentId: String): Flow<PersonReport?> =
        combine(
            classDao.observeById(classId),
            studentDao.observeById(studentId),
            assignmentDao.observeByClass(classId),
            recordDao.observeByClass(classId),
            settingsFlow
        ) { cls, stu, assignments, records, settings ->
            if (cls == null || stu == null) null
            else {
                val map = records.associateBy { it.assignmentId to it.studentId }
                val items = assignments.map { a ->
                    val r = map[a.id to stu.id]
                    PersonItem(
                        a,
                        r?.completion ?: settings.defaultCompletion,
                        r?.correction ?: settings.defaultCorrection,
                        r?.grade ?: Grade.NONE
                    )
                }
                val ok = items.count { Completion.counted(it.completion) }
                val rate = if (items.isEmpty()) 0 else (ok * 100.0 / items.size).roundToInt()
                PersonReport(StudentView(stu, codeOf(cls, stu.seq)), rate, ok, items)
            }
        }

    private fun computeStats(
        students: List<StudentEntity>,
        assignmentId: String,
        map: Map<Pair<String, String>, RecordEntity>,
        settings: SettingsEntity
    ): AssignmentStats {
        var done = 0; var miss = 0; var partial = 0; var pending = 0; var counted = 0
        students.forEach { s ->
            val r = map[assignmentId to s.id]
            val completion = r?.completion ?: settings.defaultCompletion
            val correction = r?.correction ?: settings.defaultCorrection
            when (completion) {
                Completion.DONE -> done++
                Completion.PARTIAL -> partial++
                else -> miss++
            }
            if (Completion.counted(completion)) counted++
            if (correction == Correction.PENDING) pending++
        }
        val total = students.size
        val rate = if (total == 0) 0 else (counted * 100.0 / total).roundToInt()
        return AssignmentStats(total, done, miss, partial, pending, counted, rate)
    }

    private fun codeOf(cls: ClassEntity, seq: Int): String =
        cls.prefix + seq.toString().padStart(cls.digits.coerceIn(1, 4), '0')

    private suspend fun now(): Long = System.currentTimeMillis()

    private suspend fun currentSettings(): SettingsEntity = settingsDao.get() ?: DEFAULT_SETTINGS

    private suspend fun updateSettings(transform: (SettingsEntity) -> SettingsEntity) {
        val cur = currentSettings()
        settingsDao.upsert(transform(cur))
        notifyChanged()
    }

    suspend fun ensureSettings() {
        if (settingsDao.get() == null) {
            settingsDao.upsert(DEFAULT_SETTINGS)
        }
    }

    suspend fun currentClassId(): String? = currentSettings().currentClassId

    // ---------- 首次引导 ----------

    suspend fun completeOnboarding(
        name: String,
        note: String,
        prefix: String,
        digits: Int,
        recycle: Boolean
    ): String {
        val clsId = Ids.new()
        val t = now()
        db.withTransaction {
            classDao.upsert(ClassEntity(clsId, name.trim(), note.trim(), prefix.trim(), digits.coerceIn(1, 4), recycle, t, t))
            settingsDao.upsert(currentSettings().copy(onboarded = true, currentClassId = clsId))
        }
        notifyChanged()
        return clsId
    }

    // ---------- 班级 ----------

    suspend fun createClass(name: String, prefix: String, digits: Int, recycle: Boolean): String {
        val clsId = Ids.new()
        val t = now()
        db.withTransaction {
            classDao.upsert(ClassEntity(clsId, name.trim(), "", prefix.trim(), digits.coerceIn(1, 4), recycle, t, t))
            settingsDao.upsert(currentSettings().copy(currentClassId = clsId))
        }
        notifyChanged()
        return clsId
    }

    suspend fun switchClass(classId: String) {
        updateSettings { it.copy(currentClassId = classId) }
    }

    suspend fun renameClass(classId: String, name: String) {
        val c = classDao.getById(classId) ?: return
        classDao.upsert(c.copy(name = name.trim(), updatedAt = now()))
        notifyChanged()
    }

    suspend fun updateClassNote(classId: String, note: String) {
        val c = classDao.getById(classId) ?: return
        classDao.upsert(c.copy(note = note.trim(), updatedAt = now()))
        notifyChanged()
    }

    suspend fun updateClassRule(classId: String, prefix: String, digits: Int, recycle: Boolean) {
        val c = classDao.getById(classId) ?: return
        classDao.upsert(c.copy(prefix = prefix.trim(), digits = digits.coerceIn(1, 4), recycle = recycle, updatedAt = now()))
        notifyChanged()
    }

    suspend fun renumberClass(classId: String): Int {
        val list = studentDao.listByClass(classId)
        var changed = 0
        list.forEachIndexed { i, s ->
            if (s.seq != i + 1) {
                studentDao.upsert(s.copy(seq = i + 1, updatedAt = now()))
                changed++
            }
        }
        notifyChanged()
        return changed
    }

    suspend fun deleteClass(classId: String) {
        val wasCurrent = currentSettings().currentClassId == classId
        db.withTransaction {
            recordDao.deleteByClass(classId)
            studentDao.deleteByClass(classId)
            assignmentDao.deleteByClass(classId)
            classDao.delete(classId)
        }
        if (wasCurrent) {
            val remaining = classDao.listAll().firstOrNull()?.id
            updateSettings { it.copy(currentClassId = remaining) }
        }
        notifyChanged()
    }

    // ---------- 学生 ----------

    suspend fun addStudent(classId: String, name: String): String {
        val id = Ids.new()
        val t = now()
        val cls = classDao.getById(classId) ?: return ""
        val seqs = studentDao.listByClass(classId).map { it.seq }.toSet()
        val seq = nextSeq(cls.recycle, seqs)
        studentDao.upsert(StudentEntity(id, classId, seq, name.trim(), "", t, t))
        notifyChanged()
        return id
    }

    suspend fun importStudents(classId: String, items: List<Pair<String, String>>): Int {
        if (items.isEmpty()) return 0
        val cls = classDao.getById(classId) ?: return 0
        val t = now()
        var seqs = studentDao.listByClass(classId).map { it.seq }.toMutableSet()
        val newList = mutableListOf<StudentEntity>()
        items.forEach { (name, note) ->
            val seq = nextSeq(cls.recycle, seqs)
            seqs.add(seq)
            newList.add(StudentEntity(Ids.new(), classId, seq, name, note, t, t))
        }
        studentDao.upsertAll(newList)
        notifyChanged()
        return newList.size
    }

    private fun nextSeq(recycle: Boolean, used: Set<Int>): Int {
        if (recycle) {
            var n = 1
            while (n in used) n++
            return n
        }
        return (used.maxOrNull() ?: 0) + 1
    }

    suspend fun updateStudentName(studentId: String, name: String) {
        val s = studentDao.getById(studentId) ?: return
        studentDao.upsert(s.copy(name = name.trim(), updatedAt = now()))
        notifyChanged()
    }

    suspend fun updateStudentNote(studentId: String, note: String) {
        val s = studentDao.getById(studentId) ?: return
        studentDao.upsert(s.copy(note = note.trim(), updatedAt = now()))
        notifyChanged()
    }

    /** 返回错误信息；成功返回 null。 */
    suspend fun updateStudentSeq(studentId: String, seq: Int): String? {
        if (seq < 1 || seq > 9999) return "序号需在 1-9999 之间"
        val s = studentDao.getById(studentId) ?: return "未找到该学生"
        if (seq == s.seq) return null
        val dup = studentDao.listByClass(s.classId).firstOrNull { it.id != studentId && it.seq == seq }
        if (dup != null) return "序号 $seq 已被${dup.name}占用"
        studentDao.upsert(s.copy(seq = seq, updatedAt = now()))
        notifyChanged()
        return null
    }

    suspend fun deleteStudent(studentId: String) {
        db.withTransaction {
            recordDao.deleteByStudent(studentId)
            studentDao.delete(studentId)
        }
        notifyChanged()
    }

    // ---------- 作业 ----------

    suspend fun createAssignment(classId: String, title: String): String {
        val id = Ids.new()
        val t = now()
        assignmentDao.upsert(AssignmentEntity(id, classId, title.trim(), com.hwt.teacher.util.DateUtil.today(), "", t, t))
        notifyChanged()
        return id
    }

    suspend fun updateAssignment(assignmentId: String, title: String? = null, assignedDate: String? = null) {
        val a = assignmentDao.getById(assignmentId) ?: return
        assignmentDao.upsert(
            a.copy(
                title = title?.trim()?.ifEmpty { a.title } ?: a.title,
                assignedDate = assignedDate ?: a.assignedDate,
                updatedAt = now()
            )
        )
        notifyChanged()
    }

    suspend fun deleteAssignment(assignmentId: String) {
        db.withTransaction {
            recordDao.deleteByAssignment(assignmentId)
            assignmentDao.delete(assignmentId)
        }
        notifyChanged()
    }

    // ---------- 记录 ----------

    suspend fun writeRecord(assignmentId: String, studentId: String, patch: Map<String, String>) {
        val student = studentDao.getById(studentId) ?: return
        val cur = recordDao.get(assignmentId, studentId)
        val settings = currentSettings()
        val completion = patch["completion"] ?: cur?.completion ?: settings.defaultCompletion
        val correction = patch["correction"] ?: cur?.correction ?: settings.defaultCorrection
        val grade = if (patch.containsKey("grade")) patch["grade"] ?: Grade.NONE
        else cur?.grade ?: Grade.NONE
        recordDao.upsert(
            RecordEntity(
                id = cur?.id ?: Ids.new(),
                assignmentId = assignmentId,
                studentId = studentId,
                classId = student.classId,
                completion = completion,
                correction = correction,
                grade = grade,
                comment = cur?.comment,
                updatedAt = now()
            )
        )
        notifyChanged()
    }

    /** 批量应用到全班：为尚无记录的学生一次性落库（FR-5.3）。 */
    suspend fun bulkApply(assignmentId: String, classId: String, patch: Map<String, String>) {
        val students = studentDao.listByClass(classId)
        val existing = recordDao.listByAssignment(assignmentId).associateBy { it.studentId }
        val settings = currentSettings()
        val t = now()
        val newRecords = students.map { s ->
            val cur = existing[s.id]
            val completion = patch["completion"] ?: cur?.completion ?: settings.defaultCompletion
            val correction = patch["correction"] ?: cur?.correction ?: settings.defaultCorrection
            val grade = if (patch.containsKey("grade")) patch["grade"] ?: Grade.NONE
            else cur?.grade ?: Grade.NONE
            RecordEntity(
                id = cur?.id ?: Ids.new(),
                assignmentId = assignmentId,
                studentId = s.id,
                classId = classId,
                completion = completion,
                correction = correction,
                grade = grade,
                comment = cur?.comment,
                updatedAt = t
            )
        }
        recordDao.upsertAll(newRecords)
        notifyChanged()
    }

    // ---------- 设置 ----------

    suspend fun updateDefaults(completion: String, correction: String) {
        updateSettings { it.copy(defaultCompletion = completion, defaultCorrection = correction) }
    }

    suspend fun updateQrParams(perRow: Int, margin: Int, level: String, withNo: Boolean) {
        updateSettings { it.copy(qrPerRow = perRow, qrMargin = margin, qrLevel = level, qrWithNo = withNo) }
    }

    suspend fun updateAutoBackup(on: Boolean) {
        updateSettings { it.copy(autoBackup = on) }
    }

    suspend fun webdavPassword(): String = passwordStore.get()

    suspend fun saveWebdavConfig(enabled: Boolean, url: String, username: String, password: String) {
        passwordStore.set(password)
        updateSettings {
            it.copy(
                webdavEnabled = enabled,
                webdavUrl = url.trim(),
                webdavUsername = username.trim(),
                webdavTested = null,
                webdavLastError = null
            )
        }
    }

    suspend fun setWebdavTested(result: String?, error: String? = null) {
        updateSettings { it.copy(webdavTested = result, webdavLastError = error) }
    }

    suspend fun setBackupError(err: String?) {
        updateSettings { it.copy(webdavLastError = err) }
    }

    suspend fun markBackupUploaded() {
        updateSettings { it.copy(lastBackupAt = System.currentTimeMillis(), webdavLastError = null) }
    }

    // ---------- 备份 ----------

    suspend fun exportBackup(): String {
        val settings = currentSettings()
        return com.hwt.teacher.util.BackupManager.toJson(
            settings,
            classDao.listAll(),
            studentDao.listAll(),
            assignmentDao.listAll(),
            recordDao.listAll()
        )
    }

    suspend fun importBackup(json: String): Boolean {
        val file = com.hwt.teacher.util.BackupManager.parse(json) ?: return false
        val data = file.data ?: return false
        db.withTransaction {
            recordDao.deleteAll()
            settingsDao.upsert(com.hwt.teacher.util.BackupManager.toSettings(data.settings.firstOrNull() ?: com.hwt.teacher.util.SettingsDto()))
            data.classes.map { com.hwt.teacher.util.BackupManager.toClass(it) }.forEach { classDao.upsert(it) }
            data.students.map { com.hwt.teacher.util.BackupManager.toStudent(it) }.forEach { studentDao.upsert(it) }
            data.assignments.map { com.hwt.teacher.util.BackupManager.toAssignment(it) }.forEach { assignmentDao.upsert(it) }
            data.records.map { com.hwt.teacher.util.BackupManager.toRecord(it) }.forEach { recordDao.upsert(it) }
        }
        notifyChanged()
        return true
    }

    // ---------- 数据导出快照（报表导出） ----------

    suspend fun snapshotStudents(classId: String): List<StudentEntity> = studentDao.listByClass(classId)

    suspend fun snapshotClass(classId: String): ClassEntity? = classDao.getById(classId)

    // ---------- 扫码解析（附录 A） ----------

    sealed class ScanResolution {
        object NotOurs : ScanResolution()
        object WrongClass : ScanResolution()
        object NotFound : ScanResolution()
        data class Student(val studentId: String, val name: String) : ScanResolution()
    }

    suspend fun resolveScan(assignmentId: String, raw: String): ScanResolution {
        val a = assignmentDao.getById(assignmentId) ?: return ScanResolution.NotFound
        val parsed = com.hwt.teacher.util.QrCodec.parse(raw) ?: return ScanResolution.NotOurs
        if (parsed.classId != a.classId) return ScanResolution.WrongClass
        val student = if (parsed.studentId != null) {
            studentDao.getById(parsed.studentId)?.takeIf { it.classId == a.classId }
        } else {
            val cls = classDao.getById(a.classId)
            val code = parsed.code
            val seq = code.toIntOrNull()
                ?: cls?.prefix?.let { p -> if (code.startsWith(p)) code.removePrefix(p).toIntOrNull() else null }
            if (seq != null) studentDao.bySeq(a.classId, seq) else null
        }
        if (student == null) return ScanResolution.NotFound
        return ScanResolution.Student(student.id, student.name)
    }
}
