package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScheduleFormat
import com.example.data.model.Subject
import com.example.data.model.TimetableSession
import com.example.ui.components.ActiveAlarmAlertDialog
import com.example.ui.components.EmptyState
import com.example.ui.components.WhatShouldIDoNowCard
import com.example.ui.theme.PaletteCreamIvory
import com.example.ui.theme.PaletteDarkText
import com.example.ui.theme.PaletteDeepSage
import com.example.ui.theme.PaletteMediumSage
import com.example.ui.theme.PaletteMintFoam
import com.example.ui.theme.PalettePaleCeladon
import com.example.ui.viewmodel.StudyPilotViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
  viewModel: StudyPilotViewModel,
  showAddDialogOnInit: Boolean = false
) {
  val context = LocalContext.current
  val sessions by viewModel.sessions.collectAsState()
  val subjects by viewModel.subjects.collectAsState()
  val scheduleFormats by viewModel.scheduleFormats.collectAsState()
  val scheduleStatus by viewModel.scheduleStatus.collectAsState()
  val isCalendarConnected by viewModel.isCalendarConnected.collectAsState()
  val ringingAlarmSession by viewModel.ringingAlarmSession.collectAsState()

  val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
  val displayFormat = remember { SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()) }
  val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
  val dayNameFormat = remember { SimpleDateFormat("EEEE", Locale.getDefault()) }

  val realToday = remember { dateFormat.format(Date()) }
  // Selected day to manage (defaults to today)
  var selectedDate by remember { mutableStateOf(realToday) }

  // Tabs: 0: Selected Day, 1: Schedule Formats (Templates), 2: Full Agenda (All)
  var selectedTab by remember { mutableIntStateOf(0) }

  var showAddDialog by remember { mutableStateOf(showAddDialogOnInit) }
  var sessionToEdit by remember { mutableStateOf<TimetableSession?>(null) }
  var showCopyDayDialog by remember { mutableStateOf(false) }
  var showApplyFormatDialog by remember { mutableStateOf(false) }
  var showSaveCurrentDayAsFormatDialog by remember { mutableStateOf(false) }
  var showCreateNewFormatDialog by remember { mutableStateOf(false) }
  var formatToInspect by remember { mutableStateOf<ScheduleFormat?>(null) }

  // Helpers to shift days, months, and years
  fun shiftSelectedDate(deltaDays: Int) {
    try {
      val parsed = dateFormat.parse(selectedDate) ?: Date()
      val cal = Calendar.getInstance().apply {
        time = parsed
        add(Calendar.DAY_OF_YEAR, deltaDays)
      }
      selectedDate = dateFormat.format(cal.time)
    } catch (_: Exception) {}
  }

  fun shiftSelectedMonth(deltaMonths: Int) {
    try {
      val parsed = dateFormat.parse(selectedDate) ?: Date()
      val cal = Calendar.getInstance().apply {
        time = parsed
        add(Calendar.MONTH, deltaMonths)
      }
      selectedDate = dateFormat.format(cal.time)
    } catch (_: Exception) {}
  }

  fun shiftSelectedYear(deltaYears: Int) {
    try {
      val parsed = dateFormat.parse(selectedDate) ?: Date()
      val cal = Calendar.getInstance().apply {
        time = parsed
        add(Calendar.YEAR, deltaYears)
      }
      selectedDate = dateFormat.format(cal.time)
    } catch (_: Exception) {}
  }

  // Selected date's sessions
  val selectedDaySessions = sessions.filter { it.date == selectedDate }.sortedBy { it.startTime }

  // Full agenda list
  val agendaSessions = sessions.sortedBy { it.date + it.startTime }

  // Parsed title for current selected date
  val selectedDateTitle = try {
    val d = dateFormat.parse(selectedDate) ?: Date()
    displayFormat.format(d)
  } catch (_: Exception) {
    selectedDate
  }

  val isSelectedDateToday = selectedDate == realToday

  Scaffold(
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showAddDialog = true },
        containerColor = PaletteDeepSage,
        contentColor = PaletteCreamIvory,
        shape = CircleShape
      ) {
        Icon(Icons.Default.Add, contentDescription = "Add Event Slot")
      }
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(PaletteCreamIvory)
        .padding(padding)
    ) {
      // 1. Google Calendar-Style Clean Header Bar
      Surface(
        color = PaletteCreamIvory,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, PaletteMediumSage.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
          // Top title row + Quick actions
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "GOOGLE CALENDAR SCHEDULE & ALARMS",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp
                ),
                color = PaletteDarkText
              )
              Text(
                text = if (isSelectedDateToday) "Today: $selectedDateTitle" else selectedDateTitle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = PaletteDarkText
              )
            }

            // Sync with Google Calendar Quick Button
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = PaletteMintFoam,
              border = androidx.compose.foundation.BorderStroke(1.dp, PaletteMediumSage),
              modifier = Modifier.clickable { viewModel.syncAllTodayToGoogleCalendar() }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = PaletteDarkText, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Sync G-Cal", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PaletteDarkText)
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // 2. Comprehensive Calendar Navigation & Selector (Day, Month, Year & Future/Past)
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = PaletteMintFoam,
            border = androidx.compose.foundation.BorderStroke(1.dp, PaletteMediumSage),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              // Row A: Year & Month Fast-Navigation Bar (e.g. October 2026, jump months and years freely)
              val parsedDateForHeader = try {
                dateFormat.parse(selectedDate) ?: Date()
              } catch (_: Exception) {
                Date()
              }
              val monthYearStr = monthYearFormat.format(parsedDateForHeader)

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                // Left controls: Fast Year / Month jump back
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PalettePaleCeladon,
                    border = androidx.compose.foundation.BorderStroke(1.dp, PaletteMediumSage),
                    modifier = Modifier.clickable { shiftSelectedYear(-1) }
                  ) {
                    Text(
                      text = "-1Y",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = PaletteDarkText,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                  }
                  IconButton(
                    onClick = { shiftSelectedMonth(-1) },
                    modifier = Modifier.size(28.dp)
                  ) {
                    Icon(
                      Icons.AutoMirrored.Filled.ArrowBack,
                      contentDescription = "Previous Month",
                      tint = PaletteDarkText,
                      modifier = Modifier.size(16.dp)
                    )
                  }
                }

                // Center: Month & Year Label + Quick Today Button
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Text(
                    text = monthYearStr,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = PaletteDarkText
                  )
                  if (!isSelectedDateToday) {
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = PaletteDeepSage,
                      modifier = Modifier.clickable { selectedDate = realToday }
                    ) {
                      Text(
                        text = "TODAY",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PaletteCreamIvory,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                      )
                    }
                  }
                }

                // Right controls: Fast Month / Year jump forward
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  IconButton(
                    onClick = { shiftSelectedMonth(1) },
                    modifier = Modifier.size(28.dp)
                  ) {
                    Icon(
                      Icons.AutoMirrored.Filled.ArrowForward,
                      contentDescription = "Next Month",
                      tint = PaletteDarkText,
                      modifier = Modifier.size(16.dp)
                    )
                  }
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PalettePaleCeladon,
                    border = androidx.compose.foundation.BorderStroke(1.dp, PaletteMediumSage),
                    modifier = Modifier.clickable { shiftSelectedYear(1) }
                  ) {
                    Text(
                      text = "+1Y",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = PaletteDarkText,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(8.dp))

              // Row B: Direct Selected Date Button with full Android calendar picker dialog
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = PaletteCreamIvory,
                border = androidx.compose.foundation.BorderStroke(1.dp, PaletteMediumSage),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    val cal = Calendar.getInstance()
                    try {
                      cal.time = dateFormat.parse(selectedDate) ?: Date()
                    } catch (_: Exception) {}
                    DatePickerDialog(
                      context,
                      { _, y, m, d ->
                        val newCal = Calendar.getInstance().apply { set(y, m, d) }
                        selectedDate = dateFormat.format(newCal.time)
                      },
                      cal.get(Calendar.YEAR),
                      cal.get(Calendar.MONTH),
                      cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                  }
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      Icons.Default.CalendarMonth,
                      contentDescription = "Pick Date",
                      tint = PaletteDeepSage,
                      modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                      Text(
                        text = "Active Selected Date",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PaletteDarkText.copy(alpha = 0.8f)
                      )
                      Text(
                        text = selectedDateTitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PaletteDarkText
                      )
                    }
                  }

                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isSelectedDateToday) {
                      Surface(
                        color = PaletteDeepSage,
                        shape = RoundedCornerShape(4.dp)
                      ) {
                        Text(
                          text = "TODAY",
                          fontWeight = FontWeight.ExtraBold,
                          fontSize = 10.sp,
                          color = PaletteCreamIvory,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                      }
                    } else {
                      val daysDiff = try {
                        val d1 = dateFormat.parse(realToday)?.time ?: 0L
                        val d2 = dateFormat.parse(selectedDate)?.time ?: 0L
                        ((d2 - d1) / (1000 * 60 * 60 * 24)).toInt()
                      } catch (_: Exception) { 0 }

                      val diffLabel = when {
                        daysDiff == 1 -> "Tomorrow"
                        daysDiff == -1 -> "Yesterday"
                        daysDiff > 0 -> "+${daysDiff}d ahead"
                        else -> "${daysDiff}d ago"
                      }

                      Surface(
                        color = PalettePaleCeladon,
                        shape = RoundedCornerShape(4.dp)
                      ) {
                        Text(
                          text = diffLabel,
                          fontWeight = FontWeight.Bold,
                          fontSize = 10.sp,
                          color = PaletteDarkText,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                      }
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(8.dp))

              // Row C: Rolling 7-Day Window centered on the currently selected date (-3 to +3 days)
              // Dynamically adapts whether you are viewing today, next month, or next year!
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                IconButton(
                  onClick = { shiftSelectedDate(-1) },
                  modifier = Modifier.size(28.dp)
                ) {
                  Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous Day",
                    tint = PaletteDarkText,
                    modifier = Modifier.size(16.dp)
                  )
                }

                LazyRow(
                  modifier = Modifier.weight(1f),
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  val baseCal = Calendar.getInstance().apply {
                    try {
                      time = dateFormat.parse(selectedDate) ?: Date()
                    } catch (_: Exception) {}
                  }

                  val dayPills = (-3..3).map { offset ->
                    val c = Calendar.getInstance().apply {
                      time = baseCal.time
                      add(Calendar.DAY_OF_YEAR, offset)
                    }
                    val dStr = dateFormat.format(c.time)
                    val dayName = SimpleDateFormat("EEE", Locale.getDefault()).format(c.time)
                    val dayNum = SimpleDateFormat("d", Locale.getDefault()).format(c.time)
                    Triple(dStr, dayName, dayNum)
                  }

                  items(dayPills) { (dStr, dayName, dayNum) ->
                    val isSelected = dStr == selectedDate
                    val isTodayPill = dStr == realToday
                    val sessionCount = sessions.count { it.date == dStr }

                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = if (isSelected) PaletteDeepSage else if (isTodayPill) PalettePaleCeladon else PaletteCreamIvory,
                      border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) PaletteDeepSage else PaletteMediumSage
                      ),
                      modifier = Modifier.clickable { selectedDate = dStr }
                    ) {
                      Column(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                      ) {
                        Text(
                          text = dayName,
                          fontSize = 10.sp,
                          fontWeight = FontWeight.Bold,
                          color = if (isSelected) PaletteCreamIvory else PaletteDarkText
                        )
                        Text(
                          text = dayNum,
                          fontSize = 13.sp,
                          fontWeight = FontWeight.ExtraBold,
                          color = if (isSelected) PaletteCreamIvory else PaletteDarkText
                        )
                        if (sessionCount > 0) {
                          Box(
                            modifier = Modifier
                              .size(5.dp)
                              .clip(CircleShape)
                              .background(if (isSelected) PaletteCreamIvory else PaletteDeepSage)
                          )
                        } else {
                          Spacer(modifier = Modifier.height(5.dp))
                        }
                      }
                    }
                  }
                }

                IconButton(
                  onClick = { shiftSelectedDate(1) },
                  modifier = Modifier.size(28.dp)
                ) {
                  Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Day",
                    tint = PaletteDarkText,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // 3. Clean Segmented Navigation Tabs:
          // [Day Schedule] | [Format Templates] | [Agenda (All)]
          TabRow(
            selectedTabIndex = selectedTab,
            containerColor = PaletteMintFoam,
            contentColor = PaletteDarkText,
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
          ) {
            Tab(
              selected = selectedTab == 0,
              onClick = { selectedTab = 0 },
              text = {
                Text(
                  "Day Schedule (${selectedDaySessions.size})",
                  fontWeight = if (selectedTab == 0) FontWeight.ExtraBold else FontWeight.Medium,
                  fontSize = 12.sp,
                  color = PaletteDarkText
                )
              }
            )
            Tab(
              selected = selectedTab == 1,
              onClick = { selectedTab = 1 },
              text = {
                Text(
                  "Formats (${scheduleFormats.size})",
                  fontWeight = if (selectedTab == 1) FontWeight.ExtraBold else FontWeight.Medium,
                  fontSize = 12.sp,
                  color = PaletteDarkText
                )
              }
            )
            Tab(
              selected = selectedTab == 2,
              onClick = { selectedTab = 2 },
              text = {
                Text(
                  "All Events (${sessions.size})",
                  fontWeight = if (selectedTab == 2) FontWeight.ExtraBold else FontWeight.Medium,
                  fontSize = 12.sp,
                  color = PaletteDarkText
                )
              }
            )
          }
        }
      }

      // TAB CONTENT
      when (selectedTab) {
        0 -> {
          // --- TAB 0: SELECTED DAY TIMETABLE ---
          LazyColumn(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            // Live "What Should I Do Now?" smart alarm card (when viewing today)
            if (isSelectedDateToday) {
              item {
                WhatShouldIDoNowCard(
                  status = scheduleStatus,
                  onPlanScheduleClick = { showApplyFormatDialog = true },
                  onEditSessionClick = { sessionToEdit = it },
                  onMarkSessionDone = { session ->
                    val next = if (session.status == "COMPLETED") "SCHEDULED" else "COMPLETED"
                    viewModel.updateSessionStatus(session, next)
                  },
                  onAskAiTutor = { session ->
                    viewModel.openTutorForSubject(session.subjectId, "Can you explain key concepts for ${session.topic}?")
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

            // Quick Format & Day Management Action Row: Symmetrical and even
            item {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = PaletteMintFoam,
                border = androidx.compose.foundation.BorderStroke(1.dp, PaletteMediumSage),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Text(
                    text = "Manage Schedule for $selectedDateTitle",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = PaletteDarkText
                  )
                  Spacer(modifier = Modifier.height(8.dp))

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    // Apply a saved format / template
                    Button(
                      onClick = { showApplyFormatDialog = true },
                      colors = ButtonDefaults.buttonColors(
                        containerColor = PalettePaleCeladon,
                        contentColor = PaletteDarkText
                      ),
                      border = androidx.compose.foundation.BorderStroke(1.dp, PaletteMediumSage),
                      shape = RoundedCornerShape(8.dp),
                      contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                      modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                    ) {
                      Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PaletteDarkText, modifier = Modifier.size(14.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Apply Format", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Copy this day schedule to another day
                    Button(
                      onClick = { showCopyDayDialog = true },
                      colors = ButtonDefaults.buttonColors(
                        containerColor = PalettePaleCeladon,
                        contentColor = PaletteDarkText
                      ),
                      border = androidx.compose.foundation.BorderStroke(1.dp, PaletteMediumSage),
                      shape = RoundedCornerShape(8.dp),
                      contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                      modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                    ) {
                      Icon(Icons.Default.ContentCopy, contentDescription = null, tint = PaletteDarkText, modifier = Modifier.size(14.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Copy Day", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Save as new Format / Template
                    Button(
                      onClick = { showSaveCurrentDayAsFormatDialog = true },
                      colors = ButtonDefaults.buttonColors(
                        containerColor = PaletteDeepSage,
                        contentColor = PaletteCreamIvory
                      ),
                      shape = RoundedCornerShape(8.dp),
                      contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                      modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                    ) {
                      Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(14.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Save Format", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }

            // Session List header
            item {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Time-Slots for $selectedDateTitle",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = PaletteDarkText
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                  if (selectedDaySessions.isNotEmpty()) {
                    TextButton(
                      onClick = { viewModel.clearDaySchedule(selectedDate) },
                      contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                      Text("Clear Day", fontSize = 11.sp, color = PaletteDarkText, fontWeight = FontWeight.Bold)
                    }
                  }
                  Text(
                    text = "${selectedDaySessions.size} session(s)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = PaletteDarkText
                  )
                }
              }
            }

            // Time slots
            if (selectedDaySessions.isEmpty()) {
              item {
                EmptyState(
                  icon = Icons.Default.CalendarMonth,
                  title = "No Schedule for $selectedDateTitle",
                  description = "Create a new schedule from scratch, copy from another day, or apply one of your pre-made formats.",
                  actionButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      Button(
                        onClick = { showApplyFormatDialog = true },
                        colors = ButtonDefaults.buttonColors(
                          containerColor = PaletteDeepSage,
                          contentColor = PaletteCreamIvory
                        ),
                        shape = RoundedCornerShape(10.dp)
                      ) {
                        Text("Apply Format / Template", fontWeight = FontWeight.Bold)
                      }
                      OutlinedButton(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PaletteMediumSage)
                      ) {
                        Text("Add Single Slot", color = PaletteDarkText, fontWeight = FontWeight.Bold)
                      }
                    }
                  }
                )
              }
            } else {
              items(selectedDaySessions, key = { it.id }) { session ->
                GoogleCalendarEventCard(
                  session = session,
                  onStatusChange = { newStatus -> viewModel.updateSessionStatus(session, newStatus) },
                  onEdit = { sessionToEdit = session },
                  onToggleAlarm = { viewModel.toggleSessionAlarm(session) },
                  onTestAlarm = { viewModel.triggerTestAlarm(session) },
                  onAskAiTutor = {
                    viewModel.openTutorForSubject(session.subjectId, "Help me prepare notes for ${session.topic}")
                  },
                  onSyncCalendar = {
                    viewModel.syncSessionToGoogleCalendar(session)
                    val intent = viewModel.alarmAndCalendarService.createGoogleCalendarInsertIntent(session)
                    try {
                      context.startActivity(intent)
                    } catch (e: Exception) {
                      val webUrl = viewModel.alarmAndCalendarService.getGoogleCalendarWebUrl(session)
                      context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)))
                    }
                  },
                  onDelete = { viewModel.deleteSession(session.id) }
                )
              }
            }
          }
        }

        1 -> {
          // --- TAB 1: SCHEDULE FORMATS / TEMPLATES ---
          LazyColumn(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            item {
              Surface(
                modifier = Modifier.fillMaxWidth(),
                color = PaletteMintFoam,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, PaletteMediumSage)
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(
                    modifier = Modifier
                      .weight(1f)
                      .padding(end = 12.dp)
                  ) {
                    Text(
                      text = "Saved Schedule Formats",
                      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                      color = PaletteDarkText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "Save daily templates and reuse them on any day in one tap.",
                      style = MaterialTheme.typography.bodySmall,
                      color = PaletteDarkText.copy(alpha = 0.85f)
                    )
                  }

                  Button(
                    onClick = { showCreateNewFormatDialog = true },
                    colors = ButtonDefaults.buttonColors(
                      containerColor = PaletteDeepSage,
                      contentColor = PaletteCreamIvory
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.height(38.dp)
                  ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "New Format",
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }
            }

            if (scheduleFormats.isEmpty()) {
              item {
                EmptyState(
                  icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                  title = "No Custom Formats Yet",
                  description = "Create formats for your academic routines, exam deep work days, or weekend revisions.",
                  actionButton = {
                    Button(
                      onClick = { showCreateNewFormatDialog = true },
                      colors = ButtonDefaults.buttonColors(
                        containerColor = PaletteDeepSage,
                        contentColor = PaletteCreamIvory
                      ),
                      shape = RoundedCornerShape(10.dp)
                    ) {
                      Text("Create Format", fontWeight = FontWeight.Bold)
                    }
                  }
                )
              }
            } else {
              items(scheduleFormats, key = { it.id }) { format ->
                ScheduleFormatCard(
                  format = format,
                  selectedDate = selectedDate,
                  onApplyToSelectedDate = { overwrite ->
                    viewModel.applyFormatToDate(format, selectedDate, overwrite)
                    selectedTab = 0
                  },
                  onDeleteFormat = { viewModel.deleteScheduleFormat(format.id) }
                )
              }
            }
          }
        }

        2 -> {
          // --- TAB 2: ALL EVENTS AGENDA ---
          LazyColumn(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            item {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Full Academic Agenda",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = PaletteDarkText
                )
                Text(
                  text = "${agendaSessions.size} total events",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                  color = PaletteDarkText
                )
              }
            }

            if (agendaSessions.isEmpty()) {
              item {
                EmptyState(
                  icon = Icons.Default.CalendarMonth,
                  title = "No Events Scheduled",
                  description = "Your schedule agenda is currently empty.",
                  actionButton = {
                    Button(
                      onClick = { showAddDialog = true },
                      colors = ButtonDefaults.buttonColors(
                        containerColor = PaletteDeepSage,
                        contentColor = PaletteCreamIvory
                      ),
                      shape = RoundedCornerShape(10.dp)
                    ) {
                      Text("Add First Session", fontWeight = FontWeight.Bold)
                    }
                  }
                )
              }
            } else {
              items(agendaSessions, key = { it.id }) { session ->
                GoogleCalendarEventCard(
                  session = session,
                  onStatusChange = { newStatus -> viewModel.updateSessionStatus(session, newStatus) },
                  onEdit = { sessionToEdit = session },
                  onToggleAlarm = { viewModel.toggleSessionAlarm(session) },
                  onTestAlarm = { viewModel.triggerTestAlarm(session) },
                  onAskAiTutor = {
                    viewModel.openTutorForSubject(session.subjectId, "Help me prepare notes for ${session.topic}")
                  },
                  onSyncCalendar = {
                    viewModel.syncSessionToGoogleCalendar(session)
                    val intent = viewModel.alarmAndCalendarService.createGoogleCalendarInsertIntent(session)
                    try {
                      context.startActivity(intent)
                    } catch (e: Exception) {
                      val webUrl = viewModel.alarmAndCalendarService.getGoogleCalendarWebUrl(session)
                      context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)))
                    }
                  },
                  onDelete = { viewModel.deleteSession(session.id) }
                )
              }
            }
          }
        }
      }
    }
  }

  // --- DIALOGS ---

  // 1. Add / Edit Session Dialog
  if (showAddDialog) {
    SessionEditDialog(
      sessionToEdit = null,
      subjects = subjects,
      todayDate = selectedDate,
      onDismiss = { showAddDialog = false },
      onConfirm = { newSession ->
        viewModel.addSession(newSession)
        showAddDialog = false
      }
    )
  }

  sessionToEdit?.let { existing ->
    SessionEditDialog(
      sessionToEdit = existing,
      subjects = subjects,
      todayDate = selectedDate,
      onDismiss = { sessionToEdit = null },
      onConfirm = { updatedSession ->
        viewModel.updateSession(updatedSession)
        sessionToEdit = null
      }
    )
  }

  // 2. Copy Day Schedule Dialog
  if (showCopyDayDialog) {
    CopyDayScheduleDialog(
      sourceDate = selectedDate,
      availableDates = sessions.map { it.date }.distinct().sorted(),
      onDismiss = { showCopyDayDialog = false },
      onConfirmCopy = { srcDate, targetDate, overwrite ->
        viewModel.copyDaySchedule(srcDate, targetDate, overwrite)
        showCopyDayDialog = false
        selectedDate = targetDate
        selectedTab = 0
      }
    )
  }

  // 3. Apply Format / Template Dialog
  if (showApplyFormatDialog) {
    ApplyFormatDialog(
      targetDate = selectedDate,
      formats = scheduleFormats,
      onDismiss = { showApplyFormatDialog = false },
      onApply = { format, overwrite ->
        viewModel.applyFormatToDate(format, selectedDate, overwrite)
        showApplyFormatDialog = false
      },
      onGoToManageFormats = {
        showApplyFormatDialog = false
        selectedTab = 1
      }
    )
  }

  // 4. Save Current Day Sessions as a Format
  if (showSaveCurrentDayAsFormatDialog) {
    SaveDayAsFormatDialog(
      daySessions = selectedDaySessions,
      dayDate = selectedDate,
      onDismiss = { showSaveCurrentDayAsFormatDialog = false },
      onConfirmSave = { name, desc ->
        viewModel.saveScheduleAsFormat(name, desc, selectedDaySessions)
        showSaveCurrentDayAsFormatDialog = false
      }
    )
  }

  // 5. Create Brand New Format from Scratch Dialog
  if (showCreateNewFormatDialog) {
    CreateNewFormatDialog(
      subjects = subjects,
      onDismiss = { showCreateNewFormatDialog = false },
      onSaveFormat = { name, desc, templateSessions ->
        viewModel.saveScheduleAsFormat(name, desc, templateSessions)
        showCreateNewFormatDialog = false
      }
    )
  }

  // Ringing Alarm Dialog
  ringingAlarmSession?.let { ringingSession ->
    ActiveAlarmAlertDialog(
      session = ringingSession,
      onDismiss = { viewModel.dismissAlarm() },
      onSnooze = { viewModel.snoozeAlarm(ringingSession, 5) },
      onStartSession = {
        viewModel.dismissAlarm()
        viewModel.openTutorForSubject(ringingSession.subjectId, "I'm starting my scheduled session on ${ringingSession.topic}.")
      }
    )
  }
}

