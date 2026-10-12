package com.example.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.CalendarContract
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.model.TimetableSession
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class CurrentScheduleStatus(
  val state: ScheduleState,
  val activeSession: TimetableSession? = null,
  val nextSession: TimetableSession? = null,
  val minutesRemaining: Int = 0,
  val minutesUntilNext: Int = 0,
  val progressFraction: Float = 0f,
  val formattedStatusText: String = "",
  val suggestionText: String = ""
)

enum class ScheduleState {
  ACTIVE_NOW,
  UPCOMING_NEXT,
  FREE_TIME,
  DAY_COMPLETE
}

class AlarmAndCalendarService(private val context: Context) {

  private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
  private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
  private val channelId = "studypilot_alarms_channel"

  private var activeRingtone: Ringtone? = null

  init {
    createNotificationChannel()
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      val channel = NotificationChannel(
        channelId,
        "StudyPilot Study Alarms",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Urgent reminders for scheduled study sessions and lectures"
        enableVibration(true)
        vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 400)
      }
      notificationManager?.createNotificationChannel(channel)
    }
  }

  // --- Real-Time "What Should I Do Now" Calculation ---
  fun evaluateCurrentStatus(sessions: List<TimetableSession>, currentTime: Date = Date()): CurrentScheduleStatus {
    val todayStr = dateFormat.format(currentTime)
    val todaySessions = sessions.filter { it.date == todayStr }
      .sortedBy { it.startTime }

    if (todaySessions.isEmpty()) {
      return CurrentScheduleStatus(
        state = ScheduleState.FREE_TIME,
        formattedStatusText = "No classes scheduled for today",
        suggestionText = "Tap 'Plan Today' to create your daily timetable or start a self-directed study sprint with the AI Tutor."
      )
    }

    val cal = Calendar.getInstance().apply { time = currentTime }
    val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

    // Check for currently active session
    for (session in todaySessions) {
      val (startH, startM) = parseTime(session.startTime)
      val (endH, endM) = parseTime(session.endTime)
      val startTotal = startH * 60 + startM
      val endTotal = endH * 60 + endM

      if (currentMinutes in startTotal until endTotal) {
        val remaining = endTotal - currentMinutes
        val elapsed = currentMinutes - startTotal
        val totalDuration = (endTotal - startTotal).coerceAtLeast(1)
        val progress = (elapsed.toFloat() / totalDuration).coerceIn(0f, 1f)

        return CurrentScheduleStatus(
          state = ScheduleState.ACTIVE_NOW,
          activeSession = session,
          minutesRemaining = remaining,
          progressFraction = progress,
          formattedStatusText = "${session.subjectName}: ${session.topic}",
          suggestionText = "Currently in progress ($remaining min left). Stay focused or ask AI Tutor for rapid concept explanations!"
        )
      }
    }

    // Check for upcoming next session today
    val nextSession = todaySessions.firstOrNull { session ->
      val (startH, startM) = parseTime(session.startTime)
      val startTotal = startH * 60 + startM
      startTotal > currentMinutes
    }

    if (nextSession != null) {
      val (startH, startM) = parseTime(nextSession.startTime)
      val startTotal = startH * 60 + startM
      val untilNext = startTotal - currentMinutes

      return CurrentScheduleStatus(
        state = ScheduleState.UPCOMING_NEXT,
        nextSession = nextSession,
        minutesUntilNext = untilNext,
        formattedStatusText = "Next up at ${nextSession.startTime}: ${nextSession.subjectName}",
        suggestionText = if (untilNext <= 15) {
          "Starts in $untilNext minutes! Prepare your notes, room: ${nextSession.location.ifBlank { "Campus" }}."
        } else {
          "$untilNext minutes of free time available. Great window for a quick 10-minute quiz or reviewing weak topics."
        }
      )
    }

    return CurrentScheduleStatus(
      state = ScheduleState.DAY_COMPLETE,
      formattedStatusText = "All scheduled sessions for today are finished!",
      suggestionText = "Great effort today! Take a break, review flash topics in analytics, or plan tomorrow's schedule."
    )
  }

  private fun parseTime(timeStr: String): Pair<Int, Int> {
    return try {
      val parts = timeStr.trim().split(":")
      Pair(parts[0].toInt(), parts[1].toInt())
    } catch (e: Exception) {
      Pair(9, 0)
    }
  }

  // --- Alarm Audio & Vibration Alert ---
  fun triggerAlarmAlert(session: TimetableSession) {
    // 1. Vibrate device
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        val vibrator = vibratorManager?.defaultVibrator
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 250, 500, 250, 500), -1))
      } else {
        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 250, 500, 250, 500), -1))
        } else {
          @Suppress("DEPRECATION")
          vibrator?.vibrate(longArrayOf(0, 500, 250, 500, 250, 500), -1)
        }
      }
    } catch (e: Exception) {
      // Ignore vibration error on emulators
    }

    // 2. Play gentle alarm ringtone
    try {
      stopAlarmSound()
      val alertUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
      activeRingtone = RingtoneManager.getRingtone(context, alertUri)?.apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        }
        play()
      }
    } catch (e: Exception) {
      // Ignore ringtone failure
    }

    // 3. Post System Notification
    postAlarmNotification(session)
  }

  fun stopAlarmSound() {
    try {
      activeRingtone?.stop()
      activeRingtone = null
    } catch (e: Exception) {
      // Ignore
    }
  }

  private fun postAlarmNotification(session: TimetableSession) {
    try {
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      }
      val pendingIntent = PendingIntent.getActivity(
        context,
        session.id.toInt(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

      val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        .setContentTitle("⏰ Time for ${session.subjectName}")
        .setContentText("${session.topic} (${session.startTime} - ${session.endTime})")
        .setStyle(NotificationCompat.BigTextStyle().bigText(
          "Scheduled now: ${session.topic}\nLocation: ${session.location.ifBlank { "Campus" }}\n${session.notes}"
        ))
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()

      notificationManager?.notify(session.id.toInt(), notification)
    } catch (e: Exception) {
      // Ignore
    }
  }

  // --- Google Calendar Sync Helpers ---

  /**
   * Generates a Google Calendar web & app template intent or URL.
   * This guarantees seamless synchronization and opening in Google Calendar.
   */
  fun createGoogleCalendarInsertIntent(session: TimetableSession): Intent {
    val (startH, startM) = parseTime(session.startTime)
    val (endH, endM) = parseTime(session.endTime)

    val dateParts = session.date.split("-")
    val year = dateParts.getOrNull(0)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
    val month = (dateParts.getOrNull(1)?.toIntOrNull() ?: 1) - 1
    val day = dateParts.getOrNull(2)?.toIntOrNull() ?: 1

    val startCal = Calendar.getInstance().apply {
      set(year, month, day, startH, startM, 0)
    }
    val endCal = Calendar.getInstance().apply {
      set(year, month, day, endH, endM, 0)
    }

    return Intent(Intent.ACTION_INSERT).apply {
      data = CalendarContract.Events.CONTENT_URI
      putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startCal.timeInMillis)
      putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endCal.timeInMillis)
      putExtra(CalendarContract.Events.TITLE, "${session.subjectName}: ${session.topic}")
      putExtra(CalendarContract.Events.DESCRIPTION, "StudyPilot Session\nNotes: ${session.notes}")
      putExtra(CalendarContract.Events.EVENT_LOCATION, session.location)
      putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
      putExtra(CalendarContract.Reminders.MINUTES, session.reminderMinutes)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
  }

  /**
   * Generates a Google Calendar URL that directly creates the event in Google Calendar online/app.
   */
  fun getGoogleCalendarWebUrl(session: TimetableSession): String {
    return try {
      val (startH, startM) = parseTime(session.startTime)
      val (endH, endM) = parseTime(session.endTime)

      val dateParts = session.date.split("-")
      val year = dateParts.getOrNull(0)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
      val month = (dateParts.getOrNull(1)?.toIntOrNull() ?: 1) - 1
      val day = dateParts.getOrNull(2)?.toIntOrNull() ?: 1

      val utcFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
      }

      val startCal = Calendar.getInstance().apply {
        set(year, month, day, startH, startM, 0)
      }
      val endCal = Calendar.getInstance().apply {
        set(year, month, day, endH, endM, 0)
      }

      val startStr = utcFormat.format(startCal.time)
      val endStr = utcFormat.format(endCal.time)

      val title = URLEncoder.encode("${session.subjectName}: ${session.topic}", "UTF-8")
      val details = URLEncoder.encode("StudyPilot AI Session\nNotes: ${session.notes}", "UTF-8")
      val location = URLEncoder.encode(session.location, "UTF-8")

      "https://calendar.google.com/calendar/render?action=TEMPLATE&text=$title&dates=$startStr/$endStr&details=$details&location=$location"
    } catch (e: Exception) {
      "https://calendar.google.com/calendar"
    }
  }

  /**
   * Directly inserts event into the local Android Calendar Provider if permission is available.
   */
  fun insertIntoCalendarProvider(session: TimetableSession): Long? {
    return try {
      val (startH, startM) = parseTime(session.startTime)
      val (endH, endM) = parseTime(session.endTime)

      val dateParts = session.date.split("-")
      val year = dateParts.getOrNull(0)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
      val month = (dateParts.getOrNull(1)?.toIntOrNull() ?: 1) - 1
      val day = dateParts.getOrNull(2)?.toIntOrNull() ?: 1

      val startCal = Calendar.getInstance().apply {
        set(year, month, day, startH, startM, 0)
      }
      val endCal = Calendar.getInstance().apply {
        set(year, month, day, endH, endM, 0)
      }

      val values = ContentValues().apply {
        put(CalendarContract.Events.DTSTART, startCal.timeInMillis)
        put(CalendarContract.Events.DTEND, endCal.timeInMillis)
        put(CalendarContract.Events.TITLE, "${session.subjectName}: ${session.topic}")
        put(CalendarContract.Events.DESCRIPTION, "StudyPilot AI Timetable Session\n${session.notes}")
        put(CalendarContract.Events.CALENDAR_ID, 1)
        put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        put(CalendarContract.Events.EVENT_LOCATION, session.location)
      }

      val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
      uri?.lastPathSegment?.toLongOrNull()
    } catch (e: Exception) {
      null
    }
  }
}
