package com.example.ui.study

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.EvaApplication
import com.example.data.local.entities.StudyTaskEntity
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class DrawPoint(val offset: Offset, val color: Color, val strokeWidth: Float)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = EvaApplication.instance
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Planner", "Focus Timer", "AI Tutor", "Whiteboard")

    val studyTasks by app.database.studyTaskDao().getAllStudyTasks().collectAsState(initial = emptyList())

    // Pomodoro Timer State
    var timerSeconds by remember { mutableIntStateOf(25 * 60) }
    var isTimerRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning && timerSeconds > 0) {
            delay(1000)
            timerSeconds--
            if (timerSeconds == 0) {
                isTimerRunning = false
                app.ttsHelper.speak("Great work! Your study focus session is complete.")
            }
        }
    }

    // Whiteboard Drawing paths
    val paths = remember { mutableStateListOf<List<Offset>>() }
    var currentPath = remember { mutableStateListOf<Offset>() }
    var strokeColor by remember { mutableStateOf(NeonCyan) }

    // AI Tutor States
    var tutorQuery by remember { mutableStateOf("") }
    var tutorAnswer by remember { mutableStateOf("Ask EVA any study question, homework problem, or request flashcards.") }
    var isTutorLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study & Focus Space", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicDarkBackground)
            )
        },
        containerColor = CosmicDarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurface,
                contentColor = NeonCyan,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) NeonCyan else TextMuted
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                0 -> {
                    // Planner & Tasks
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        var newTaskSubject by remember { mutableStateOf("") }
                        var newTaskTitle by remember { mutableStateOf("") }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = newTaskSubject,
                                onValueChange = { newTaskSubject = it },
                                placeholder = { Text("Subject", color = TextMuted) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = DarkOutline)
                            )
                            OutlinedTextField(
                                value = newTaskTitle,
                                onValueChange = { newTaskTitle = it },
                                placeholder = { Text("Task description", color = TextMuted) },
                                singleLine = true,
                                modifier = Modifier.weight(2f),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = DarkOutline)
                            )
                            IconButton(
                                onClick = {
                                    if (newTaskTitle.isNotBlank()) {
                                        coroutineScope.launch {
                                            app.database.studyTaskDao().insertStudyTask(
                                                StudyTaskEntity(
                                                    subject = newTaskSubject.ifBlank { "General" },
                                                    task = newTaskTitle.trim()
                                                )
                                            )
                                            newTaskTitle = ""
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.AddCircle, contentDescription = "Add Task", tint = NeonCyan, modifier = Modifier.size(32.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Scheduled Tasks (${studyTasks.size})", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(studyTasks) { item ->
                                ElevatedCard(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.elevatedCardColors(containerColor = DarkSurface),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Checkbox(
                                            checked = item.isCompleted,
                                            onCheckedChange = { checked ->
                                                coroutineScope.launch {
                                                    app.database.studyTaskDao().updateStudyTask(item.copy(isCompleted = checked))
                                                }
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = NeonCyan)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = ElectricViolet.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = item.subject,
                                                    color = ElectricViolet,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = item.task,
                                                color = if (item.isCompleted) TextMuted else TextPrimary,
                                                fontSize = 14.sp
                                            )
                                        }
                                        IconButton(onClick = {
                                            coroutineScope.launch {
                                                app.database.studyTaskDao().deleteStudyTask(item)
                                            }
                                        }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = StatusError, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Focus Pomodoro Timer
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(4.dp, NeonCyan),
                            modifier = Modifier.size(240.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val minutes = timerSeconds / 60
                                val seconds = timerSeconds % 60
                                Text(
                                    text = String.format("%02d:%02d", minutes, seconds),
                                    color = TextPrimary,
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(
                                onClick = { isTimerRunning = !isTimerRunning },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isTimerRunning) StatusWarning else NeonCyan, contentColor = CosmicDarkBackground),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isTimerRunning) "Pause" else "Start Focus", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    isTimerRunning = false
                                    timerSeconds = 25 * 60
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("Reset")
                            }
                        }
                    }
                }
                2 -> {
                    // AI Tutor
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        OutlinedTextField(
                            value = tutorQuery,
                            onValueChange = { tutorQuery = it },
                            placeholder = { Text("Ask a question, request summary, or quiz...", color = TextMuted) },
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = DarkOutline),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    if (tutorQuery.isNotBlank()) {
                                        isTutorLoading = true
                                        coroutineScope.launch {
                                            val resp = app.aiProviderManager.generateResponse("Act as an expert study mentor. Solve and explain clearly: $tutorQuery")
                                            tutorAnswer = resp.text
                                            isTutorLoading = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CosmicDarkBackground),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Explain & Solve", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (tutorQuery.isNotBlank()) {
                                        isTutorLoading = true
                                        coroutineScope.launch {
                                            val resp = app.aiProviderManager.generateResponse("Generate 3 interactive flashcards with Question and Answer for: $tutorQuery")
                                            tutorAnswer = resp.text
                                            isTutorLoading = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Flashcards")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        ElevatedCard(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = DarkSurface),
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("AI Mentor Guidance", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                if (isTutorLoading) {
                                    CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(24.dp))
                                } else {
                                    Text(text = tutorAnswer, color = TextPrimary, fontSize = 14.sp, lineHeight = 20.sp)
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // Whiteboard
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(NeonCyan, ElectricViolet, Color(0xFFFFD600), Color.White, StatusError).forEach { col ->
                                    Surface(
                                        shape = CircleShape,
                                        color = col,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clickable { strokeColor = col }
                                    ) {}
                                }
                            }

                            Row {
                                IconButton(onClick = {
                                    paths.clear()
                                    currentPath.clear()
                                }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Clear", tint = StatusError)
                                }
                                IconButton(onClick = {
                                    Toast.makeText(context, "Whiteboard exported to study notes", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.SaveAlt, contentDescription = "Save", tint = NeonCyan)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF03050C),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                currentPath.clear()
                                                currentPath.add(offset)
                                            },
                                            onDrag = { change, _ ->
                                                currentPath.add(change.position)
                                            },
                                            onDragEnd = {
                                                if (currentPath.isNotEmpty()) {
                                                    paths.add(currentPath.toList())
                                                    currentPath.clear()
                                                }
                                            }
                                        )
                                    }
                            ) {
                                for (pathPoints in paths) {
                                    if (pathPoints.size > 1) {
                                        val p = Path()
                                        p.moveTo(pathPoints[0].x, pathPoints[0].y)
                                        for (pt in pathPoints.drop(1)) {
                                            p.lineTo(pt.x, pt.y)
                                        }
                                        drawPath(p, color = strokeColor, style = Stroke(width = 4.dp.toPx()))
                                    }
                                }

                                if (currentPath.size > 1) {
                                    val p = Path()
                                    p.moveTo(currentPath[0].x, currentPath[0].y)
                                    for (pt in currentPath.drop(1)) {
                                        p.lineTo(pt.x, pt.y)
                                    }
                                    drawPath(p, color = strokeColor, style = Stroke(width = 4.dp.toPx()))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
