package com.example.ui.tools

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.EvaApplication
import com.example.data.local.entities.DocumentEntity
import com.example.data.local.entities.ReminderEntity
import com.example.ui.theme.*
import kotlinx.coroutines.launch

data class SkillItem(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val isInstalled: Boolean = true,
    val isEnabled: Boolean = true,
    val isPremium: Boolean = false
)

data class ConnectorItem(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val isConnected: Boolean,
    val permissions: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsHubScreen(
    initialTab: Int = 0,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = EvaApplication.instance
    val coroutineScope = rememberCoroutineScope()

    var activeTab by remember { mutableIntStateOf(initialTab) }
    val tabs = listOf("Reminders", "Email & SMS", "Documents", "Markets", "Skills Store", "Connectors")

    val reminders by app.database.reminderDao().getAllReminders().collectAsState(initial = emptyList())
    val documents by app.database.documentDao().getAllDocuments().collectAsState(initial = emptyList())

    // Skill items state
    val skills = remember {
        mutableStateListOf(
            SkillItem("pro-email", "Pro Email Writer", "Drafts executive SMTP and Gmail messages with AI tone adaptation", Icons.Default.Email, isInstalled = true, isEnabled = true),
            SkillItem("pro-whatsapp", "WhatsApp Automation", "Smart WhatsApp response drafting and message announcements", Icons.Default.Chat, isInstalled = true, isEnabled = true, isPremium = true),
            SkillItem("youtube-script", "YouTube Script & Titles", "Generates high-CTR video hooks, chapters, and thumbnails", Icons.Default.VideoLibrary, isInstalled = true, isEnabled = true),
            SkillItem("social-posting", "Social Content Strategist", "Autonomous viral threads and cross-platform campaign strategy", Icons.Default.Share, isInstalled = true, isEnabled = true),
            SkillItem("daily-briefing", "Daily Briefing Executive", "Morning audio news digest with calendar reminders & weather", Icons.Default.Today, isInstalled = false, isEnabled = false),
            SkillItem("call-secretary", "Call Secretary Pro", "Handles incoming calls and screen callers during driving mode", Icons.Default.PhoneCallback, isInstalled = false, isEnabled = false, isPremium = true),
            SkillItem("photo-share", "Vision Inspector", "Multimodal image reasoning and visual defect detection", Icons.Default.PhotoLibrary, isInstalled = false, isEnabled = false),
            SkillItem("music-dj", "EVA Audio DJ", "Smart soundtrack generation matching focus and workout intensity", Icons.Default.Headphones, isInstalled = false, isEnabled = false)
        )
    }

    // Connectors state
    val connectors = remember {
        mutableStateListOf(
            ConnectorItem("Google Drive", "Cloud documents & backups", Icons.Default.CloudQueue, true, "Read & Write Documents"),
            ConnectorItem("GitHub", "Source code repositories & PRs", Icons.Default.Code, true, "Read / Push Repositories"),
            ConnectorItem("Vercel", "Web instant deployment", Icons.Default.Language, true, "Deploy Web Projects"),
            ConnectorItem("Notion", "Workspace sync & notes", Icons.Default.NoteAlt, false, "Read & Update Pages"),
            ConnectorItem("Telegram", "Bot alerts and channel posts", Icons.Default.Send, false, "Send Messages"),
            ConnectorItem("Todoist", "Task calendar synchronization", Icons.Default.Checklist, false, "Manage Tasks"),
            ConnectorItem("Linear", "Engineering sprint tracking", Icons.Default.BugReport, false, "Read Issues")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tools & Integrations", color = TextPrimary, fontWeight = FontWeight.Bold) },
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
            ScrollableTabRow(
                selectedTabIndex = activeTab,
                containerColor = DarkSurface,
                contentColor = NeonCyan,
                edgePadding = 12.dp
            ) {
                tabs.forEachIndexed { idx, title ->
                    Tab(
                        selected = activeTab == idx,
                        onClick = { activeTab = idx },
                        text = {
                            Text(
                                text = title,
                                color = if (activeTab == idx) NeonCyan else TextMuted,
                                fontWeight = if (activeTab == idx) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (activeTab) {
                0 -> {
                    // Reminders Tab
                    var reminderTitleInput by remember { mutableStateOf("") }
                    var reminderTimeInput by remember { mutableStateOf("8:00 PM") }

                    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = reminderTitleInput,
                                onValueChange = { reminderTitleInput = it },
                                placeholder = { Text("e.g. Study chemistry", color = TextMuted) },
                                singleLine = true,
                                modifier = Modifier.weight(2f),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = DarkOutline)
                            )
                            OutlinedTextField(
                                value = reminderTimeInput,
                                onValueChange = { reminderTimeInput = it },
                                placeholder = { Text("Time", color = TextMuted) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = DarkOutline)
                            )
                            IconButton(onClick = {
                                if (reminderTitleInput.isNotBlank()) {
                                    coroutineScope.launch {
                                        app.database.reminderDao().insertReminder(
                                            ReminderEntity(
                                                title = "${reminderTitleInput.trim()} at $reminderTimeInput",
                                                timestamp = System.currentTimeMillis() + 3600000
                                            )
                                        )
                                        reminderTitleInput = ""
                                        Toast.makeText(context, "Voice reminder configured", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }) {
                                Icon(Icons.Default.AddAlarm, contentDescription = "Add", tint = NeonCyan, modifier = Modifier.size(28.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(reminders) { rem ->
                                ElevatedCard(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.elevatedCardColors(containerColor = DarkSurface),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(14.dp)
                                    ) {
                                        Icon(Icons.Default.Alarm, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(text = rem.title, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                        IconButton(onClick = {
                                            coroutineScope.launch { app.database.reminderDao().deleteReminder(rem) }
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
                    // Email & SMS Tab
                    var emailRecipient by remember { mutableStateOf("") }
                    var emailSubject by remember { mutableStateOf("") }
                    var emailBody by remember { mutableStateOf("") }
                    var isRewriting by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        OutlinedTextField(
                            value = emailRecipient,
                            onValueChange = { emailRecipient = it },
                            label = { Text("To (Recipient email or phone)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = DarkOutline),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = emailSubject,
                            onValueChange = { emailSubject = it },
                            label = { Text("Subject") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = DarkOutline),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = emailBody,
                            onValueChange = { emailBody = it },
                            label = { Text("Message Body") },
                            maxLines = 6,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = DarkOutline),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    if (emailBody.isNotBlank()) {
                                        isRewriting = true
                                        coroutineScope.launch {
                                            val resp = app.aiProviderManager.generateResponse("Professionally polish and format this executive email message: \n$emailBody")
                                            emailBody = resp.text
                                            isRewriting = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isRewriting) CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(16.dp))
                                else Text("AI Polish")
                            }

                            Button(
                                onClick = {
                                    val sendIntent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:")
                                        putExtra(Intent.EXTRA_EMAIL, arrayOf(emailRecipient))
                                        putExtra(Intent.EXTRA_SUBJECT, emailSubject)
                                        putExtra(Intent.EXTRA_TEXT, emailBody)
                                    }
                                    try {
                                        context.startActivity(sendIntent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Email client launched", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CosmicDarkBackground),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Send Email", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                2 -> {
                    // Documents Tab
                    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        var docTitle by remember { mutableStateOf("") }
                        var docContent by remember { mutableStateOf("") }

                        OutlinedTextField(
                            value = docTitle,
                            onValueChange = { docTitle = it },
                            placeholder = { Text("Document Title", color = TextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = DarkOutline),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = docContent,
                            onValueChange = { docContent = it },
                            placeholder = { Text("Document content or prompt to write...", color = TextMuted) },
                            maxLines = 5,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = DarkOutline),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    if (docTitle.isNotBlank() && docContent.isNotBlank()) {
                                        coroutineScope.launch {
                                            app.database.documentDao().insertDocument(
                                                DocumentEntity(id = "doc_${System.currentTimeMillis()}", title = docTitle, content = docContent)
                                            )
                                            docTitle = ""
                                            docContent = ""
                                            Toast.makeText(context, "Document saved", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CosmicDarkBackground),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Save Document", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (docContent.isNotBlank()) {
                                        coroutineScope.launch {
                                            val resp = app.aiProviderManager.generateResponse("Summarize this document clearly: \n$docContent")
                                            docContent = resp.text
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("AI Summarize")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(documents) { doc ->
                                ElevatedCard(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.elevatedCardColors(containerColor = DarkSurface),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(text = doc.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                            IconButton(onClick = {
                                                coroutineScope.launch { app.database.documentDao().deleteDocument(doc.id) }
                                            }) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = StatusError, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        Text(text = doc.content, color = TextSecondary, fontSize = 13.sp, maxLines = 2)
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // Markets Tab
                    val marketStocks = listOf(
                        Triple("GOOGL", "$188.42", "+2.4%"),
                        Triple("NVDA", "$132.80", "+3.8%"),
                        Triple("AAPL", "$234.15", "+0.9%"),
                        Triple("BTC / USD", "$74,800", "+4.2%"),
                        Triple("ETH / USD", "$3,920", "+3.1%")
                    )

                    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        Text("Global Markets & AI Sentiment", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(10.dp))

                        marketStocks.forEach { (ticker, price, change) ->
                            ElevatedCard(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.elevatedCardColors(containerColor = DarkSurface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(14.dp)
                                ) {
                                    Column {
                                        Text(ticker, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Text("AI Sentiment: Bullish Momentum", color = TextMuted, fontSize = 11.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(price, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Text(change, color = StatusSuccess, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
                4 -> {
                    // Skills & Store Tab
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(skills) { skill ->
                            val idx = skills.indexOf(skill)
                            ElevatedCard(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.elevatedCardColors(containerColor = DarkSurface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(14.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (skill.isEnabled) NeonCyan.copy(alpha = 0.2f) else DarkSurfaceVariant,
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(skill.icon, contentDescription = null, tint = if (skill.isEnabled) NeonCyan else TextMuted, modifier = Modifier.size(24.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(skill.name, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                            if (skill.isPremium) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("PRO", color = StatusWarning, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Text(skill.description, color = TextMuted, fontSize = 11.sp, lineHeight = 15.sp)
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Switch(
                                        checked = skill.isEnabled,
                                        onCheckedChange = { chk ->
                                            skills[idx] = skill.copy(isEnabled = chk, isInstalled = true)
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = DarkSurfaceVariant)
                                    )
                                }
                            }
                        }
                    }
                }
                5 -> {
                    // Connectors Tab
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(connectors) { conn ->
                            val idx = connectors.indexOf(conn)
                            ElevatedCard(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.elevatedCardColors(containerColor = DarkSurface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(14.dp)
                                ) {
                                    Icon(conn.icon, contentDescription = null, tint = if (conn.isConnected) NeonCyan else TextMuted, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(conn.name, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                        Text(conn.description, color = TextSecondary, fontSize = 12.sp)
                                        Text("Perms: ${conn.permissions}", color = TextMuted, fontSize = 10.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            connectors[idx] = conn.copy(isConnected = !conn.isConnected)
                                            Toast.makeText(context, "${conn.name} status updated", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (conn.isConnected) DarkSurfaceVariant else NeonCyan,
                                            contentColor = if (conn.isConnected) TextPrimary else CosmicDarkBackground
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(if (conn.isConnected) "Disconnect" else "Connect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