/**
 * Format Card in Formats Tab
 */
@Composable
fun ScheduleFormatCard(
  format: ScheduleFormat,
  selectedDate: String,
  onApplyToSelectedDate: (Boolean) -> Unit,
  onDeleteFormat: () -> Unit
) {
  var sessionCount by remember { mutableIntStateOf(0) }
  var sessionsList by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

  remember(format.formatSessionsJson) {
    try {
      val arr = org.json.JSONArray(format.formatSessionsJson)
      sessionCount = arr.length()
      val list = mutableListOf<Pair<String, String>>()
      for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        val time = "${obj.optString("startTime")} - ${obj.optString("endTime")}".trim()
        val topic = obj.optString("topic").ifBlank { obj.optString("subjectName", "Session") }
        list.add(time to topic)
      }
      sessionsList = list
    } catch (_: Exception) {
      sessionCount = 0
      sessionsList = emptyList()
    }
  }

  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    color = PaletteMintFoam,
    border = androidx.compose.foundation.BorderStroke(1.2.dp, PaletteMediumSage)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      // Header row: Icon + Format Name + Badge + Delete
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = PaletteDeepSage,
            modifier = Modifier.size(32.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                Icons.Default.Bookmark,
                contentDescription = null,
                tint = PaletteCreamIvory,
                modifier = Modifier.size(16.dp)
              )
            }
          }

          Column {
            Text(
              text = format.name,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
              color = PaletteDarkText,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = if (format.description.isNotBlank()) format.description else "Reusable Daily Routine",
              style = MaterialTheme.typography.bodySmall,
              color = PaletteDarkText.copy(alpha = 0.8f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        IconButton(
          onClick = onDeleteFormat,
          modifier = Modifier
            .size(32.dp)
            .background(PalettePaleCeladon, RoundedCornerShape(8.dp))
        ) {
          Icon(
            Icons.Default.Delete,
            contentDescription = "Delete Format",
            tint = PaletteDarkText,
            modifier = Modifier.size(16.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Session count pill badge
      Surface(
        color = PalettePaleCeladon,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, PaletteMediumSage),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "Total Scheduled Blocks",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = PaletteDarkText
          )
          Text(
            text = "$sessionCount Sessions",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PaletteDarkText
          )
        }
      }

      // Preview blocks list: clean, evenly aligned rows
      if (sessionsList.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PaletteCreamIvory)
            .border(0.8.dp, PaletteMediumSage.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(8.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          sessionsList.take(3).forEach { (time, topic) ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "• $topic",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = PaletteDarkText,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = time,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PaletteDarkText.copy(alpha = 0.7f)
              )
            }
          }
          if (sessionsList.size > 3) {
            Text(
              text = "+ ${sessionsList.size - 3} more block(s)",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = PaletteDeepSage,
              modifier = Modifier.padding(top = 2.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Two evenly sized, symmetrical action buttons (height 40dp, equal weight 1f)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = { onApplyToSelectedDate(false) },
          colors = ButtonDefaults.buttonColors(
            containerColor = PaletteDeepSage,
            contentColor = PaletteCreamIvory
          ),
          shape = RoundedCornerShape(10.dp),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
          modifier = Modifier
            .weight(1f)
            .height(40.dp)
        ) {
          Text(
            text = "Add to $selectedDate",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        OutlinedButton(
          onClick = { onApplyToSelectedDate(true) },
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = PaletteDarkText),
          border = androidx.compose.foundation.BorderStroke(1.2.dp, PaletteMediumSage),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
          modifier = Modifier
            .weight(1f)
            .height(40.dp)
        ) {
          Text(
            text = "Replace Day Schedule",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }
  }
}

/**
 * Dialog to Copy one day's schedule to another date
 */
@Composable
fun CopyDayScheduleDialog(
  sourceDate: String,
  availableDates: List<String>,
  onDismiss: () -> Unit,
  onConfirmCopy: (source: String, target: String, overwrite: Boolean) -> Unit
) {
  val context = LocalContext.current
  val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

  var currentSourceDate by remember { mutableStateOf(sourceDate) }
  var targetDate by remember {
    val cal = Calendar.getInstance()
    try {
      cal.time = dateFormat.parse(sourceDate) ?: Date()
      cal.add(Calendar.DAY_OF_YEAR, 1)
    } catch (_: Exception) {}
    mutableStateOf(dateFormat.format(cal.time))
  }
  var overwriteExisting by remember { mutableStateOf(true) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text("Copy Schedule to Another Day", fontWeight = FontWeight.Bold, color = PaletteDarkText)
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "Replicate all scheduled lecture slots and study alarms from one date to another date.",
          fontSize = 12.sp,
          color = PaletteDarkText
        )

        // Source Date input
        OutlinedTextField(
          value = currentSourceDate,
          onValueChange = { currentSourceDate = it },
          label = { Text("Source Date (Copy From)", color = PaletteDarkText) },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        )

        // Target Date picker / field
        OutlinedTextField(
          value = targetDate,
          onValueChange = { targetDate = it },
          label = { Text("Target Date (Copy To)", color = PaletteDarkText) },
          trailingIcon = {
            IconButton(onClick = {
              val cal = Calendar.getInstance()
              try { cal.time = dateFormat.parse(targetDate) ?: Date() } catch (_: Exception) {}
              DatePickerDialog(
                context,
                { _, y, m, d ->
                  val newCal = Calendar.getInstance().apply { set(y, m, d) }
                  targetDate = dateFormat.format(newCal.time)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
              ).show()
            }) {
              Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Date", tint = PaletteDarkText)
            }
          },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        )

        // Overwrite checkbox
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.clickable { overwriteExisting = !overwriteExisting }
        ) {
          Checkbox(
            checked = overwriteExisting,
            onCheckedChange = { overwriteExisting = it }
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Clear target date before copying",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = PaletteDarkText
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (currentSourceDate.isNotBlank() && targetDate.isNotBlank()) {
            onConfirmCopy(currentSourceDate.trim(), targetDate.trim(), overwriteExisting)
          }
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = PaletteDeepSage,
          contentColor = PaletteCreamIvory
        ),
        shape = RoundedCornerShape(10.dp)
      ) {
        Text("Copy Schedule", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = PaletteDarkText, fontWeight = FontWeight.Bold)
      }
    },
    containerColor = PaletteCreamIvory,
    shape = RoundedCornerShape(16.dp)
  )
}

/**
 * Apply Format to Target Date Dialog
 */
@Composable
fun ApplyFormatDialog(
  targetDate: String,
  formats: List<ScheduleFormat>,
  onDismiss: () -> Unit,
  onApply: (ScheduleFormat, Boolean) -> Unit,
  onGoToManageFormats: () -> Unit
) {
  var selectedFormat by remember { mutableStateOf<ScheduleFormat?>(formats.firstOrNull()) }
  var overwrite by remember { mutableStateOf(true) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text("Apply Schedule Format to $targetDate", fontWeight = FontWeight.Bold, color = PaletteDarkText)
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "Select a saved routine format to apply to $targetDate:",
          fontSize = 12.sp,
          color = PaletteDarkText
        )

        if (formats.isEmpty()) {
          Text("No formats saved yet. Create a format first!", color = PaletteDarkText, fontWeight = FontWeight.Bold)
        } else {
          formats.forEach { format ->
            val isSelected = selectedFormat?.id == format.id
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (isSelected) PalettePaleCeladon else PaletteMintFoam,
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isSelected) PaletteDeepSage else PaletteMediumSage
              ),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedFormat = format }
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(format.name, fontWeight = FontWeight.Bold, color = PaletteDarkText)
                if (format.description.isNotBlank()) {
                  Text(format.description, fontSize = 11.sp, color = PaletteDarkText)
                }
              }
            }
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.clickable { overwrite = !overwrite }
        ) {
          Checkbox(checked = overwrite, onCheckedChange = { overwrite = it })
          Spacer(modifier = Modifier.width(4.dp))
          Text("Replace any existing sessions on $targetDate", fontSize = 12.sp, color = PaletteDarkText)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          selectedFormat?.let { onApply(it, overwrite) }
        },
        enabled = selectedFormat != null,
        colors = ButtonDefaults.buttonColors(
          containerColor = PaletteDeepSage,
          contentColor = PaletteCreamIvory
        ),
        shape = RoundedCornerShape(10.dp)
      ) {
        Text("Apply to $targetDate", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = PaletteDarkText, fontWeight = FontWeight.Bold)
      }
    },
    containerColor = PaletteCreamIvory,
    shape = RoundedCornerShape(16.dp)
  )
}

