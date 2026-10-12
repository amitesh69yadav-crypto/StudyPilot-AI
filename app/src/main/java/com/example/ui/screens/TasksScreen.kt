package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudyTask
import com.example.data.model.Subject
import com.example.ui.components.EmptyState
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError
import com.example.ui.viewmodel.StudyPilotViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TasksScreen(
  viewModel: StudyPilotViewModel
) {
  val tasks by viewModel.tasks.collectAsState()
  val subjects by viewModel.subjects.collectAsState()

  var selectedTab by remember { mutableIntStateOf(0) } // 0: All, 1: Exams, 2: Assignments
  var showAddDialog by remember { mutableStateOf(false) }

  val filteredTasks = when (selectedTab) {
    1 -> tasks.filter { it.type == "EXAM" }
    2 -> tasks.filter { it.type == "ASSIGNMENT" }
    else -> tasks
  }

  Scaffold(
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showAddDialog = true },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = CircleShape
      ) {
        Icon(Icons.Default.Add, contentDescription = "Add Task or Exam")
      }
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
    ) {
      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
      ) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = { Text("All (${tasks.size})", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = { Text("Exams (${tasks.count { it.type == "EXAM" }})", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
        )
        Tab(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          text = { Text("Assignments (${tasks.count { it.type == "ASSIGNMENT" }})", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
        )
      }

      if (filteredTasks.isEmpty()) {
        EmptyState(
          icon = Icons.AutoMirrored.Filled.EventNote,
          title = "No Deadlines Scheduled",
          description = "Keep track of midterm exams, finals, laboratory reports, and assignment submissions.",
          actionButton = {
            Button(onClick = { showAddDialog = true }, shape = RoundedCornerShape(12.dp)) {
              Text("Add Deadline")
            }
          }
        )
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(filteredTasks, key = { it.id }) { task ->
            TaskItemCard(
              task = task,
              onToggle = { viewModel.toggleTaskCompletion(task) },
              onDelete = { viewModel.deleteTask(task.id) }
            )
          }
        }
      }
    }
  }

  if (showAddDialog) {
    AddTaskDialog(
      subjects = subjects,
      onDismiss = { showAddDialog = false },
      onConfirm = { title, subName, dueDate, type, priority ->
        viewModel.addTask(title, subName, dueDate, type, priority)
        showAddDialog = false
      }
    )
  }
}

@Composable
fun TaskItemCard(
  task: StudyTask,
  onToggle: () -> Unit,
  onDelete: () -> Unit
) {
  val priorityColor = when (task.priority) {
    "HIGH" -> RoseError
    "MEDIUM" -> AmberAccent
    else -> EmeraldSuccess
  }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(onClick = onToggle, modifier = Modifier.size(36.dp)) {
        Icon(
          imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
          contentDescription = "Toggle completion",
          tint = if (task.isCompleted) EmeraldSuccess else MaterialTheme.colorScheme.outline,
          modifier = Modifier.size(24.dp)
        )
      }

      Spacer(modifier = Modifier.width(10.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (task.type == "EXAM") RoseError.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer
          ) {
            Text(
              text = task.type,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = if (task.type == "EXAM") RoseError else MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = priorityColor.copy(alpha = 0.15f)
          ) {
            Text(
              text = "${task.priority} Priority",
              style = MaterialTheme.typography.labelSmall,
              color = priorityColor,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = task.title,
          style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.SemiBold,
            textDecoration = if (task.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
          ),
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "${task.subjectName} • Due: ${task.dueDate}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
      }
    }
  }
}

@Composable
fun AddTaskDialog(
  subjects: List<Subject>,
  onDismiss: () -> Unit,
  onConfirm: (String, String, String, String, String) -> Unit
) {
  var title by remember { mutableStateOf("") }
  var selectedSubject by remember { mutableStateOf(subjects.firstOrNull()?.name ?: "General") }
  var type by remember { mutableStateOf("ASSIGNMENT") } // ASSIGNMENT, EXAM
  var priority by remember { mutableStateOf("MEDIUM") } // HIGH, MEDIUM, LOW

  val cal = Calendar.getInstance()
  cal.add(Calendar.DAY_OF_YEAR, 7)
  var dueDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Assignment or Exam", fontWeight = FontWeight.Bold) },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Title (e.g. Midterm 1, Problem Set 3)") },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = selectedSubject,
          onValueChange = { selectedSubject = it },
          label = { Text("Course / Subject Name") },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = dueDate,
          onValueChange = { dueDate = it },
          label = { Text("Due Date (YYYY-MM-DD)") },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf("ASSIGNMENT", "EXAM").forEach { t ->
            Button(
              onClick = { type = t },
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = if (type == t) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (type == t) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
              ),
              modifier = Modifier
                .weight(1f)
                .height(40.dp)
            ) {
              Text(t, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf("HIGH", "MEDIUM", "LOW").forEach { p ->
            Button(
              onClick = { priority = p },
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = if (priority == p) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (priority == p) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
              ),
              modifier = Modifier
                .weight(1f)
                .height(36.dp)
            ) {
              Text(p, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isNotBlank()) {
            onConfirm(title, selectedSubject, dueDate, type, priority)
          }
        },
        shape = RoundedCornerShape(10.dp)
      ) {
        Text("Save Deadline")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
