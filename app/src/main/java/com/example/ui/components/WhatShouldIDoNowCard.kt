package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TimetableSession
import com.example.data.service.CurrentScheduleStatus
import com.example.data.service.ScheduleState
import com.example.ui.theme.PaletteCreamIvory
import com.example.ui.theme.PaletteDarkText
import com.example.ui.theme.PaletteDeepSage
import com.example.ui.theme.PaletteMediumSage
import com.example.ui.theme.PaletteMintFoam
import com.example.ui.theme.PalettePaleCeladon

@Composable
fun WhatShouldIDoNowCard(
  status: CurrentScheduleStatus,
  onPlanScheduleClick: () -> Unit,
  onEditSessionClick: (TimetableSession) -> Unit,
  onMarkSessionDone: (TimetableSession) -> Unit,
  onAskAiTutor: (TimetableSession) -> Unit,
  onToggleAlarm: (TimetableSession) -> Unit,
  onTestAlarm: (TimetableSession) -> Unit,
  onSyncGoogleCalendar: (TimetableSession) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = when (status.state) {
        ScheduleState.ACTIVE_NOW -> PaletteMintFoam
        ScheduleState.UPCOMING_NEXT -> PaletteCreamIvory
        ScheduleState.FREE_TIME, ScheduleState.DAY_COMPLETE -> PaletteMintFoam
      }
    ),
    border = androidx.compose.foundation.BorderStroke(
      width = 1.dp,
      color = when (status.state) {
        ScheduleState.ACTIVE_NOW -> PaletteDeepSage
        ScheduleState.UPCOMING_NEXT -> PaletteMediumSage
        else -> PaletteMediumSage.copy(alpha = 0.5f)
      }
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // Header: Alarm indicator + Google Calendar Status
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(
                when (status.state) {
                  ScheduleState.ACTIVE_NOW -> PaletteDeepSage
                  ScheduleState.UPCOMING_NEXT -> PaletteDeepSage
                  else -> PalettePaleCeladon
                }
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = when (status.state) {
                ScheduleState.ACTIVE_NOW -> Icons.Default.NotificationsActive
                ScheduleState.UPCOMING_NEXT -> Icons.Default.Alarm
                else -> Icons.Default.Schedule
              },
              contentDescription = "Status Icon",
              tint = when (status.state) {
                ScheduleState.ACTIVE_NOW, ScheduleState.UPCOMING_NEXT -> PaletteCreamIvory
                else -> PaletteDarkText
              },
              modifier = Modifier.size(18.dp)
            )
          }

          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "WHAT SHOULD I DO NOW?",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.8.sp
              ),
              color = PaletteDarkText
            )
            Text(
              text = when (status.state) {
                ScheduleState.ACTIVE_NOW -> "In Session Right Now • ${status.minutesRemaining}m left"
                ScheduleState.UPCOMING_NEXT -> "Starts in ${status.minutesUntilNext} min"
                ScheduleState.FREE_TIME -> "Free Study Window"
                ScheduleState.DAY_COMPLETE -> "Day Schedule Completed"
              },
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
              color = PaletteDarkText
            )
          }
        }

        // Live Google Calendar sync indicator pill
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = PalettePaleCeladon
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Sync,
              contentDescription = "Google Calendar Synced",
              tint = PaletteDarkText,
              modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "G-Calendar",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = PaletteDarkText
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      when (status.state) {
        ScheduleState.ACTIVE_NOW -> {
          val active = status.activeSession
          if (active != null) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = PaletteDeepSage
            ) {
              Text(
                text = active.subjectName,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = PaletteCreamIvory,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = active.topic,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = PaletteDarkText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "${active.startTime} - ${active.endTime} • ${active.location.ifBlank { "Campus Hall" }}",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
              color = PaletteDarkText
            )

            Spacer(modifier = Modifier.height(10.dp))
            // Progress Bar
            Column {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "Elapsed Time",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium,
                  color = PaletteDarkText
                )
                Text(
                  text = "${(status.progressFraction * 100).toInt()}%",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = PaletteDarkText
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              LinearProgressIndicator(
                progress = { status.progressFraction },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(6.dp)
                  .clip(RoundedCornerShape(3.dp)),
                color = PaletteDeepSage,
                trackColor = PalettePaleCeladon
              )
            }

            Spacer(modifier = Modifier.height(12.dp))
            // Symmetrical, evenly-spaced action buttons
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Button(
                onClick = { onMarkSessionDone(active) },
                colors = ButtonDefaults.buttonColors(
                  containerColor = PaletteDeepSage,
                  contentColor = PaletteCreamIvory
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .weight(1f)
                  .height(38.dp)
              ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Mark Done", fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }

              Button(
                onClick = { onAskAiTutor(active) },
                colors = ButtonDefaults.buttonColors(
                  containerColor = PalettePaleCeladon,
                  contentColor = PaletteDarkText
                ),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PaletteMediumSage),
                modifier = Modifier
                  .weight(1f)
                  .height(38.dp)
              ) {
                Icon(Icons.Default.Psychology, contentDescription = null, tint = PaletteDarkText, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("AI Tutor", fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              TextButton(onClick = { onEditSessionClick(active) }) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = PaletteDarkText)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Edit Schedule", color = PaletteDarkText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onTestAlarm(active) }, modifier = Modifier.size(32.dp)) {
                  Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = "Test Alarm",
                    tint = PaletteDeepSage,
                    modifier = Modifier.size(18.dp)
                  )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { onSyncGoogleCalendar(active) }, modifier = Modifier.size(32.dp)) {
                  Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Sync with Google Calendar",
                    tint = PaletteDarkText,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
            }
          }
        }

        ScheduleState.UPCOMING_NEXT -> {
          val next = status.nextSession
          if (next != null) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = PalettePaleCeladon
            ) {
              Text(
                text = next.subjectName,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = PaletteDarkText,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = next.topic,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = PaletteDarkText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Starts at ${next.startTime} - ${next.endTime} • ${next.location.ifBlank { "Campus Hall" }}",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
              color = PaletteDarkText
            )

            Spacer(modifier = Modifier.height(8.dp))
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = PaletteMintFoam
            ) {
              Text(
                text = status.suggestionText,
                style = MaterialTheme.typography.bodySmall,
                color = PaletteDarkText,
                modifier = Modifier.padding(10.dp)
              )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Button(
                onClick = { onAskAiTutor(next) },
                colors = ButtonDefaults.buttonColors(
                  containerColor = PaletteDeepSage,
                  contentColor = PaletteCreamIvory
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .weight(1f)
                  .height(38.dp)
              ) {
                Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Pre-Read with AI", fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }

              OutlinedButton(
                onClick = { onTestAlarm(next) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PaletteDarkText),
                border = androidx.compose.foundation.BorderStroke(1.dp, PaletteDeepSage),
                modifier = Modifier
                  .weight(1f)
                  .height(38.dp)
              ) {
                Icon(Icons.Default.Alarm, contentDescription = null, tint = PaletteDarkText, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ring Alarm", fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              TextButton(onClick = { onEditSessionClick(next) }) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = PaletteDarkText)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Edit Schedule", color = PaletteDarkText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }

              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (next.isAlarmEnabled) PalettePaleCeladon else PaletteMintFoam,
                modifier = Modifier.clickable { onToggleAlarm(next) }
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = if (next.isAlarmEnabled) Icons.Default.AlarmOn else Icons.Default.AlarmOff,
                    contentDescription = "Alarm status",
                    tint = PaletteDarkText,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = if (next.isAlarmEnabled) "Alarm ON" else "Alarm OFF",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PaletteDarkText
                  )
                }
              }
            }
          }
        }

        ScheduleState.FREE_TIME, ScheduleState.DAY_COMPLETE -> {
          Text(
            text = status.suggestionText,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = PaletteDarkText
          )

          Spacer(modifier = Modifier.height(12.dp))
          Button(
            onClick = onPlanScheduleClick,
            colors = ButtonDefaults.buttonColors(
              containerColor = PaletteDeepSage,
              contentColor = PaletteCreamIvory
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(40.dp)
          ) {
            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Plan / Edit Today's Schedule", fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }
        }
      }
    }
  }
}

