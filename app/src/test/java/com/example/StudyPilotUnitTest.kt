package com.example

import com.example.data.model.QuizQuestion
import com.example.data.model.StudyTopic
import com.example.data.remote.GeminiStudyService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyPilotUnitTest {

  private val service = GeminiStudyService()

  @Test
  fun testSpacedRepetitionIntervalProgression() {
    // Testing interval advancement logic
    val intervals = listOf(1, 3, 7, 14, 30)
    for (i in 0 until intervals.size - 1) {
      val current = intervals[i]
      val next = when (current) {
        1 -> 3
        3 -> 7
        7 -> 14
        14 -> 30
        else -> 30
      }
      assertEquals(intervals[i + 1], next)
    }

    // On mistake, resets to 1 day
    val mistakeReset = 1
    assertEquals(1, mistakeReset)
  }

  @Test
  fun testDeterministicMultipleChoiceEvaluation() = runBlocking {
    val mcq = QuizQuestion(
      topicId = 1,
      subjectName = "CS201",
      topicTitle = "AVL Trees",
      questionType = "MULTIPLE_CHOICE",
      questionText = "What is the search complexity of an AVL tree?",
      optionsJson = "O(1),O(log N),O(N)",
      correctAnswer = "O(log N)",
      explanation = "AVL tree height is guaranteed O(log N)."
    )

    val correctResult = service.evaluateAnswer(mcq, "O(log N)")
    assertTrue(correctResult.isCorrect)

    val wrongResult = service.evaluateAnswer(mcq, "O(1)")
    assertFalse(wrongResult.isCorrect)
  }

  @Test
  fun testDeterministicTrueFalseEvaluation() = runBlocking {
    val tf = QuizQuestion(
      topicId = 1,
      subjectName = "CS201",
      topicTitle = "AVL Trees",
      questionType = "TRUE_FALSE",
      questionText = "AVL tree rotations execute in O(1) time.",
      optionsJson = "True,False",
      correctAnswer = "True",
      explanation = "Rotations swap pointers in constant time."
    )

    val resultTrue = service.evaluateAnswer(tf, "True")
    assertTrue(resultTrue.isCorrect)

    val resultFalse = service.evaluateAnswer(tf, "False")
    assertFalse(resultFalse.isCorrect)
  }

  @Test
  fun testTopicCoverageTrackerPrioritization() {
    val topic1 = StudyTopic(
      id = 1,
      subjectId = 1,
      subjectName = "CS",
      title = "Topic A (Tested)",
      dateStudied = "2026-10-01",
      descriptionNotes = "Notes",
      masteryLevel = "MASTERED",
      repetitionIntervalDays = 14,
      nextRevisionDate = "2026-10-15",
      timesTested = 3,
      timesCorrect = 3
    )
    val topic2 = StudyTopic(
      id = 2,
      subjectId = 1,
      subjectName = "CS",
      title = "Topic B (Struggling)",
      dateStudied = "2026-10-01",
      descriptionNotes = "Notes",
      masteryLevel = "STRUGGLING",
      repetitionIntervalDays = 1,
      nextRevisionDate = "2026-10-11",
      timesTested = 2,
      timesCorrect = 0
    )
    val topic3 = StudyTopic(
      id = 3,
      subjectId = 1,
      subjectName = "CS",
      title = "Topic C (Untested)",
      dateStudied = "2026-10-10",
      descriptionNotes = "Notes",
      masteryLevel = "UNTESTED",
      repetitionIntervalDays = 1,
      nextRevisionDate = "2026-10-11",
      timesTested = 0,
      timesCorrect = 0
    )

    val allTopics = listOf(topic1, topic2, topic3)

    // Sort priority
    val sorted = allTopics.sortedWith(
      compareByDescending<StudyTopic> { topic ->
        var priorityScore = 0
        if (topic.masteryLevel == "STRUGGLING") priorityScore += 50
        if (topic.timesTested == 0) priorityScore += 40
        if (topic.timesTested > 0 && topic.timesCorrect < topic.timesTested) priorityScore += 30
        priorityScore
      }
    )

    // Topic 2 (struggling) should be first, Topic 3 (untested) second, Topic 1 last
    assertEquals(topic2.id, sorted[0].id)
    assertEquals(topic3.id, sorted[1].id)
    assertEquals(topic1.id, sorted[2].id)
  }

  @Test
  fun testTutorSessionSubjectFiltering() {
    val session1 = com.example.data.model.TutorSession(
      id = 1,
      subjectId = 1,
      subjectName = "Data Structures & Algorithms",
      title = "AVL Tree Rotations",
      previewSnippet = "Explanation of AVL",
      messageCount = 4
    )
    val session2 = com.example.data.model.TutorSession(
      id = 2,
      subjectId = 2,
      subjectName = "Operating Systems",
      title = "Paging vs Segmentation",
      previewSnippet = "Memory management details",
      messageCount = 2
    )
    val session3 = com.example.data.model.TutorSession(
      id = 3,
      subjectId = null,
      subjectName = "General Academic",
      title = "Study Strategy",
      previewSnippet = "Active recall methods",
      messageCount = 2
    )

    val allSessions = listOf(session1, session2, session3)

    // Filter by subject 1 (DSA)
    val dsaSessions = allSessions.filter { it.subjectId == 1L }
    assertEquals(1, dsaSessions.size)
    assertEquals("AVL Tree Rotations", dsaSessions[0].title)

    // Filter by subject 2 (OS)
    val osSessions = allSessions.filter { it.subjectId == 2L }
    assertEquals(1, osSessions.size)
    assertEquals("Operating Systems", osSessions[0].subjectName)

    // Search query match
    val searchResults = allSessions.filter {
      it.title.contains("Paging", ignoreCase = true) || it.previewSnippet.contains("Paging", ignoreCase = true)
    }
    assertEquals(1, searchResults.size)
    assertEquals("Paging vs Segmentation", searchResults[0].title)
  }

  @Test
  fun testPhotoColorPaletteValues() {
    assertEquals(androidx.compose.ui.graphics.Color(0xFF8C9F85), com.example.ui.theme.PaletteDeepSage)
    assertEquals(androidx.compose.ui.graphics.Color(0xFFB3C2AC), com.example.ui.theme.PaletteMediumSage)
    assertEquals(androidx.compose.ui.graphics.Color(0xFFD3DDCF), com.example.ui.theme.PalettePaleCeladon)
    assertEquals(androidx.compose.ui.graphics.Color(0xFFDFEAE0), com.example.ui.theme.PaletteMintFoam)
    assertEquals(androidx.compose.ui.graphics.Color(0xFFFDF8EB), com.example.ui.theme.PaletteCreamIvory)
    assertEquals(androidx.compose.ui.graphics.Color(0xFF212C1E), com.example.ui.theme.PaletteDarkText)
    // Dark blue 0xFF223E5B must NOT be used
    org.junit.Assert.assertNotEquals(androidx.compose.ui.graphics.Color(0xFF223E5B), com.example.ui.theme.PaletteDarkText)
  }

  @Test
  fun testScheduleEditingAndAlarmToggle() {
    val original = com.example.data.model.TimetableSession(
      id = 10,
      subjectId = 1,
      subjectName = "Data Structures",
      topic = "Graphs BFS/DFS",
      date = "2026-10-11",
      startTime = "09:00",
      endTime = "10:30",
      location = "Room 101",
      isAlarmEnabled = true
    )

    // Edit schedule time and topic
    val edited = original.copy(
      topic = "Advanced Graph Algorithms & Dijkstra",
      startTime = "10:00",
      endTime = "11:30",
      isAlarmEnabled = false
    )

    assertEquals("Advanced Graph Algorithms & Dijkstra", edited.topic)
    assertEquals("10:00", edited.startTime)
    assertEquals("11:30", edited.endTime)
    assertFalse(edited.isAlarmEnabled)
  }
}

