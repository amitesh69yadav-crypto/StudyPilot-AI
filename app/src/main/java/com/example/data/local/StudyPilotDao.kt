package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.QuizAttempt
import com.example.data.model.QuizQuestion
import com.example.data.model.StudyTask
import com.example.data.model.StudyTopic
import com.example.data.model.Subject
import com.example.data.model.TimetableSession
import com.example.data.model.TutorMessageEntity
import com.example.data.model.TutorSession
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyPilotDao {

  // --- Subjects ---
  @Query("SELECT * FROM subjects ORDER BY name ASC")
  fun getAllSubjects(): Flow<List<Subject>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSubject(subject: Subject): Long

  @Delete
  suspend fun deleteSubject(subject: Subject)

  // --- Timetable Sessions ---
  @Query("SELECT * FROM timetable_sessions ORDER BY date ASC, startTime ASC")
  fun getAllSessions(): Flow<List<TimetableSession>>

  @Query("SELECT * FROM timetable_sessions ORDER BY date ASC, startTime ASC")
  suspend fun getAllSessionsList(): List<TimetableSession>

  @Query("SELECT * FROM timetable_sessions WHERE date = :date ORDER BY startTime ASC")
  fun getSessionsForDate(date: String): Flow<List<TimetableSession>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSession(session: TimetableSession): Long

  @Update
  suspend fun updateSession(session: TimetableSession)

  @Query("DELETE FROM timetable_sessions WHERE id = :id")
  suspend fun deleteSessionById(id: Long)

  // --- Study Topics ---
  @Query("SELECT * FROM study_topics ORDER BY dateStudied DESC, id DESC")
  fun getAllTopics(): Flow<List<StudyTopic>>

  @Query("SELECT * FROM study_topics WHERE subjectId = :subjectId")
  fun getTopicsForSubject(subjectId: Long): Flow<List<StudyTopic>>

  @Query("SELECT * FROM study_topics WHERE id = :id LIMIT 1")
  suspend fun getTopicById(id: Long): StudyTopic?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTopic(topic: StudyTopic): Long

  @Update
  suspend fun updateTopic(topic: StudyTopic)

  @Query("DELETE FROM study_topics WHERE id = :id")
  suspend fun deleteTopicById(id: Long)

  // --- Quiz Questions ---
  @Query("SELECT * FROM quiz_questions WHERE topicId = :topicId")
  fun getQuestionsForTopic(topicId: Long): Flow<List<QuizQuestion>>

  @Query("SELECT * FROM quiz_questions")
  fun getAllQuestions(): Flow<List<QuizQuestion>>

  @Query("SELECT * FROM quiz_questions WHERE topicId IN (:topicIds)")
  suspend fun getQuestionsForTopicIds(topicIds: List<Long>): List<QuizQuestion>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertQuestions(questions: List<QuizQuestion>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertQuestion(question: QuizQuestion): Long

  // --- Quiz Attempts ---
  @Query("SELECT * FROM quiz_attempts ORDER BY id DESC")
  fun getAllQuizAttempts(): Flow<List<QuizAttempt>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertQuizAttempt(attempt: QuizAttempt): Long

  // --- Study Tasks & Exams ---
  @Query("SELECT * FROM study_tasks ORDER BY dueDate ASC")
  fun getAllTasks(): Flow<List<StudyTask>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTask(task: StudyTask): Long

  @Update
  suspend fun updateTask(task: StudyTask)

  @Query("DELETE FROM study_tasks WHERE id = :id")
  suspend fun deleteTaskById(id: Long)

  // Batch insert for sample data
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSubjects(subjects: List<Subject>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSessions(sessions: List<TimetableSession>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTopics(topics: List<StudyTopic>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTasks(tasks: List<StudyTask>)

  // --- AI Tutor Sessions & Messages ---
  @Query("SELECT * FROM tutor_sessions ORDER BY lastUpdatedAt DESC")
  fun getAllTutorSessions(): Flow<List<TutorSession>>

  @Query("SELECT * FROM tutor_sessions WHERE subjectId = :subjectId ORDER BY lastUpdatedAt DESC")
  fun getTutorSessionsForSubject(subjectId: Long): Flow<List<TutorSession>>

  @Query("SELECT * FROM tutor_sessions WHERE id = :id LIMIT 1")
  suspend fun getTutorSessionById(id: Long): TutorSession?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTutorSession(session: TutorSession): Long

  @Update
  suspend fun updateTutorSession(session: TutorSession)

  @Query("DELETE FROM tutor_sessions WHERE id = :id")
  suspend fun deleteTutorSessionById(id: Long)

  @Query("DELETE FROM tutor_sessions")
  suspend fun clearAllTutorSessions()

  @Query("SELECT * FROM tutor_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
  fun getMessagesForSession(sessionId: Long): Flow<List<TutorMessageEntity>>

  @Query("SELECT * FROM tutor_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
  suspend fun getMessagesForSessionSync(sessionId: Long): List<TutorMessageEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTutorMessage(message: TutorMessageEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTutorMessages(messages: List<TutorMessageEntity>)

  @Query("DELETE FROM tutor_messages WHERE sessionId = :sessionId")
  suspend fun deleteMessagesForSession(sessionId: Long)

  @Query("DELETE FROM tutor_messages")
  suspend fun clearAllTutorMessages()

  // --- Schedule Formats / Templates ---
  @Query("SELECT * FROM schedule_formats ORDER BY createdAt ASC")
  fun getAllScheduleFormats(): Flow<List<com.example.data.model.ScheduleFormat>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertScheduleFormat(format: com.example.data.model.ScheduleFormat): Long

  @Query("DELETE FROM schedule_formats WHERE id = :id")
  suspend fun deleteScheduleFormatById(id: Long)

  @Query("DELETE FROM timetable_sessions WHERE date = :date")
  suspend fun deleteSessionsForDate(date: String)
}

