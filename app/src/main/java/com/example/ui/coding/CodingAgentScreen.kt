package com.example.ui.coding

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.EvaApplication
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodingAgentScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = EvaApplication.instance
    val coroutineScope = rememberCoroutineScope()

    var userCodingPrompt by remember { mutableStateOf("") }
    var codeContent by remember {
        mutableStateOf(
            """// EVA AI Agentic Code Workspace
fun main() {
    println("Hello, EVA AI Assistant!")
}"""
        )
    }
    var codeExplanation by remember { mutableStateOf("Ready to generate, review, or debug code.") }
    var isLoading by remember { mutableStateOf(false) }

    val codeActions = listOf("Generate", "Explain", "Debug & Fix", "Refactor", "Review", "Deploy")

    fun runCodeAction(action: String) {
        if (userCodingPrompt.isBlank() && codeContent.isBlank()) return
        isLoading = true
        coroutineScope.launch {
            val fullPrompt = when (action) {
                "Generate" -> "Generate clean, modern, production code for: $userCodingPrompt. Return only the code block."
                "Explain" -> "Explain how this code works step by step: \n$codeContent"
                "Debug & Fix" -> "Find any bugs, memory leaks, or logical errors in this code and provide the fixed code: \n$codeContent"
                "Refactor" -> "Refactor this code for optimal performance, readability, and modern idioms: \n$codeContent"
                "Review" -> "Conduct a senior software engineering code review: \n$codeContent"
                "Deploy" -> "Prepare deployment scripts and configuration for: \n$codeContent"
                else -> userCodingPrompt
            }

            val response = app.aiProviderManager.generateResponse(fullPrompt)
            isLoading = false
            if (response.isSuccess) {
                if (action in listOf("Generate", "Debug & Fix", "Refactor")) {
                    codeContent = response.text
                }
                codeExplanation = response.text
            } else {
                codeExplanation = response.text
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Coding Agent", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Code", codeContent))
                        Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code", tint = NeonCyan)
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
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = userCodingPrompt,
                onValueChange = { userCodingPrompt = it },
                placeholder = { Text("Describe code to build, fix, or analyze...", color = TextMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = DarkOutline)
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(codeActions) { action ->
                    Button(
                        onClick = { runCodeAction(action) },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant, contentColor = NeonCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(action, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Code Editor Box
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF04060E),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("CODE WORKSPACE", color = ElectricViolet, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        if (isLoading) {
                            CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = codeContent,
                        onValueChange = { codeContent = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = NeonCyan
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Explanation / Terminal Output
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("AI Engineer Feedback:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = codeExplanation, color = TextPrimary, fontSize = 12.sp, lineHeight = 16.sp)
                }
            }
        }
    }
}
