package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.QuizAttempt
import com.example.data.model.QuizConfig
import com.example.data.model.StudyTopic
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError
import com.example.ui.viewmodel.StudyPilotViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuizScreen(
  viewModel: StudyPilotViewModel
) {
  val topics by viewModel.topics.collectAsState()
  val quizAttempts by viewModel.quizAttempts.collectAsState()
  val quizConfig by viewModel.quizConfig.collectAsState()
  val isAiLoading by viewModel.isAiLoading.collectAsState()

  var showConfigSection by remember { mutableStateOf(false) }

  val testedTopics = topics.filter { it.timesTested > 0 }
  val untestedTopics = topics.filter { it.timesTested == 0 }
  val strugglingTopics = topics.filter { it.masteryLevel == "STRUGGLING" }

  val coveragePercent = if (topics.isNotEmpty()) (testedTopics.size * 100 / topics.size) else 0

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)
  ) {
    // 1. Primary Quiz Trigger Hero Card
    item {
      ElevatedCard(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
          containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(44.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.AutoAwesome,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(24.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Generate Weekend Quiz",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "${quizConfig.questionCount} Questions • ${quizConfig.difficulty} Difficulty",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            IconButton(onClick = { showConfigSection = !showConfigSection }) {
              Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Quiz Config",
                tint = if (showConfigSection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "Assembles questions from your recorded lecture notes. Prioritizes untested and struggling concepts, rotating all eligible topics fairly across weekend quizzes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(16.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = { viewModel.startWeekendQuiz(forceWeakOnly = false) },
              enabled = !isAiLoading && topics.isNotEmpty(),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .weight(1f)
                .height(44.dp)
            ) {
              if (isAiLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Assembling...")
              } else {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Start Weekend Quiz", fontWeight = FontWeight.SemiBold)
              }
            }

            if (strugglingTopics.isNotEmpty()) {
              OutlinedButton(
                onClick = { viewModel.startWeekendQuiz(forceWeakOnly = true) },
                enabled = !isAiLoading,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .weight(1f)
                  .height(44.dp)
              ) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = RoseError, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Revise Weak (${strugglingTopics.size})", fontWeight = FontWeight.SemiBold)
              }
            }
          }
        }
      }
    }

    // 2. Quiz Configuration Panel (Collapsible)
    if (showConfigSection) {
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text("Quiz Assembly Settings", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(10.dp))

            // Quiz Days
            Text("Scheduled Days:", style = MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              listOf("Saturday", "Sunday", "Both").forEach { day ->
                FilterChip(
                  selected = quizConfig.quizDays.contains(day, ignoreCase = true),
                  onClick = { viewModel.updateQuizConfig(quizConfig.copy(quizDays = day)) },
                  label = { Text(day, fontSize = 12.sp) },
                  shape = RoundedCornerShape(10.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))
            // Question Count
            Text("Number of Questions:", style = MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              listOf(5, 10, 15, 20).forEach { count ->
                FilterChip(
                  selected = quizConfig.questionCount == count,
                  onClick = { viewModel.updateQuizConfig(quizConfig.copy(questionCount = count)) },
                  label = { Text("$count Qs", fontSize = 12.sp) },
                  shape = RoundedCornerShape(10.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))
            // Difficulty
            Text("Difficulty Level:", style = MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              listOf("MIXED", "EASY", "MEDIUM", "HARD").forEach { diff ->
                FilterChip(
                  selected = quizConfig.difficulty == diff,
                  onClick = { viewModel.updateQuizConfig(quizConfig.copy(difficulty = diff)) },
                  label = { Text(diff, fontSize = 12.sp) },
                  shape = RoundedCornerShape(10.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))
            // Prioritize Weak Switch
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("Prioritize Weak Topics", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                Text("Give overdue and struggling concepts higher weight", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Switch(
                checked = quizConfig.prioritizeWeakTopics,
                onCheckedChange = { viewModel.updateQuizConfig(quizConfig.copy(prioritizeWeakTopics = it)) }
              )
            }
          }
        }
      }
    }

    // 3. Topic Coverage Tracker Card
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Topic Coverage Tracker",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "$coveragePercent% Covered",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.primary
            )
          }

          Spacer(modifier = Modifier.height(8.dp))
          LinearProgressIndicator(
            progress = { if (topics.isNotEmpty()) (testedTopics.size.toFloat() / topics.size.toFloat()) else 0f },
            modifier = Modifier
              .fillMaxWidth()
              .height(8.dp)
              .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
          )

          Spacer(modifier = Modifier.height(14.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(
              modifier = Modifier.weight(1f),
              horizontalAlignment = Alignment.Start
            ) {
              Text(text = "${testedTopics.size}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = EmeraldSuccess)
              Text(text = "Topics Tested", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(
              modifier = Modifier.weight(1f),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(text = "${untestedTopics.size}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = AmberAccent)
              Text(text = "Untested (Queued)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(
              modifier = Modifier.weight(1f),
              horizontalAlignment = Alignment.End
            ) {
              Text(text = "${strugglingTopics.size}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = RoseError)
              Text(text = "Needs Review", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }

          if (untestedTopics.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = AmberAccent.copy(alpha = 0.1f)
            ) {
              Text(
                text = "📌 Untested topics queued for rotation: ${untestedTopics.joinToString(", ") { it.title }.take(100)}...",
                style = MaterialTheme.typography.labelSmall,
                color = AmberAccent,
                modifier = Modifier.padding(8.dp)
              )
            }
          }
        }
      }
    }

    // 4. Past Quiz Attempts History
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp)
      ) {
        SectionHeader(title = "Quiz History (${quizAttempts.size})")

        if (quizAttempts.isEmpty()) {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
          ) {
            Text(
              text = "No quizzes completed yet. Complete your first weekend revision quiz to track improvement!",
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
            quizAttempts.forEach { attempt ->
              QuizAttemptItemRow(attempt = attempt)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun QuizAttemptItemRow(attempt: QuizAttempt) {
  val scoreColor = when {
    attempt.scorePercentage >= 80 -> EmeraldSuccess
    attempt.scorePercentage >= 60 -> CyanAccent
    else -> AmberAccent
  }

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
          .size(46.dp)
          .clip(CircleShape)
          .background(scoreColor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "${attempt.scorePercentage}%",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          color = scoreColor
        )
      }
      Spacer(modifier = Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = attempt.title,
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "${attempt.correctCount}/${attempt.totalQuestions} correct • ${attempt.dateAttempted}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = "${attempt.topicsCoveredCount} topics tested" + if (attempt.topicsSkippedCount > 0) " (${attempt.topicsSkippedCount} queued in rotation)" else " (all covered)",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary
        )
      }
    }
  }
}
