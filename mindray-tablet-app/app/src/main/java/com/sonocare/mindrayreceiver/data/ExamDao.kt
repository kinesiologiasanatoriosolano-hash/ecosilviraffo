package com.sonocare.mindrayreceiver.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ExamDao {

    @Query("SELECT * FROM exams ORDER BY receivedAt DESC")
    fun observeAll(): LiveData<List<Exam>>

    @Query("SELECT * FROM exams ORDER BY receivedAt DESC")
    suspend fun getAll(): List<Exam>

    @Query("SELECT * FROM exams WHERE examId = :examId LIMIT 1")
    suspend fun getById(examId: String): Exam?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exam: Exam)

    @Update
    suspend fun update(exam: Exam)

    @Query("DELETE FROM exams WHERE examId = :examId")
    suspend fun delete(examId: String)
}
