package com.hwt.teacher.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 'app'")
    fun observe(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 'app'")
    suspend fun get(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(s: SettingsEntity)
}

@Dao
interface ClassDao {
    @Query("SELECT * FROM classes ORDER BY createdAt ASC, name ASC")
    fun observeAll(): Flow<List<ClassEntity>>

    @Query("SELECT * FROM classes WHERE id = :id")
    suspend fun getById(id: String): ClassEntity?

    @Query("SELECT * FROM classes WHERE id = :id")
    fun observeById(id: String): Flow<ClassEntity?>

    @Query("SELECT * FROM classes ORDER BY createdAt ASC, name ASC")
    suspend fun listAll(): List<ClassEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(c: ClassEntity)

    @Query("DELETE FROM classes WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM classes")
    suspend fun deleteAll()
}

@Dao
interface StudentDao {
    @Query("SELECT * FROM students WHERE classId = :classId ORDER BY seq ASC")
    fun observeByClass(classId: String): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE classId = :classId ORDER BY seq ASC")
    suspend fun listByClass(classId: String): List<StudentEntity>

    @Query("SELECT * FROM students ORDER BY classId, seq ASC")
    fun observeAll(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students ORDER BY classId, seq ASC")
    suspend fun listAll(): List<StudentEntity>

    @Query("SELECT * FROM students WHERE id = :id")
    suspend fun getById(id: String): StudentEntity?

    @Query("SELECT * FROM students WHERE id = :id")
    fun observeById(id: String): Flow<StudentEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(s: StudentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<StudentEntity>)

    @Query("DELETE FROM students WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM students WHERE classId = :classId")
    suspend fun deleteByClass(classId: String)

    @Query("DELETE FROM students")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM students WHERE classId = :classId")
    suspend fun countByClass(classId: String): Int

    @Query("SELECT MAX(seq) FROM students WHERE classId = :classId")
    suspend fun maxSeq(classId: String): Int?

    @Query("SELECT * FROM students WHERE classId = :classId AND seq = :seq")
    suspend fun bySeq(classId: String, seq: Int): StudentEntity?

    @Query("SELECT name FROM students WHERE classId = :classId")
    suspend fun names(classId: String): List<String>
}

@Dao
interface AssignmentDao {
    @Query("SELECT * FROM assignments WHERE classId = :classId ORDER BY assignedDate DESC, createdAt DESC")
    fun observeByClass(classId: String): Flow<List<AssignmentEntity>>

    @Query("SELECT * FROM assignments WHERE classId = :classId ORDER BY assignedDate DESC, createdAt DESC")
    suspend fun listByClass(classId: String): List<AssignmentEntity>

    @Query("SELECT * FROM assignments ORDER BY assignedDate DESC, createdAt DESC")
    fun observeAll(): Flow<List<AssignmentEntity>>

    @Query("SELECT * FROM assignments ORDER BY assignedDate DESC, createdAt DESC")
    suspend fun listAll(): List<AssignmentEntity>

    @Query("SELECT * FROM assignments WHERE id = :id")
    fun observeById(id: String): Flow<AssignmentEntity?>

    @Query("SELECT * FROM assignments WHERE id = :id")
    suspend fun getById(id: String): AssignmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(a: AssignmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<AssignmentEntity>)

    @Query("DELETE FROM assignments WHERE classId = :classId")
    suspend fun deleteByClass(classId: String)

    @Query("DELETE FROM assignments WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM assignments")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM assignments WHERE classId = :classId")
    suspend fun countByClass(classId: String): Int
}

@Dao
interface RecordDao {
    @Query("SELECT * FROM records WHERE assignmentId = :assignmentId")
    fun observeByAssignment(assignmentId: String): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE classId = :classId")
    fun observeByClass(classId: String): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE assignmentId = :assignmentId")
    suspend fun listByAssignment(assignmentId: String): List<RecordEntity>

    @Query("SELECT * FROM records WHERE classId = :classId")
    suspend fun listByClass(classId: String): List<RecordEntity>

    @Query("SELECT * FROM records")
    fun observeAll(): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records")
    suspend fun listAll(): List<RecordEntity>

    @Query("SELECT * FROM records WHERE assignmentId = :assignmentId AND studentId = :studentId")
    suspend fun get(assignmentId: String, studentId: String): RecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(r: RecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<RecordEntity>)

    @Query("DELETE FROM records WHERE studentId = :studentId")
    suspend fun deleteByStudent(studentId: String)

    @Query("DELETE FROM records WHERE assignmentId = :assignmentId")
    suspend fun deleteByAssignment(assignmentId: String)

    @Query("DELETE FROM records WHERE classId = :classId")
    suspend fun deleteByClass(classId: String)

    @Query("DELETE FROM records")
    suspend fun deleteAll()
}
