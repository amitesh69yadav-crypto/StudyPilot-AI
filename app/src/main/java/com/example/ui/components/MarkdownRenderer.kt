package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess

private sealed class MarkdownBlock {
  data class Heading(val level: Int, val text: String) : MarkdownBlock()
  data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
  data class BulletItem(val text: String) : MarkdownBlock()
  data class NumberedItem(val number: String, val text: String) : MarkdownBlock()
  data class BlockQuote(val text: String) : MarkdownBlock()
  data class Paragraph(val text: String) : MarkdownBlock()
}

@Composable
fun FormattedTutorResponse(
  content: String,
  modifier: Modifier = Modifier,
  isUser: Boolean = false
) {
  val context = LocalContext.current
  val blocks = parseMarkdownBlocks(content)

  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    blocks.forEach { block ->
      when (block) {
        is MarkdownBlock.Heading -> {
          HeadingView(block = block, isUser = isUser)
        }
        is MarkdownBlock.CodeBlock -> {
          CodeBlockView(
            language = block.language,
            code = block.code,
            onCopy = {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              clipboard.setPrimaryClip(ClipData.newPlainText("Code Snippet", block.code))
              Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
            }
          )
        }
        is MarkdownBlock.BulletItem -> {
          BulletItemView(text = block.text, isUser = isUser)
        }
        is MarkdownBlock.NumberedItem -> {
          NumberedItemView(number = block.number, text = block.text, isUser = isUser)
        }
        is MarkdownBlock.BlockQuote -> {
          BlockQuoteView(text = block.text, isUser = isUser)
        }
        is MarkdownBlock.Paragraph -> {
          ParagraphView(text = block.text, isUser = isUser)
        }
      }
    }
  }
}

@Composable
private fun HeadingView(block: MarkdownBlock.Heading, isUser: Boolean) {
  val color = if (isUser) {
    MaterialTheme.colorScheme.onPrimary
  } else {
    when (block.level) {
      1 -> MaterialTheme.colorScheme.primary
      2 -> MaterialTheme.colorScheme.secondary
      else -> MaterialTheme.colorScheme.onSurface
    }
  }

  val style = when (block.level) {
    1 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp)
    2 -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp)
    else -> MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
  }

  Column(modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)) {
    Text(
      text = parseInlineMarkdown(block.text, isUser = isUser),
      style = style,
      color = color
    )
    if (block.level == 1 && !isUser) {
      Spacer(modifier = Modifier.height(4.dp))
      HorizontalDivider(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
        thickness = 1.dp
      )
    }
  }
}

@Composable
private fun CodeBlockView(
  language: String,
  code: String,
  onCopy: () -> Unit
) {
  val codeBackground = Color(0xFF1E1E2E) // Modern dark code container
  val codeTextColor = Color(0xFFCDD6F4)
  val barColor = Color(0xFF181825)

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(containerColor = codeBackground),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column {
      // Code Header Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(barColor)
          .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Terminal,
            contentDescription = null,
            tint = CyanAccent,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = language.ifBlank { "CODE" }.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp,
              fontFamily = FontFamily.Monospace
            ),
            color = CyanAccent,
            fontSize = 11.sp
          )
        }

        Surface(
          shape = RoundedCornerShape(6.dp),
          color = Color.White.copy(alpha = 0.08f),
          modifier = Modifier.clip(RoundedCornerShape(6.dp))
        ) {
          Row(
            modifier = Modifier
              .clickable { onCopy() }
              .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.ContentCopy,
              contentDescription = "Copy code",
              tint = Color(0xFFA6ADC8),
              modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Copy",
              fontSize = 10.sp,
              color = Color(0xFFA6ADC8),
              fontWeight = FontWeight.Medium
            )
          }
        }
      }

      // Code body with horizontal scroll
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 10.dp)
          .horizontalScroll(rememberScrollState())
      ) {
        Text(
          text = code,
          style = MaterialTheme.typography.bodySmall.copy(
            fontFamily = FontFamily.Monospace,
            lineHeight = 18.sp,
            fontSize = 12.sp
          ),
          color = codeTextColor
        )
      }
    }
  }
}

@Composable
private fun BulletItemView(text: String, isUser: Boolean) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 2.dp),
    verticalAlignment = Alignment.Top
  ) {
    Box(
      modifier = Modifier
        .padding(top = 7.dp, end = 10.dp, start = 2.dp)
        .size(6.dp)
        .clip(CircleShape)
        .background(if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
    )
    Text(
      text = parseInlineMarkdown(text, isUser = isUser),
      style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
      color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.weight(1f)
    )
  }
}

@Composable
private fun NumberedItemView(number: String, text: String, isUser: Boolean) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 2.dp),
    verticalAlignment = Alignment.Top
  ) {
    Surface(
      shape = RoundedCornerShape(6.dp),
      color = if (isUser) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer,
      modifier = Modifier.padding(top = 2.dp, end = 10.dp)
    ) {
      Text(
        text = number,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        fontSize = 11.sp
      )
    }
    Text(
      text = parseInlineMarkdown(text, isUser = isUser),
      style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
      color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.weight(1f)
    )
  }
}

