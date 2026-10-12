package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudyTask
import com.example.data.model.StudyTopic
import com.example.data.model.TimetableSession
import com.example.ui.components.MasteryBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatCard
import com.example.ui.components.WhatShouldIDoNowCard
import com.example.ui.theme.PaletteCreamIvory
import com.example.ui.theme.PaletteDarkText
import com.example.ui.theme.PaletteDeepSage
import com.example.ui.theme.PaletteMediumSage
import com.example.ui.theme.PaletteMintFoam
import com.example.ui.theme.PalettePaleCeladon
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.StudyPilotViewModel

@Composable
fun DashboardScreen(
  viewModel: StudyPilotViewModel,
  onNavigate: (Screen) -> Unit,
  onQuickAddTopic: () -> Unit,
  onQuickAddSession: () -> Unit
) {
  val sessions by viewModel.sessions.collectAsState()
  val topics by viewModel.topics.collectAsState()
  val tasks by viewModel.tasks.collectAsState()
  val attempts by viewModel.quizAttempts.collectAsState()
  val scheduleStatus by viewModel.scheduleStatus.collectAsState()
  val currentUser by viewModel.currentUser.collectAsState()
  val context = LocalContext.current

  val averageScore = if (attempts.isNotEmpty()) {
    attempts.map { it.scorePercentage }.average().toInt()
  } else 0

  val masteredCount = topics.count { it.masteryLevel == "MASTERED" }
  val strugglingCount = topics.count { it.masteryLevel == "STRUGGLING" }
  val pendingTasks = tasks.filter { !it.isCompleted }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(bottom = 24.dp)
  ) {
    // 1. Streak & Status Subheader Banner
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          val firstName = currentUser.name.trim().split(" ").firstOrNull() ?: "Scholar"
          Text(
            text = "Welcome back, $firstName",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
          )
          Text(
            text = "${currentUser.university} • Command Center",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Surface(
          shape = RoundedCornerShape(20.dp),
          color = PalettePaleCeladon,
          border = androidx.compose.foundation.BorderStroke(1.dp, PaletteMediumSage)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.LocalFireDepartment,
              contentDescription = "Streak",
              tint = PaletteDarkText,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "5-Day Streak",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = PaletteDarkText
            )
          }
        }
      }
    }

    // 2. Real-Time "What Should I Do Now?" Live Card
    item {
      Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        WhatShouldIDoNowCard(
          status = scheduleStatus,
          onPlanScheduleClick = { onNavigate(Screen.Timetable) },
          onEditSessionClick = { onNavigate(Screen.Timetable) },
          onMarkSessionDone = { session ->
            val next = if (session.status == "COMPLETED") "SCHEDULED" else "COMPLETED"
            viewModel.updateSessionStatus(session, next)
          },
          onAskAiTutor = { session ->
            viewModel.openTutorForSubject(session.subjectId, "Help me prepare notes and key concepts for ${session.topic}")
          },
          onToggleAlarm = { viewModel.toggleSessionAlarm(it) },
          onTestAlarm = { viewModel.triggerTestAlarm(it) },
          onSyncGoogleCalendar = { session ->
            viewModel.syncSessionToGoogleCalendar(session)
            val intent = viewModel.alarmAndCalendarService.createGoogleCalendarInsertIntent(session)
            try {
              context.startActivity(intent)
            } catch (e: Exception) {
              val webUrl = viewModel.alarmAndCalendarService.getGoogleCalendarWebUrl(session)
              context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)))
            }
          }
        )
      }
    }

    // 2. Next Scheduled Weekend Quiz Hero Banner
    item {
      ElevatedCard(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
      ) {
        Column(
          modifier = Modifier.padding(18.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "AI Quiz",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "AI Weekend Revision Quiz",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
              Text(
                text = "Scheduled: Saturday & Sunday at 10:00 AM",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "Prepares adaptive questions covering this week's topics. Balances overdue and weak areas so every topic is rotated.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
          )

          Spacer(modifier = Modifier.height(14.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = { viewModel.startWeekendQuiz(forceWeakOnly = false) },
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .weight(1f)
                .height(44.dp)
            ) {
              Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Start Quiz", fontWeight = FontWeight.SemiBold)
            }

            if (strugglingCount > 0) {
              OutlinedButton(
                onClick = { viewModel.startWeekendQuiz(forceWeakOnly = true) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .weight(1f)
                  .height(44.dp)
              ) {
                Text("Revise Weak ($strugglingCount)", fontWeight = FontWeight.SemiBold)
              }
            }
          }
        }
      }
    }

    // 3. Perfectly Balanced 2x2 Quick Action Buttons Grid
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = onQuickAddSession,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
              .weight(1f)
              .height(46.dp)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add Schedule", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
          }

          Button(
            onClick = onQuickAddTopic,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
              .weight(1f)
              .height(46.dp)
          ) {
            Icon(Icons.Default.Book, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Record Topic", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = { onNavigate(Screen.WeekendQuizzes) },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
              .weight(1f)
              .height(46.dp)
          ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Quiz Hub", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
          }

          Button(
            onClick = { onNavigate(Screen.AiTutor) },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier
              .weight(1f)
              .height(46.dp)
          ) {
            Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("AI Study Tutor", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // 4. Learning Stats Grid
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp)
      ) {
        SectionHeader(title = "Learning Overview")
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          StatCard(
            title = "Topics Studied",
            value = topics.size.toString(),
            subtitle = "$masteredCount mastered",
            icon = Icons.Default.Book,
            iconTint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
          )
          StatCard(
            title = "Avg Quiz Score",
            value = if (attempts.isNotEmpty()) "$averageScore%" else "—",
            subtitle = "${attempts.size} quizzes taken",
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            iconTint = PaletteDeepSage,
            modifier = Modifier.weight(1f)
          )
        }
      }
    }

    // 5. Today's Lectures & Study Sessions
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp)
      ) {
        SectionHeader(
          title = "Today's Timetable",
          actionText = "View All",
          onActionClick = { onNavigate(Screen.Timetable) }
        )

        if (sessions.isEmpty()) {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
          ) {
            Text(
              text = "No lectures scheduled for today. Take time to revise!",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(16.dp)
            )
          }
        } else {
          Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            sessions.take(3).forEach { session ->
              SessionItemRow(session = session, onToggleStatus = {
                val next = if (session.status == "COMPLETED") "SCHEDULED" else "COMPLETED"
                viewModel.updateSessionStatus(session, next)
              })
            }
          }
        }
      }
    }

    // 6. Topics Studied Recently
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp)
      ) {
        SectionHeader(
          title = "Recent Topics",
          actionText = "My Library",
          onActionClick = { onNavigate(Screen.MyTopics) }
        )

        if (topics.isEmpty()) {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
          ) {
            Text(
              text = "No study topics recorded yet. Record what you learned in lectures!",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(16.dp)
            )
          }
        } else {
          Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            topics.take(3).forEach { topic ->
              RecentTopicRow(topic = topic)
            }
          }
        }
      }
    }

    // 7. Pending Tasks & Upcoming Exams
    if (pendingTasks.isNotEmpty()) {
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
        ) {
          SectionHeader(
            title = "Assignments & Exam Deadlines",
            actionText = "Manage",
            onActionClick = { onNavigate(Screen.Tasks) }
          )

          Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            pendingTasks.take(2).forEach { task ->
              DashboardTaskRow(task = task)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SessionItemRow(
  session: TimetableSession,
  onToggleStatus: () -> Unit
) {
  val isDone = session.status == "COMPLETED"
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Schedule,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp)
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = session.topic,
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "${session.subjectName} • ${session.startTime} - ${session.endTime}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (session.location.isNotBlank()) {
          Text(
            text = "📍 ${session.location}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }
      Spacer(modifier = Modifier.width(8.dp))
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isDone) PaletteMintFoam else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable { onToggleStatus() }
      ) {
        Text(
          text = if (isDone) "Completed" else "Scheduled",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
          color = if (isDone) PaletteDarkText else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
      }
    }
  }
}

@Composable
private fun RecentTopicRow(topic: StudyTopic) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(PaletteMintFoam),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Book,
          contentDescription = null,
          tint = PaletteDarkText,
          modifier = Modifier.size(20.dp)
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = topic.title,
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "${topic.subjectName} • Studied ${topic.dateStudied}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (topic.timesTested > 0) {
          Text(
            text = "Tested ${topic.timesTested}x (${topic.timesCorrect} correct)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
      Spacer(modifier = Modifier.width(8.dp))
      MasteryBadge(level = topic.masteryLevel)
    }
  }
}

@Composable
private fun DashboardTaskRow(task: StudyTask) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(PaletteMintFoam),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Assignment,
          contentDescription = null,
          tint = PaletteDarkText,
          modifier = Modifier.size(20.dp)
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = task.title,
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "${task.subjectName} • Due: ${task.dueDate}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = PalettePaleCeladon,
        border = androidx.compose.foundation.BorderStroke(1.dp, PaletteMediumSage)
      ) {
        Text(
          text = task.priority,
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
          color = PaletteDarkText,
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
      }
    }
  }
}