/**
 * Ringing Alarm Alert Dialog that pops up when an alarm rings.
 */
@Composable
fun ActiveAlarmAlertDialog(
  session: TimetableSession,
  onDismiss: () -> Unit,
  onSnooze: () -> Unit,
  onStartSession: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(PaletteDeepSage),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.NotificationsActive,
            contentDescription = "Alarm Ringing",
            tint = PaletteCreamIvory,
            modifier = Modifier.size(22.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "ALARM RINGING!",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = PaletteDarkText
          )
          Text(
            text = "Time to study",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = PaletteDarkText
          )
        }
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = PalettePaleCeladon
        ) {
          Text(
            text = session.subjectName,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = PaletteDarkText,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
        Text(
          text = session.topic,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = PaletteDarkText
        )
        Text(
          text = "⏰ ${session.startTime} - ${session.endTime} (${session.date})",
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
          color = PaletteDarkText
        )
        if (session.location.isNotBlank()) {
          Text(
            text = "📍 Room / Location: ${session.location}",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = PaletteDarkText
          )
        }
        if (session.notes.isNotBlank()) {
          Text(
            text = "📝 Notes: ${session.notes}",
            style = MaterialTheme.typography.bodySmall,
            color = PaletteDarkText
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onStartSession,
        colors = ButtonDefaults.buttonColors(
          containerColor = PaletteDeepSage,
          contentColor = PaletteCreamIvory
        ),
        shape = RoundedCornerShape(10.dp)
      ) {
        Text("I'm On It! (Start)", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      Row {
        TextButton(onClick = onSnooze) {
          Text("Snooze 5m", color = PaletteDarkText, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(4.dp))
        TextButton(onClick = onDismiss) {
          Text("Dismiss", color = PaletteDarkText)
        }
      }
    },
    containerColor = PaletteCreamIvory,
    shape = RoundedCornerShape(16.dp)
  )
}