/**
 * Save Current Day as Format Dialog
 */
@Composable
fun SaveDayAsFormatDialog(
  daySessions: List<TimetableSession>,
  dayDate: String,
  onDismiss: () -> Unit,
  onConfirmSave: (name: String, description: String) -> Unit
) {
  var name by remember { mutableStateOf("Format from $dayDate") }
  var description by remember { mutableStateOf("${daySessions.size} scheduled blocks") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text("Save Day as Reusable Format", fontWeight = FontWeight.Bold, color = PaletteDarkText)
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "Save all ${daySessions.size} sessions from $dayDate as a format template so you can apply it to any day later.",
          fontSize = 12.sp,
          color = PaletteDarkText
        )

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Format Name", color = PaletteDarkText) },
          placeholder = { Text("e.g. Wednesday Heavy Lectures") },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Description (Optional)", color = PaletteDarkText) },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isNotBlank()) {
            onConfirmSave(name.trim(), description.trim())
          }
        },
        enabled = daySessions.isNotEmpty() && name.isNotBlank(),
        colors = ButtonDefaults.buttonColors(
          containerColor = PaletteDeepSage,
          contentColor = PaletteCreamIvory
        ),
        shape = RoundedCornerShape(10.dp)
      ) {
        Text("Save Format", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = PaletteDarkText, fontWeight = FontWeight.Bold)
      }
    },
    containerColor = PaletteCreamIvory,
    shape = RoundedCornerShape(16.dp)
  )
}