@Composable
private fun BlockQuoteView(text: String, isUser: Boolean) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp)
      .clip(RoundedCornerShape(8.dp))
      .background(
        if (isUser) Color.White.copy(alpha = 0.12f)
        else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
      )
      .padding(vertical = 8.dp, horizontal = 10.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .width(3.dp)
        .height(28.dp)
        .clip(RoundedCornerShape(2.dp))
        .background(if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
    )
    Spacer(modifier = Modifier.width(10.dp))
    Text(
      text = parseInlineMarkdown(text, isUser = isUser),
      style = MaterialTheme.typography.bodyMedium.copy(
        fontStyle = FontStyle.Italic,
        lineHeight = 20.sp
      ),
      color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
private fun ParagraphView(text: String, isUser: Boolean) {
  Text(
    text = parseInlineMarkdown(text, isUser = isUser),
    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
    color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
  )
}

/**
 * Parses markdown text into structural blocks (Headings, Code, Lists, Quotes, Paragraphs).
 */
private fun parseMarkdownBlocks(rawText: String): List<MarkdownBlock> {
  val blocks = mutableListOf<MarkdownBlock>()
  val lines = rawText.lines()
  var i = 0

  while (i < lines.size) {
    val line = lines[i]

    // 1. Check for Code Block start
    if (line.trimStart().startsWith("```")) {
      val lang = line.trimStart().removePrefix("```").trim()
      val codeLines = mutableListOf<String>()
      i++
      while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
        codeLines.add(lines[i])
        i++
      }
      if (i < lines.size) {
        // Skip closing ```
        i++
      }
      blocks.add(MarkdownBlock.CodeBlock(lang, codeLines.joinToString("\n")))
      continue
    }

    val trimmed = line.trim()
    if (trimmed.isEmpty()) {
      i++
      continue
    }

    // 2. Check for Headings
    if (trimmed.startsWith("#")) {
      val level = trimmed.takeWhile { it == '#' }.length.coerceIn(1, 3)
      val headingText = trimmed.dropWhile { it == '#' }.trim()
      blocks.add(MarkdownBlock.Heading(level, headingText))
      i++
      continue
    }

    // 3. Check for Blockquote
    if (trimmed.startsWith(">")) {
      val quoteText = trimmed.removePrefix(">").trim()
      blocks.add(MarkdownBlock.BlockQuote(quoteText))
      i++
      continue
    }

    // 4. Check for Bullet List Items
    if (trimmed.startsWith("* ") || trimmed.startsWith("- ") || trimmed.startsWith("• ")) {
      val itemText = trimmed.drop(2).trim()
      blocks.add(MarkdownBlock.BulletItem(itemText))
      i++
      continue
    }

    // 5. Check for Numbered List Items (e.g., "1. ", "2) ")
    val numberMatch = Regex("""^(\d+)[\.\)]\s+(.*)$""").find(trimmed)
    if (numberMatch != null) {
      val num = numberMatch.groupValues[1]
      val itemText = numberMatch.groupValues[2]
      blocks.add(MarkdownBlock.NumberedItem(num, itemText))
      i++
      continue
    }

    // 6. Regular Paragraph (Group consecutive non-empty normal lines)
    val paragraphLines = mutableListOf(trimmed)
    i++
    while (i < lines.size) {
      val nextTrimmed = lines[i].trim()
      if (nextTrimmed.isEmpty() ||
        nextTrimmed.startsWith("```") ||
        nextTrimmed.startsWith("#") ||
        nextTrimmed.startsWith(">") ||
        nextTrimmed.startsWith("* ") ||
        nextTrimmed.startsWith("- ") ||
        nextTrimmed.startsWith("• ") ||
        Regex("""^(\d+)[\.\)]\s+""").containsMatchIn(nextTrimmed)
      ) {
        break
      }
      paragraphLines.add(nextTrimmed)
      i++
    }
    blocks.add(MarkdownBlock.Paragraph(paragraphLines.joinToString(" ")))
  }

  return blocks
}

/**
 * Parses inline formatting: **bold**, *italic*, and `inline code`.
 */
@Composable
private fun parseInlineMarkdown(text: String, isUser: Boolean): AnnotatedString {
  val boldColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
  val codeBgColor = if (isUser) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
  val codeTextColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary

  return buildAnnotatedString {
    var index = 0
    val length = text.length

    while (index < length) {
      // Inline Code: `...`
      if (text[index] == '`') {
        val nextTick = text.indexOf('`', index + 1)
        if (nextTick != -1) {
          val codeText = text.substring(index + 1, nextTick)
          val start = this.length
          append(codeText)
          addStyle(
            SpanStyle(
              fontFamily = FontFamily.Monospace,
              background = codeBgColor,
              color = codeTextColor,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold
            ),
            start,
            this.length
          )
          index = nextTick + 1
          continue
        }
      }

      // Bold: **...**
      if (index + 1 < length && text[index] == '*' && text[index + 1] == '*') {
        val nextAsterisks = text.indexOf("**", index + 2)
        if (nextAsterisks != -1) {
          val boldText = text.substring(index + 2, nextAsterisks)
          val start = this.length
          append(boldText)
          addStyle(
            SpanStyle(
              fontWeight = FontWeight.Bold,
              color = boldColor
            ),
            start,
            this.length
          )
          index = nextAsterisks + 2
          continue
        }
      }

      // Italic: *...*
      if (text[index] == '*') {
        val nextAsterisk = text.indexOf('*', index + 1)
        if (nextAsterisk != -1) {
          val italicText = text.substring(index + 1, nextAsterisk)
          val start = this.length
          append(italicText)
          addStyle(
            SpanStyle(
              fontStyle = FontStyle.Italic
            ),
            start,
            this.length
          )
          index = nextAsterisk + 1
          continue
        }
      }

      append(text[index])
      index++
    }
  }
}
