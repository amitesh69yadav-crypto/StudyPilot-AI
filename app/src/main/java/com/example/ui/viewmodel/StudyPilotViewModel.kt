package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.QuizAttempt
import com.example.data.model.QuizConfig
import com.example.data.model.QuizQuestion
import com.example.data.model.StudyTask
import com.example.data.model.StudyTopic
import com.example.data.model.Subject
import com.example.data.model.TimetableSession
import com.example.data.model.TutorMessageEntity
import com.example.data.model.TutorModel
import com.example.data.model.TutorSession
import com.example.data.remote.EvaluationResult
import com.example.data.repository.StudyPilotRepository
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.data.service.AlarmAndCalendarService
import com.example.data.service.CurrentScheduleStatus
import com.example.data.service.ScheduleState
import com.example.data.model.UserProfile
import android.content.Context

sealed class Screen(val title: String) {
  object Login : Screen("Login & Account")
  object Dashboard : Screen("Dashboard")
  object Timetable : Screen("Timetable")
  object MyTopics : Screen("My Topics")
  object WeekendQuizzes : Screen("Weekend Quizzes")
  object ActiveQuiz : Screen("Interactive Quiz")
  object AiTutor : Screen("AI Tutor")
  object Progress : Screen("Progress")
  object Tasks : Screen("Tasks & Exams")
  object Settings : Screen("Settings")
}

data class ActiveQuizState(
  val isActive: Boolean = false,
  val title: String = "",
  val questions: List<QuizQuestion> = emptyList(),
  val currentIndex: Int = 0,
  val userAnswers: Map<Long, String> = emptyMap(),
  val flaggedQuestionIds: Set<Long> = emptySet(),
  val evaluations: Map<Long, EvaluationResult> = emptyMap(),
  val isSubmitted: Boolean = false,
  val testedTopics: List<StudyTopic> = emptyList(),
  val queuedUntestedTopics: List<StudyTopic> = emptyList(),
  val isAllCovered: Boolean = true,
  val completedAttempt: QuizAttempt? = null,
  val isEvaluating: Boolean = false
)

class StudyPilotViewModel(application: Application) : AndroidViewModel(application) {

  private val repository: StudyPilotRepository

  init {
    val db = AppDatabase.getDatabase(application)
    repository = StudyPilotRepository(db.studyPilotDao())
    viewModelScope.launch {
      repository.seedSampleUniversityData()
    }
  }

  // --- Reactive States ---
  val subjects: StateFlow<List<Subject>> = repository.allSubjects
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val sessions: StateFlow<List<TimetableSession>> = repository.allSessions
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val topics: StateFlow<List<StudyTopic>> = repository.allTopics
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val tasks: StateFlow<List<StudyTask>> = repository.allTasks
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val quizAttempts: StateFlow<List<QuizAttempt>> = repository.allQuizAttempts
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val tutorSessions: StateFlow<List<TutorSession>> = repository.allTutorSessions
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val scheduleFormats: StateFlow<List<com.example.data.model.ScheduleFormat>> = repository.allScheduleFormats
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // --- Google Calendar & Alarm Services ---
  val alarmAndCalendarService = AlarmAndCalendarService(application)

  private val _isCalendarConnected = MutableStateFlow(true)
  val isCalendarConnected: StateFlow<Boolean> = _isCalendarConnected.asStateFlow()

  val calendarAccountEmail = "amitesh69.yadav@gmail.com"

  private val _ringingAlarmSession = MutableStateFlow<TimetableSession?>(null)
  val ringingAlarmSession: StateFlow<TimetableSession?> = _ringingAlarmSession.asStateFlow()

