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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudyTopic
import com.example.data.model.Subject
import com.example.ui.components.EmptyState
import com.example.ui.components.MasteryBadge
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError
import com.example.ui.viewmodel.StudyPilotViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicsScreen(
  viewModel: StudyPilotViewModel,
  showAddDialogOnInit: Boolean = false
) {
  val topics by viewModel.topics.collectAsState()
  val subjects by viewModel.subjects.collectAsState()
  val isAiLoading by viewModel.isAiLoading.collectAsState()

  var searchQuery by remember { mutableStateOf("") }
  var selectedSubjectFilter by remember { mutableStateOf<Long?>(null) }
  var selectedMasteryFilter by remember { mutableStateOf<String?>(null) }

  var showAddDialog by remember { mutableStateOf(showAddDialogOnInit) }
  var detailTopic by remember { mutableStateOf<StudyTopic?>(null) }

  val filteredTopics = topics.filter { topic ->
    val matchesSearch = searchQuery.isBlank() ||
      topic.title.contains(searchQuery, ignoreCase = true) ||
      topic.subjectName.contains(searchQuery, ignoreCase = true) ||
      topic.descriptionNotes.contains(searchQuery, ignoreCase = true)

    val matchesSubject = selectedSubjectFilter == null || topic.subjectId == selectedSubjectFilter
    val matchesMastery = selectedMasteryFilter == null || topic.masteryLevel.equals(selectedMasteryFilter, ignoreCase = true)

    matchesSearch && matchesSubject && matchesMastery
  }

  Scaffold(
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showAddDialog = true },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = CircleShape
      ) {
        Icon(Icons.Default.Add, contentDescription = "Record Topic")
      }
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
    ) {
      // Search Bar
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
          if (searchQuery.isNotBlank()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(Icons.Default.Clear, contentDescription = "Clear")
            }
          }
        },
        placeholder = { Text("Search topics, keywords, notes...") },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
        singleLine = true
      )

      // Smooth horizontal scrolling Subject Filter Chips
      LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        item {
          FilterChip(
            selected = selectedSubjectFilter == null,
            onClick = { selectedSubjectFilter = null },
            label = { Text("All Subjects (${topics.size})") },
            shape = RoundedCornerShape(10.dp)
          )
        }
        items(subjects, key = { it.id }) { sub ->
          val count = topics.count { it.subjectId == sub.id }
          FilterChip(
            selected = selectedSubjectFilter == sub.id,
            onClick = {
              selectedSubjectFilter = if (selectedSubjectFilter == sub.id) null else sub.id
            },
            label = { Text("${sub.code} ($count)") },
            shape = RoundedCornerShape(10.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      if (filteredTopics.isEmpty()) {
        EmptyState(
          icon = Icons.Default.Book,
          title = "No Topics Found",
          description = "Record topics after your lectures using manual entry, pasting lecture notes, or uploading materials.",
          actionButton = {
            Button(onClick = { showAddDialog = true }, shape = RoundedCornerShape(12.dp)) {
              Text("Record Study Topic")
            }
          }
        )
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(filteredTopics, key = { it.id }) { topic ->
            TopicCard(
              topic = topic,
              onClick = { detailTopic = topic },
              onDelete = { viewModel.deleteTopic(topic.id) }
            )
          }
        }
      }
    }
  }

  // Add Topic Dialog (3 input methods: Manual, Upload, Paste)
  if (showAddDialog) {
    AddTopicDialog(
      subjects = subjects,
      onDismiss = { showAddDialog = false },
      onConfirm = { subjectId, subjectName, title, notes, sourceType, fileName, generateQuestions ->
        viewModel.addTopic(
          subjectId = subjectId,
          subjectName = subjectName,
          title = title,
          descriptionNotes = notes,
          sourceType = sourceType,
          uploadedFileName = fileName,
          generateQuestionsImmediately = generateQuestions
        )
        showAddDialog = false
      }
    )
  }

  // Topic Details & Spaced Repetition Dialog
  detailTopic?.let { topic ->
    TopicDetailDialog(
      topic = topic,
      isAiLoading = isAiLoading,
      onDismiss = { detailTopic = null },
      onSummarizeAi = {
        viewModel.summarizeTopicWithAi(topic.title, topic.descriptionNotes) { summary ->
          detailTopic = topic.copy(aiSummary = summary)
        }
      },
      onUpdateMastery = { newMastery ->
        viewModel.updateTopicMastery(topic, newMastery)
        detailTopic = topic.copy(masteryLevel = newMastery)
      },
      onAskTutor = {
        detailTopic = null
        viewModel.openTutorForSubject(topic.subjectId, "Can you explain '${topic.title}' from ${topic.subjectName} in detail?")
      }
    )
  }
}

