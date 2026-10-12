package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.data.model.Subject
import com.example.data.model.TutorModel
import com.example.data.model.TutorSession
import com.example.ui.components.FormattedTutorResponse
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError
import com.example.ui.viewmodel.StudyPilotViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class TutorTab {
  CHAT,
  HISTORY
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiTutorScreen(
  viewModel: StudyPilotViewModel
) {
  val context = LocalContext.current
  val messages by viewModel.chatMessages.collectAsState()
  val selectedModel by viewModel.selectedModel.collectAsState()
  val useGoogleSearch by viewModel.useGoogleSearch.collectAsState()
  val systemInstruction by viewModel.systemInstruction.collectAsState()
  val isChatLoading by viewModel.isChatLoading.collectAsState()

  val subjects by viewModel.subjects.collectAsState()
  val selectedSubject by viewModel.selectedTutorSubject.collectAsState()
  val tutorSessions by viewModel.tutorSessions.collectAsState()
  val activeSessionId by viewModel.activeSessionId.collectAsState()

  var selectedTab by remember { mutableStateOf(TutorTab.CHAT) }
  var isHeaderExpanded by remember { mutableStateOf(true) }
  var inputText by remember { mutableStateOf("") }
  var showSystemInstructionDialog by remember { mutableStateOf(false) }
  var showClearHistoryDialog by remember { mutableStateOf(false) }

  val listState = rememberLazyListState()

  // Auto scroll to bottom when new messages arrive in Chat tab
  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty() && selectedTab == TutorTab.CHAT) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  Column(
    modifier = Modifier.fillMaxSize()
  ) {
    // 1. Top Mode Selector Bar (Chat vs History tabs)
    Surface(
      color = MaterialTheme.colorScheme.surface,
      shadowElevation = 1.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Tab Switcher Buttons (Symmetrical Pill Switcher)
          Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(vertical = 4.dp)
          ) {
            Row(modifier = Modifier.padding(3.dp)) {
              Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (selectedTab == TutorTab.CHAT) MaterialTheme.colorScheme.primary else Color.Transparent,
                modifier = Modifier
                  .clickable { selectedTab = TutorTab.CHAT }
                  .clip(RoundedCornerShape(20.dp))
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    tint = if (selectedTab == TutorTab.CHAT) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Active Chat",
                    fontSize = 12.sp,
                    fontWeight = if (selectedTab == TutorTab.CHAT) FontWeight.Bold else FontWeight.Medium,
                    color = if (selectedTab == TutorTab.CHAT) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (selectedTab == TutorTab.HISTORY) MaterialTheme.colorScheme.primary else Color.Transparent,
                modifier = Modifier
                  .clickable { selectedTab = TutorTab.HISTORY }
                  .clip(RoundedCornerShape(20.dp))
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = if (selectedTab == TutorTab.HISTORY) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "History (${tutorSessions.size})",
                    fontSize = 12.sp,
                    fontWeight = if (selectedTab == TutorTab.HISTORY) FontWeight.Bold else FontWeight.Medium,
                    color = if (selectedTab == TutorTab.HISTORY) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }

          // Top Right Quick Actions + Hide/Show Elements Toggle
          Row(verticalAlignment = Alignment.CenterVertically) {
            // Hide/Show controls button for maximizing chat area
            if (selectedTab == TutorTab.CHAT) {
              IconButton(
                onClick = { isHeaderExpanded = !isHeaderExpanded },
                modifier = Modifier.size(36.dp)
              ) {
                Icon(
                  imageVector = if (isHeaderExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                  contentDescription = if (isHeaderExpanded) "Collapse Toolbar" else "Expand Toolbar",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(22.dp)
                )
              }
            }

            IconButton(
              onClick = {
                viewModel.startNewTutorChat()
                selectedTab = TutorTab.CHAT
              },
              modifier = Modifier.size(36.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "New Chat Session",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
            }

            IconButton(
              onClick = { showSystemInstructionDialog = true },
              modifier = Modifier.size(36.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Tutor Persona",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }

        // 2. Subject Scoping Selector (collapsible to maximize chat space)
        AnimatedVisibility(visible = isHeaderExpanded || selectedTab == TutorTab.HISTORY) {
          LazyRow(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
          item {
            FilterChip(
              selected = selectedSubject == null,
              onClick = { viewModel.selectTutorSubject(null) },
              label = {
                Text(
                  text = "🌐 All Subjects",
                  fontSize = 12.sp,
                  fontWeight = if (selectedSubject == null) FontWeight.Bold else FontWeight.Normal
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
              )
            )
          }

          items(subjects, key = { it.id }) { subject ->
            val isSelected = selectedSubject?.id == subject.id
            val subjectColor = try {
              Color(android.graphics.Color.parseColor(subject.colorHex))
            } catch (e: Exception) {
              MaterialTheme.colorScheme.primary
            }

            FilterChip(
              selected = isSelected,
              onClick = { viewModel.selectTutorSubject(subject) },
              leadingIcon = {
                Box(
                  modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(subjectColor)
                )
              },
              label = {
                Text(
                  text = "${subject.code} • ${subject.name}",
                  fontSize = 12.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = subjectColor.copy(alpha = 0.2f),
                selectedLabelColor = MaterialTheme.colorScheme.onSurface
              )
            )
          }
        }
      }
    }
  }

    // 3. Tab Contents
    if (selectedTab == TutorTab.CHAT) {
      ChatTabContent(
        viewModel = viewModel,
        messages = messages,
        selectedSubject = selectedSubject,
        selectedModel = selectedModel,
        useGoogleSearch = useGoogleSearch,
        isChatLoading = isChatLoading,
        isHeaderExpanded = isHeaderExpanded,
        onToggleHeader = { isHeaderExpanded = !isHeaderExpanded },
        inputText = inputText,
        onInputTextChange = { inputText = it },
        listState = listState,
        onClearChat = { viewModel.clearChat() }
      )
    } else {
      HistoryTabContent(
        tutorSessions = tutorSessions,
        selectedSubject = selectedSubject,
        activeSessionId = activeSessionId,
        onSelectSession = { session ->
          viewModel.loadTutorSession(session)
          selectedTab = TutorTab.CHAT
        },
        onDeleteSession = { sessionId ->
          viewModel.deleteTutorSession(sessionId)
        },
        onClearAllHistory = { showClearHistoryDialog = true },
        onStartNewChat = {
          viewModel.startNewTutorChat()
          selectedTab = TutorTab.CHAT
        }
      )
    }
  }

  // System Instruction Dialog
  if (showSystemInstructionDialog) {
    var editedInstruction by remember { mutableStateOf(systemInstruction) }
    AlertDialog(
      onDismissRequest = { showSystemInstructionDialog = false },
      title = { Text("AI Tutor Role & Academic Persona", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Fine-tune how the AI Tutor explains concepts and approaches problems:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          // Persona Presets
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
              onClick = {
                editedInstruction = "You are StudyPilot Academic Tutor, an expert professor explaining university topics clearly, breaking down proofs and derivations into digestible steps."
              },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f)
            ) {
              Text("Professor", fontSize = 11.sp)
            }
            Button(
              onClick = {
                editedInstruction = "You are an Elite Academic Research Guide specializing in STEM literature, citing recent arXiv and IEEE papers with precision."
              },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f)
            ) {
              Text("Researcher", fontSize = 11.sp)
            }
            Button(
              onClick = {
                editedInstruction = "You are a Principal Software Engineer & Algorithms Coach. Provide clean, well-tested Kotlin code and asymptotic runtime analysis."
              },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f)
            ) {
              Text("Coder", fontSize = 11.sp)
            }
          }

          OutlinedTextField(
            value = editedInstruction,
            onValueChange = { editedInstruction = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            maxLines = 6
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.updateSystemInstruction(editedInstruction)
            showSystemInstructionDialog = false
          }
        ) {
          Text("Save Persona")
        }
      },
      dismissButton = {
        TextButton(onClick = { showSystemInstructionDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Clear History Confirmation Dialog
  if (showClearHistoryDialog) {
    AlertDialog(
      onDismissRequest = { showClearHistoryDialog = false },
      title = { Text("Clear All Tutor History?", fontWeight = FontWeight.Bold) },
      text = {
        Text("This will permanently delete all past saved tutor conversations across all subjects.")
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.clearAllTutorHistory()
            showClearHistoryDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = RoseError)
        ) {
          Text("Clear All")
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearHistoryDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
private fun ChatTabContent(
  viewModel: StudyPilotViewModel,
  messages: List<ChatMessage>,
  selectedSubject: Subject?,
  selectedModel: TutorModel,
  useGoogleSearch: Boolean,
  isChatLoading: Boolean,
  isHeaderExpanded: Boolean,
  onToggleHeader: () -> Unit,
  inputText: String,
  onInputTextChange: (String) -> Unit,
  listState: androidx.compose.foundation.lazy.LazyListState,
  onClearChat: () -> Unit
) {
  val context = LocalContext.current

  Column(modifier = Modifier.fillMaxSize()) {
    // Subject Active Banner (if a subject is selected)
    if (selectedSubject != null) {
      val subColor = try {
        Color(android.graphics.Color.parseColor(selectedSubject.colorHex))
      } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
      }

      Surface(
        color = subColor.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.School,
              contentDescription = null,
              tint = subColor,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "${selectedSubject.code}: ${selectedSubject.name}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Tutor prompts & answers focused on this syllabus",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          TextButton(
            onClick = { viewModel.selectTutorSubject(null) }
          ) {
            Text("Reset", fontSize = 11.sp, color = subColor)
          }
        }
      }
    }

    // Compact banner when header is collapsed with quick expand tap
    AnimatedVisibility(visible = !isHeaderExpanded) {
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onToggleHeader() }
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "⚡ ${selectedModel.displayName}",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
            if (useGoogleSearch) {
              Text(
                text = "• 🌐 Web Search On",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            if (selectedSubject != null) {
              Text(
                text = "• ${selectedSubject.code}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Show controls", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            Icon(Icons.Default.ExpandMore, contentDescription = "Expand", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
          }
        }
      }
    }

    // Model Selector & Grounding Controls (Collapsible to maximize chat window)
    AnimatedVisibility(visible = isHeaderExpanded) {
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
      ) {
      Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        // Model Selection Chips
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          TutorModel.values().forEach { model ->
            val isSelected = selectedModel == model
            FilterChip(
              modifier = Modifier.weight(1f),
              selected = isSelected,
              onClick = {
                viewModel.selectTutorModel(model)
                if (model != TutorModel.FLASH && useGoogleSearch) {
                  viewModel.toggleGoogleSearch(false)
                }
              },
              label = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                  Text(
                    text = model.iconBadge,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                  )
                }
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
              )
            )
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Google Search Grounding Toggle Bar
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (useGoogleSearch) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface)
            .padding(horizontal = 10.dp, vertical = 5.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Public,
              contentDescription = null,
              tint = if (useGoogleSearch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "Google Search Grounding",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (useGoogleSearch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = if (useGoogleSearch) "Live web search active (gemini-3.5-flash)" else "Tap to fetch live web citations",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Switch(
            checked = useGoogleSearch,
            onCheckedChange = { viewModel.toggleGoogleSearch(it) },
            colors = SwitchDefaults.colors(
              checkedThumbColor = MaterialTheme.colorScheme.primary,
              checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
          )
        }
      }
    }
  }

    // Contextual Subject Quick Prompts (Collapsible)
    val subjectPrompts = getPromptsForSubject(selectedSubject)
    AnimatedVisibility(visible = isHeaderExpanded && subjectPrompts.isNotEmpty()) {
      LazyRow(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(subjectPrompts) { prompt ->
        PromptChip(
          text = prompt.first,
          onClick = {
            if (prompt.third) {
              viewModel.toggleGoogleSearch(true)
            }
            if (prompt.second != null) {
              viewModel.selectTutorModel(prompt.second)
            }
            viewModel.sendChatMessage(prompt.fourth)
          }
        )
      }
    }
  }

    // Scrollable Chat Thread
    LazyColumn(
      state = listState,
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      items(messages, key = { it.id }) { msg ->
        ChatMessageBubble(
          message = msg,
          onCopy = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Tutor Message", msg.text))
            Toast.makeText(context, "Copied response to clipboard", Toast.LENGTH_SHORT).show()
          },
          onOpenUrl = { url ->
            try {
              val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
              context.startActivity(intent)
            } catch (e: Exception) {
              Toast.makeText(context, "Could not open URL: $url", Toast.LENGTH_SHORT).show()
            }
          }
        )
      }

      if (isChatLoading) {
        item {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (useGoogleSearch) "Searching Google & synthesizing response..." else "Tutor is formatting explanation...",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }

    // Input Bar
    Surface(
      color = MaterialTheme.colorScheme.surface,
      shadowElevation = 8.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = inputText,
          onValueChange = onInputTextChange,
          placeholder = {
            Text(
              text = if (selectedSubject != null) "Ask a concept in ${selectedSubject.code}..." else "Ask a concept, proof, or question...",
              fontSize = 13.sp
            )
          },
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(24.dp),
          colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
          ),
          maxLines = 3
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
          onClick = {
            if (inputText.isNotBlank() && !isChatLoading) {
              viewModel.sendChatMessage(inputText)
              onInputTextChange("")
            }
          },
          enabled = inputText.isNotBlank() && !isChatLoading,
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (inputText.isNotBlank() && !isChatLoading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Send",
            tint = if (inputText.isNotBlank() && !isChatLoading) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun HistoryTabContent(
  tutorSessions: List<TutorSession>,
  selectedSubject: Subject?,
  activeSessionId: Long?,
  onSelectSession: (TutorSession) -> Unit,
  onDeleteSession: (Long) -> Unit,
  onClearAllHistory: () -> Unit,
  onStartNewChat: () -> Unit
) {
  val context = LocalContext.current
  var searchQuery by remember { mutableStateOf("") }

  val filteredSessions = tutorSessions.filter { session ->
    val matchesSubject = selectedSubject == null || session.subjectId == selectedSubject.id
    val matchesSearch = searchQuery.isBlank() ||
      session.title.contains(searchQuery, ignoreCase = true) ||
      session.previewSnippet.contains(searchQuery, ignoreCase = true) ||
      session.subjectName.contains(searchQuery, ignoreCase = true)
    matchesSubject && matchesSearch
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 8.dp)
  ) {
    // History Search Bar
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      placeholder = { Text("Search past questions, topics, and answers...", fontSize = 13.sp) },
      leadingIcon = {
        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
      },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(onClick = { searchQuery = "" }) {
            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
          }
        }
      },
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
      ),
      singleLine = true
    )

    Spacer(modifier = Modifier.height(10.dp))

    // Top status row with Clear History action
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = if (selectedSubject != null) "Sessions for ${selectedSubject.name} (${filteredSessions.size})" else "All Past Tutoring Sessions (${filteredSessions.size})",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground
      )

      if (tutorSessions.isNotEmpty()) {
        TextButton(onClick = onClearAllHistory) {
          Text("Clear All", fontSize = 12.sp, color = RoseError)
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    if (filteredSessions.isEmpty()) {
      // Empty state
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.padding(24.dp)
        ) {
          Box(
            modifier = Modifier
              .size(56.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.History,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(28.dp)
            )
          }

          Text(
            text = if (searchQuery.isNotBlank()) "No sessions matching '$searchQuery'" else "No past tutor sessions yet",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )

          Text(
            text = "Ask the AI Tutor a question to automatically log your learning history and derivations here.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp)
          )

          Spacer(modifier = Modifier.height(6.dp))

          Button(
            onClick = onStartNewChat,
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Start a New Chat")
          }
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(filteredSessions, key = { it.id }) { session ->
          TutorSessionHistoryCard(
            session = session,
            isActive = session.id == activeSessionId,
            onClick = { onSelectSession(session) },
            onCopy = {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              clipboard.setPrimaryClip(ClipData.newPlainText("Session Snippet", session.previewSnippet))
              Toast.makeText(context, "Snippet copied to clipboard", Toast.LENGTH_SHORT).show()
            },
            onDelete = { onDeleteSession(session.id) }
          )
        }
      }
    }
  }
}

@Composable
private fun TutorSessionHistoryCard(
  session: TutorSession,
  isActive: Boolean,
  onClick: () -> Unit,
  onCopy: () -> Unit,
  onDelete: () -> Unit
) {
  val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
  val dateStr = dateFormat.format(Date(session.lastUpdatedAt))

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() },
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
    ),
    border = if (isActive) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Subject Badge
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = MaterialTheme.colorScheme.surfaceVariant
        ) {
          Text(
            text = session.subjectName,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (session.searchGroundingUsed) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = CyanAccent.copy(alpha = 0.15f),
              modifier = Modifier.padding(end = 6.dp)
            ) {
              Text(
                text = "Web Search",
                style = MaterialTheme.typography.labelSmall,
                color = CyanAccent,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }

          Text(
            text = dateStr,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Session Title
      Text(
        text = session.title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      Spacer(modifier = Modifier.height(4.dp))

      // Answer Snippet
      Text(
        text = session.previewSnippet,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Footer: Actions & Model Pill
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
          Text(
            text = "${session.modelUsed} • ${session.messageCount} turns",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
            Icon(
              imageVector = Icons.Default.ContentCopy,
              contentDescription = "Copy Snippet",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(15.dp)
            )
          }

          IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = "Delete Session",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(15.dp)
            )
          }

          Spacer(modifier = Modifier.width(4.dp))

          Button(
            onClick = onClick,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(30.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp)
          ) {
            Text("Open Chat", fontSize = 11.sp)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChatMessageBubble(
  message: ChatMessage,
  onCopy: () -> Unit,
  onOpenUrl: (String) -> Unit
) {
  val isUser = message.role == "user"
  val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
  val timeStr = timeFormat.format(Date(message.timestamp))

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
  ) {
    if (!isUser) {
      Box(
        modifier = Modifier
          .size(32.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
      }
      Spacer(modifier = Modifier.width(8.dp))
    }

    Column(
      modifier = Modifier.widthIn(max = 330.dp),
      horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
      // Model & Source Badge
      if (!isUser) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(bottom = 4.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
          ) {
            Text(
              text = message.modelName,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }

          if (message.searchGroundingUsed || message.groundingSources.isNotEmpty()) {
            Spacer(modifier = Modifier.width(4.dp))
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = CyanAccent.copy(alpha = 0.15f)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Icon(Icons.Default.Public, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(10.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("Google Search", style = MaterialTheme.typography.labelSmall, color = CyanAccent)
              }
            }
          }
        }
      }

      // Message Card with Formatted Markdown Rendering
      Card(
        shape = RoundedCornerShape(
          topStart = 16.dp,
          topEnd = 16.dp,
          bottomStart = if (isUser) 16.dp else 4.dp,
          bottomEnd = if (isUser) 4.dp else 16.dp
        ),
        colors = CardDefaults.cardColors(
          containerColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isUser) 0.dp else 2.dp)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          // Render rich formatted response (Code blocks, headings, lists, callouts)
          FormattedTutorResponse(
            content = message.text,
            isUser = isUser
          )

          // Grounding Sources & Citations Section
          if (!isUser && message.groundingSources.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "Search Grounding Citations",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                  )
                }
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  message.groundingSources.take(4).forEach { source ->
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = MaterialTheme.colorScheme.surface,
                      modifier = Modifier.clickable { onOpenUrl(source.url) }
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Text(
                          text = source.title.take(28),
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(10.dp))
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }

      // Timestamp & Copy
      Row(
        modifier = Modifier.padding(top = 3.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = timeStr,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
        if (!isUser) {
          Spacer(modifier = Modifier.width(8.dp))
          Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = "Copy message",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
              .size(13.dp)
              .clickable { onCopy() }
          )
        }
      }
    }
  }
}

@Composable
private fun PromptChip(
  text: String,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    shadowElevation = 1.dp,
    modifier = Modifier.clickable { onClick() }
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
    )
  }
}

/**
 * Returns a quadruple of (Chip Label, Optional Model, Use Search, Prompt Text)
 */
private fun getPromptsForSubject(subject: Subject?): List<PromptOption> {
  if (subject == null) {
    return listOf(
      PromptOption("🔍 AVL trees research", TutorModel.FLASH, true, "What are the latest research advancements and real-world variants of AVL and self-balancing trees? Include citations."),
      PromptOption("🧠 Prove Matrix Diagonalization", TutorModel.PRO, false, "Provide a rigorous step-by-step mathematical proof of the condition for matrix diagonalizability with eigenvalues."),
      PromptOption("⚡ Spaced repetition protocol", TutorModel.FLASH_LITE, false, "Give me a concise 3-bullet breakdown of the optimal 1-3-7-14 day spaced repetition protocol for semester exams."),
      PromptOption("💻 Code review: AVL rotation", TutorModel.PRO, false, "Show a production-quality Kotlin implementation of Left and Right AVL rotations with edge-case handling.")
    )
  }

  val name = subject.name.lowercase()
  val code = subject.code.lowercase()

  return when {
    name.contains("data structure") || name.contains("algorithm") || code.contains("cs2") -> listOf(
      PromptOption("💻 AVL Double Rotations in Kotlin", TutorModel.PRO, false, "Explain how Left-Right and Right-Left double rotations work in AVL trees, and provide clean Kotlin code."),
      PromptOption("⚡ Dijkstra vs Bellman-Ford", TutorModel.FLASH_LITE, false, "Compare Dijkstra and Bellman-Ford algorithms regarding time complexity, negative edges, and cycle detection."),
      PromptOption("🧠 Prove Master Theorem case 2", TutorModel.PRO, false, "Derive and prove Case 2 of the Master Theorem for divide-and-conquer algorithmic recurrences."),
      PromptOption("🔍 Self-balancing trees research", TutorModel.FLASH, true, "What are the modern variants of self-balancing search trees (e.g. WAVL, Zip trees) used in database indexing?")
    )
    name.contains("linear algebra") || name.contains("calculus") || code.contains("math") -> listOf(
      PromptOption("🧠 Matrix Diagonalization Proof", TutorModel.PRO, false, "Prove the necessary and sufficient conditions for matrix diagonalizability with eigenvalues and eigenspaces."),
      PromptOption("📐 Gram-Schmidt Step-by-Step", TutorModel.PRO, false, "Walk me through the Gram-Schmidt orthogonalization algorithm with an example step-by-step derivation."),
      PromptOption("⚡ Eigenvalues of Orthogonal Matrix", TutorModel.FLASH_LITE, false, "Explain why all eigenvalues of a real orthogonal matrix must have absolute value equal to 1."),
      PromptOption("🔍 Spectral Theorem application", TutorModel.FLASH, true, "Explain the Spectral Theorem and its modern applications in machine learning SVD and PCA with citations.")
    )
    name.contains("physics") || code.contains("phys") -> listOf(
      PromptOption("🔍 Davisson-Germer electron diffraction", TutorModel.FLASH, true, "Explain the Davisson-Germer experiment that verified de Broglie's wave hypothesis, with citations."),
      PromptOption("⚡ Schrödinger 1D Infinite Well", TutorModel.PRO, false, "Derive the energy eigenstates and wavefunctions for a particle in a 1D infinite square well."),
      PromptOption("🧠 Heisenberg Uncertainty Derivation", TutorModel.PRO, false, "Derive Heisenberg's uncertainty principle using operators and the Cauchy-Schwarz inequality.")
    )
    name.contains("software") || name.contains("engineering") || code.contains("cs3") -> listOf(
      PromptOption("💻 SOLID Principles in Kotlin", TutorModel.PRO, false, "Explain all 5 SOLID principles with concrete, clean Kotlin code examples."),
      PromptOption("⚡ Dependency Injection vs Factory", TutorModel.FLASH_LITE, false, "Explain the key differences and design trade-offs between Dependency Injection and Factory patterns."),
      PromptOption("🔍 Thread-safe Singleton in Kotlin", TutorModel.PRO, false, "How is the thread-safe Singleton pattern implemented idiomatically in Kotlin?")
    )
    else -> listOf(
      PromptOption("🔍 Key theorems in ${subject.code}", TutorModel.FLASH, true, "What are the most crucial core theorems and exam questions in ${subject.name}? Cite academic sources."),
      PromptOption("🧠 Step-by-step concept breakdown", TutorModel.PRO, false, "Break down the core principles of ${subject.name} into clear conceptual pillars with exam tips."),
      PromptOption("⚡ 5-minute summary", TutorModel.FLASH_LITE, false, "Give me a high-yield 5-minute study summary for ${subject.name}.")
    )
  }
}

private data class PromptOption(
  val first: String,
  val second: TutorModel?,
  val third: Boolean,
  val fourth: String
)