/**
 * Create Brand New Format Dialog
 */
@Composable
fun CreateNewFormatDialog(
  subjects: List<Subject>,
  onDismiss: () -> Unit,
  onSaveFormat: (name: String, description: String, sessions: List<TimetableSession>) -> Unit
) {
  var formatName by remember { mutableStateOf("") }
  var formatDesc by remember { mutableStateOf("") }

  // Quick preset templates or custom
  var selectedPreset by remember { mutableIntStateOf(0) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text("Create New Schedule Format", fontWeight = FontWeight.Bold, color = PaletteDarkText)
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = formatName,
          onValueChange = { formatName = it },
          label = { Text("Format Name", color = PaletteDarkText) },
          placeholder = { Text("e.g. Friday Lab & Review") },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = formatDesc,
          onValueChange = { formatDesc = it },
          label = { Text("Description", color = PaletteDarkText) },
          placeholder = { Text("e.g. 3 sessions with evening revision alarm") },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        )

        Text("Select Base Template Layout:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PaletteDarkText)

        // Preset 0: Academic 3-Lecture
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = if (selectedPreset == 0) PalettePaleCeladon else PaletteMintFoam,
          border = androidx.compose.foundation.BorderStroke(1.dp, PaletteMediumSage),
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              selectedPreset = 0
              if (formatName.isBlank()) formatName = "Standard 3-Session Day"
            }
        ) {
          Column(modifier = Modifier.padding(8.dp)) {
            Text("📚 Standard 3-Session Day", fontWeight = FontWeight.Bold, color = PaletteDarkText, fontSize = 12.sp)
            Text("09:00 Lecture, 11:30 Tutorial, 14:00 Lab", fontSize = 11.sp, color = PaletteDarkText)
          }
        }

        // Preset 1: Intensive Exam Study Sprints
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = if (selectedPreset == 1) PalettePaleCeladon else PaletteMintFoam,
          border = androidx.compose.foundation.BorderStroke(1.dp, PaletteMediumSage),
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              selectedPreset = 1
              if (formatName.isBlank()) formatName = "Intensive 4-Sprint Revision"
            }
        ) {
          Column(modifier = Modifier.padding(8.dp)) {
            Text("⚡ Intensive 4-Sprint Revision", fontWeight = FontWeight.Bold, color = PaletteDarkText, fontSize = 12.sp)
            Text("09:00 Sprint 1, 11:00 Sprint 2, 14:00 Sprint 3, 16:30 Sprint 4", fontSize = 11.sp, color = PaletteDarkText)
          }
        }

        // Preset 2: Half Day
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = if (selectedPreset == 2) PalettePaleCeladon else PaletteMintFoam,
          border = androidx.compose.foundation.BorderStroke(1.dp, PaletteMediumSage),
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              selectedPreset = 2
              if (formatName.isBlank()) formatName = "Half Day Focus"
            }
        ) {
          Column(modifier = Modifier.padding(8.dp)) {
            Text("☕ Morning-Only Half Day (2 Sessions)", fontWeight = FontWeight.Bold, color = PaletteDarkText, fontSize = 12.sp)
            Text("09:30 Deep Lecture, 11:30 Problem Set", fontSize = 11.sp, color = PaletteDarkText)
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (formatName.isNotBlank()) {
            val sessionsToSave = when (selectedPreset) {
              0 -> listOf(
                TimetableSession(
                  subjectId = subjects.getOrNull(0)?.id ?: 1L,
                  subjectName = subjects.getOrNull(0)?.name ?: "Computer Science",
                  topic = "Morning Core Lecture",
                  date = "",
                  startTime = "09:00",
                  endTime = "10:30",
                  location = "Hall C",
                  isAlarmEnabled = true
                ),
                TimetableSession(
                  subjectId = subjects.getOrNull(1)?.id ?: 2L,
                  subjectName = subjects.getOrNull(1)?.name ?: "Mathematics",
                  topic = "Theoretical Proofs & Exercises",
                  date = "",
                  startTime = "11:30",
                  endTime = "13:00",
                  location = "Hall 2",
                  isAlarmEnabled = true
                ),
                TimetableSession(
                  subjectId = subjects.getOrNull(2)?.id ?: 3L,
                  subjectName = subjects.getOrNull(2)?.name ?: "Physics",
                  topic = "Afternoon Practical Lab",
                  date = "",
                  startTime = "14:00",
                  endTime = "16:00",
                  location = "Lab 4",
                  isAlarmEnabled = true
                )
              )
              1 -> listOf(
                TimetableSession(
                  subjectId = subjects.getOrNull(0)?.id ?: 1L,
                  subjectName = subjects.getOrNull(0)?.name ?: "Revision",
                  topic = "Deep Sprint 1: Formula Review & Quizzes",
                  date = "",
                  startTime = "09:00",
                  endTime = "10:30",
                  location = "Study Desk",
                  isAlarmEnabled = true
                ),
                TimetableSession(
                  subjectId = subjects.getOrNull(1)?.id ?: 2L,
                  subjectName = subjects.getOrNull(1)?.name ?: "Revision",
                  topic = "Deep Sprint 2: Hard Problem Sets",
                  date = "",
                  startTime = "11:00",
                  endTime = "12:30",
                  location = "Study Desk",
                  isAlarmEnabled = true
                ),
                TimetableSession(
                  subjectId = subjects.getOrNull(2)?.id ?: 3L,
                  subjectName = subjects.getOrNull(2)?.name ?: "Revision",
                  topic = "Deep Sprint 3: Past Papers Practice",
                  date = "",
                  startTime = "14:00",
                  endTime = "15:30",
                  location = "Library",
                  isAlarmEnabled = true
                ),
                TimetableSession(
                  subjectId = subjects.getOrNull(0)?.id ?: 1L,
                  subjectName = subjects.getOrNull(0)?.name ?: "Revision",
                  topic = "Deep Sprint 4: AI Tutor Doubt Clearing",
                  date = "",
                  startTime = "16:30",
                  endTime = "18:00",
                  location = "Home Desk",
                  isAlarmEnabled = true
                )
              )
              else -> listOf(
                TimetableSession(
                  subjectId = subjects.getOrNull(0)?.id ?: 1L,
                  subjectName = subjects.getOrNull(0)?.name ?: "Study",
                  topic = "Morning Deep Concept Reading",
                  date = "",
                  startTime = "09:30",
                  endTime = "11:00",
                  location = "Library",
                  isAlarmEnabled = true
                ),
                TimetableSession(
                  subjectId = subjects.getOrNull(1)?.id ?: 2L,
                  subjectName = subjects.getOrNull(1)?.name ?: "Study",
                  topic = "Problem Set Practice & Flashcards",
                  date = "",
                  startTime = "11:30",
                  endTime = "13:00",
                  location = "Study Room",
                  isAlarmEnabled = true
                )
              )
            }
            onSaveFormat(formatName.trim(), formatDesc.trim(), sessionsToSave)
          }
        },
        enabled = formatName.isNotBlank(),
        colors = ButtonDefaults.buttonColors(
          containerColor = PaletteDeepSage,
          contentColor = PaletteCreamIvory
        ),
        shape = RoundedCornerShape(10.dp)
      ) {
        Text("Save New Format", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = PaletteDarkText, fontWeight = FontWeight.Bold)
      }
    },
    containerColor = PaletteCreamIvory,
    shape = RoundedCornerShape(16.dp)
  )
}

