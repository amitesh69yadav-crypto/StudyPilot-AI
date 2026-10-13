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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
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

  private val repository: StudyPilotRepository =
    StudyPilotRepository(AppDatabase.getDatabase(application).studyPilotDao())

  // --- UI notifications ---
  private val _snackbarMessage = MutableSharedFlow<String>()
  val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

  // --- Google Calendar & Alarm Services ---
  val alarmAndCalendarService = AlarmAndCalendarService(application)

  private val _isCalendarConnected = MutableStateFlow(true)
  val isCalendarConnected: StateFlow<Boolean> = _isCalendarConnected.asStateFlow()

  val calendarAccountEmail = "amitesh69.yadav@gmail.com"

  private val _ringingAlarmSession = MutableStateFlow<TimetableSession?>(null)
  val ringingAlarmSession: StateFlow<TimetableSession?> = _ringingAlarmSession.asStateFlow()

  // --- Registered Student Accounts & Multi-Student Account Isolation ---
  private val authPrefs = application.getSharedPreferences("studypilot_auth_prefs", Context.MODE_PRIVATE)

  private val defaultAccounts = listOf(
    UserProfile(
      id = "student_amitesh",
      name = "Amitesh Yadav",
      email = "amitesh69.yadav@gmail.com",
      role = "Computer Science Scholar",
      university = "Imperial College London",
      studentId = "CS-2024-8841",
      avatarInitials = "AY",
      isLoggedIn = true,
      authProvider = "Google / Academic SSO"
    ),
    UserProfile(
      id = "student_sarah",
      name = "Sarah Chen",
      email = "sarah.chen@cambridge.ac.uk",
      role = "Bioinformatics & Pre-Med",
      university = "University of Cambridge",
      studentId = "MED-2024-1049",
      avatarInitials = "SC",
      isLoggedIn = false,
      authProvider = "Cambridge Student Portal"
    ),
    UserProfile(
      id = "student_marcus",
      name = "Marcus Vance",
      email = "marcus.vance@mit.edu",
      role = "Mechanical Engineering Scholar",
      university = "Massachusetts Institute of Technology",
      studentId = "ME-2024-7723",
      avatarInitials = "MV",
      isLoggedIn = false,
      authProvider = "MIT Kerberos SSO"
    )
  )

  private fun loadRegisteredAccounts(): List<UserProfile> {
    val jsonStr = authPrefs.getString("registered_students_json", null) ?: return defaultAccounts
    return try {
      val arr = JSONArray(jsonStr)
      val list = mutableListOf<UserProfile>()
      for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        list.add(
          UserProfile(
            id = obj.optString("id", "user_${System.currentTimeMillis()}"),
            name = obj.optString("name", "Student Scholar"),
            email = obj.optString("email", "student@university.edu"),
            role = obj.optString("role", "University Scholar"),
            university = obj.optString("university", "Academic University"),
            studentId = obj.optString("studentId", "ID-2024"),
            avatarInitials = obj.optString("avatarInitials", "ST"),
            isLoggedIn = obj.optBoolean("isLoggedIn", false),
            authProvider = obj.optString("authProvider", "Academic Portal"),
            rememberMe = obj.optBoolean("rememberMe", true),
            lastLoginTime = obj.optLong("lastLoginTime", System.currentTimeMillis())
          )
        )
      }
      if (list.isEmpty()) defaultAccounts else list
    } catch (_: Exception) {
      defaultAccounts
    }
  }

  private fun saveRegisteredAccounts(list: List<UserProfile>) {
    try {
      val arr = JSONArray()
      for (u in list) {
        arr.put(JSONObject().apply {
          put("id", u.id)
          put("name", u.name)
          put("email", u.email)
          put("role", u.role)
          put("university", u.university)
          put("studentId", u.studentId)
          put("avatarInitials", u.avatarInitials)
          put("isLoggedIn", u.isLoggedIn)
          put("authProvider", u.authProvider)
          put("rememberMe", u.rememberMe)
          put("lastLoginTime", u.lastLoginTime)
        })
      }
      authPrefs.edit().putString("registered_students_json", arr.toString()).apply()
    } catch (_: Exception) {}
  }

  private fun loadSavedUser(): UserProfile {
    val activeId = authPrefs.getString("active_user_id", "student_amitesh") ?: "student_amitesh"
    val accounts = loadRegisteredAccounts()
    val match = accounts.find { it.id == activeId }
    if (match != null) return match

    val name = authPrefs.getString("user_name", "Amitesh Yadav") ?: "Amitesh Yadav"
    val email = authPrefs.getString("user_email", "amitesh69.yadav@gmail.com") ?: "amitesh69.yadav@gmail.com"
    val role = authPrefs.getString("user_role", "Computer Science Scholar") ?: "Computer Science Scholar"
    val uni = authPrefs.getString("user_university", "Imperial College London") ?: "Imperial College London"
    val studentId = authPrefs.getString("user_student_id", "CS-2024-8841") ?: "CS-2024-8841"
    val initials = authPrefs.getString("user_initials", "AY") ?: "AY"
    val provider = authPrefs.getString("auth_provider", "Academic SSO") ?: "Academic SSO"
    val isLoggedIn = authPrefs.getBoolean("is_logged_in", true)
    return UserProfile(
      id = activeId,
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
      .putString("active_user_id", user.id)
      .putString("user_name", user.name)
      .putString("user_email", user.email)
      .putString("user_role", user.role)
      .putString("user_university", user.university)
      .putString("user_student_id", user.studentId)
      .putString("user_initials", user.avatarInitials)
      .putString("auth_provider", user.authProvider)
      .putBoolean("is_logged_in", isLoggedIn)
      .apply()

    val currentList = _savedAccounts.value.toMutableList()
    val idx = currentList.indexOfFirst { it.id == user.id || it.email.equals(user.email, ignoreCase = true) }
    if (idx >= 0) {
      currentList[idx] = user.copy(isLoggedIn = isLoggedIn)
    } else {
      currentList.add(user.copy(isLoggedIn = isLoggedIn))
    }
    _savedAccounts.value = currentList
    saveRegisteredAccounts(currentList)
  }

  private val _savedAccounts = MutableStateFlow<List<UserProfile>>(loadRegisteredAccounts())
  val savedAccounts: StateFlow<List<UserProfile>> = _savedAccounts.asStateFlow()

  private val _currentUser = MutableStateFlow<UserProfile>(loadSavedUser())
  val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

  private val _isAuthenticated = MutableStateFlow(authPrefs.getBoolean("is_logged_in", true))
  val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

  // --- Reactive States (Per-Student Isolated Data Stream) ---
  @OptIn(ExperimentalCoroutinesApi::class)
  val subjects: StateFlow<List<Subject>> = _currentUser
    .flatMapLatest { user -> repository.getSubjectsForUser(user.id) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  @OptIn(ExperimentalCoroutinesApi::class)
  val sessions: StateFlow<List<TimetableSession>> = _currentUser
    .flatMapLatest { user -> repository.getSessionsForUser(user.id) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  @OptIn(ExperimentalCoroutinesApi::class)
  val topics: StateFlow<List<StudyTopic>> = _currentUser
    .flatMapLatest { user -> repository.getTopicsForUser(user.id) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  @OptIn(ExperimentalCoroutinesApi::class)
  val tasks: StateFlow<List<StudyTask>> = _currentUser
    .flatMapLatest { user -> repository.getTasksForUser(user.id) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  @OptIn(ExperimentalCoroutinesApi::class)
  val quizAttempts: StateFlow<List<QuizAttempt>> = _currentUser
    .flatMapLatest { user -> repository.getQuizAttemptsForUser(user.id) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  @OptIn(ExperimentalCoroutinesApi::class)
  val tutorSessions: StateFlow<List<TutorSession>> = _currentUser
    .flatMapLatest { user -> repository.getTutorSessionsForUser(user.id) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  @OptIn(ExperimentalCoroutinesApi::class)
  val scheduleFormats: StateFlow<List<com.example.data.model.ScheduleFormat>> = _currentUser
    .flatMapLatest { user -> repository.getScheduleFormatsForUser(user.id) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val scheduleStatus: StateFlow<CurrentScheduleStatus> = sessions
    .map { list -> alarmAndCalendarService.evaluateCurrentStatus(list) }
    .stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      CurrentScheduleStatus(ScheduleState.FREE_TIME, formattedStatusText = "Checking schedule status...")
    )

  init {
    viewModelScope.launch {
      repository.seedSampleUniversityData(_currentUser.value.id)
    }

    // 1. Listen for background & broadcast alarm events from AlarmReceiver
    viewModelScope.launch {
      com.example.data.service.AlarmEventBus.events.collect { event ->
        when (event) {
          is com.example.data.service.AlarmEvent.Triggered -> {
            val session = sessions.value.find { it.id == event.sessionId }
              ?: TimetableSession(
                id = event.sessionId,
                subjectId = 0L,
                subjectName = event.subjectName,
                topic = event.topic,
                date = "",
                startTime = event.startTime,
                endTime = event.endTime,
                location = event.location,
                notes = event.notes,
                isAlarmEnabled = true
              )
            _ringingAlarmSession.value = session
          }
          is com.example.data.service.AlarmEvent.Dismissed -> {
            _ringingAlarmSession.value = null
          }
          is com.example.data.service.AlarmEvent.Snoozed -> {
            _ringingAlarmSession.value = null
          }
        }
      }
    }

    // 2. Real-time in-app alarm trigger: checks upcoming sessions every 10 seconds
    viewModelScope.launch {
      val triggeredSessionIdsToday = mutableSetOf<Long>()
      while (true) {
        kotlinx.coroutines.delay(10_000)
        try {
          val now = System.currentTimeMillis()
          val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(now))
          val todaySessions = sessions.value.filter { it.date == todayStr && it.isAlarmEnabled }
          for (session in todaySessions) {
            if (triggeredSessionIdsToday.contains(session.id)) continue
            val triggerTime = alarmAndCalendarService.computeSessionTriggerTimeMillis(session)
            // Trigger if within 60 seconds window
            if (now >= triggerTime && now <= triggerTime + 70_000) {
              triggeredSessionIdsToday.add(session.id)
              triggerTestAlarm(session)
              break
            }
          }
        } catch (e: Exception) {
          // Ignore
        }
      }
    }

    // 3. Automatically schedule exact alarms for all enabled sessions
    viewModelScope.launch {
      sessions.collect { list ->
        list.filter { it.isAlarmEnabled }.forEach { s ->
          alarmAndCalendarService.scheduleSessionAlarm(s)
        }
      }
    }
  }

  // --- Account Actions ---
  fun switchAccount(user: UserProfile) {
    val updated = user.copy(isLoggedIn = true, lastLoginTime = System.currentTimeMillis())
    saveUser(updated, isLoggedIn = true)
    _currentUser.value = updated
    _isAuthenticated.value = true
    _currentScreen.value = Screen.Dashboard
    viewModelScope.launch {
      repository.seedSampleUniversityData(updated.id)
      _snackbarMessage.emit("Switched to student ${updated.name} (${updated.studentId})")
    }
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

    val existing = _savedAccounts.value.find { it.email.equals(cleanEmail, ignoreCase = true) }
    val userToActivate = if (existing != null) {
      existing.copy(isLoggedIn = true, rememberMe = rememberMe, lastLoginTime = System.currentTimeMillis())
    } else {
      val namePart = cleanEmail.substringBefore("@").replace(".", " ").split(" ")
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
      val initials = namePart.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
        .ifBlank { "ST" }
      val generatedId = "student_" + cleanEmail.replace("@", "_").replace(".", "_")
      UserProfile(
        id = generatedId,
        name = namePart,
        email = cleanEmail,
        role = "University Student",
        university = "Academic Scholar University",
        studentId = "STU-${(1000..9999).random()}",
        avatarInitials = initials,
        isLoggedIn = true,
        authProvider = "Academic Email & Password",
        rememberMe = rememberMe,
        lastLoginTime = System.currentTimeMillis()
      )
    }

    saveUser(userToActivate, isLoggedIn = true)
    _currentUser.value = userToActivate
    _isAuthenticated.value = true
    _currentScreen.value = Screen.Dashboard
    viewModelScope.launch {
      repository.seedSampleUniversityData(userToActivate.id)
      _snackbarMessage.emit("Signed in successfully as ${userToActivate.name}")
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
    val generatedId = "student_" + cleanEmail.replace("@", "_").replace(".", "_")
    val newUser = UserProfile(
      id = generatedId,
      name = name.trim(),
      email = cleanEmail,
      role = "University Student",
      university = university.ifBlank { "University Academic Scholar" },
      studentId = studentId.ifBlank { "ID-${(1000..9999).random()}" },
      avatarInitials = initials,
      isLoggedIn = true,
      authProvider = "Academic Email & Password",
      rememberMe = rememberMe,
      lastLoginTime = System.currentTimeMillis()
    )
    saveUser(newUser, isLoggedIn = true)
    _currentUser.value = newUser
    _isAuthenticated.value = true
    _currentScreen.value = Screen.Dashboard
    viewModelScope.launch {
      repository.seedSampleUniversityData(newUser.id)
      _snackbarMessage.emit("Welcome to StudyPilot, ${newUser.name}!")
    }
    return true
  }

  fun signInWithGoogle(
    idToken: String,
    displayName: String?,
    email: String?,
    onComplete: (Boolean, String?) -> Unit
  ) {
    try {
      val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
      com.google.firebase.auth.FirebaseAuth.getInstance().signInWithCredential(credential)
        .addOnSuccessListener { authResult ->
          val fbUser = authResult.user
          val studentName = displayName ?: fbUser?.displayName ?: "Amitesh Yadav"
          val studentEmail = email ?: fbUser?.email ?: "amitesh69.yadav@gmail.com"
          val initials = studentName.trim().split(" ")
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .take(2)
            .joinToString("")
            .ifBlank { "AY" }
          val userId = fbUser?.uid ?: ("google_" + studentEmail.replace("@", "_").replace(".", "_"))
          val user = UserProfile(
            id = userId,
            name = studentName,
            email = studentEmail,
            role = "University Scholar",
            university = _currentUser.value.university.ifBlank { "Imperial College London" },
            studentId = _currentUser.value.studentId.ifBlank { "CS-2024-8841" },
            avatarInitials = initials,
            isLoggedIn = true,
            authProvider = "Google Authentication (Firebase)",
            rememberMe = true,
            lastLoginTime = System.currentTimeMillis()
          )
          saveUser(user, isLoggedIn = true)
          _currentUser.value = user
          _isAuthenticated.value = true
          _currentScreen.value = Screen.Dashboard
          viewModelScope.launch {
            repository.seedSampleUniversityData(user.id)
            _snackbarMessage.emit("Welcome, ${user.name}!")
          }
          onComplete(true, null)
        }
        .addOnFailureListener { exception ->
          android.util.Log.e("StudyPilotVM", "Firebase signInWithCredential failed", exception)
          onComplete(false, exception.localizedMessage)
        }
    } catch (e: Exception) {
      android.util.Log.e("StudyPilotVM", "Firebase Auth error", e)
      onComplete(false, e.localizedMessage)
    }
  }

  fun loginWithGoogle() {
    val fbUser = try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser } catch (_: Exception) { null }
    val studentEmail = fbUser?.email ?: "amitesh69.yadav@gmail.com"
    val studentName = fbUser?.displayName ?: "Amitesh Yadav"
    val userId = fbUser?.uid ?: "student_amitesh"
    val user = UserProfile(
      id = userId,
      name = studentName,
      email = studentEmail,
      role = "Computer Science Scholar",
      university = "Imperial College London",
      studentId = "CS-2024-8841",
      avatarInitials = "AY",
      isLoggedIn = true,
      authProvider = "Google Authentication (Firebase)",
      rememberMe = true,
      lastLoginTime = System.currentTimeMillis()
    )
    saveUser(user, isLoggedIn = true)
    _currentUser.value = user
    _isAuthenticated.value = true
    _currentScreen.value = Screen.Dashboard
    viewModelScope.launch {
      repository.seedSampleUniversityData(user.id)
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
      repository.seedSampleUniversityData(guestUser.id)
      _snackbarMessage.emit("Exploring in Guest Demo Mode")
    }
  }

  fun logout() {
    try {
      com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
    } catch (_: Exception) {}
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
            searchGroundingUsed = _useGoogleSearch.value,
            userId = _currentUser.value.id
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
      repository.addSubject(name, code, colorHex, userId = _currentUser.value.id)
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
      val sessionWithUser = if (session.userId.isBlank()) session.copy(userId = _currentUser.value.id) else session
      repository.addSession(sessionWithUser)
      _snackbarMessage.emit("Class '${session.topic}' scheduled")
    }
  }

  fun updateSessionStatus(session: TimetableSession, newStatus: String) {
    viewModelScope.launch {
      val sessionWithUser = if (session.userId.isBlank()) session.copy(userId = _currentUser.value.id) else session
      repository.updateSession(sessionWithUser.copy(status = newStatus))
      _snackbarMessage.emit("Session marked as $newStatus")
    }
  }

  fun deleteSession(id: Long) {
    viewModelScope.launch {
      repository.deleteSession(id, userId = _currentUser.value.id)
      _snackbarMessage.emit("Class schedule deleted")
    }
  }

  fun updateSession(session: TimetableSession) {
    viewModelScope.launch {
      val sessionWithUser = if (session.userId.isBlank()) session.copy(userId = _currentUser.value.id) else session
      repository.updateSession(sessionWithUser)
      _snackbarMessage.emit("Updated '${session.topic}' (${session.startTime} - ${session.endTime})")
    }
  }

  fun toggleSessionAlarm(session: TimetableSession) {
    viewModelScope.launch {
      val updated = session.copy(isAlarmEnabled = !session.isAlarmEnabled)
      repository.updateSession(updated)
      if (updated.isAlarmEnabled) {
        alarmAndCalendarService.scheduleSessionAlarm(updated)
        _snackbarMessage.emit("⏰ Alarm enabled for '${session.topic}' (${session.startTime})")
      } else {
        alarmAndCalendarService.cancelSessionAlarm(updated)
        _snackbarMessage.emit("Alarm turned off for '${session.topic}'")
      }
    }
  }

  fun triggerTestAlarm(session: TimetableSession) {
    _ringingAlarmSession.value = session
    alarmAndCalendarService.triggerAlarmAlert(session)
    viewModelScope.launch {
      _snackbarMessage.emit("⏰ Alarm ringing for '${session.topic}'!")
    }
  }

  fun scheduleTestAlarm(session: TimetableSession, delaySeconds: Int = 5) {
    alarmAndCalendarService.scheduleTestAlarm(session, delaySeconds)
    viewModelScope.launch {
      _snackbarMessage.emit("⏰ Alarm set for $delaySeconds seconds from now! Testing ringtone & notification...")
    }
  }

  fun triggerSessionAlarmById(sessionId: Long) {
    val session = sessions.value.find { it.id == sessionId }
    if (session != null) {
      triggerTestAlarm(session)
    }
  }

  fun dismissAlarm() {
    alarmAndCalendarService.stopAlarmSound()
    _ringingAlarmSession.value = null
    viewModelScope.launch {
      _snackbarMessage.emit("Alarm dismissed")
    }
  }

  fun snoozeAlarm(session: TimetableSession, minutes: Int = 5) {
    alarmAndCalendarService.stopAlarmSound()
    _ringingAlarmSession.value = null
    alarmAndCalendarService.scheduleSnoozeAlarm(session.id, session.subjectName, session.topic, session.startTime, minutes)
    viewModelScope.launch {
      _snackbarMessage.emit("⏰ Snoozed for $minutes minutes for '${session.topic}'")
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
      val uid = _currentUser.value.id
      newSessions.forEach { repository.addSession(it.copy(userId = uid)) }
      _snackbarMessage.emit("Created ${newSessions.size} schedule session(s)!")
    }
  }

  fun replaceDayScheduleWithSessions(targetDate: String, newSessions: List<TimetableSession>) {
    viewModelScope.launch {
      val uid = _currentUser.value.id
      repository.clearSessionsForDate(targetDate, userId = uid)
      newSessions.forEach { session ->
        repository.addSession(session.copy(id = 0L, date = targetDate, userId = uid))
      }
      _snackbarMessage.emit("Applied ${newSessions.size} session(s) to $targetDate!")
    }
  }

  fun copyDaySchedule(sourceDate: String, targetDate: String, overwriteExisting: Boolean = false) {
    viewModelScope.launch {
      val uid = _currentUser.value.id
      val sourceSessions = sessions.value.filter { it.date == sourceDate }
      if (sourceSessions.isEmpty()) {
        _snackbarMessage.emit("No sessions found on $sourceDate to copy.")
        return@launch
      }

      if (overwriteExisting) {
        repository.clearSessionsForDate(targetDate, userId = uid)
      }

      sourceSessions.forEach { s ->
        repository.addSession(
          s.copy(
            id = 0L,
            userId = uid,
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
        userId = _currentUser.value.id,
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
      val uid = _currentUser.value.id
      val parsedSessions = mutableListOf<TimetableSession>()
      try {
        val jsonArray = org.json.JSONArray(format.formatSessionsJson)
        for (i in 0 until jsonArray.length()) {
          val obj = jsonArray.getJSONObject(i)
          parsedSessions.add(
            TimetableSession(
              userId = uid,
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
        repository.clearSessionsForDate(targetDate, userId = uid)
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
      repository.clearSessionsForDate(targetDate, userId = _currentUser.value.id)
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
        val uid = _currentUser.value.id
        val topic = StudyTopic(
          userId = uid,
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
            filterSubjectId = subjectId,
            userId = uid
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
          userId = _currentUser.value.id,
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
          forceWeakOnly = forceWeakOnly,
          userId = _currentUser.value.id
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
          topicsSkippedCount = current.queuedUntestedTopics.size,
          userId = _currentUser.value.id
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
