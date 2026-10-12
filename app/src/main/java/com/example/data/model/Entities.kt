package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class Subject(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val code: String,
  val colorHex: String,
  val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "timetable_sessions")
data class TimetableSession(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val subjectId: Long,
  val subjectName: String,
  val topic: String,
  val date: String, // "YYYY-MM-DD"
  val startTime: String, // "HH:mm"
  val endTime: String, // "HH:mm"
  val location: String = "Campus Lecture Hall",
  val notes: String = "",
  val recurringDays: String = "", // e.g., "Mon,Wed,Fri" or empty
  val reminderMinutes: Int = 15,
  val status: String = "SCHEDULED", // SCHEDULED, COMPLETED, MISSED, RESCHEDULED
  val isAlarmEnabled: Boolean = true,
  val googleCalendarEventId: String? = null
)

@Entity(tableName = "study_topics")
data class StudyTopic(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val subjectId: Long,
  val subjectName: String,
  val title: String,
  val dateStudied: String, // "YYYY-MM-DD"
  val descriptionNotes: String,
  val sourceType: String = "MANUAL", // MANUAL, UPLOAD, PASTED
  val uploadedFileName: String? = null,
  val masteryLevel: String = "UNTESTED", // UNTESTED, STRUGGLING, FAMILIAR, MASTERED
  val repetitionIntervalDays: Int = 1,
  val nextRevisionDate: String, // "YYYY-MM-DD"
  val timesTested: Int = 0,
  val timesCorrect: Int = 0,
  val lastTestedDate: String? = null,
  val aiSummary: String? = null
)

@Entity(tableName = "quiz_questions")
data class QuizQuestion(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val topicId: Long,
  val subjectName: String,
  val topicTitle: String,
  val questionType: String, // MULTIPLE_CHOICE, TRUE_FALSE, SHORT_ANSWER, CODING
  val difficulty: String = "MEDIUM", // EASY, MEDIUM, HARD
  val questionText: String,
  val optionsJson: String = "", // Comma/pipe separated or JSON list
  val correctAnswer: String,
  val explanation: String,
  val codeStarter: String? = null
)

@Entity(tableName = "quiz_attempts")
data class QuizAttempt(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val title: String,
  val dateAttempted: String, // "YYYY-MM-DD HH:mm"
  val totalQuestions: Int,
  val correctCount: Int,
  val scorePercentage: Int,
  val topicsCoveredCount: Int,
  val topicsSkippedCount: Int,
  val detailsJson: String = "" // Serialized answer breakdown
)

@Entity(tableName = "study_tasks")
data class StudyTask(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val subjectName: String,
  val title: String,
  val dueDate: String, // "YYYY-MM-DD"
  val type: String = "ASSIGNMENT", // ASSIGNMENT, EXAM, TASK
  val isCompleted: Boolean = false,
  val priority: String = "MEDIUM" // HIGH, MEDIUM, LOW
)

data class QuizConfig(
  val quizDays: String = "Saturday & Sunday",
  val quizTime: String = "10:00 AM",
  val questionCount: Int = 10,
  val difficulty: String = "MIXED", // EASY, MEDIUM, HARD, MIXED
  val questionTypes: Set<String> = setOf("MULTIPLE_CHOICE", "TRUE_FALSE", "SHORT_ANSWER", "CODING"),
  val prioritizeWeakTopics: Boolean = true
)

@Entity(tableName = "tutor_sessions")
data class TutorSession(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val subjectId: Long? = null,
  val subjectName: String = "General",
  val title: String,
  val previewSnippet: String,
  val modelUsed: String = "gemini-3.5-flash",
  val searchGroundingUsed: Boolean = false,
  val messageCount: Int = 0,
  val createdAt: Long = System.currentTimeMillis(),
  val lastUpdatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tutor_messages")
data class TutorMessageEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val sessionId: Long,
  val role: String, // "user" or "model"
  val text: String,
  val timestamp: Long = System.currentTimeMillis(),
  val modelName: String = "gemini-3.5-flash",
  val searchGroundingUsed: Boolean = false,
  val groundingSourcesJson: String = "",
  val searchQueriesJson: String = "",
  val isError: Boolean = false
)

@Entity(tableName = "schedule_formats")
data class ScheduleFormat(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val description: String = "",
  val formatSessionsJson: String = "[]", // List of SessionTemplate items serialized to JSON
  val createdAt: Long = System.currentTimeMillis()
)

