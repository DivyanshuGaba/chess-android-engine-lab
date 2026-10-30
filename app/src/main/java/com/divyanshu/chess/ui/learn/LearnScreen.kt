package com.divyanshu.chess.ui.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.divyanshu.chess.domain.engine.Fen
import com.divyanshu.chess.domain.model.Square
import com.divyanshu.chess.ui.components.ScreenHeader
import com.divyanshu.chess.ui.game.PieceView
import com.divyanshu.chess.ui.theme.BoardColors

private sealed interface LearnEntry {
    data class Header(val group: LearnGroup) : LearnEntry

    data class Topic(val topic: LearnTopic) : LearnEntry
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnScreen(topicId: String?) {
    val entries = remember {
        buildList {
            LearnGroup.entries.forEach { group ->
                add(LearnEntry.Header(group))
                LearnTopics.all.filter { it.group == group }.forEach { add(LearnEntry.Topic(it)) }
            }
        }
    }
    val listState = rememberLazyListState()

    LaunchedEffect(topicId) {
        val index = entries.indexOfFirst { it is LearnEntry.Topic && it.topic.id == topicId }
        if (index >= 0) listState.scrollToItem(index + 1)
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding).testTag("learn_list"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { ScreenHeader("Learn", subtitle = "How games end, and the special moves", modifier = Modifier.padding(horizontal = 0.dp)) }
            items(entries.size) { index ->
                when (val entry = entries[index]) {
                    is LearnEntry.Header -> Text(
                        text = entry.group.title.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    is LearnEntry.Topic -> TopicCard(entry.topic)
                }
            }
        }
    }
}

@Composable
private fun TopicCard(topic: LearnTopic) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp)
            .testTag("topic_${topic.id}"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(topic.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

        topic.fen?.let { fen ->
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                MiniBoard(fen, topic.marks, Modifier.widthIn(max = 340.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                topic.legend.forEach { (kind, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(14.dp).clip(CircleShape).background(kind.color))
                        Text(label, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Section("What it is", topic.summary)
        Section("Why it happens", topic.why)
    }
}

@Composable
private fun Section(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** A read-only board for the worked examples; marks colour the squares the text talks about. */
@Composable
private fun MiniBoard(fen: String, marks: List<LearnMark>, modifier: Modifier = Modifier) {
    val state = remember(fen) { Fen.parse(fen) }
    val markBySquare = remember(marks) { marks.associate { Square.parse(it.square) to it.kind } }
    val frame = RoundedCornerShape(10.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(8.dp, frame)
            .clip(frame)
            .background(BoardColors.frame)
            .padding(5.dp),
    ) {
        Column(Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp))) {
            for (row in 0..7) {
                Row(Modifier.weight(1f)) {
                    for (col in 0..7) {
                        val square = Square.of(col, 7 - row)
                        val isLight = (square.file + square.rank) % 2 == 1
                        val base = if (isLight) BoardColors.light else BoardColors.dark
                        val mark = markBySquare[square]
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .background(mark?.color?.copy(alpha = 0.65f)?.compositeOver(base) ?: base),
                        ) {
                            val labelColor = if (isLight) BoardColors.dark else BoardColors.light
                            if (col == 0) {
                                Text("${square.rank + 1}", color = labelColor, fontSize = 8.sp, modifier = Modifier.padding(start = 2.dp))
                            }
                            if (row == 7) {
                                Text(
                                    "${'a' + square.file}",
                                    color = labelColor,
                                    fontSize = 8.sp,
                                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 2.dp),
                                )
                            }
                            state.board[square]?.let { piece ->
                                PieceView(piece, Modifier.fillMaxSize().padding(1.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
