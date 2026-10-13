package com.example.data.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.model.TimetableSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed class AlarmEvent {
  data class Triggered(
    val sessionId: Long,
    val subjectName: String,
    val topic: String,
    val startTime: String,
    val endTime: String,
    val location: String,
    val notes: String
  ) : AlarmEvent()

  data class Dismissed(val sessionId: Long) : AlarmEvent()
  data class Snoozed(val sessionId: Long, val minutes: Int) : AlarmEvent()
}

object AlarmEventBus {
  private val _events = MutableSharedFlow<AlarmEvent>(extraBufferCapacity = 10)
  val events: SharedFlow<AlarmEvent> = _events.asSharedFlow()

  fun emit(event: AlarmEvent) {
    _events.tryEmit(event)
  }
}

class AlarmReceiver : BroadcastReceiver() {

  companion object {
    const val ACTION_TRIGGER_STUDY_ALARM = "com.example.ACTION_TRIGGER_STUDY_ALARM"
    const val ACTION_DISMISS_ALARM = "com.example.ACTION_DISMISS_ALARM"
    const val ACTION_SNOOZE_ALARM = "com.example.ACTION_SNOOZE_ALARM"

    const val EXTRA_SESSION_ID = "extra_session_id"
    const val EXTRA_SUBJECT_NAME = "extra_subject_name"
    const val EXTRA_TOPIC = "extra_topic"
    const val EXTRA_START_TIME = "extra_start_time"
    const val EXTRA_END_TIME = "extra_end_time"
    const val EXTRA_LOCATION = "extra_location"
    const val EXTRA_NOTES = "extra_notes"
  }

  override fun onReceive(context: Context, intent: Intent) {
    val service = AlarmAndCalendarService(context)

    when (intent.action) {
      ACTION_TRIGGER_STUDY_ALARM -> {
        val sessionId = intent.getLongExtra(EXTRA_SESSION_ID, 0L)
        val subjectName = intent.getStringExtra(EXTRA_SUBJECT_NAME) ?: "Class Session"
        val topic = intent.getStringExtra(EXTRA_TOPIC) ?: "Study Session"
        val startTime = intent.getStringExtra(EXTRA_START_TIME) ?: "Now"
        val endTime = intent.getStringExtra(EXTRA_END_TIME) ?: ""
        val location = intent.getStringExtra(EXTRA_LOCATION) ?: "Campus"
        val notes = intent.getStringExtra(EXTRA_NOTES) ?: ""

        val session = TimetableSession(
          id = sessionId,
          subjectId = 0L,
          subjectName = subjectName,
          topic = topic,
          date = "",
          startTime = startTime,
          endTime = endTime,
          location = location,
          notes = notes,
          isAlarmEnabled = true
        )

        // 1. Play sound, vibration, and post system notification
        service.triggerAlarmAlert(session)

        // 2. Notify in-app state bus so the dialog immediately shows
        AlarmEventBus.emit(
          AlarmEvent.Triggered(
            sessionId = sessionId,
            subjectName = subjectName,
            topic = topic,
            startTime = startTime,
            endTime = endTime,
            location = location,
            notes = notes
          )
        )
      }

      ACTION_DISMISS_ALARM -> {
        val sessionId = intent.getLongExtra(EXTRA_SESSION_ID, 0L)
        service.stopAlarmSound()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(sessionId.toInt())

        AlarmEventBus.emit(AlarmEvent.Dismissed(sessionId))
      }

      ACTION_SNOOZE_ALARM -> {
        val sessionId = intent.getLongExtra(EXTRA_SESSION_ID, 0L)
        val subjectName = intent.getStringExtra(EXTRA_SUBJECT_NAME) ?: "Study Session"
        val topic = intent.getStringExtra(EXTRA_TOPIC) ?: "Review"
        val startTime = intent.getStringExtra(EXTRA_START_TIME) ?: "Now"

        service.stopAlarmSound()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(sessionId.toInt())

        // Reschedule alarm for 5 minutes in future
        service.scheduleSnoozeAlarm(sessionId, subjectName, topic, startTime, 5)

        AlarmEventBus.emit(AlarmEvent.Snoozed(sessionId, 5))
      }

      Intent.ACTION_BOOT_COMPLETED -> {
        // Reschedule all upcoming alarms from database
        CoroutineScope(Dispatchers.IO).launch {
          try {
            val db = AppDatabase.getDatabase(context)
            val allSessions = db.studyPilotDao().getAllSessionsList()
            allSessions.filter { it.isAlarmEnabled }.forEach { s ->
              service.scheduleSessionAlarm(s)
            }
          } catch (e: Exception) {
            // Ignore
          }
        }
      }
    }
  }
}
