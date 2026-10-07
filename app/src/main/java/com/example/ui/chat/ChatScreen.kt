package com.example.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.EvaApplication
import com.example.data.local.entities.ConversationEntity
import com.example.data.local.entities.MessageEntity
import com.example.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    initialPrompt: String? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = EvaApplication.instance
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var activeConversationId by remember { mutableStateOf("conv_${System.currentTimeMillis()}") }
    var currentTitle by remember { mutableStateOf("New Conversation") }
    var inputText by remember { mutableStateOf(initialPrompt ?: "") }
    var isGenerating by remember { mutableStateOf(false) }
    var streamingJob by remember { mutableStateOf<Job?>(null) }
    var currentStreamingChunk by remember { mutableStateOf("") }

    val conversations by app.database.conversationDao().getAllConversations().collectAsState(initial = emptyList())
    val messages by app.database.messageDao().getMessagesForConversation(activeConversationId).collectAsState(initial = emptyList())

    var showHistoryDrawer by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedAttachmentUri by remember { mutableStateOf<Uri?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        selectedAttachmentUri = uri
        if (uri != null) {
            Toast.makeText(context, "Attachment loaded: ${uri.lastPathSegment}", Toast.LENGTH_SHORT).show()
        }
    }

    // Auto-scroll when messages update
    LaunchedEffect(messages.size, currentStreamingChunk) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    // Auto-send initial prompt if provided
    LaunchedEffect(initialPrompt) {
        if (!initialPrompt.isNullOrBlank()) {
            val promptToSend = initialPrompt
            inputText = ""
            // Trigger send
            coroutineScope.launch {
                val userMsg = MessageEntity(
                    id = "msg_${System.currentTimeMillis()}",
                    conversationId = activeConversationId,
                    sender = "user",
                    content = promptToSend
                )
                app.database.messageDao().insertMessage(userMsg)
                app.database.conversationDao().insertConversation(
                    ConversationEntity(id = activeConversationId, title = promptToSend.take(30))
                )

                isGenerating = true
                currentStreamingChunk = ""
                val history = messages.map { it.sender to it.content }

                val response = app.aiProviderManager.generateResponse(
                    prompt = promptToSend,
                    history = history,
                    onChunk = { chunk ->
                        currentStreamingChunk += chunk
                    }
                )

                val assistantMsg = MessageEntity(
                    id = "msg_${System.currentTimeMillis() + 1}",
                    conversationId = activeConversationId,
                    sender = "eva",
                    content = if (currentStreamingChunk.isNotBlank()) currentStreamingChunk else response.text,
                    providerUsed = response.providerUsed
                )
                app.database.messageDao().insertMessage(assistantMsg)
                isGenerating = false
                currentStreamingChunk = ""
                if (response.isSuccess) {
                    app.ttsHelper.speak(assistantMsg.content)
                }
            }
        }
    }

    fun sendMessage(promptText: String) {
        if (promptText.isBlank()) return
        val currentText = promptText.trim()
        inputText = ""

        streamingJob?.cancel()
        streamingJob = coroutineScope.launch {
            isGenerating = true
            currentStreamingChunk = ""

            val userMsg = MessageEntity(
                id = "msg_${System.currentTimeMillis()}",
                conversationId = activeConversationId,
                sender = "user",
                content = currentText,
                attachmentPath = selectedAttachmentUri?.toString()
            )
            selectedAttachmentUri = null
            app.database.messageDao().insertMessage(userMsg)

            // Ensure conversation recorded
            val currentConv = app.database.conversationDao().getConversationById(activeConversationId)
            if (currentConv == null) {
                val title = currentText.take(28)
                currentTitle = title
                app.database.conversationDao().insertConversation(
                    ConversationEntity(id = activeConversationId, title = title)
                )
            }

            val history = messages.map { it.sender to it.content }

            val response = app.aiProviderManager.generateResponse(
                prompt = currentText,
                history = history,
                onChunk = { chunk ->
                    currentStreamingChunk += chunk
                }
            )

            val finalText = if (currentStreamingChunk.isNotBlank()) currentStreamingChunk else response.text
            val assistantMsg = MessageEntity(
                id = "msg_${System.currentTimeMillis() + 1}",
                conversationId = activeConversationId,
                sender = "eva",
                content = finalText,
                providerUsed = response.providerUsed
            )
            app.database.messageDao().insertMessage(assistantMsg)
            isGenerating = false
            currentStreamingChunk = ""

            // Speak if enabled
            if (response.isSuccess) {
                app.ttsHelper.speak(finalText)
            }

            // Execute parsed tool calls
            for (tool in response.toolCalls) {
                val toolResult = app.aiProviderManager.executeTool(tool)
                if (toolResult.requiresConfirmation) {
                    Toast.makeText(context, toolResult.pendingActionDescription ?: "Confirmation needed", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = currentTitle, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(
                            text = "Provider: ${app.preferences.getProviderMode().name}",
                            color = NeonCyan,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showHistoryDrawer = true }) {
                        Icon(Icons.Default.History, contentDescription = "History", tint = TextPrimary)
                    }
                    IconButton(onClick = {
                        activeConversationId = "conv_${System.currentTimeMillis()}"
                        currentTitle = "New Conversation"
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "New Chat", tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicDarkBackground)
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // If generating, show Stop button
                if (isGenerating) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        OutlinedButton(
                            onClick = {
                                streamingJob?.cancel()
                                isGenerating = false
                                if (currentStreamingChunk.isNotBlank()) {
                                    coroutineScope.launch {
                                        app.database.messageDao().insertMessage(
                                            MessageEntity(
                                                id = "msg_${System.currentTimeMillis()}",
                                                conversationId = activeConversationId,
                                                sender = "eva",
                                                content = currentStreamingChunk,
                                                providerUsed = "Stopped"
                                            )
                                        )
                                        currentStreamingChunk = ""
                                    }
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stop Generation", fontSize = 12.sp)
                        }
                    }
                }

                selectedAttachmentUri?.let { uri ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Attached: ${uri.lastPathSegment}", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { selectedAttachmentUri = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { filePicker.launch("*/*") }) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Attach File", tint = TextMuted)
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ask EVA anything...", color = TextMuted, fontSize = 14.sp) },
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkOutline
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = CircleShape,
                        color = if (inputText.isNotBlank()) NeonCyan else DarkSurfaceVariant,
                        modifier = Modifier
                            .size(46.dp)
                            .clickable {
                                if (inputText.isNotBlank()) {
                                    sendMessage(inputText)
                                } else {
                                    app.speechHelper.startListening(
                                        onFinalResult = { recognized ->
                                            sendMessage(recognized)
                                        },
                                        onError = { err ->
                                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                            .testTag("chat_send_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (inputText.isNotBlank()) Icons.Default.Send else Icons.Default.Mic,
                                contentDescription = "Send",
                                tint = if (inputText.isNotBlank()) CosmicDarkBackground else TextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        },
        containerColor = CosmicDarkBackground
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isUser = msg.sender.equals("user", ignoreCase = true)
                ChatBubble(
                    message = msg,
                    isUser = isUser,
                    onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("EVA Chat", msg.content))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    onRegenerate = {
                        sendMessage(msg.content)
                    }
                )
            }

            // Live streaming bubble
            if (isGenerating && currentStreamingChunk.isNotBlank()) {
                item {
                    ChatBubble(
                        message = MessageEntity(
                            id = "streaming",
                            conversationId = activeConversationId,
                            sender = "eva",
                            content = currentStreamingChunk,
                            providerUsed = app.preferences.getProviderMode().name
                        ),
                        isUser = false,
                        onCopy = {},
                        onRegenerate = {}
                    )
                }
            }
        }
    }

    // Conversation History Drawer / Modal
    if (showHistoryDrawer) {
        ModalBottomSheet(
            onDismissRequest = { showHistoryDrawer = false },
            containerColor = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Conversations History",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = {
                        activeConversationId = "conv_${System.currentTimeMillis()}"
                        currentTitle = "New Conversation"
                        showHistoryDrawer = false
                    }) {
                        Icon(Icons.Default.AddComment, contentDescription = "New", tint = NeonCyan)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search conversations...", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkOutline
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                val filtered = conversations.filter {
                    it.title.contains(searchQuery, ignoreCase = true)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered) { conv ->
                        ElevatedCard(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = if (conv.id == activeConversationId) DarkSurfaceVariant else CosmicDarkBackground
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    activeConversationId = conv.id
                                    currentTitle = conv.title
                                    showHistoryDrawer = false
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                if (conv.isPinned) {
                                    Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = StatusWarning, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = conv.title,
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1
                                )
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            app.database.conversationDao().updateConversation(
                                                conv.copy(isPinned = !conv.isPinned)
                                            )
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = "Pin",
                                        tint = if (conv.isPinned) StatusWarning else TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            app.database.conversationDao().deleteConversation(conv.id)
                                            app.database.messageDao().deleteMessagesForConversation(conv.id)
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = StatusError, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: MessageEntity,
    isUser: Boolean,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) NeonCyan.copy(alpha = 0.2f) else DarkSurface,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isUser) NeonCyan.copy(alpha = 0.6f) else DarkOutline
            ),
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clickable { showMenu = true }
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = ElectricViolet.copy(alpha = 0.2f),
                            modifier = Modifier.size(18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("E", color = ElectricViolet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "EVA • ${message.providerUsed}",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = message.content,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                // Message Timestamp
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)),
                    color = TextMuted,
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(DarkSurfaceVariant)
        ) {
            DropdownMenuItem(
                text = { Text("Copy", color = TextPrimary) },
                onClick = {
                    onCopy()
                    showMenu = false
                },
                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = TextMuted) }
            )
            if (isUser) {
                DropdownMenuItem(
                    text = { Text("Regenerate", color = TextPrimary) },
                    onClick = {
                        onRegenerate()
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = NeonCyan) }
                )
            }
        }
    }
}