@Composable
fun TopicCard(
  topic: StudyTopic,
  onClick: () -> Unit,
  onDelete: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() },
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.Top
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(CyanAccent.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Book,
          contentDescription = null,
          tint = CyanAccent,
          modifier = Modifier.size(20.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
          ) {
            Text(
              text = topic.subjectName,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            MasteryBadge(level = topic.masteryLevel)
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
              Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = topic.title,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = topic.descriptionNotes.take(120) + if (topic.descriptionNotes.length > 120) "..." else "",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Studied: ${topic.dateStudied} • Interval: ${topic.repetitionIntervalDays}d",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "Next: ${topic.nextRevisionDate}",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTopicDialog(
  subjects: List<Subject>,
  onDismiss: () -> Unit,
  onConfirm: (Long, String, String, String, String, String?, Boolean) -> Unit
) {
  var selectedTab by remember { mutableIntStateOf(0) } // 0: Manual, 1: Upload Materials, 2: Paste Lecture Notes
  var selectedSubject by remember { mutableStateOf(subjects.firstOrNull()) }
  var title by remember { mutableStateOf("") }
  var manualNotes by remember { mutableStateOf("") }
  var pastedNotes by remember { mutableStateOf("") }
  var uploadedFileName by remember { mutableStateOf<String?>(null) }
  var uploadedFileContent by remember { mutableStateOf("") }
  var generateQuestionsImmediate by remember { mutableStateOf(true) }

  var expandedDropdown by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Record Study Topic", fontWeight = FontWeight.Bold) },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // 3 Input Methods Tabs
        TabRow(
          selectedTabIndex = selectedTab,
          containerColor = MaterialTheme.colorScheme.surfaceVariant,
          contentColor = MaterialTheme.colorScheme.primary
        ) {
          Tab(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            text = { Text("Manual", fontSize = 12.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
          )
          Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            text = { Text("Upload", fontSize = 12.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
          )
          Tab(
            selected = selectedTab == 2,
            onClick = { selectedTab = 2 },
            text = { Text("Paste", fontSize = 12.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
          )
        }

        // Subject selector
        ExposedDropdownMenuBox(
          expanded = expandedDropdown,
          onExpandedChange = { expandedDropdown = !expandedDropdown }
        ) {
          OutlinedTextField(
            value = selectedSubject?.name ?: "Select Subject",
            onValueChange = {},
            readOnly = true,
            label = { Text("Subject") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
          )
          ExposedDropdownMenu(
            expanded = expandedDropdown,
            onDismissRequest = { expandedDropdown = false }
          ) {
            subjects.forEach { sub ->
              DropdownMenuItem(
                text = { Text("${sub.code}: ${sub.name}") },
                onClick = {
                  selectedSubject = sub
                  expandedDropdown = false
                }
              )
            }
          }
        }

        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Topic Title (e.g. Graph Algorithms)") },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        when (selectedTab) {
          0 -> {
            OutlinedTextField(
              value = manualNotes,
              onValueChange = { manualNotes = it },
              label = { Text("Key Takeaways & Lecture Notes") },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth(),
              minLines = 3,
              maxLines = 5
            )
          }
          1 -> {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(14.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(Icons.Default.UploadFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = uploadedFileName ?: "Attach slides or lecture PDF",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
              )
              Spacer(modifier = Modifier.height(8.dp))
              Button(
                onClick = {
                  uploadedFileName = "lecture_${title.ifBlank { "notes" }}.pdf"
                  uploadedFileContent = "Key definitions and theorem slides on $title with theoretical proofs, algorithms, and application problems."
                },
                shape = RoundedCornerShape(10.dp)
              ) {
                Text(if (uploadedFileName == null) "Select Study Material" else "File Attached")
              }
            }
          }
          2 -> {
            OutlinedTextField(
              value = pastedNotes,
              onValueChange = { pastedNotes = it },
              label = { Text("Paste Lecture Slides or Text Here") },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth(),
              minLines = 4,
              maxLines = 6
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isNotBlank()) {
            val subName = selectedSubject?.name ?: "General Study"
            val subId = selectedSubject?.id ?: 1L
            val (notes, type, fileName) = when (selectedTab) {
              0 -> Triple(manualNotes.ifBlank { "Studied core concepts of $title." }, "MANUAL", null)
              1 -> Triple(uploadedFileContent.ifBlank { "Uploaded slides on $title." }, "UPLOAD", uploadedFileName)
              else -> Triple(pastedNotes.ifBlank { "Pasted notes on $title." }, "PASTED", null)
            }
            onConfirm(subId, subName, title, notes, type, fileName, generateQuestionsImmediate)
          }
        },
        shape = RoundedCornerShape(10.dp)
      ) {
        Text("Save & Add to Revision")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun TopicDetailDialog(
  topic: StudyTopic,
  isAiLoading: Boolean,
  onDismiss: () -> Unit,
  onSummarizeAi: () -> Unit,
  onUpdateMastery: (String) -> Unit,
  onAskTutor: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(topic.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        MasteryBadge(level = topic.masteryLevel)
      }
    },
    text = {
      LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        item {
          Text(
            text = "${topic.subjectName} • Recorded ${topic.dateStudied}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        item {
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text("Lecture Notes", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
              Spacer(modifier = Modifier.height(4.dp))
              Text(topic.descriptionNotes, style = MaterialTheme.typography.bodyMedium)
            }
          }
        }

        // Spaced Repetition Stats Card
        item {
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text("Spaced Repetition Schedule", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.height(4.dp))
              Text("• Current Interval: ${topic.repetitionIntervalDays} days", style = MaterialTheme.typography.bodySmall)
              Text("• Next Revision Due: ${topic.nextRevisionDate}", style = MaterialTheme.typography.bodySmall)
              Text("• Times Tested: ${topic.timesTested} (${topic.timesCorrect} correct)", style = MaterialTheme.typography.bodySmall)
            }
          }
        }

        // AI Summary if available
        if (!topic.aiSummary.isNullOrBlank()) {
          item {
            Card(
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("AI Concept Summary", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(topic.aiSummary, style = MaterialTheme.typography.bodySmall)
              }
            }
          }
        }

        // AI Summarize Action Button
        item {
          Button(
            onClick = onSummarizeAi,
            enabled = !isAiLoading,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            if (isAiLoading) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Analyzing Material...")
            } else {
              Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Generate AI Study Summary")
            }
          }
        }

        // Ask AI Tutor Action Button
        item {
          OutlinedButton(
            onClick = onAskTutor,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ask AI Tutor about this Topic")
          }
        }

        // Manual Spaced Repetition Overrides
        item {
          Column {
            Text("Manual Mastery Adjustment:", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedButton(
                onClick = { onUpdateMastery("MASTERED") },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
              ) {
                Text("Understood", fontSize = 11.sp, color = EmeraldSuccess)
              }
              OutlinedButton(
                onClick = { onUpdateMastery("STRUGGLING") },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
              ) {
                Text("Needs Review", fontSize = 11.sp, color = RoseError)
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Done")
      }
    }
  )
}
