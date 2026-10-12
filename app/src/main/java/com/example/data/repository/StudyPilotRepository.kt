package com.example.data.repository

import com.example.data.local.StudyPilotDao
import com.example.data.model.QuizAttempt
import com.example.data.model.QuizConfig
import com.example.data.model.QuizQuestion
import com.example.data.model.StudyTask
import com.example.data.model.StudyTopic
import com.example.data.model.Subject
import com.example.data.model.TimetableSession
import com.example.data.model.TutorMessageEntity
import com.example.data.model.TutorSession
import com.example.data.remote.EvaluationResult
import com.example.data.remote.GeminiStudyService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StudyPilotRepository(
  private val dao: StudyPilotDao,
  private val geminiService: GeminiStudyService = GeminiStudyService()
) {

  val allSubjects: Flow<List<Subject>> = dao.getAllSubjects()
  val allSessions: Flow<List<TimetableSession>> = dao.getAllSessions()
  val allTopics: Flow<List<StudyTopic>> = dao.getAllTopics()
  val allTasks: Flow<List<StudyTask>> = dao.getAllTasks()
  val allQuizAttempts: Flow<List<QuizAttempt>> = dao.getAllQuizAttempts()
  val allTutorSessions: Flow<List<TutorSession>> = dao.getAllTutorSessions()
  val allScheduleFormats: Flow<List<com.example.data.model.ScheduleFormat>> = dao.getAllScheduleFormats()

  private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
  private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

  fun getTodayDateString(): String = dateFormat.format(Date())

  // --- CRUD Operations ---
  suspend fun addSubject(name: String, code: String, colorHex: String): Long {
    return dao.insertSubject(Subject(name = name, code = code, colorHex = colorHex))
  }

  suspend fun deleteSubject(subject: Subject) {
    dao.deleteSubject(subject)
  }

  suspend fun addSession(session: TimetableSession): Long {
    return dao.insertSession(session)
  }

  suspend fun updateSession(session: TimetableSession) {
    dao.updateSession(session)
  }

  suspend fun deleteSession(id: Long) {
    dao.deleteSessionById(id)
  }

  suspend fun clearSessionsForDate(date: String) {
    dao.deleteSessionsForDate(date)
  }

  suspend fun addScheduleFormat(format: com.example.data.model.ScheduleFormat): Long {
    return dao.insertScheduleFormat(format)
  }

  suspend fun deleteScheduleFormat(id: Long) {
    dao.deleteScheduleFormatById(id)
  }


  suspend fun addTopic(topic: StudyTopic): Long {
    return dao.insertTopic(topic)
  }

  suspend fun updateTopic(topic: StudyTopic) {
    dao.updateTopic(topic)
  }

  suspend fun deleteTopic(id: Long) {
    dao.deleteTopicById(id)
  }

  suspend fun addTask(task: StudyTask): Long {
    return dao.insertTask(task)
  }

  suspend fun updateTask(task: StudyTask) {
    dao.updateTask(task)
  }

  suspend fun deleteTask(id: Long) {
    dao.deleteTaskById(id)
  }

  // --- AI Summarization & Chat ---
  suspend fun summarizeMaterial(title: String, rawContent: String): String {
    return geminiService.summarizeMaterial(title, rawContent)
  }

  suspend fun chatWithTutor(
    history: List<com.example.data.model.ChatMessage>,
    model: com.example.data.model.TutorModel,
    useGoogleSearch: Boolean,
    systemInstruction: String
  ): com.example.data.remote.ChatResponse {
    return geminiService.chatWithGemini(history, model, useGoogleSearch, systemInstruction)
  }

  // --- AI Tutor History & Persistence ---
  fun getTutorSessionsForSubject(subjectId: Long): Flow<List<TutorSession>> {
    return dao.getTutorSessionsForSubject(subjectId)
  }

  suspend fun getTutorSessionById(id: Long): TutorSession? {
    return dao.getTutorSessionById(id)
  }

  suspend fun getMessagesForSessionSync(sessionId: Long): List<TutorMessageEntity> {
    return dao.getMessagesForSessionSync(sessionId)
  }

  fun getMessagesForSession(sessionId: Long): Flow<List<TutorMessageEntity>> {
    return dao.getMessagesForSession(sessionId)
  }

  suspend fun createTutorSession(
    subjectId: Long?,
    subjectName: String,
    title: String,
    previewSnippet: String,
    modelUsed: String,
    searchGroundingUsed: Boolean
  ): Long {
    val session = TutorSession(
      subjectId = subjectId,
      subjectName = subjectName,
      title = title,
      previewSnippet = previewSnippet,
      modelUsed = modelUsed,
      searchGroundingUsed = searchGroundingUsed,
      messageCount = 0,
      createdAt = System.currentTimeMillis(),
      lastUpdatedAt = System.currentTimeMillis()
    )
    return dao.insertTutorSession(session)
  }

  suspend fun saveTutorMessage(
    sessionId: Long,
    role: String,
    text: String,
    modelName: String,
    searchGroundingUsed: Boolean,
    groundingSourcesJson: String = "",
    searchQueriesJson: String = "",
    isError: Boolean = false
  ): Long {
    val msgId = dao.insertTutorMessage(
      TutorMessageEntity(
        sessionId = sessionId,
        role = role,
        text = text,
        timestamp = System.currentTimeMillis(),
        modelName = modelName,
        searchGroundingUsed = searchGroundingUsed,
        groundingSourcesJson = groundingSourcesJson,
        searchQueriesJson = searchQueriesJson,
        isError = isError
      )
    )
    val session = dao.getTutorSessionById(sessionId)
    if (session != null) {
      val snippet = if (role == "model") text.take(160) else session.previewSnippet
      dao.updateTutorSession(
        session.copy(
          messageCount = session.messageCount + 1,
          previewSnippet = snippet,
          lastUpdatedAt = System.currentTimeMillis()
        )
      )
    }
    return msgId
  }

  suspend fun deleteTutorSession(sessionId: Long) {
    dao.deleteMessagesForSession(sessionId)
    dao.deleteTutorSessionById(sessionId)
  }

  suspend fun clearAllTutorHistory() {
    dao.clearAllTutorMessages()
    dao.clearAllTutorSessions()
  }

  // --- Quiz Generation & Assembly ---
  data class GeneratedQuiz(
    val questions: List<QuizQuestion>,
    val testedTopics: List<StudyTopic>,
    val queuedUntestedTopics: List<StudyTopic>,
    val isAllCovered: Boolean
  )

  suspend fun assembleWeekendQuiz(
    config: QuizConfig,
    filterSubjectId: Long? = null,
    forceWeakOnly: Boolean = false
  ): GeneratedQuiz {
    val allTopicsList = dao.getAllTopics().first()
    if (allTopicsList.isEmpty()) {
      return GeneratedQuiz(emptyList(), emptyList(), emptyList(), isAllCovered = true)
    }

    val eligibleTopics = if (filterSubjectId != null) {
      allTopicsList.filter { it.subjectId == filterSubjectId }
    } else {
      allTopicsList
    }

    if (eligibleTopics.isEmpty()) {
      return GeneratedQuiz(emptyList(), emptyList(), emptyList(), isAllCovered = true)
    }

    // Sort topics by priority:
    // 1. Weak / Struggling topics
    // 2. Overdue / Never tested topics
    // 3. Spaced repetition interval reached
    val sortedTopics = eligibleTopics.sortedWith(
      compareByDescending<StudyTopic> { topic ->
        var priorityScore = 0
        if (topic.masteryLevel == "STRUGGLING") priorityScore += 50
        if (topic.timesTested == 0) priorityScore += 40
        if (topic.timesTested > 0 && topic.timesCorrect < topic.timesTested) priorityScore += 30
        if (topic.nextRevisionDate <= getTodayDateString()) priorityScore += 20
        priorityScore
      }.thenBy { it.lastTestedDate ?: "1970-01-01" }
    )

    val targetTopicList = if (forceWeakOnly) {
      sortedTopics.filter { it.masteryLevel == "STRUGGLING" || it.timesCorrect < it.timesTested }
        .ifEmpty { sortedTopics }
    } else {
      sortedTopics
    }

    // Determine how many topics we can cover based on questionCount
    val maxQuestions = config.questionCount.coerceAtLeast(1)
    val questionsPerTopic = if (targetTopicList.size >= maxQuestions) 1 else (maxQuestions / targetTopicList.size).coerceAtLeast(1)

    val selectedTopics = mutableListOf<StudyTopic>()
    val assembledQuestions = mutableListOf<QuizQuestion>()

    for (topic in targetTopicList) {
      if (assembledQuestions.size >= maxQuestions) break

      // 1. Fetch cached questions from Room DB first (MINIMIZING AI CREDITS)
      val cached = dao.getQuestionsForTopicIds(listOf(topic.id))
      val usableCached = cached.filter { q ->
        (config.difficulty == "MIXED" || q.difficulty.equals(config.difficulty, ignoreCase = true)) &&
          (config.questionTypes.isEmpty() || config.questionTypes.contains(q.questionType))
      }

      val needed = (maxQuestions - assembledQuestions.size).coerceAtMost(questionsPerTopic)
      val chosenForTopic = mutableListOf<QuizQuestion>()

      if (usableCached.size >= needed) {
        chosenForTopic.addAll(usableCached.take(needed))
      } else {
        chosenForTopic.addAll(usableCached)
        val stillNeeded = needed - chosenForTopic.size
        if (stillNeeded > 0) {
          // Generate new questions and save in DB for future reuse
          val generated = geminiService.generateQuestionsForTopic(topic, stillNeeded, config.difficulty)
          if (generated.isNotEmpty()) {
            dao.insertQuestions(generated)
            chosenForTopic.addAll(generated.take(stillNeeded))
          }
        }
      }

      if (chosenForTopic.isNotEmpty()) {
        selectedTopics.add(topic)
        assembledQuestions.addAll(chosenForTopic)
      }
    }

    val selectedIds = selectedTopics.map { it.id }.toSet()
    val queuedUntested = eligibleTopics.filter { !selectedIds.contains(it.id) }
    val isAllCovered = queuedUntested.isEmpty()

    return GeneratedQuiz(
      questions = assembledQuestions,
      testedTopics = selectedTopics,
      queuedUntestedTopics = queuedUntested,
      isAllCovered = isAllCovered
    )
  }

  suspend fun evaluateUserAnswer(
    question: QuizQuestion,
    userAnswer: String
  ): EvaluationResult {
    return geminiService.evaluateAnswer(question, userAnswer)
  }

  // --- Spaced Repetition Updates ---
  suspend fun recordQuizResults(
    title: String,
    questions: List<QuizQuestion>,
    userAnswers: Map<Long, String>,
    evaluations: Map<Long, EvaluationResult>,
    topicsCoveredCount: Int,
    topicsSkippedCount: Int
  ): QuizAttempt {
    val total = questions.size
    val correctCount = questions.count { q ->
      evaluations[q.id]?.isCorrect == true
    }
    val scorePercentage = if (total > 0) (correctCount * 100 / total) else 0
    val today = getTodayDateString()

    // Group question results by topic to update spaced repetition
    val topicResults = mutableMapOf<Long, MutableList<Boolean>>()
    for (q in questions) {
      val isCorrect = evaluations[q.id]?.isCorrect ?: false
      topicResults.getOrPut(q.topicId) { mutableListOf() }.add(isCorrect)
    }

    for ((topicId, results) in topicResults) {
      val topic = dao.getTopicById(topicId) ?: continue
      val allCorrect = results.all { it }
      val totalTested = topic.timesTested + results.size
      val totalCorrect = topic.timesCorrect + results.count { it }

      // Spaced repetition interval algorithm: 1d -> 3d -> 7d -> 14d -> 30d
      val newInterval = if (allCorrect) {
        when (topic.repetitionIntervalDays) {
          1 -> 3
          3 -> 7
          7 -> 14
          14 -> 30
          else -> 30
        }
      } else {
        1 // Reset to 1 day on mistake
      }

      val newMastery = when {
        !allCorrect -> "STRUGGLING"
        totalCorrect >= 3 -> "MASTERED"
        else -> "FAMILIAR"
      }

      val cal = Calendar.getInstance()
      cal.add(Calendar.DAY_OF_YEAR, newInterval)
      val nextRevDate = dateFormat.format(cal.time)

      dao.updateTopic(
        topic.copy(
          timesTested = totalTested,
          timesCorrect = totalCorrect,
          repetitionIntervalDays = newInterval,
          masteryLevel = newMastery,
          nextRevisionDate = nextRevDate,
          lastTestedDate = today
        )
      )
    }

    val attempt = QuizAttempt(
      title = title,
      dateAttempted = dateTimeFormat.format(Date()),
      totalQuestions = total,
      correctCount = correctCount,
      scorePercentage = scorePercentage,
      topicsCoveredCount = topicsCoveredCount,
      topicsSkippedCount = topicsSkippedCount,
      detailsJson = ""
    )
    val id = dao.insertQuizAttempt(attempt)
    return attempt.copy(id = id)
  }

  // --- Seed Initial University Data ---
  suspend fun seedSampleUniversityData() {
    val existing = dao.getAllSubjects().first()
    if (existing.isNotEmpty()) return

    val csSub = Subject(name = "Data Structures & Algorithms", code = "CS201", colorHex = "#D86343")
    val mathSub = Subject(name = "Linear Algebra & Calculus", code = "MATH220", colorHex = "#F69176")
    val physSub = Subject(name = "Classical & Quantum Physics", code = "PHYS140", colorHex = "#EBCBB0")
    val engSub = Subject(name = "Software Engineering Principles", code = "CS310", colorHex = "#E9F4EE")

    val csId = dao.insertSubject(csSub)
    val mathId = dao.insertSubject(mathSub)
    val physId = dao.insertSubject(physSub)
    val engId = dao.insertSubject(engSub)

    val today = getTodayDateString()

    // Sessions
    val sessions = listOf(
      TimetableSession(
        subjectId = csId,
        subjectName = "Data Structures & Algorithms",
        topic = "Binary Search Trees & AVL Rotations",
        date = today,
        startTime = "09:00",
        endTime = "10:30",
        location = "Hall C, Turing Building",
        notes = "Bring laptop for live tree balancing exercise.",
        recurringDays = "Mon,Wed,Fri",
        reminderMinutes = 15,
        status = "SCHEDULED"
      ),
      TimetableSession(
        subjectId = mathId,
        subjectName = "Linear Algebra & Calculus",
        topic = "Eigenvalues & Eigenvector Transformations",
        date = today,
        startTime = "11:00",
        endTime = "12:30",
        location = "Lecture Theatre 2",
        notes = "Review Matrix diagonalization theorem.",
        recurringDays = "Tue,Thu",
        reminderMinutes = 30,
        status = "SCHEDULED"
      ),
      TimetableSession(
        subjectId = physId,
        subjectName = "Classical & Quantum Physics",
        topic = "Wave-Particle Duality & Schrödinger Eq",
        date = today,
        startTime = "14:00",
        endTime = "15:30",
        location = "Physics Lab 4",
        notes = "Laboratory experiment: Double slit interference.",
        recurringDays = "Wed",
        reminderMinutes = 15,
        status = "SCHEDULED"
      )
    )
    dao.insertSessions(sessions)

    // Default Pre-built Schedule Formats
    val standardDayJson = org.json.JSONArray().apply {
      put(org.json.JSONObject().apply {
        put("subjectId", csId)
        put("subjectName", "Data Structures & Algorithms")
        put("topic", "Core Computer Science Lecture & Lab")
        put("startTime", "09:00")
        put("endTime", "10:30")
        put("location", "Hall C, Turing Building")
        put("notes", "Morning lecture & live coding lab")
        put("isAlarmEnabled", true)
      })
      put(org.json.JSONObject().apply {
        put("subjectId", mathId)
        put("subjectName", "Linear Algebra & Calculus")
        put("topic", "Theoretical Proofs & Practice Exercises")
        put("startTime", "11:00")
        put("endTime", "12:30")
        put("location", "Lecture Theatre 2")
        put("notes", "Matrix diagonalization & homework review")
        put("isAlarmEnabled", true)
      })
      put(org.json.JSONObject().apply {
        put("subjectId", physId)
        put("subjectName", "Classical & Quantum Physics")
        put("topic", "Laboratory Experiment & Group Problem Set")
        put("startTime", "14:00")
        put("endTime", "15:30")
        put("location", "Physics Lab 4")
        put("notes", "Hands-on measurement and lab report prep")
        put("isAlarmEnabled", true)
      })
      put(org.json.JSONObject().apply {
        put("subjectId", csId)
        put("subjectName", "Study Sprint")
        put("topic", "Active Recall & Evening AI Revision")
        put("startTime", "16:30")
        put("endTime", "18:00")
        put("location", "Library Quiet Area")
        put("notes", "Practice questions with AI tutor")
        put("isAlarmEnabled", true)
      })
    }.toString()

    val deepWorkJson = org.json.JSONArray().apply {
      put(org.json.JSONObject().apply {
        put("subjectId", csId)
        put("subjectName", "Exam Focus")
        put("topic", "Deep Work Sprint 1: Past Papers & Quizzes")
        put("startTime", "09:30")
        put("endTime", "11:30")
        put("location", "Home Study Desk")
        put("notes", "Zero distraction exam conditions")
        put("isAlarmEnabled", true)
      })
      put(org.json.JSONObject().apply {
        put("subjectId", mathId)
        put("subjectName", "Problem Solving")
        put("topic", "Deep Work Sprint 2: Hard Problem Sets")
        put("startTime", "13:00")
        put("endTime", "15:00")
        put("location", "University Library Floor 3")
        put("notes", "Step-by-step proofs and calculations")
        put("isAlarmEnabled", true)
      })
      put(org.json.JSONObject().apply {
        put("subjectId", engId)
        put("subjectName", "Review & Consolidation")
        put("topic", "Deep Work Sprint 3: AI Tutor Doubt Clearing")
        put("startTime", "16:00")
        put("endTime", "17:30")
        put("location", "Quiet Study Pod")
        put("notes", "Review weak areas with AI tutor")
        put("isAlarmEnabled", true)
      })
    }.toString()

    val lightReviewJson = org.json.JSONArray().apply {
      put(org.json.JSONObject().apply {
        put("subjectId", csId)
        put("subjectName", "Light Study")
        put("topic", "Morning Reading & Concept Mapping")
        put("startTime", "10:00")
        put("endTime", "11:30")
        put("location", "Campus Café")
        put("notes", "High-level summary review")
        put("isAlarmEnabled", true)
      })
      put(org.json.JSONObject().apply {
        put("subjectId", mathId)
        put("subjectName", "Light Study")
        put("topic", "Afternoon Problem Solving Warmup")
        put("startTime", "15:00")
        put("endTime", "16:30")
        put("location", "Student Lounge")
        put("notes", "Quick drill questions")
        put("isAlarmEnabled", true)
      })
    }.toString()

    dao.insertScheduleFormat(
      com.example.data.model.ScheduleFormat(
        name = "Standard Academic Day",
        description = "Balanced 4-session day: Morning Lectures, Afternoon Lab, Evening Review",
        formatSessionsJson = standardDayJson
      )
    )
    dao.insertScheduleFormat(
      com.example.data.model.ScheduleFormat(
        name = "Exam Prep Deep Work",
        description = "3 intensive 2-hour revision intervals with active recall & alarms",
        formatSessionsJson = deepWorkJson
      )
    )
    dao.insertScheduleFormat(
      com.example.data.model.ScheduleFormat(
        name = "Weekend Light Revision",
        description = "2 relaxed study blocks for consolidating concepts and notes",
        formatSessionsJson = lightReviewJson
      )
    )

    // Topics
    val topics = listOf(
      StudyTopic(
        subjectId = csId,
        subjectName = "Data Structures & Algorithms",
        title = "AVL Tree Rotations & Self-Balancing",
        dateStudied = today,
        descriptionNotes = "Self-balancing binary search trees maintain O(log n) height using Left-Left, Right-Right, Left-Right, and Right-Left rotations when balance factor exceeds +/-1.",
        sourceType = "MANUAL",
        masteryLevel = "FAMILIAR",
        repetitionIntervalDays = 3,
        nextRevisionDate = today,
        timesTested = 2,
        timesCorrect = 2,
        lastTestedDate = today
      ),
      StudyTopic(
        subjectId = csId,
        subjectName = "Data Structures & Algorithms",
        title = "Dijkstra's Shortest Path Algorithm",
        dateStudied = today,
        descriptionNotes = "Greedy graph algorithm finding single-source shortest path with non-negative edge weights using Min-Heap priority queue in O((V + E) log V) time.",
        sourceType = "UPLOAD",
        uploadedFileName = "dijkstra_lecture_slides.pdf",
        masteryLevel = "STRUGGLING",
        repetitionIntervalDays = 1,
        nextRevisionDate = today,
        timesTested = 3,
        timesCorrect = 1,
        lastTestedDate = today
      ),
      StudyTopic(
        subjectId = mathId,
        subjectName = "Linear Algebra & Calculus",
        title = "Eigenvalues and Characteristic Equations",
        dateStudied = today,
        descriptionNotes = "Roots of det(A - lambda*I) = 0 give the eigenvalues. Geometric multiplicity versus algebraic multiplicity determines matrix diagonalizability.",
        sourceType = "PASTED",
        masteryLevel = "UNTESTED",
        repetitionIntervalDays = 1,
        nextRevisionDate = today,
        timesTested = 0,
        timesCorrect = 0,
        lastTestedDate = null
      ),
      StudyTopic(
        subjectId = physId,
        subjectName = "Classical & Quantum Physics",
        title = "Conservation of Angular Momentum",
        dateStudied = today,
        descriptionNotes = "When net external torque on a system is zero, total angular momentum L = I*omega remains constant throughout motion.",
        sourceType = "MANUAL",
        masteryLevel = "MASTERED",
        repetitionIntervalDays = 14,
        nextRevisionDate = today,
        timesTested = 4,
        timesCorrect = 4,
        lastTestedDate = today
      ),
      StudyTopic(
        subjectId = engId,
        subjectName = "Software Engineering Principles",
        title = "SOLID Architecture Principles",
        dateStudied = today,
        descriptionNotes = "Single Responsibility, Open/Closed, Liskov Substitution, Interface Segregation, and Dependency Inversion. Foundation of clean decoupled architecture.",
        sourceType = "MANUAL",
        masteryLevel = "FAMILIAR",
        repetitionIntervalDays = 7,
        nextRevisionDate = today,
        timesTested = 2,
        timesCorrect = 2,
        lastTestedDate = today
      )
    )
    dao.insertTopics(topics)

    // Initial questions for instant weekend quiz
    val allInsertedTopics = dao.getAllTopics().first()
    val questions = mutableListOf<QuizQuestion>()
    for (t in allInsertedTopics) {
      if (t.title.contains("AVL")) {
        questions.add(
          QuizQuestion(
            topicId = t.id,
            subjectName = t.subjectName,
            topicTitle = t.title,
            questionType = "MULTIPLE_CHOICE",
            difficulty = "MEDIUM",
            questionText = "What is the worst-case search time complexity in an AVL Tree with N elements?",
            optionsJson = "O(log N),O(N),O(N log N),O(1)",
            correctAnswer = "O(log N)",
            explanation = "Because AVL trees enforce a strict balance factor difference <= 1, tree height is guaranteed O(log N)."
          )
        )
        questions.add(
          QuizQuestion(
            topicId = t.id,
            subjectName = t.subjectName,
            topicTitle = t.title,
            questionType = "TRUE_FALSE",
            difficulty = "EASY",
            questionText = "True or False: An AVL tree rotation takes O(1) constant time to rebalance a node.",
            optionsJson = "True,False",
            correctAnswer = "True",
            explanation = "Rotations simply rearrange child and parent pointers, taking strictly O(1) operations."
          )
        )
      } else if (t.title.contains("Dijkstra")) {
        questions.add(
          QuizQuestion(
            topicId = t.id,
            subjectName = t.subjectName,
            topicTitle = t.title,
            questionType = "MULTIPLE_CHOICE",
            difficulty = "HARD",
            questionText = "Why does standard Dijkstra's algorithm fail on graphs with negative edge weights?",
            optionsJson = "It assumes once a node is visited its distance is finalized,Min-heap doesn't support negative values,Negative weights cause cycles,Graph vertices become disconnected",
            correctAnswer = "It assumes once a node is visited its distance is finalized",
            explanation = "Dijkstra greedily marks vertices finalized; a negative weight later could invalidate already finalized shortest paths."
          )
        )
        questions.add(
          QuizQuestion(
            topicId = t.id,
            subjectName = t.subjectName,
            topicTitle = t.title,
            questionType = "CODING",
            difficulty = "HARD",
            questionText = "Implement the distance relaxation step in Dijkstra's algorithm.",
            optionsJson = "",
            correctAnswer = "dist[v] = dist[u] + weight",
            explanation = "If dist[u] + weight < dist[v], update dist[v] and push into priority queue.",
            codeStarter = "fun relax(u: Int, v: Int, weight: Int, dist: MutableMap<Int, Int>) {\n    // Write relaxation logic:\n    if (dist.getValue(u) + weight < dist.getOrDefault(v, Int.MAX_VALUE)) {\n        dist[v] = dist.getValue(u) + weight\n    }\n}"
          )
        )
      } else if (t.title.contains("Eigenvalues")) {
        questions.add(
          QuizQuestion(
            topicId = t.id,
            subjectName = t.subjectName,
            topicTitle = t.title,
            questionType = "SHORT_ANSWER",
            difficulty = "MEDIUM",
            questionText = "What equation is solved to determine the eigenvalues lambda of an n x n matrix A?",
            optionsJson = "",
            correctAnswer = "det(A - lambda*I) = 0",
            explanation = "The characteristic polynomial det(A - lambda*I) = 0 yields the eigenvalues."
          )
        )
      }
    }
    if (questions.isNotEmpty()) {
      dao.insertQuestions(questions)
    }

    // Tasks & Exams
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, 4)
    val examDate = dateFormat.format(cal.time)
    cal.add(Calendar.DAY_OF_YEAR, 3)
    val assignDate = dateFormat.format(cal.time)

    val tasks = listOf(
      StudyTask(
        subjectName = "Data Structures & Algorithms",
        title = "Midterm Examination (Sorting & Trees)",
        dueDate = examDate,
        type = "EXAM",
        priority = "HIGH"
      ),
      StudyTask(
        subjectName = "Linear Algebra & Calculus",
        title = "Problem Set 4: Vector Spaces & Inner Products",
        dueDate = assignDate,
        type = "ASSIGNMENT",
        priority = "MEDIUM"
      ),
      StudyTask(
        subjectName = "Software Engineering Principles",
        title = "System Design & UML Class Diagram Submission",
        dueDate = assignDate,
        type = "ASSIGNMENT",
        priority = "LOW"
      )
    )
    dao.insertTasks(tasks)

    // Seed Sample AI Tutor History across subjects
    val now = System.currentTimeMillis()
    val hour = 3600_000L

    // 1. Data Structures & Algorithms Tutor Session
    val session1Id = dao.insertTutorSession(
      TutorSession(
        subjectId = csId,
        subjectName = "Data Structures & Algorithms",
        title = "AVL Tree Left-Right (LR) Rotation Implementation",
        previewSnippet = "An LR rotation is performed when a node is inserted into the right subtree of the left child. We first rotate left on the child, then right on the root.",
        modelUsed = "gemini-3.5-flash",
        searchGroundingUsed = false,
        messageCount = 2,
        createdAt = now - (3 * hour),
        lastUpdatedAt = now - (3 * hour)
      )
    )
    dao.insertTutorMessage(
      TutorMessageEntity(
        sessionId = session1Id,
        role = "user",
        text = "Can you explain how AVL tree double rotations (Left-Right) work and show clean Kotlin code for it?",
        timestamp = now - (3 * hour),
        modelName = "gemini-3.5-flash"
      )
    )
    dao.insertTutorMessage(
      TutorMessageEntity(
        sessionId = session1Id,
        role = "model",
        text = """
# AVL Tree Double Rotations (LR Rotation)

An **LR Rotation** is required when a node is inserted into the *right subtree* of the *left child*, causing the balance factor of the ancestor to exceed `+1` (left-heavy) while the left child's balance factor is `-1`.

## Step-by-Step Mechanism
1. **First Rotation**: Perform a **Left Rotation** on the left child `node.left`.
2. **Second Rotation**: Perform a **Right Rotation** on the unbalanced root node `node`.

## Kotlin Implementation
```kotlin
fun rotateLeftRight(node: AvlNode): AvlNode {
    // Step 1: Left rotate on left child
    node.left = rotateLeft(node.left!!)
    // Step 2: Right rotate on root
    return rotateRight(node)
}
```

> **Invariant Guaranteed**: After the double rotation, tree height balance is restored and all subtrees strictly maintain `|height(left) - height(right)| <= 1`.

### Asymptotic Complexity
* **Time Complexity**: `O(1)` constant pointer re-assignments.
* **Search & Insertion Overall**: `O(log N)` guaranteed worst-case boundary.
        """.trimIndent(),
        timestamp = now - (3 * hour) + 2000L,
        modelName = "gemini-3.5-flash"
      )
    )

    // 2. Linear Algebra Tutor Session
    val session2Id = dao.insertTutorSession(
      TutorSession(
        subjectId = mathId,
        subjectName = "Linear Algebra & Calculus",
        title = "Matrix Diagonalization & Eigenvalue Criterion",
        previewSnippet = "An n x n matrix A is diagonalizable if and only if it has n linearly independent eigenvectors, meaning geometric multiplicity equals algebraic multiplicity.",
        modelUsed = "gemini-3.1-pro-preview",
        searchGroundingUsed = false,
        messageCount = 2,
        createdAt = now - (6 * hour),
        lastUpdatedAt = now - (6 * hour)
      )
    )
    dao.insertTutorMessage(
      TutorMessageEntity(
        sessionId = session2Id,
        role = "user",
        text = "Prove the necessary and sufficient condition for an n x n matrix A to be diagonalizable.",
        timestamp = now - (6 * hour),
        modelName = "gemini-3.1-pro-preview"
      )
    )
    dao.insertTutorMessage(
      TutorMessageEntity(
        sessionId = session2Id,
        role = "model",
        text = """
# Diagonalizability Criterion for Linear Operators

An n × n matrix A is **diagonalizable** if and only if there exists an invertible modal matrix P such that:
`P⁻¹ A P = D`
where D is a diagonal matrix containing the eigenvalues λ₁, ..., λₙ.

## Fundamental Theorem
> **Theorem**: An n × n matrix A is diagonalizable if and only if Rⁿ admits an eigenbasis consisting of n linearly independent eigenvectors.

### Proof Outline
1. **Sufficiency (<=)**:
   * Suppose v₁, ..., vₙ are linearly independent eigenvectors with A·vᵢ = λᵢ·vᵢ.
   * Define P = [v₁ | ... | vₙ]. Since vectors are independent, det(P) != 0 so P⁻¹ exists.
   * Then A P = [A·v₁ | ... | A·vₙ] = [λ₁·v₁ | ... | λₙ·vₙ] = P D.
   * Multiply both sides by P⁻¹ yields `P⁻¹ A P = D`.
2. **Necessity (=>)**:
   * If P⁻¹ A P = D, then A P = P D.
   * Equating the j-th columns shows A·pⱼ = dⱼⱼ·pⱼ.
   * Because P is invertible, its columns p₁, ..., pₙ are non-zero and linearly independent.

### Key Exam Check
* For every eigenvalue λ, ensure **Geometric Multiplicity** = dim(ker(A - λ·I)) matches its **Algebraic Multiplicity**!
        """.trimIndent(),
        timestamp = now - (6 * hour) + 3000L,
        modelName = "gemini-3.1-pro-preview"
      )
    )

    // 3. Physics Session with Search Grounding
    val session3Id = dao.insertTutorSession(
      TutorSession(
        subjectId = physId,
        subjectName = "Classical & Quantum Physics",
        title = "Wave-Particle Duality & Davisson-Germer Experiment",
        previewSnippet = "De Broglie proposed lambda = h / p. Verified experimentally by Davisson & Germer through electron diffraction through nickel crystal lattice.",
        modelUsed = "gemini-3.5-flash",
        searchGroundingUsed = true,
        messageCount = 2,
        createdAt = now - (12 * hour),
        lastUpdatedAt = now - (12 * hour)
      )
    )
    dao.insertTutorMessage(
      TutorMessageEntity(
        sessionId = session3Id,
        role = "user",
        text = "What was the critical experimental verification of de Broglie matter waves?",
        timestamp = now - (12 * hour),
        modelName = "gemini-3.5-flash",
        searchGroundingUsed = true
      )
    )
    dao.insertTutorMessage(
      TutorMessageEntity(
        sessionId = session3Id,
        role = "model",
        text = """
# Experimental Verification of De Broglie Hypothesis

In 1924, Louis de Broglie hypothesized that particles such as electrons possess wave-like characteristics with wavelength:
`λ = h / p = h / (m · v)`

## The Davisson-Germer Experiment (1927)
Clinton Davisson and Lester Germer fired slow-moving electrons at a crystalline **nickel target**:
* The scattered electrons exhibited **constructive and destructive interference patterns**.
* The peak scattering angle corresponded precisely to **Bragg's Law**:
`n · λ = 2d · sin(θ)`
* This proved beyond doubt that massive particles undergo wave diffraction.

> **Key Takeaway**: Matter waves are not electromagnetic; they are probability amplitude waves governed by the Schrödinger equation.
        """.trimIndent(),
        timestamp = now - (12 * hour) + 2500L,
        modelName = "gemini-3.5-flash",
        searchGroundingUsed = true,
        groundingSourcesJson = """[{"title":"Nobel Prize in Physics 1937 - Wave Properties of Electrons","url":"https://www.nobelprize.org"},{"title":"Davisson-Germer Experiment on HyperPhysics","url":"http://hyperphysics.phy-astr.gsu.edu"}]"""
      )
    )

    // 4. General Academic Session
    val session4Id = dao.insertTutorSession(
      TutorSession(
        subjectId = null,
        subjectName = "General",
        title = "Active Recall & Spaced Repetition Scheduling",
        previewSnippet = "Combine the Leitner 5-stage box method with active retrieval practice to prevent the Ebbinghaus forgetting curve.",
        modelUsed = "gemini-3.5-flash",
        searchGroundingUsed = false,
        messageCount = 2,
        createdAt = now - (24 * hour),
        lastUpdatedAt = now - (24 * hour)
      )
    )
    dao.insertTutorMessage(
      TutorMessageEntity(
        sessionId = session4Id,
        role = "user",
        text = "How should I structure my weekly revision for university exams using spaced repetition?",
        timestamp = now - (24 * hour),
        modelName = "gemini-3.5-flash"
      )
    )
    dao.insertTutorMessage(
      TutorMessageEntity(
        sessionId = session4Id,
        role = "model",
        text = """
# Optimal Spaced Repetition Framework for University Exams

To counter the exponential **Ebbinghaus Forgetting Curve**, structure your revision intervals as follows:

## 1. The 1-3-7-14-30 Day Protocol
* **Day 0**: Attend lecture, take notes, and log the topic in StudyPilot.
* **Day 1**: Initial recall test (10-minute quiz or concept reconstruction).
* **Day 3**: Second retrieval practice. Focus on edge cases.
* **Day 7**: Weekend Revision Quiz covering all topics logged this week.
* **Day 14**: Interleaved testing with related subjects.
* **Day 30**: Full past-exam simulation.

## 2. Active Recall vs Passive Rereading
> Rereading lecture slides provides the illusion of competence. **Self-testing without notes** triggers synaptic consolidation and produces 2.5x higher retention.
        """.trimIndent(),
        timestamp = now - (24 * hour) + 2000L,
        modelName = "gemini-3.5-flash"
      )
    )
  }
}