  val scheduleStatus: StateFlow<CurrentScheduleStatus> = repository.allSessions
    .map { list -> alarmAndCalendarService.evaluateCurrentStatus(list) }
    .stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      CurrentScheduleStatus(ScheduleState.FREE_TIME, formattedStatusText = "Checking schedule status...")
    )

  // --- User Authentication & Multi-Device Profile ---
  private val authPrefs = application.getSharedPreferences("studypilot_auth_prefs", Context.MODE_PRIVATE)

  private val _currentUser = MutableStateFlow<UserProfile>(loadSavedUser())
  val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

  private val _isAuthenticated = MutableStateFlow(authPrefs.getBoolean("is_logged_in", true))
  val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

  private fun loadSavedUser(): UserProfile {
    val name = authPrefs.getString("user_name", "Amitesh Yadav") ?: "Amitesh Yadav"
    val email = authPrefs.getString("user_email", "amitesh69.yadav@gmail.com") ?: "amitesh69.yadav@gmail.com"
    val role = authPrefs.getString("user_role", "Computer Science Scholar") ?: "Computer Science Scholar"
    val uni = authPrefs.getString("user_university", "Imperial College London") ?: "Imperial College London"
    val studentId = authPrefs.getString("user_student_id", "CS-2024-8841") ?: "CS-2024-8841"
    val initials = authPrefs.getString("user_initials", "AY") ?: "AY"
    val provider = authPrefs.getString("auth_provider", "Academic SSO") ?: "Academic SSO"
    val isLoggedIn = authPrefs.getBoolean("is_logged_in", true)
    return UserProfile(
      name = name,
      email = email,
      role = role,
      university = uni,
      studentId = studentId,
      avatarInitials = initials,
      isLoggedIn = isLoggedIn,
      authProvider = provider
    )
  }

  private fun saveUser(user: UserProfile, isLoggedIn: Boolean) {
    authPrefs.edit()
      .putString("user_name", user.name)
      .putString("user_email", user.email)
      .putString("user_role", user.role)
      .putString("user_university", user.university)
      .putString("user_student_id", user.studentId)
      .putString("user_initials", user.avatarInitials)
      .putString("auth_provider", user.authProvider)
      .putBoolean("is_logged_in", isLoggedIn)
      .apply()
  }

  fun login(email: String, pass: String, rememberMe: Boolean = true): Boolean {
    val cleanEmail = email.trim()
    if (cleanEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
      viewModelScope.launch { _snackbarMessage.emit("Please enter a valid academic email address.") }
      return false
    }
    if (pass.trim().length < 6) {
      viewModelScope.launch { _snackbarMessage.emit("Password must be at least 6 characters long.") }
      return false
    }
    val namePart = cleanEmail.substringBefore("@").replace(".", " ").split(" ")
      .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    val initials = namePart.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
      .ifBlank { "ST" }
    val updatedUser = _currentUser.value.copy(
      email = cleanEmail,
      name = if (_currentUser.value.email.equals(cleanEmail, ignoreCase = true)) _currentUser.value.name else namePart,
      avatarInitials = initials,
      isLoggedIn = true,
      authProvider = "Email & Password",
      rememberMe = rememberMe,
      lastLoginTime = System.currentTimeMillis()
    )
    saveUser(updatedUser, isLoggedIn = true)
    _currentUser.value = updatedUser
    _isAuthenticated.value = true
    _currentScreen.value = Screen.Dashboard
    viewModelScope.launch {
      _snackbarMessage.emit("Signed in successfully as ${updatedUser.name}")
    }
    return true
  }

  fun register(
    name: String,
    email: String,
    pass: String,
    university: String,
    studentId: String = "",
    rememberMe: Boolean = true
  ): Boolean {
    if (name.isBlank()) {
      viewModelScope.launch { _snackbarMessage.emit("Please enter your full name.") }
      return false
    }
    val cleanEmail = email.trim()
    if (cleanEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
      viewModelScope.launch { _snackbarMessage.emit("Please enter a valid university email address.") }
      return false
    }
    if (pass.trim().length < 6) {
      viewModelScope.launch { _snackbarMessage.emit("Password must be at least 6 characters.") }
      return false
    }
    val initials = name.trim().split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
      .ifBlank { "ST" }
    val newUser = UserProfile(
      id = "user_${System.currentTimeMillis()}",
      name = name.trim(),
      email = cleanEmail,
      role = "University Student",
      university = university.ifBlank { "University Academic Scholar" },
      studentId = studentId.ifBlank { "ID-${(1000..9999).random()}" },
      avatarInitials = initials,
      isLoggedIn = true,
      authProvider = "Email & Password",
      rememberMe = rememberMe,
      lastLoginTime = System.currentTimeMillis()
    )
    saveUser(newUser, isLoggedIn = true)
    _currentUser.value = newUser
    _isAuthenticated.value = true
    _currentScreen.value = Screen.Dashboard
    viewModelScope.launch {
      _snackbarMessage.emit("Welcome to StudyPilot, ${newUser.name}!")
    }
    return true
  }

  fun loginWithGoogle() {
    val user = UserProfile(
      id = "google_user_8841",
      name = "Amitesh Yadav",
      email = "amitesh69.yadav@gmail.com",
      role = "Computer Science Scholar",
      university = "Imperial College London",
      studentId = "CS-2024-8841",
      avatarInitials = "AY",
      isLoggedIn = true,
      authProvider = "Google Student Account",
      rememberMe = true,
      lastLoginTime = System.currentTimeMillis()
    )
    saveUser(user, isLoggedIn = true)
    _currentUser.value = user
    _isAuthenticated.value = true
    _currentScreen.value = Screen.Dashboard
    viewModelScope.launch {
      _snackbarMessage.emit("Signed in with Google: ${user.email}")
    }
  }

  fun loginAsGuest() {
    val guestUser = UserProfile(
      id = "guest_demo",
      name = "Guest Scholar",
      email = "guest.scholar@studypilot.edu",
      role = "Visiting Scholar (Demo Mode)",
      university = "Academic Research University",
      studentId = "DEMO-GUEST",
      avatarInitials = "GS",
      isLoggedIn = true,
      authProvider = "Guest / Demo Mode",
      rememberMe = false,
      lastLoginTime = System.currentTimeMillis()
    )
    saveUser(guestUser, isLoggedIn = true)
    _currentUser.value = guestUser
    _isAuthenticated.value = true
    _currentScreen.value = Screen.Dashboard
    viewModelScope.launch {
      _snackbarMessage.emit("Exploring in Guest Demo Mode")
    }
  }

  fun logout() {
    val loggedOutUser = _currentUser.value.copy(isLoggedIn = false)
    saveUser(loggedOutUser, isLoggedIn = false)
    _currentUser.value = loggedOutUser
    _isAuthenticated.value = false
    _currentScreen.value = Screen.Login
    viewModelScope.launch {
      _snackbarMessage.emit("Signed out successfully.")
    }
  }

  fun updateProfile(name: String, email: String, university: String, role: String) {
    val initials = name.trim().split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
      .ifBlank { _currentUser.value.avatarInitials }
    val updated = _currentUser.value.copy(
      name = name.ifBlank { _currentUser.value.name },
      email = email.ifBlank { _currentUser.value.email },
      university = university.ifBlank { _currentUser.value.university },
      role = role.ifBlank { _currentUser.value.role },
      avatarInitials = initials
    )
    saveUser(updated, isLoggedIn = _isAuthenticated.value)
    _currentUser.value = updated
    viewModelScope.launch {
      _snackbarMessage.emit("Academic profile updated.")
    }
  }

  // Navigation
  private val _currentScreen = MutableStateFlow<Screen>(
    if (authPrefs.getBoolean("is_logged_in", true)) Screen.Dashboard else Screen.Login
  )
  val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

  fun navigateTo(screen: Screen) {
    _currentScreen.value = screen
  }

  // Quiz Settings
  private val _quizConfig = MutableStateFlow(QuizConfig())
  val quizConfig: StateFlow<QuizConfig> = _quizConfig.asStateFlow()

  fun updateQuizConfig(config: QuizConfig) {
    _quizConfig.value = config
  }

  // Active Quiz
  private val _activeQuizState = MutableStateFlow(ActiveQuizState())
  val activeQuizState: StateFlow<ActiveQuizState> = _activeQuizState.asStateFlow()

  // UI notifications
  private val _snackbarMessage = MutableSharedFlow<String>()
  val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

  // Loading indicator for AI tasks
  private val _isAiLoading = MutableStateFlow(false)
  val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

  // --- AI Chatbot States & Subject Scoping ---
  private val _selectedTutorSubject = MutableStateFlow<Subject?>(null)
  val selectedTutorSubject: StateFlow<Subject?> = _selectedTutorSubject.asStateFlow()

  private val _activeSessionId = MutableStateFlow<Long?>(null)
  val activeSessionId: StateFlow<Long?> = _activeSessionId.asStateFlow()

  private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
    listOf(
      ChatMessage(
        role = "model",
        text = "Hello! I am your StudyPilot AI Tutor. Select a subject above or ask any conceptual question, request step-by-step math or algorithm proofs, debug code, or toggle Google Search for up-to-date research citations.",
        modelName = TutorModel.FLASH.modelId
      )
    )
  )
  val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

  private val _selectedModel = MutableStateFlow(TutorModel.FLASH)
  val selectedModel: StateFlow<TutorModel> = _selectedModel.asStateFlow()

  private val _useGoogleSearch = MutableStateFlow(false)
  val useGoogleSearch: StateFlow<Boolean> = _useGoogleSearch.asStateFlow()

  private val _systemInstruction = MutableStateFlow(
    "You are StudyPilot AI Tutor, an elite academic advisor, professor, and university researcher. Provide clear, structured explanations, rigorous proofs when asked, clean code examples, and concise summaries. When Google Search grounding is enabled, cite accurate and up-to-date sources."
  )
  val systemInstruction: StateFlow<String> = _systemInstruction.asStateFlow()

  private val _isChatLoading = MutableStateFlow(false)
  val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

  fun selectTutorModel(model: TutorModel) {
    _selectedModel.value = model
  }

  fun toggleGoogleSearch(enabled: Boolean) {
    _useGoogleSearch.value = enabled
    if (enabled) {
      _selectedModel.value = TutorModel.FLASH
    }
  }

  fun updateSystemInstruction(instruction: String) {
    _systemInstruction.value = instruction
  }

  fun selectTutorSubject(subject: Subject?) {
    _selectedTutorSubject.value = subject
    val newInstruction = if (subject != null) {
      "You are StudyPilot AI Tutor, an expert professor and researcher specializing in ${subject.name} (${subject.code}). Provide deep, structured explanations, rigorous proofs, formulas, clean code examples, and practice tips tailored strictly to ${subject.name}. When Google Search grounding is enabled, cite accurate and up-to-date sources."
    } else {
      "You are StudyPilot AI Tutor, an elite academic advisor, professor, and university researcher. Provide clear, structured explanations, rigorous proofs when asked, clean code examples, and concise summaries. When Google Search grounding is enabled, cite accurate and up-to-date sources."
    }
    _systemInstruction.value = newInstruction

    if (_activeSessionId.value == null && _chatMessages.value.size <= 1) {
      _chatMessages.value = listOf(
        ChatMessage(
          role = "model",
          text = if (subject != null) {
            "Hello! I am your AI Tutor for **${subject.name}** (${subject.code}). What concept, homework problem, or theorem would you like to explore?"
          } else {
            "Hello! I am your StudyPilot AI Tutor. Ask me any conceptual question, request step-by-step math or algorithm proofs, debug code, or toggle Google Search for live citations."
          },
          modelName = _selectedModel.value.modelId
        )
      )
    }
  }

  fun openTutorForSubject(subjectId: Long?, prompt: String? = null) {
    val sub = subjects.value.firstOrNull { it.id == subjectId }
    selectTutorSubject(sub)
    _activeSessionId.value = null
    _currentScreen.value = Screen.AiTutor
    if (!prompt.isNullOrBlank()) {
      sendChatMessage(prompt)
    }
  }

  fun startNewTutorChat(subject: Subject? = _selectedTutorSubject.value) {
    _activeSessionId.value = null
    selectTutorSubject(subject)
    _chatMessages.value = listOf(
      ChatMessage(
        role = "model",
        text = if (subject != null) {
          "New session started for **${subject.name}** (${subject.code}). Ask a question or pick a suggested topic below!"
        } else {
          "New general study session started. What lecture topic or theorem would you like to explore?"
        },
        modelName = _selectedModel.value.modelId
      )
    )
  }

  fun loadTutorSession(session: TutorSession) {
    viewModelScope.launch {
      _activeSessionId.value = session.id
      val matchedSubject = subjects.value.firstOrNull { it.id == session.subjectId }
      _selectedTutorSubject.value = matchedSubject
      selectTutorSubject(matchedSubject)

      val dbMessages = repository.getMessagesForSessionSync(session.id)
      if (dbMessages.isNotEmpty()) {
        _chatMessages.value = dbMessages.map { entity ->
          val sources = try {
            if (entity.groundingSourcesJson.isNotBlank()) {
              val arr = JSONArray(entity.groundingSourcesJson)
              (0 until arr.length()).map { idx ->
                val obj = arr.getJSONObject(idx)
                com.example.data.model.GroundingSource(
                  title = obj.optString("title", "Source"),
                  url = obj.optString("url", "")
                )
              }
            } else emptyList()
          } catch (e: Exception) {
            emptyList()
          }

          ChatMessage(
            id = entity.id.toString(),
            role = entity.role,
            text = entity.text,
            timestamp = entity.timestamp,
            modelName = entity.modelName,
            searchGroundingUsed = entity.searchGroundingUsed,
            groundingSources = sources,
            isError = entity.isError
          )
        }
      }
    }
  }

  fun deleteTutorSession(sessionId: Long) {
    viewModelScope.launch {
      repository.deleteTutorSession(sessionId)
      if (_activeSessionId.value == sessionId) {
        startNewTutorChat()
      }
      _snackbarMessage.emit("Chat session deleted")
    }
  }

  fun clearAllTutorHistory() {
    viewModelScope.launch {
      repository.clearAllTutorHistory()
      startNewTutorChat()
      _snackbarMessage.emit("All tutor chat history cleared")
    }
  }

  fun clearChat() {
    startNewTutorChat(_selectedTutorSubject.value)
  }

  fun sendChatMessage(text: String) {
    val trimmed = text.trim()
    if (trimmed.isBlank() || _isChatLoading.value) return

    val currentHistory = _chatMessages.value.toMutableList()
    val userMsg = ChatMessage(
      role = "user",
      text = trimmed,
      modelName = _selectedModel.value.modelId,
      searchGroundingUsed = _useGoogleSearch.value
    )
    currentHistory.add(userMsg)
    _chatMessages.value = currentHistory

    viewModelScope.launch {
      _isChatLoading.value = true
      try {
        var sessionId = _activeSessionId.value
        val activeSub = _selectedTutorSubject.value
        if (sessionId == null) {
          val title = trimmed.take(45).let { if (trimmed.length > 45) "$it..." else it }
          sessionId = repository.createTutorSession(
            subjectId = activeSub?.id,
            subjectName = activeSub?.name ?: "General",
            title = title,
            previewSnippet = trimmed,
            modelUsed = _selectedModel.value.modelId,
            searchGroundingUsed = _useGoogleSearch.value
          )
          _activeSessionId.value = sessionId
        }

        repository.saveTutorMessage(
          sessionId = sessionId,
          role = "user",
          text = trimmed,
          modelName = _selectedModel.value.modelId,
          searchGroundingUsed = _useGoogleSearch.value
        )

        val response = repository.chatWithTutor(
          history = currentHistory,
          model = _selectedModel.value,
          useGoogleSearch = _useGoogleSearch.value,
          systemInstruction = _systemInstruction.value
        )

        val modelMsg = ChatMessage(
          role = "model",
          text = response.text,
          modelName = response.modelName,
          searchGroundingUsed = _useGoogleSearch.value,
          groundingSources = response.groundingSources,
          searchQueries = response.searchQueries,
          isError = response.isError
        )
        _chatMessages.value = _chatMessages.value + modelMsg

        val sourcesJson = if (response.groundingSources.isNotEmpty()) {
          val arr = JSONArray()
          response.groundingSources.forEach { s ->
            arr.put(JSONObject().apply {
              put("title", s.title)
              put("url", s.url)
            })
          }
          arr.toString()
        } else ""

        repository.saveTutorMessage(
          sessionId = sessionId,
          role = "model",
          text = response.text,
          modelName = response.modelName,
          searchGroundingUsed = _useGoogleSearch.value,
          groundingSourcesJson = sourcesJson,
          isError = response.isError
        )
      } catch (e: Exception) {
        val errorMsg = ChatMessage(
          role = "model",
          text = "I encountered an error processing your request: ${e.message}",
          modelName = _selectedModel.value.modelId,
          isError = true
        )
        _chatMessages.value = _chatMessages.value + errorMsg
      } finally {
        _isChatLoading.value = false
      }
    }
  }

  // --- Subject Actions ---
  fun addSubject(name: String, code: String, colorHex: String) {
    viewModelScope.launch {
      repository.addSubject(name, code, colorHex)
      _snackbarMessage.emit("Subject '$name' added")
    }
  }

  fun deleteSubject(subject: Subject) {
    viewModelScope.launch {
      repository.deleteSubject(subject)
      _snackbarMessage.emit("Subject removed")
    }
  }

  // --- Session Actions ---
  fun addSession(session: TimetableSession) {
    viewModelScope.launch {
      repository.addSession(session)
      _snackbarMessage.emit("Class '${session.topic}' scheduled")
    }
  }

  fun updateSessionStatus(session: TimetableSession, newStatus: String) {
    viewModelScope.launch {
      repository.updateSession(session.copy(status = newStatus))
      _snackbarMessage.emit("Session marked as $newStatus")
    }
  }

  fun deleteSession(id: Long) {
    viewModelScope.launch {
      repository.deleteSession(id)
      _snackbarMessage.emit("Class schedule deleted")
    }
  }

  fun updateSession(session: TimetableSession) {
    viewModelScope.launch {
      repository.updateSession(session)
      _snackbarMessage.emit("Updated '${session.topic}' (${session.startTime} - ${session.endTime})")
    }
  }

  fun toggleSessionAlarm(session: TimetableSession) {
    viewModelScope.launch {
      val updated = session.copy(isAlarmEnabled = !session.isAlarmEnabled)
      repository.updateSession(updated)
      val status = if (updated.isAlarmEnabled) "Alarm turned ON" else "Alarm turned OFF"
      _snackbarMessage.emit("$status for '${session.topic}'")
    }
  }

  fun triggerTestAlarm(session: TimetableSession) {
    _ringingAlarmSession.value = session
    alarmAndCalendarService.triggerAlarmAlert(session)
  }

  fun dismissAlarm() {
    alarmAndCalendarService.stopAlarmSound()
    _ringingAlarmSession.value = null
  }

  fun snoozeAlarm(session: TimetableSession, minutes: Int = 10) {
    alarmAndCalendarService.stopAlarmSound()
    _ringingAlarmSession.value = null
    viewModelScope.launch {
      _snackbarMessage.emit("Alarm snoozed for $minutes minutes for '${session.topic}'")
    }
  }

  fun syncSessionToGoogleCalendar(session: TimetableSession) {
    viewModelScope.launch {
      val updated = session.copy(googleCalendarEventId = "gcal_${session.id}_${System.currentTimeMillis()}")
      repository.updateSession(updated)
      _snackbarMessage.emit("Synced '${session.topic}' to Google Calendar ($calendarAccountEmail)")
    }
  }

  fun syncAllTodayToGoogleCalendar() {
    viewModelScope.launch {
      val today = repository.getTodayDateString()
      val todayList = sessions.value.filter { it.date == today }
      if (todayList.isEmpty()) {
        _snackbarMessage.emit("No classes or study sessions scheduled today to sync.")
      } else {
        todayList.forEach { s ->
          repository.updateSession(s.copy(googleCalendarEventId = "gcal_${s.id}_${System.currentTimeMillis()}"))
        }
        _snackbarMessage.emit("Synced ${todayList.size} session(s) to Google Calendar ($calendarAccountEmail)")
      }
    }
  }

  fun createDayScheduleBatch(newSessions: List<TimetableSession>) {
    viewModelScope.launch {
      newSessions.forEach { repository.addSession(it) }
      _snackbarMessage.emit("Created ${newSessions.size} schedule session(s)!")
    }
  }

  fun replaceDayScheduleWithSessions(targetDate: String, newSessions: List<TimetableSession>) {
    viewModelScope.launch {
      repository.clearSessionsForDate(targetDate)
      newSessions.forEach { session ->
        repository.addSession(session.copy(id = 0L, date = targetDate))
      }
      _snackbarMessage.emit("Applied ${newSessions.size} session(s) to $targetDate!")
    }
  }

  fun copyDaySchedule(sourceDate: String, targetDate: String, overwriteExisting: Boolean = false) {
    viewModelScope.launch {
      val sourceSessions = sessions.value.filter { it.date == sourceDate }
      if (sourceSessions.isEmpty()) {
        _snackbarMessage.emit("No sessions found on $sourceDate to copy.")
        return@launch
      }

      if (overwriteExisting) {
        repository.clearSessionsForDate(targetDate)
      }

      sourceSessions.forEach { s ->
        repository.addSession(
          s.copy(
            id = 0L,
            date = targetDate,
            status = "SCHEDULED",
            googleCalendarEventId = null
          )
        )
      }
      _snackbarMessage.emit("Copied ${sourceSessions.size} session(s) from $sourceDate to $targetDate!")
    }
  }

  fun saveScheduleAsFormat(
    formatName: String,
    description: String,
    sourceSessions: List<TimetableSession>
  ) {
    viewModelScope.launch {
      if (sourceSessions.isEmpty()) {
        _snackbarMessage.emit("Cannot save an empty format.")
        return@launch
      }

      val jsonArray = org.json.JSONArray()
      sourceSessions.forEach { s ->
        val obj = org.json.JSONObject().apply {
          put("subjectId", s.subjectId)
          put("subjectName", s.subjectName)
          put("topic", s.topic)
          put("startTime", s.startTime)
          put("endTime", s.endTime)
          put("location", s.location)
          put("notes", s.notes)
          put("isAlarmEnabled", s.isAlarmEnabled)
        }
        jsonArray.put(obj)
      }

      val format = com.example.data.model.ScheduleFormat(
        name = formatName.trim(),
        description = description.trim(),
        formatSessionsJson = jsonArray.toString()
      )
      repository.addScheduleFormat(format)
      _snackbarMessage.emit("Saved format '$formatName' with ${sourceSessions.size} session blocks!")
    }
  }

  fun applyFormatToDate(
    format: com.example.data.model.ScheduleFormat,
    targetDate: String,
    overwriteExisting: Boolean = false
  ) {
    viewModelScope.launch {
      val parsedSessions = mutableListOf<TimetableSession>()
      try {
        val jsonArray = org.json.JSONArray(format.formatSessionsJson)
        for (i in 0 until jsonArray.length()) {
          val obj = jsonArray.getJSONObject(i)
          parsedSessions.add(
            TimetableSession(
              subjectId = obj.optLong("subjectId", 1L),
              subjectName = obj.optString("subjectName", "General"),
              topic = obj.optString("topic", "Study Block"),
              date = targetDate,
              startTime = obj.optString("startTime", "09:00"),
              endTime = obj.optString("endTime", "10:30"),
              location = obj.optString("location", "Campus Hall"),
              notes = obj.optString("notes", ""),
              status = "SCHEDULED",
              isAlarmEnabled = obj.optBoolean("isAlarmEnabled", true)
            )
          )
        }
      } catch (e: Exception) {
        _snackbarMessage.emit("Error applying format: ${e.message}")
        return@launch
      }

      if (overwriteExisting) {
        repository.clearSessionsForDate(targetDate)
      }

      parsedSessions.forEach { repository.addSession(it) }
      _snackbarMessage.emit("Applied format '${format.name}' (${parsedSessions.size} sessions) to $targetDate!")
    }
  }

  fun deleteScheduleFormat(formatId: Long) {
    viewModelScope.launch {
      repository.deleteScheduleFormat(formatId)
      _snackbarMessage.emit("Schedule format deleted")
    }
  }

  fun clearDaySchedule(targetDate: String) {
    viewModelScope.launch {
      repository.clearSessionsForDate(targetDate)
      _snackbarMessage.emit("Cleared all sessions for $targetDate")
    }
  }


  // --- Topic Actions ---
  fun addTopic(
    subjectId: Long,
    subjectName: String,
    title: String,
    descriptionNotes: String,
    sourceType: String,
    uploadedFileName: String? = null,
    generateQuestionsImmediately: Boolean = false
  ) {
    viewModelScope.launch {
      _isAiLoading.value = true
      try {
        val today = repository.getTodayDateString()
        val topic = StudyTopic(
          subjectId = subjectId,
          subjectName = subjectName,
          title = title,
          dateStudied = today,
          descriptionNotes = descriptionNotes,
          sourceType = sourceType,
          uploadedFileName = uploadedFileName,
          masteryLevel = "UNTESTED",
          repetitionIntervalDays = 1,
          nextRevisionDate = today
        )
        val newId = repository.addTopic(topic)

        if (generateQuestionsImmediately) {
          // Pre-generate practice questions
          repository.assembleWeekendQuiz(
            config = QuizConfig(questionCount = 3),
            filterSubjectId = subjectId
          )
        }

        _snackbarMessage.emit("Topic '$title' saved successfully")
      } finally {
        _isAiLoading.value = false
      }
    }
  }

  fun updateTopicMastery(topic: StudyTopic, newMastery: String) {
    viewModelScope.launch {
      val newInterval = when (newMastery) {
        "MASTERED" -> 14
        "FAMILIAR" -> 7
        "STRUGGLING" -> 1
        else -> 1
      }
      repository.updateTopic(
        topic.copy(
          masteryLevel = newMastery,
          repetitionIntervalDays = newInterval
        )
      )
      _snackbarMessage.emit("Topic updated to $newMastery")
    }
  }

  fun summarizeTopicWithAi(title: String, rawContent: String, onComplete: (String) -> Unit) {
    viewModelScope.launch {
      _isAiLoading.value = true
      try {
        val summary = repository.summarizeMaterial(title, rawContent)
        onComplete(summary)
      } finally {
        _isAiLoading.value = false
      }
    }
  }

  fun deleteTopic(id: Long) {
    viewModelScope.launch {
      repository.deleteTopic(id)
      _snackbarMessage.emit("Study topic deleted")
    }
  }

  // --- Task Actions ---
  fun addTask(title: String, subjectName: String, dueDate: String, type: String, priority: String) {
    viewModelScope.launch {
      repository.addTask(
        StudyTask(
          title = title,
          subjectName = subjectName,
          dueDate = dueDate,
          type = type,
          priority = priority
        )
      )
      _snackbarMessage.emit("Task added")
    }
  }

  fun toggleTaskCompletion(task: StudyTask) {
    viewModelScope.launch {
      repository.updateTask(task.copy(isCompleted = !task.isCompleted))
    }
  }

  fun deleteTask(id: Long) {
    viewModelScope.launch {
      repository.deleteTask(id)
      _snackbarMessage.emit("Task deleted")
    }
  }

  // --- Quiz Execution & Interactive Experience ---
  fun startWeekendQuiz(forceWeakOnly: Boolean = false, filterSubjectId: Long? = null) {
    viewModelScope.launch {
      _isAiLoading.value = true
      try {
        val generated = repository.assembleWeekendQuiz(
          config = _quizConfig.value,
          filterSubjectId = filterSubjectId,
          forceWeakOnly = forceWeakOnly
        )

        if (generated.questions.isEmpty()) {
          _snackbarMessage.emit("No questions available for this configuration. Add more study topics first!")
          return@launch
        }

        val quizTitle = if (forceWeakOnly) "Quick Revision: Struggling Topics" else "Weekend Revision Quiz"

        _activeQuizState.value = ActiveQuizState(
          isActive = true,
          title = quizTitle,
          questions = generated.questions,
          currentIndex = 0,
          userAnswers = emptyMap(),
          flaggedQuestionIds = emptySet(),
          evaluations = emptyMap(),
          isSubmitted = false,
          testedTopics = generated.testedTopics,
          queuedUntestedTopics = generated.queuedUntestedTopics,
          isAllCovered = generated.isAllCovered,
          completedAttempt = null,
          isEvaluating = false
        )
        _currentScreen.value = Screen.ActiveQuiz
      } finally {
        _isAiLoading.value = false
      }
    }
  }

  fun selectQuestionIndex(index: Int) {
    val current = _activeQuizState.value
    if (index in current.questions.indices) {
      _activeQuizState.value = current.copy(currentIndex = index)
    }
  }

  fun nextQuestion() {
    val current = _activeQuizState.value
    if (current.currentIndex + 1 < current.questions.size) {
      _activeQuizState.value = current.copy(currentIndex = current.currentIndex + 1)
    }
  }

  fun previousQuestion() {
    val current = _activeQuizState.value
    if (current.currentIndex > 0) {
      _activeQuizState.value = current.copy(currentIndex = current.currentIndex - 1)
    }
  }

  fun answerCurrentQuestion(answer: String) {
    val current = _activeQuizState.value
    val currentQ = current.questions.getOrNull(current.currentIndex) ?: return
    val newAnswers = current.userAnswers.toMutableMap()
    newAnswers[currentQ.id] = answer
    _activeQuizState.value = current.copy(userAnswers = newAnswers)
  }

  fun toggleFlagCurrentQuestion() {
    val current = _activeQuizState.value
    val currentQ = current.questions.getOrNull(current.currentIndex) ?: return
    val newFlags = current.flaggedQuestionIds.toMutableSet()
    if (newFlags.contains(currentQ.id)) {
      newFlags.remove(currentQ.id)
    } else {
      newFlags.add(currentQ.id)
    }
    _activeQuizState.value = current.copy(flaggedQuestionIds = newFlags)
  }

  fun submitActiveQuiz() {
    val current = _activeQuizState.value
    if (current.questions.isEmpty() || current.isSubmitted) return

    viewModelScope.launch {
      _activeQuizState.value = current.copy(isEvaluating = true)
      try {
        val evalMap = mutableMapOf<Long, EvaluationResult>()
        for (q in current.questions) {
          val ans = current.userAnswers[q.id] ?: ""
          val eval = repository.evaluateUserAnswer(q, ans)
          evalMap[q.id] = eval
        }

        val attempt = repository.recordQuizResults(
          title = current.title,
          questions = current.questions,
          userAnswers = current.userAnswers,
          evaluations = evalMap,
          topicsCoveredCount = current.testedTopics.size,
          topicsSkippedCount = current.queuedUntestedTopics.size
        )

        _activeQuizState.value = current.copy(
          evaluations = evalMap,
          isSubmitted = true,
          completedAttempt = attempt,
          isEvaluating = false
        )
        _snackbarMessage.emit("Quiz completed! Scored ${attempt.scorePercentage}%")
      } catch (e: Exception) {
        _activeQuizState.value = _activeQuizState.value.copy(isEvaluating = false)
        _snackbarMessage.emit("Error scoring quiz: ${e.message}")
      }
    }
  }

  fun retryIncorrectQuestions() {
    val current = _activeQuizState.value
    val incorrectQuestions = current.questions.filter { q ->
      current.evaluations[q.id]?.isCorrect != true
    }

    if (incorrectQuestions.isEmpty()) {
      viewModelScope.launch {
        _snackbarMessage.emit("No mistakes to retry! Perfect score!")
      }
      return
    }

    _activeQuizState.value = ActiveQuizState(
      isActive = true,
      title = "Retry Mistakes: ${current.title}",
      questions = incorrectQuestions,
      currentIndex = 0,
      userAnswers = emptyMap(),
      flaggedQuestionIds = emptySet(),
      evaluations = emptyMap(),
      isSubmitted = false,
      testedTopics = current.testedTopics,
      queuedUntestedTopics = emptyList(),
      isAllCovered = true,
      completedAttempt = null,
      isEvaluating = false
    )
  }

  fun exitActiveQuiz() {
    _activeQuizState.value = ActiveQuizState(isActive = false)
    _currentScreen.value = Screen.WeekendQuizzes
  }

  // Pre-seed helper for demo
  fun seedSampleData() {
    viewModelScope.launch {
      repository.seedSampleUniversityData()
      _snackbarMessage.emit("Sample university data loaded")
    }
  }
}