/**
 * Google Calendar-style clean event row card:
 * Left column: Time and duration
 * Center: Clean colored strip, Subject badge, Topic, location
 * Bottom / Right: Aligned actions (Complete, Alarm, AI Tutor, Sync)
 */
@Composable
fun GoogleCalendarEventCard(
  session: TimetableSession,
  onStatusChange: (String) -> Unit,
  onEdit: () -> Unit,
  onToggleAlarm: () -> Unit,
  onTestAlarm: () -> Unit,
  onAskAiTutor: () -> Unit,
  onSyncCalendar: () -> Unit,
  onDelete: () -> Unit
) {
  val isCompleted = session.status == "COMPLETED"

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isCompleted) PaletteMintFoam.copy(alpha = 0.5f) else PaletteMintFoam
    ),
    border = androidx.compose.foundation.BorderStroke(1.dp, PaletteMediumSage),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Row(modifier = Modifier.fillMaxWidth()) {
      // Left indicator color bar (Google Calendar style)
      Box(
        modifier = Modifier
          .width(6.dp)
          .height(130.dp)
          .background(PaletteDeepSage)
      )

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(12.dp)
      ) {
        // Top row: Time + Subject Tag + Actions
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "${session.startTime} - ${session.endTime}",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
              color = PaletteDarkText
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = PalettePaleCeladon
            ) {
              Text(
                text = session.subjectName,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = PaletteDarkText,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
              Icon(Icons.Default.Edit, contentDescription = "Edit", tint = PaletteDarkText, modifier = Modifier.size(15.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
              Icon(Icons.Default.Delete, contentDescription = "Delete", tint = PaletteDarkText, modifier = Modifier.size(15.dp))
            }
          }
        }

        Spacer(modifier = Modifier.height(4.dp))
        // Event Topic / Title
        Text(
          text = session.topic,
          style = MaterialTheme.typography.bodyLarge.copy(
            fontWeight = FontWeight.Bold,
            textDecoration = if (isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
          ),
          color = PaletteDarkText
        )

        // Location & Notes
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (session.location.isNotBlank()) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = PaletteDarkText, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(2.dp))
            Text(
              text = session.location,
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
              color = PaletteDarkText
            )
            Spacer(modifier = Modifier.width(10.dp))
          }
          Text(
            text = "📅 ${session.date}",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = PaletteDarkText
          )
        }

        if (session.notes.isNotBlank()) {
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Notes: ${session.notes}",
            style = MaterialTheme.typography.bodySmall,
            color = PaletteDarkText,
            maxLines = 1
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Aligned Action Strip: Complete, Alarm toggle, AI Tutor, Calendar Sync
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Status completion button
          Button(
            onClick = {
              val next = if (isCompleted) "SCHEDULED" else "COMPLETED"
              onStatusChange(next)
            },
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isCompleted) PalettePaleCeladon else PaletteDeepSage,
              contentColor = if (isCompleted) PaletteDarkText else PaletteCreamIvory
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier
              .weight(1f)
              .height(34.dp)
          ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (isCompleted) "Completed" else "Done", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }

          // AI Tutor Help
          Button(
            onClick = onAskAiTutor,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = PalettePaleCeladon,
              contentColor = PaletteDarkText
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, PaletteMediumSage),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier
              .weight(1f)
              .height(34.dp)
          ) {
            Icon(Icons.Default.Psychology, contentDescription = null, tint = PaletteDarkText, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("AI Tutor", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }

          // Alarm Toggle Pill
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (session.isAlarmEnabled) PalettePaleCeladon else PaletteCreamIvory,
            border = androidx.compose.foundation.BorderStroke(1.dp, PaletteMediumSage),
            modifier = Modifier
              .height(34.dp)
              .clickable { onToggleAlarm() }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = if (session.isAlarmEnabled) Icons.Default.AlarmOn else Icons.Default.AlarmOff,
                contentDescription = null,
                tint = PaletteDarkText,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = if (session.isAlarmEnabled) "Alarm" else "Off",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PaletteDarkText
              )
            }
          }

          // Google Calendar Sync Icon Button
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = PalettePaleCeladon,
            border = androidx.compose.foundation.BorderStroke(1.dp, PaletteMediumSage),
            modifier = Modifier
              .size(34.dp)
              .clickable { onSyncCalendar() }
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(Icons.Default.CalendarMonth, contentDescription = "Sync G-Cal", tint = PaletteDarkText, modifier = Modifier.size(15.dp))
            }
          }
        }
      }
    }
  }
}

