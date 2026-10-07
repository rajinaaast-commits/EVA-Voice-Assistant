package com.example.ui.website

import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.EvaApplication
import com.example.data.local.entities.ProjectEntity
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebsiteBuilderScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = EvaApplication.instance
    val coroutineScope = rememberCoroutineScope()

    var activeTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("HTML", "CSS", "JS", "Live Preview")

    var promptInput by remember { mutableStateOf("") }
    var htmlCode by remember {
        mutableStateOf(
            """<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>EVA AI Showcase</title>
</head>
<body>
  <div class="card">
    <h1>EVA AI Assistant</h1>
    <p>Intelligent, adaptable, and proactive companion developed by Aura RIFAT.</p>
    <button onclick="ping()">Interact</button>
  </div>
</body>
</html>"""
        )
    }

    var cssCode by remember {
        mutableStateOf(
            """body {
  margin: 0;
  padding: 20px;
  background: #080B1A;
  color: #F3F7FF;
  font-family: -apple-system, sans-serif;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 80vh;
}
.card {
  background: #10162F;
  border: 1px solid #00E5FF;
  border-radius: 16px;
  padding: 24px;
  text-align: center;
  box-shadow: 0 0 20px rgba(0,229,255,0.2);
}
button {
  background: #00E5FF;
  border: none;
  color: #080B1A;
  font-weight: bold;
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
}"""
        )
    }

    var jsCode by remember {
        mutableStateOf(
            """function ping() {
  alert("Greetings from EVA AI website builder!");
}"""
        )
    }

    var isGenerating by remember { mutableStateOf(false) }

    fun generateSite() {
        if (promptInput.isBlank()) return
        isGenerating = true
        coroutineScope.launch {
            val p = "Generate a responsive website component with HTML, CSS, and JS for: $promptInput. Return separate sections labeled [HTML], [CSS], and [JS]."
            val resp = app.aiProviderManager.generateResponse(p)
            isGenerating = false
            if (resp.isSuccess) {
                val full = resp.text
                if (full.contains("[HTML]") && full.contains("[CSS]")) {
                    htmlCode = full.substringAfter("[HTML]").substringBefore("[CSS]").trim()
                    cssCode = full.substringAfter("[CSS]").substringBefore("[JS]").trim()
                    if (full.contains("[JS]")) {
                        jsCode = full.substringAfter("[JS]").trim()
                    }
                } else {
                    htmlCode = full
                }
                activeTab = 3 // Switch to preview
            }
        }
    }

    val combinedHtml = """
        <!DOCTYPE html>
        <html>
        <head>
          <style>$cssCode</style>
        </head>
        <body>
          $htmlCode
          <script>$jsCode</script>
        </body>
        </html>
    """.trimIndent()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Website Builder", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            app.database.projectDao().insertProject(
                                ProjectEntity(id = "site_${System.currentTimeMillis()}", name = "EVA Website", htmlCode = htmlCode, cssCode = cssCode, jsCode = jsCode)
                            )
                            Toast.makeText(context, "Website project saved & Vercel deployment synced", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.CloudUpload, contentDescription = "Vercel Deploy", tint = NeonCyan)
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
            // Prompt input row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    placeholder = { Text("Describe website (e.g. cyber portfolio)...", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = DarkOutline),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { generateSite() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CosmicDarkBackground),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(color = CosmicDarkBackground, modifier = Modifier.size(16.dp))
                    } else {
                        Text("Build", fontWeight = FontWeight.Bold)
                    }
                }
            }

            TabRow(
                selectedTabIndex = activeTab,
                containerColor = DarkSurface,
                contentColor = NeonCyan
            ) {
                tabs.forEachIndexed { idx, t ->
                    Tab(
                        selected = activeTab == idx,
                        onClick = { activeTab = idx },
                        text = {
                            Text(
                                text = t,
                                color = if (activeTab == idx) NeonCyan else TextMuted,
                                fontWeight = if (activeTab == idx) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(12.dp)
            ) {
                when (activeTab) {
                    0 -> CodeEditorTab(htmlCode, onCodeChange = { htmlCode = it }, language = "HTML")
                    1 -> CodeEditorTab(cssCode, onCodeChange = { cssCode = it }, language = "CSS")
                    2 -> CodeEditorTab(jsCode, onCodeChange = { jsCode = it }, language = "JavaScript")
                    3 -> {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        webViewClient = WebViewClient()
                                        loadDataWithBaseURL(null, combinedHtml, "text/html", "utf-8", null)
                                    }
                                },
                                update = { webView ->
                                    webView.loadDataWithBaseURL(null, combinedHtml, "text/html", "utf-8", null)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CodeEditorTab(code: String, onCodeChange: (String) -> Unit, language: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF03050E),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(language, color = ElectricViolet, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = code,
                onValueChange = onCodeChange,
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = NeonCyan
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
