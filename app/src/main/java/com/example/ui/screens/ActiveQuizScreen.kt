package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.QuizQuestion
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.viewmodel.ActiveQuizState
import com.example.ui.viewmodel.StudyPilotViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ActiveQuizScreen(
  viewModel: StudyPilotViewModel
) {
  val quizState by viewModel.activeQuizState.collectAsState()
  var showExitConfirmDialog by remember { mutableStateOf(false) }

  BackHandler {
    if (!quizState.isSubmitted) {
      showExitConfirmDialog = true
    } else {
      viewModel.exitActiveQuiz()
    }
  }

  if (quizState.isSubmitted) {
    // Score Summary & Review View
    QuizResultsView(
      quizState = quizState,
      onRetryMistakes = { viewModel.retryIncorrectQuestions() },
      onDone = { viewModel.exitActiveQuiz() }
    )
    return
  }

  val total = quizState.questions.size
  val currentIndex = quizState.currentIndex
  val currentQuestion = quizState.questions.getOrNull(currentIndex)

  if (currentQuestion == null) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text("No questions in active session")
    }
    return
  }

  val userAnswer = quizState.userAnswers[currentQuestion.id] ?: ""
  val isFlagged = quizState.flaggedQuestionIds.contains(currentQuestion.id)

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(quizState.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text("Question ${currentIndex + 1} of $total", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        },
        navigationIcon = {
          IconButton(onClick = { showExitConfirmDialog = true }) {
            Icon(Icons.Default.Close, contentDescription = "Exit Quiz")
          }
        },
        actions = {
          IconButton(onClick = { viewModel.toggleFlagCurrentQuestion() }) {
            Icon(
              imageVector = if (isFlagged) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
              contentDescription = "Flag for review",
              tint = if (isFlagged) AmberAccent else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
      )
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
    ) {
      // Progress Bar
      LinearProgressIndicator(
        progress = { ((currentIndex + 1).toFloat() / total.toFloat()) },
        modifier = Modifier
          .fillMaxWidth()
          .height(4.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant
      )

      LazyColumn(
        modifier = Modifier
          .weight(1f)
          .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Question Jump Palette
        item {
          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            quizState.questions.forEachIndexed { idx, q ->
              val isAnswered = quizState.userAnswers.containsKey(q.id) && quizState.userAnswers[q.id]?.isNotBlank() == true
              val isCurrent = idx == currentIndex
              val isQFlagged = quizState.flaggedQuestionIds.contains(q.id)

              val (bg, textColor) = when {
                isCurrent -> Pair(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
                isQFlagged -> Pair(AmberAccent.copy(alpha = 0.2f), AmberAccent)
                isAnswered -> Pair(EmeraldSuccess.copy(alpha = 0.2f), EmeraldSuccess)
                else -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
              }

              Box(
                modifier = Modifier
                  .size(34.dp)
                  .clip(CircleShape)
                  .background(bg)
                  .clickable { viewModel.selectQuestionIndex(idx) },
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "${idx + 1}",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                  color = textColor
                )
              }
            }
          }
        }

        // Question Card
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
          ) {
            Column(modifier = Modifier.padding(18.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = MaterialTheme.colorScheme.primaryContainer
                ) {
                  Text(
                    text = "${currentQuestion.subjectName} • ${currentQuestion.topicTitle}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                  )
                }

                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                  Text(
                    text = currentQuestion.questionType.replace("_", " "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(14.dp))
              Text(
                text = currentQuestion.questionText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, lineHeight = 24.sp),
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }

        // Answer Input Area
        item {
          when (currentQuestion.questionType) {
            "MULTIPLE_CHOICE" -> {
              val options = currentQuestion.optionsJson.split(",").map { it.trim() }.filter { it.isNotBlank() }
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { opt ->
                  val isSelected = userAnswer.equals(opt, ignoreCase = true)
                  Card(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clickable { viewModel.answerCurrentQuestion(opt) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                      containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                  ) {
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      RadioButton(
                        selected = isSelected,
                        onClick = { viewModel.answerCurrentQuestion(opt) }
                      )
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(
                        text = opt,
                        style = MaterialTheme.typography.bodyMedium.copy(
                          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                      )
                    }
                  }
                }
              }
            }

            "TRUE_FALSE" -> {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                listOf("True", "False").forEach { tf ->
                  val isSelected = userAnswer.equals(tf, ignoreCase = true)
                  Button(
                    onClick = { viewModel.answerCurrentQuestion(tf) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                      containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                      contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier
                      .weight(1f)
                      .height(56.dp)
                  ) {
                    Text(tf, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }

            "SHORT_ANSWER" -> {
              Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
              ) {
                Column(modifier = Modifier.padding(14.dp)) {
                  Text("Enter your answer:", style = MaterialTheme.typography.labelMedium)
                  Spacer(modifier = Modifier.height(6.dp))
                  OutlinedTextField(
                    value = userAnswer,
                    onValueChange = { viewModel.answerCurrentQuestion(it) },
                    placeholder = { Text("Type short answer...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                  )
                }
              }
            }

            "CODING" -> {
              Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
              ) {
                Column(modifier = Modifier.padding(14.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Code, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Code Editor / Implementation", style = MaterialTheme.typography.labelSmall, color = CyanAccent)
                  }
                  Spacer(modifier = Modifier.height(8.dp))
                  OutlinedTextField(
                    value = if (userAnswer.isBlank() && currentQuestion.codeStarter != null) currentQuestion.codeStarter else userAnswer,
                    onValueChange = { viewModel.answerCurrentQuestion(it) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = Color.White),
                    minLines = 6,
                    maxLines = 10,
                    shape = RoundedCornerShape(8.dp)
                  )
                }
              }
            }
          }
        }
      }

      // Bottom Navigation & Submit Bar
      Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = { viewModel.previousQuestion() },
            enabled = currentIndex > 0,
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Prev")
          }

          if (currentIndex == total - 1) {
            Button(
              onClick = { viewModel.submitActiveQuiz() },
              enabled = !quizState.isEvaluating,
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
            ) {
              if (quizState.isEvaluating) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Grading...")
              } else {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Submit Quiz")
              }
            }
          } else {
            Button(
              onClick = { viewModel.nextQuestion() },
              shape = RoundedCornerShape(10.dp)
            ) {
              Text("Next")
              Spacer(modifier = Modifier.width(4.dp))
              Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
          }
        }
      }
    }
  }

  if (showExitConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showExitConfirmDialog = false },
      title = { Text("Quit Active Quiz?") },
      text = { Text("Your in-progress answers will not be recorded in your revision history until submitted.") },
      confirmButton = {
        Button(
          onClick = {
            showExitConfirmDialog = false
            viewModel.exitActiveQuiz()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Exit")
        }
      },
      dismissButton = {
        TextButton(onClick = { showExitConfirmDialog = false }) {
          Text("Resume")
        }
      }
    )
  }
}

@Composable
fun QuizResultsView(
  quizState: ActiveQuizState,
  onRetryMistakes: () -> Unit,
  onDone: () -> Unit
) {
  val total = quizState.questions.size
  val correctCount = quizState.questions.count { q ->
    quizState.evaluations[q.id]?.isCorrect == true
  }
  val scorePercent = if (total > 0) (correctCount * 100 / total) else 0

  val scoreColor = when {
    scorePercent >= 80 -> EmeraldSuccess
    scorePercent >= 60 -> CyanAccent
    else -> AmberAccent
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 20.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Score Summary Card
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Box(
            modifier = Modifier
              .size(80.dp)
              .clip(CircleShape)
              .background(scoreColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "$scorePercent%",
              style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
              color = scoreColor
            )
          }

          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = if (scorePercent >= 80) "Outstanding Revision!" else if (scorePercent >= 60) "Solid Performance!" else "Revision Needed",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Scored $correctCount of $total questions correct",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "✅ Spaced repetition intervals automatically updated for all tested topics.",
            style = MaterialTheme.typography.labelSmall,
            color = EmeraldSuccess
          )

          Spacer(modifier = Modifier.height(16.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            if (correctCount < total) {
              OutlinedButton(
                onClick = onRetryMistakes,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Retry Mistakes")
              }
            }

            Button(
              onClick = onDone,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f)
            ) {
              Text("Back to Hub")
            }
          }
        }
      }
    }

    // Detailed Question Breakdown & Feedback
    item {
      Text(
        text = "Detailed Answer Breakdown",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground
      )
    }

    itemsIndexed(quizState.questions) { index, q ->
      val eval = quizState.evaluations[q.id]
      val isCorrect = eval?.isCorrect == true
      val userAns = quizState.userAnswers[q.id] ?: "(No answer)"

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Q${index + 1}: ${q.topicTitle}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (isCorrect) EmeraldSuccess.copy(alpha = 0.15f) else RoseError.copy(alpha = 0.15f)
            ) {
              Text(
                text = if (isCorrect) "Correct" else "Incorrect",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isCorrect) EmeraldSuccess else RoseError,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text(q.questionText, style = MaterialTheme.typography.bodyMedium)

          Spacer(modifier = Modifier.height(8.dp))
          Text("Your Answer: $userAns", style = MaterialTheme.typography.bodySmall, color = if (isCorrect) EmeraldSuccess else RoseError)
          Text("Correct Answer: ${q.correctAnswer}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))

          Spacer(modifier = Modifier.height(6.dp))
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
          ) {
            Text(
              text = "💡 Explanation: ${eval?.feedback ?: q.explanation}",
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(8.dp)
            )
          }
        }
      }
    }
  }
}