/**
 * Dialog for both Adding and Editing a Session.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionEditDialog(
  sessionToEdit: TimetableSession?,
  subjects: List<Subject>,
  todayDate: String,
  onDismiss: () -> Unit,
  onConfirm: (TimetableSession) -> Unit
) {
  var selectedSubject by remember {
    mutableStateOf(
      if (sessionToEdit != null) {
        subjects.firstOrNull { it.id == sessionToEdit.subjectId } ?: subjects.firstOrNull()
      } else {
        subjects.firstOrNull()
      }
    )
  }
  var topic by remember { mutableStateOf(sessionToEdit?.topic ?: "") }
  var date by remember { mutableStateOf(sessionToEdit?.date ?: todayDate) }
  var startTime by remember { mutableStateOf(sessionToEdit?.startTime ?: "10:00") }
  var endTime by remember { mutableStateOf(sessionToEdit?.endTime ?: "11:30") }
  var location by remember { mutableStateOf(sessionToEdit?.location ?: "Lecture Hall C") }
  var notes by remember { mutableStateOf(sessionToEdit?.notes ?: "") }
  var recurringDays by remember { mutableStateOf(sessionToEdit?.recurringDays ?: "Mon,Wed,Fri") }
  var reminderMinutes by remember { mutableIntStateOf(sessionToEdit?.reminderMinutes ?: 15) }
  var isAlarmEnabled by remember { mutableStateOf(sessionToEdit?.isAlarmEnabled ?: true) }

  var expandedDropdown by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (sessionToEdit != null) "Edit Schedule Session" else "Schedule New Class / Study",
        fontWeight = FontWeight.Bold,
        color = PaletteDarkText
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Subject dropdown
        ExposedDropdownMenuBox(
          expanded = expandedDropdown,
          onExpandedChange = { expandedDropdown = !expandedDropdown }
        ) {
          OutlinedTextField(
            value = selectedSubject?.name ?: "Select Subject",
            onValueChange = {},
            readOnly = true,
            label = { Text("Subject", color = PaletteDarkText) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
            shape = RoundedCornerShape(10.dp),
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
                text = { Text("${sub.code} - ${sub.name}", color = PaletteDarkText) },
                onClick = {
                  selectedSubject = sub
                  expandedDropdown = false
                }
              )
            }
          }
        }

        OutlinedTextField(
          value = topic,
          onValueChange = { topic = it },
          label = { Text("Topic or Study Task", color = PaletteDarkText) },
          placeholder = { Text("e.g. Graph Algorithms / Revision") },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = startTime,
            onValueChange = { startTime = it },
            label = { Text("Start Time", color = PaletteDarkText) },
            placeholder = { Text("09:00") },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = endTime,
            onValueChange = { endTime = it },
            label = { Text("End Time", color = PaletteDarkText) },
            placeholder = { Text("10:30") },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          )
        }

        OutlinedTextField(
          value = date,
          onValueChange = { date = it },
          label = { Text("Date (YYYY-MM-DD)", color = PaletteDarkText) },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = location,
          onValueChange = { location = it },
          label = { Text("Location / Room", color = PaletteDarkText) },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Notes & Objectives", color = PaletteDarkText) },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        )

        // Alarm toggle
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = if (isAlarmEnabled) PalettePaleCeladon else PaletteMintFoam,
          border = androidx.compose.foundation.BorderStroke(1.dp, PaletteMediumSage),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { isAlarmEnabled = !isAlarmEnabled }
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = if (isAlarmEnabled) Icons.Default.AlarmOn else Icons.Default.AlarmOff,
              contentDescription = null,
              tint = PaletteDarkText
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = if (isAlarmEnabled) "Sound Alarm Enabled" else "Sound Alarm Disabled",
                fontWeight = FontWeight.Bold,
                color = PaletteDarkText
              )
              Text(
                text = "Will alert when this session starts",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = PaletteDarkText
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (selectedSubject != null && topic.isNotBlank()) {
            val session = TimetableSession(
              id = sessionToEdit?.id ?: 0L,
              subjectId = selectedSubject!!.id,
              subjectName = selectedSubject!!.name,
              topic = topic.trim(),
              date = date.trim(),
              startTime = startTime.trim(),
              endTime = endTime.trim(),
              location = location.trim(),
              notes = notes.trim(),
              recurringDays = recurringDays.trim(),
              reminderMinutes = reminderMinutes,
              status = sessionToEdit?.status ?: "SCHEDULED",
              isAlarmEnabled = isAlarmEnabled,
              googleCalendarEventId = sessionToEdit?.googleCalendarEventId
            )
            onConfirm(session)
          }
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = PaletteDeepSage,
          contentColor = PaletteCreamIvory
        ),
        shape = RoundedCornerShape(10.dp)
      ) {
        Text("Save Schedule", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = PaletteDarkText, fontWeight = FontWeight.Bold)
      }
    },
    containerColor = PaletteCreamIvory,
    shape = RoundedCornerShape(16.dp)
  )
}
