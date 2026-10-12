package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.BuildConfig
import com.example.data.model.Subject
import com.example.ui.components.SectionHeader
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.viewmodel.StudyPilotViewModel

@Composable
fun SettingsScreen(
  viewModel: StudyPilotViewModel
) {
  val subjects by viewModel.subjects.collectAsState()
  val currentUser by viewModel.currentUser.collectAsState()
  var showAddSubjectDialog by remember { mutableStateOf(false) }
  var showEditProfileDialog by remember { mutableStateOf(false) }

  var editName by remember(currentUser) { mutableStateOf(currentUser.name) }
  var editEmail by remember(currentUser) { mutableStateOf(currentUser.email) }
  var editUniversity by remember(currentUser) { mutableStateOf(currentUser.university) }
  var editRole by remember(currentUser) { mutableStateOf(currentUser.role) }

  val isGeminiConfigured = BuildConfig.GEMINI_API_KEY.isNotBlank() &&
    BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)
  ) {
    // 0. Account Profile & Device Sync Card
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
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
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(48.dp)
                  .clip(CircleShape)
                  .background(com.example.ui.theme.PaletteDeepSage),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = currentUser.avatarInitials,
                  fontWeight = FontWeight.ExtraBold,
                  color = com.example.ui.theme.PaletteCreamIvory,
                  fontSize = 16.sp
                )
              }
              Column {
                Text(
                  text = currentUser.name,
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = currentUser.email,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = "${currentUser.university} • ${currentUser.studentId}",
                  style = MaterialTheme.typography.labelSmall,
                  color = com.example.ui.theme.PaletteDeepSage,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = EmeraldSuccess.copy(alpha = 0.15f),
              border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.4f))
            ) {
              Text(
                text = "Active Session",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldSuccess,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Account Management Buttons
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = {
                editName = currentUser.name
                editEmail = currentUser.email
                editUniversity = currentUser.university
                editRole = currentUser.role
                showEditProfileDialog = true
              },
              shape = RoundedCornerShape(10.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.PaletteMediumSage),
              modifier = Modifier
                .weight(1f)
                .height(38.dp)
            ) {
              Text("Edit Profile", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
              onClick = { viewModel.logout() },
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
              ),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .weight(1f)
                .height(38.dp)
            ) {
              Text("Sign Out / Switch", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(
                text = "💻 Laptop + 📱 Mobile Dual Mode (${currentUser.authProvider})",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Adaptive responsive UI enables side-by-side desktop navigation on your laptop and compact touch navigation on your mobile device.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }

    // 1. AI Status & Cost Optimization Card
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text("AI Engine Status", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(
                  text = if (isGeminiConfigured) "Gemini 3.5 Flash Active" else "Deterministic Academic Mode",
                  style = MaterialTheme.typography.bodySmall,
                  color = if (isGeminiConfigured) EmeraldSuccess else CyanAccent
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (isGeminiConfigured) EmeraldSuccess.copy(alpha = 0.15f) else CyanAccent.copy(alpha = 0.15f)
            ) {
              Text(
                text = if (isGeminiConfigured) "Connected" else "Zero AI Cost",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isGeminiConfigured) EmeraldSuccess else CyanAccent,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "• Questions and summaries are cached locally in SQLite to prevent redundant API calls.\n• Spaced repetition algorithm and topic rotation run locally on-device.\n• Complete offline support for timetable, notes, and revision.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    // 2. Subject & Course Manager
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Enrolled Subjects (${subjects.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Button(
              onClick = { showAddSubjectDialog = true },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(36.dp)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Add Subject", fontSize = 12.sp)
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          subjects.forEach { sub ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = MaterialTheme.colorScheme.primaryContainer
                ) {
                  Text(
                    text = sub.code,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                  )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(sub.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
              }

              IconButton(onClick = { viewModel.deleteSubject(sub) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
              }
            }
          }
        }
      }
    }

    // 3. Data Persistence & Sample Data
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Local Database Persistence", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "All schedules, study topics, quiz attempts, and spaced repetition intervals are stored in on-device SQLite via Room. Your progress is completely preserved across sessions.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(14.dp))
          OutlinedButton(
            onClick = { viewModel.seedSampleData() },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(44.dp)
          ) {
            Text("Reload Sample University Courses & Topics", fontWeight = FontWeight.SemiBold)
          }
        }
      }
    }
  }

  if (showAddSubjectDialog) {
    AddSubjectDialog(
      onDismiss = { showAddSubjectDialog = false },
      onConfirm = { name, code ->
        viewModel.addSubject(name, code, "#2563EB")
        showAddSubjectDialog = false
      }
    )
  }

  if (showEditProfileDialog) {
    AlertDialog(
      onDismissRequest = { showEditProfileDialog = false },
      title = { Text("Edit Academic Profile", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = editName,
            onValueChange = { editName = it },
            label = { Text("Full Name") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
          OutlinedTextField(
            value = editEmail,
            onValueChange = { editEmail = it },
            label = { Text("Academic Email") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
          OutlinedTextField(
            value = editUniversity,
            onValueChange = { editUniversity = it },
            label = { Text("University / Institution") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
          OutlinedTextField(
            value = editRole,
            onValueChange = { editRole = it },
            label = { Text("Academic Major / Role") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.updateProfile(editName, editEmail, editUniversity, editRole)
            showEditProfileDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.PaletteDeepSage),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("Save Profile")
        }
      },
      dismissButton = {
        TextButton(onClick = { showEditProfileDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
fun AddSubjectDialog(
  onDismiss: () -> Unit,
  onConfirm: (String, String) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var code by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Subject / Course", fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = code,
          onValueChange = { code = it },
          label = { Text("Course Code (e.g. CS201, BIO101)") },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Course Name (e.g. Operating Systems)") },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isNotBlank() && code.isNotBlank()) {
            onConfirm(name, code)
          }
        },
        shape = RoundedCornerShape(10.dp)
      ) {
        Text("Add Subject")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
