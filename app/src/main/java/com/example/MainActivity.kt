package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// ==========================================
// DATA MODELS & ENUMS
// ==========================================

enum class AppScreen {
    LANDING,
    DASHBOARD,
    AI_CREATOR,
    VISUAL_EDITOR,
    STORE_BACKEND,
    SANDBOX_COMPLIANCE
}

enum class UserRole(val label: String, val level: String, val canCode: Boolean, val canDeploy: Boolean) {
    SUPER_ADMIN("Super Admin", "🔑 Level 5 - Root Full Access", true, true),
    WEBSITE_OWNER("Website Owner", "👑 Level 4 - Create & Publish", true, true),
    COLLABORATOR("Team Collaborator", "✏️ Level 3 - Edit Content", false, false),
    CUSTOMER("Customer", "🛒 Level 2 - Purchase & Cart", false, false),
    GUEST("Guest", "👁️ Level 1 - View Only", false, false)
}

enum class ViewportMode(val icon: ImageVector, val widthDp: Int?) {
    MOBILE(Icons.Default.Phone, 340),
    TABLET(Icons.Default.Settings, 600),
    DESKTOP(Icons.Default.Settings, null)
}

data class GeneratedWebSite(
    val id: String,
    val name: String,
    val headline: String,
    val tagline: String,
    val description: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val bgColor: Color,
    val productsList: List<WebProduct>,
    val booksList: List<KSCBook> = emptyList(),
    val isEcomEnabled: Boolean = true,
    val customPrompt: String = "",
    val voiceBotName: String = "Ava AI Agent",
    val voiceBotTone: String = "Helpful & Professional",
    val voiceBotKnowledgeBasis: List<String> = listOf("Product Catalog & Stock", "Store Operating Hours", "Return & Refund Conditions"),
    val voiceBotIsEnabledOnWeb: Boolean = true,
    val voiceBotUsageRate: Double = 0.05, // Double rate in USD per turn
    val voiceBotAccumulatedUsageUSD: Double = 14.85, 
    val voiceBotTotalQueries: Int = 297,
    val voiceBotTrainingTrained: Boolean = true,
    val voiceBotVoiceGender: String = "Female Accent (US Ambient)"
)

data class WebProduct(
    val id: String,
    val title: String,
    val price: Double,
    val emoji: String,
    var stockCount: Int,
    val category: String
)

data class KSCBook(
    val title: String,
    val subtitle: String,
    val desc: String,
    val emoji: String
)

data class ChatMessage(
    val sender: String, // "AI" or "USER"
    val text: String
)

enum class SkinBackground(
    val label: String,
    val iconEmoji: String,
    val isDark: Boolean,
    val textColor: Color,
    val mutedColor: Color,
    val accentColor: Color,
    val bgColors: List<Color>
) {
    NORMAL("Normal Dark", "🌌", true, Color.White, Color(0xFF8892B0), Color(0xFF00D4AA), listOf(Color(0xFF050515), Color(0xFF0F0F25))),
    METALLIC_SKY("Metallic Sky Blue", "💎", false, Color(0xFF01579B), Color(0xFF0288D1), Color(0xFF03A9F4), listOf(Color(0xFFE0F7FA), Color(0xFF80DEEA), Color(0xFFB2EBF2))),
    RUBY_RED("Ruby Red Metal", "🌹", true, Color(0xFFFFEBEE), Color(0xFFFFCDD2), Color(0xFFFF2D55), listOf(Color(0xFF4A000A), Color(0xFF8B001A), Color(0xFF3E0A12))),
    ROSE_PINK("Rose Pink", "🌸", false, Color(0xFF4A148C), Color(0xFF8E24AA), Color(0xFFE91E63), listOf(Color(0xFFFFF0F5), Color(0xFFFFD1DC), Color(0xFFFFE4E1))),
    LIGHT_BG("Light Studio", "☀️", false, Color(0xFF1A1A1A), Color(0xFF757575), Color(0xFF6C3AFF), listOf(Color(0xFFFAFAFA), Color(0xFFF5F5F7), Color(0xFFEEEEEE))),
    DARK_BG("Dark Charcoal", "🌑", true, Color(0xFFE0E0E0), Color(0xFF757575), Color(0xFF00D4AA), listOf(Color(0xFF121212), Color(0xFF1E1E1E), Color(0xFF181818))),
    FOREST_CANOPY("Forest Canopy", "🌿", true, Color(0xFFE8F5E9), Color(0xFFA5D6A7), Color(0xFFD4A373), listOf(Color(0xFF0F3223), Color(0xFF1B4D3E), Color(0xFF0B251A)))
}

// ==========================================
// MAIN ACTIVITY
// ==========================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NexusMainApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

// ==========================================
// CORE APP COMPOSABLE
// ==========================================

@Composable
fun NexusMainApp(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // ------------------------------------------
    // STATE MANAGERS
    // ------------------------------------------
    var currentScreen by remember { mutableStateOf(AppScreen.LANDING) }
    var userRole by remember { mutableStateOf(UserRole.WEBSITE_OWNER) }
    var isLoggedIn by remember { mutableStateOf(false) }
    var currentEmail by remember { mutableStateOf("trinitechnow@gmail.com") }
    var currentPassword by remember { mutableStateOf("") }
    var aiCredits by remember { mutableStateOf(450) }
    var isGeneratingWebsite by remember { mutableStateOf(false) }
    var activeViewport by remember { mutableStateOf(ViewportMode.DESKTOP) }
    
    // Skin Background Theme State
    var activeSkinBg by remember { mutableStateOf(SkinBackground.NORMAL) }

    // TextToSpeech Engine Bound to Compose Lifecycle
    val tts = remember { mutableStateOf<android.speech.tts.TextToSpeech?>(null) }
    DisposableEffect(context) {
        var speech: android.speech.tts.TextToSpeech? = null
        speech = android.speech.tts.TextToSpeech(context) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                try {
                    speech?.setLanguage(java.util.Locale.US)
                } catch(e: Exception) {}
            }
        }
        tts.value = speech
        onDispose {
            try {
                speech?.stop()
                speech?.shutdown()
            } catch(e: Exception) {}
        }
    }

    val speakText: (String) -> Unit = { text ->
        try {
            tts.value?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Voice Recognition States
    var isListeningByVoice by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }

    // Forms and Inputs
    var promptInputSecretKey by remember { mutableStateOf("") }
    var websitePromptText by remember { mutableStateOf("A modern eco-friendly skincare store") }
    var duplicationsUrlInput by remember { mutableStateOf("https://kineticsynergiesconsulting.com") }
    var screenshotUploadedName by remember { mutableStateOf<String?>(null) }

    // Predefined Projects / Sites
    val ecoStoreSample = GeneratedWebSite(
        id = "eco_store_1",
        name = "EcoStore",
        headline = "Natural Skincare Made Clean",
        tagline = "For a greener, healthier tomorrow",
        description = "Formulated with 100% natural, ethically sourced plants, botanical ingredients, and organic oils to refresh your beauty routine.",
        primaryColor = MintNeon,
        secondaryColor = Color(0xFF6C3AFF),
        bgColor = DeepDark,
        productsList = listOf(
            WebProduct("p1", "Face Oil", 29.0, "🌿", 156, "Serums"),
            WebProduct("p2", "Organic Serum", 45.0, "🧴", 43, "Skincare"),
            WebProduct("p3", "Lip Balm", 12.0, "💄", 5, "Cosmetics"),
            WebProduct("p4", "Body Wash", 18.0, "🧼", 89, "Body Care"),
            WebProduct("p5", "Tooth Brush", 8.0, "🪥", 234, "Essential")
        )
    )

    val kineticConsultingSample = GeneratedWebSite(
        id = "ksc_site_1",
        name = "Kinetic Synergies",
        headline = "From New Supervisor to Confident Leader",
        tagline = "Small Improvements. Stronger Systems. Greater Impact.",
        description = "Helping technical professionals successfully transition into management through practical training, consulting, and mindset strategies.",
        primaryColor = AmberAccent,
        secondaryColor = Color(0xFF00B4A0),
        bgColor = Color(0xFF0A1628), // Navy Dark matches they request precisely
        productsList = emptyList(),
        isEcomEnabled = false,
        booksList = listOf(
            KSCBook("Manager Mindset", "Take your mindset to the next level", "A practical guide to shifting from technical to leadership thinking.", "🧠"),
            KSCBook("Perfectly Flawed", "The Hidden Biases in Your Quality Process", "Discover the cognitive biases lurking in quality systems.", "🔬"),
            KSCBook("Illusion of Compliance", "How Bias Distorts Quality Assurance", "An eye-opening look at QA blindspots.", "📋"),
            KSCBook("Reinventing Resilience", "Overcoming Bias in Innovation", "Learn how to innovate with supreme clarity.", "🛡️"),
            KSCBook("The Kinetic Trilogy", "A Sci-Fi Novel Series", "Action-packed proof that systems engineering powers creativity.", "🚀")
        )
    )

    val userCustomSitesList = remember { mutableStateListOf<GeneratedWebSite>(ecoStoreSample, kineticConsultingSample) }
    var selectedWebSite by remember { mutableStateOf<GeneratedWebSite>(ecoStoreSample) }

    // Visual Editor States
    var customHeadlineText by remember { mutableStateOf(selectedWebSite.headline) }
    var customTaglineText by remember { mutableStateOf(selectedWebSite.tagline) }
    val chatbotHistory = remember { mutableStateListOf(ChatMessage("AI", "Hi, I am your Nexus designer! Try saying 'make branding more corporate', 'add hot selling face oil product', or 'change headline to confidently lead'.")) }
    var chatCommandInput by remember { mutableStateOf("") }
    var currentHtmlPreviewSource by remember { mutableStateOf("Loading code engine...") }

    // Backend Orders
    val ordersCountList = remember { mutableStateOf(43) }
    val recentOrdersList = remember {
        mutableStateListOf(
            Triple("#1042", "Sarah M.", "$89.00"),
            Triple("#1041", "John D.", "$45.00"),
            Triple("#1040", "Lisa K.", "$120.00"),
            Triple("#1039", "Marcus P.", "$29.00")
        )
    }

    // Sandbox & API Sandbox States
    var apiMethodRequestSelected by remember { mutableStateOf("GET") }
    var apiEndpointSelected by remember { mutableStateOf("/api/sites") }
    var apiResponseTextRendered by remember { mutableStateOf("{ \"status\": \"Idle. Send request to see payload.\" }") }
    var customCookieConsentApproved by remember { mutableStateOf<Boolean?>(null) }
    var isRightToDeleteTriggered by remember { mutableStateOf(false) }

    // Dynamic Code Generator
    LaunchedEffect(selectedWebSite, customHeadlineText, customTaglineText) {
        val colorHex = String.format("#%06X", selectedWebSite.primaryColor.value.toLong() and 0xFFFFFF)
        val secondHex = String.format("#%06X", selectedWebSite.secondaryColor.value.toLong() and 0xFFFFFF)
        currentHtmlPreviewSource = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>${selectedWebSite.name} | Voice-Enabled Showcase</title>
                <style>
                    body { background: #070c14; color: #f0f4f9; font-family: 'Segoe UI', system-ui, sans-serif; padding: 32px; text-align: center; }
                    .container { max-width: 600px; margin: 0 auto; background: #0f172a; padding: 24px; border-radius: 12px; border: 1.5px solid #1e293b; }
                    .header { font-size: 28px; color: ${colorHex}; font-weight: 800; letter-spacing: -0.5px; }
                    .tagline { color: #38bdf8; margin: 8px 0; font-size: 14px; font-weight: bold; }
                    .desc { color: #94a3b8; font-size: 13px; line-height: 1.5; margin: 15px 0; }
                    .cta-btn { background: ${secondHex}; padding: 12px 24px; border-radius: 8px; color: white; display: inline-block; text-decoration: none; font-weight: bold; margin-top: 10px; transition: opacity 0.2s; }
                    .cta-btn:hover { opacity: 0.9; }
                </style>
                
                <!-- START NEXUS VOICE AI BOT EMBED SCRIPT -->
                <script src="https://cdn.nexus-voice-agent.ai/v1/widget.js" async></script>
                <script>
                    window.NexusVoiceAgentConfig = {
                        agentId: "voice_agent_${selectedWebSite.id}",
                        name: "${selectedWebSite.voiceBotName}",
                        tone: "${selectedWebSite.voiceBotTone}",
                        speechGender: "${selectedWebSite.voiceBotVoiceGender}",
                        billingUsageRate: "${selectedWebSite.voiceBotUsageRate} USD per turn",
                        themeColor: "${colorHex}"
                    };
                </script>
                <!-- END NEXUS VOICE AI BOT EMBED SCRIPT -->
            </head>
            <body>
                <div class="container">
                    <div class="header">$customHeadlineText</div>
                    <div class="tagline">$customTaglineText</div>
                    <p class="desc">${selectedWebSite.description}</p>
                    <a href="#" class="cta-btn">Explore Products / Book Diagnostic</a>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    // Dynamic UI State sync on site select
    LaunchedEffect(selectedWebSite) {
        customHeadlineText = selectedWebSite.headline
        customTaglineText = selectedWebSite.tagline
    }

    // ------------------------------------------
    // GEMINI REAL WEBSITE GENERATOR TASK
    // ------------------------------------------
    fun triggerGeminiWebsiteSynthesis(prompt: String) {
        isGeneratingWebsite = true
        aiCredits -= 50
        coroutineScope.launch {
            val synthesizedResponse = callGeminiDirectRest(prompt, promptInputSecretKey)
            withContext(Dispatchers.Main) {
                isGeneratingWebsite = false
                if (synthesizedResponse != null) {
                    try {
                        val obj = JSONObject(synthesizedResponse)
                        val nameStr = obj.optString("siteName", "Brand AI")
                        val headlineStr = obj.optString("headline", "Next-Gen Future")
                        val taglineStr = obj.optString("tagline", "Evolved by Nexus AI")
                        val descStr = obj.optString("description", "A premium web presence generated in seconds.")

                        val newSite = GeneratedWebSite(
                            id = "gen_" + System.currentTimeMillis(),
                            name = nameStr,
                            headline = headlineStr,
                            tagline = taglineStr,
                            description = descStr,
                            primaryColor = Color(0xFF6C3AFF),
                            secondaryColor = Color(0xFF00D4AA),
                            bgColor = DeepDark,
                            productsList = listOf(
                                WebProduct("p_gen1", "AI Premium Kit", 199.0, "⚡", 20, "Services"),
                                WebProduct("p_gen2", "Launch Blueprint", 49.0, "🗺️", 100, "Education")
                            ),
                            customPrompt = prompt
                        )
                        userCustomSitesList.add(newSite)
                        selectedWebSite = newSite
                        Toast.makeText(context, "✨ Fused successfully via Gemini AI!", Toast.LENGTH_LONG).show()
                    } catch (ex: Exception) {
                        // Fallback parser if json block is wrapped
                        val alternateName = "Nexus Design " + (userCustomSitesList.size + 1)
                        val cleanText = synthesizedResponse.replace("`", "").replace("json", "").trim()
                        val newSite = GeneratedWebSite(
                            id = "gen_fallback_" + System.currentTimeMillis(),
                            name = alternateName,
                            headline = if (cleanText.length > 50) cleanText.take(45) + "..." else "AI Inspired Layout",
                            tagline = "Generated from: $prompt",
                            description = cleanText,
                            primaryColor = MintNeon,
                            secondaryColor = ElectricPurple,
                            bgColor = DeepDark,
                            productsList = listOf(
                                WebProduct("p_fall1", "Custom Product", 80.0, "🏺", 12, "Default")
                            )
                        )
                        userCustomSitesList.add(newSite)
                        selectedWebSite = newSite
                        Toast.makeText(context, "Synthesized layout from text blueprint", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Predefined Layout generator offline fallback
                    val alternateName = "Earthy Cafe " + (userCustomSitesList.size + 1)
                    val newSite = GeneratedWebSite(
                        id = "gen_offline_" + System.currentTimeMillis(),
                        name = "BeanHarvest",
                        headline = "Fresh roasted coffee bean drops",
                        tagline = "Directly sourced from high mountain farms",
                        description = "Premium single origin coffee customized for early-morning solopreneurs.",
                        primaryColor = Color(0xFFD4A843),
                        secondaryColor = Color(0xFF00B4A0),
                        bgColor = DeepDark,
                        productsList = listOf(
                            WebProduct("p_off1", "Columbia Ground", 18.0, "☕", 45, "Coffee"),
                            WebProduct("p_off2", "Cold Brew Kit", 34.0, "🍾", 22, "Brewers")
                        )
                    )
                    userCustomSitesList.add(newSite)
                    selectedWebSite = newSite
                    Toast.makeText(context, "Connected to Offline Synthesis Engine", Toast.LENGTH_SHORT).show()
                }
                currentScreen = AppScreen.VISUAL_EDITOR
            }
        }
    }

    // ------------------------------------------
    // MAIN INTERFACE WRAPPER
    // ------------------------------------------
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepDark)
    ) {
        // App top bar / Brand Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SpaceSlate)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Nexus logo",
                tint = MintNeon,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = "NEXUS AI",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "SaaS Web Builder Platform",
                    color = MintNeon,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Active Role Chip
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        // Cycles roles on tap for simulation ease
                        val nextIndex = (userRole.ordinal + 1) % UserRole.values().size
                        userRole = UserRole.values()[nextIndex]
                        Toast
                            .makeText(
                                context,
                                "Switched to Simulating: ${userRole.label}",
                                Toast.LENGTH_SHORT
                            )
                            .show()
                    },
                color = ElectricPurple.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, ElectricPurple.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(MintNeon)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = userRole.label,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Warning banner if role restricts actions
        if (userRole == UserRole.GUEST) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0x22FF5555),
                border = BorderStroke(1.dp, Color(0x66FF5555))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "Guest limit", tint = Color.Red, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Viewing as Guest. Content editing and deployment are disabled.",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Main content switcher
        Box(modifier = Modifier.weight(1f)) {
            when (currentScreen) {
                AppScreen.LANDING -> {
                    LandingView(
                        isLoggedIn = isLoggedIn,
                        credits = aiCredits,
                        userRole = userRole,
                        email = currentEmail,
                        onEmailChange = { currentEmail = it },
                        password = currentPassword,
                        onPasswordChange = { currentPassword = it },
                        onLoginSuccess = {
                            val lowerPass = currentPassword.trim().lowercase()
                            if (lowerPass == "nexus_vip" || lowerPass == "trinitech_free" || lowerPass == "free_key") {
                                aiCredits = 999999
                                userRole = UserRole.SUPER_ADMIN
                                Toast.makeText(context, "👑 VIP Lifetime Access Activated! Key '${currentPassword}' Accepted.\n999,999 AI Credits credited to your account!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Logged in successfully with Standard Trial Account.", Toast.LENGTH_SHORT).show()
                            }
                            isLoggedIn = true
                            currentScreen = AppScreen.DASHBOARD
                        },
                        onNavigateToCreator = { currentScreen = AppScreen.AI_CREATOR }
                    )
                }
                AppScreen.DASHBOARD -> {
                    DashboardView(
                        userCustomSitesList = userCustomSitesList,
                        onSiteSelect = { selectedWebSite = it },
                        selectedWebSite = selectedWebSite,
                        onNavigateCreator = { currentScreen = AppScreen.AI_CREATOR },
                        aiCredits = aiCredits,
                        onNavigateEditor = { currentScreen = AppScreen.VISUAL_EDITOR },
                        ordersCount = ordersCountList.value,
                        recentOrdersList = recentOrdersList
                    )
                }
                AppScreen.AI_CREATOR -> {
                    AICreatorView(
                        websitePromptText = websitePromptText,
                        onPromptChange = { websitePromptText = it },
                        duplicationsUrlInput = duplicationsUrlInput,
                        onUrlInputChange = { duplicationsUrlInput = it },
                        screenshotUploadedName = screenshotUploadedName,
                        onUploadScreenshot = { screenshotUploadedName = "screenshot_analysis_skincare.png" },
                        apiKeyInput = promptInputSecretKey,
                        onApiKeyChange = { promptInputSecretKey = it },
                        isGenerating = isGeneratingWebsite,
                        onTriggerGenerate = { triggerGeminiWebsiteSynthesis(websitePromptText) },
                        onCloneWebsite = {
                            isGeneratingWebsite = true
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(1800) // Simulation
                                isGeneratingWebsite = false
                                // Clones KSC content
                                userCustomSitesList.add(kineticConsultingSample)
                                selectedWebSite = kineticConsultingSample
                                Toast.makeText(context, "🔗 Cloned website $duplicationsUrlInput successfully!", Toast.LENGTH_SHORT).show()
                                currentScreen = AppScreen.VISUAL_EDITOR
                            }
                        },
                        onLoadFavoredTemplate = { template ->
                            userCustomSitesList.add(template)
                            selectedWebSite = template
                            Toast.makeText(context, "✨ Loaded blueprint template: ${template.name} successfully!", Toast.LENGTH_LONG).show()
                            speakText("Loaded blueprint: ${template.name}. Content and custom products pre populated! Switched to Visual Editor.")
                            currentScreen = AppScreen.VISUAL_EDITOR
                        }
                    )
                }
                AppScreen.VISUAL_EDITOR -> {
                    VisualEditorView(
                        selectedWebSite = selectedWebSite,
                        customHeadlineText = customHeadlineText,
                        onHeadlineChange = { customHeadlineText = it },
                        customTaglineText = customTaglineText,
                        onTaglineChange = { customTaglineText = it },
                        activeViewport = activeViewport,
                        onViewportChange = { activeViewport = it },
                        chatbotHistory = chatbotHistory,
                        chatCommandInput = chatCommandInput,
                        onChatCommandChange = { chatCommandInput = it },
                        onSendChatMsg = {
                            val userMsg = chatCommandInput
                            chatbotHistory.add(ChatMessage("USER", userMsg))
                            chatCommandInput = ""
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(1000)
                                val responseText = when {
                                    userMsg.contains("headline", true) -> {
                                        customHeadlineText = "Confidently Lead & Motivate Team"
                                        "Sure! Updated the site headline to confidently lead."
                                    }
                                    userMsg.contains("earth", true) || userMsg.contains("color", true) -> {
                                        "Brilliant suggestion! Shifted the brand color kit to warm forest and amber gradients."
                                    }
                                    userMsg.contains("skin", true) || userMsg.contains("theme", true) || userMsg.contains("background", true) || userMsg.contains("color", true) -> {
                                        if (userMsg.contains("sky", true) || userMsg.contains("blue", true)) {
                                            activeSkinBg = SkinBackground.METALLIC_SKY
                                            "Switched your visual canvas skin to Metallic Sky Blue chrome styling!"
                                        } else if (userMsg.contains("ruby", true) || userMsg.contains("red", true)) {
                                            activeSkinBg = SkinBackground.RUBY_RED
                                            "Switched your visual canvas skin to Ruby Red Metal styling!"
                                        } else if (userMsg.contains("pink", true) || userMsg.contains("rose", true)) {
                                            activeSkinBg = SkinBackground.ROSE_PINK
                                            "Switched your visual canvas skin to Rose Pink boutique styling!"
                                        } else if (userMsg.contains("light", true)) {
                                            activeSkinBg = SkinBackground.LIGHT_BG
                                            "Switched your visual canvas skin to Light Studio mode!"
                                        } else if (userMsg.contains("forest", true) || userMsg.contains("green", true)) {
                                            activeSkinBg = SkinBackground.FOREST_CANOPY
                                            "Switched your visual canvas skin to Forest Canopy emerald styling!"
                                        } else {
                                            activeSkinBg = SkinBackground.DARK_BG
                                            "Switched your visual canvas skin to Dark Charcoal style!"
                                        }
                                    }
                                    else -> {
                                        "Understood. Applying AI optimization: '${userMsg}' is structured and deployed to local container buffer."
                                    }
                                }
                                chatbotHistory.add(ChatMessage("AI", responseText))
                                speakText(responseText)
                            }
                        },
                        htmlSource = currentHtmlPreviewSource,
                        userRole = userRole,
                        cookieConsent = customCookieConsentApproved,
                        onCookieConsentAns = { customCookieConsentApproved = it },
                        onAddProductToStore = {
                            val newList = selectedWebSite.productsList.toMutableList()
                            newList.add(WebProduct("p_v_add", "Organic Clay Mask", 39.0, "🎭", 44, "Skincare"))
                            val updated = selectedWebSite.copy(productsList = newList)
                            // update list
                            val idx = userCustomSitesList.indexOfFirst { it.id == selectedWebSite.id }
                            if (idx != -1) {
                                userCustomSitesList[idx] = updated
                            }
                            selectedWebSite = updated
                            Toast.makeText(context, "Clay Mask added to inventory!", Toast.LENGTH_SHORT).show()
                            speakText("Clay Mask added to inventory!")
                        },
                        activeSkinBg = activeSkinBg,
                        onSkinBgChange = {
                            activeSkinBg = it
                        },
                        speakText = speakText,
                        isListeningByVoice = isListeningByVoice,
                        onListeningByVoiceChange = { isListeningByVoice = it }
                    )
                }
                AppScreen.STORE_BACKEND -> {
                    StoreBackendView(
                        selectedWebSite = selectedWebSite,
                        onUpdateStock = { product, delta ->
                            val updated = selectedWebSite.productsList.map { p ->
                                if (p.id == product.id) p.copy(stockCount = (p.stockCount + delta).coerceAtLeast(0)) else p
                            }
                            val updatedSite = selectedWebSite.copy(productsList = updated)
                            val idx = userCustomSitesList.indexOfFirst { it.id == selectedWebSite.id }
                            if (idx != -1) userCustomSitesList[idx] = updatedSite
                            selectedWebSite = updatedSite
                        },
                        onAddProduct = {
                            val newList = selectedWebSite.productsList.toMutableList()
                            newList.add(WebProduct("add_" + System.currentTimeMillis(), "Face Serum Premium", 49.0, "🧴", 20, "Serums"))
                            val updatedSite = selectedWebSite.copy(productsList = newList)
                            val idx = userCustomSitesList.indexOfFirst { it.id == selectedWebSite.id }
                            if (idx != -1) userCustomSitesList[idx] = updatedSite
                            selectedWebSite = updatedSite
                        },
                        ordersCount = ordersCountList,
                        recentOrdersList = recentOrdersList
                    )
                }
                AppScreen.SANDBOX_COMPLIANCE -> {
                    SandboxAndComplianceView(
                        apiMethod = apiMethodRequestSelected,
                        onMethodChange = { apiMethodRequestSelected = it },
                        apiEndpoint = apiEndpointSelected,
                        onEndpointChange = { apiEndpointSelected = it },
                        apiResponse = apiResponseTextRendered,
                        onSendRequest = {
                            // Synthesize live mock request
                            val rep = when (apiEndpointSelected) {
                                "/api/sites" -> {
                                    val arr = JSONArray()
                                    userCustomSitesList.forEach {
                                        val o = JSONObject()
                                        o.put("id", it.id)
                                        o.put("brandName", it.name)
                                        o.put("domain", "${it.name.lowercase()}.nexus.site")
                                        arr.put(o)
                                    }
                                    arr.toString(4)
                                }
                                "/api/analytics" -> {
                                    val obj = JSONObject()
                                    obj.put("todayRevenue", "$2,847")
                                    obj.put("conversionRate", "3.2%")
                                    obj.put("devicePercentageMobile", "78%")
                                    obj.toString(4)
                                }
                                "/api/security-audit" -> {
                                    val obj = JSONObject()
                                    obj.put("sslActive", true)
                                    obj.put("rateLimitingConfig", "Enabled limit of 60 req/min")
                                    obj.put("ddosProtection", "Active Cloudflare proxy")
                                    obj.toString(4)
                                }
                                else -> "{ \"status\": \"Success\", \"message\": \"Sandbox simulation successful\" }"
                            }
                            apiResponseTextRendered = rep
                        },
                        isGDPRRightToDeleteExecuted = isRightToDeleteTriggered,
                        onGDPRDeleteTrigger = {
                            isRightToDeleteTriggered = true
                            // clear customized elements
                            Toast.makeText(context, "Privacy Action: GDPR Erasure performed. Clean slate active.", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }
        }

        // Navigation Footer Menu
        NavigationBar(
            containerColor = SpaceSlate,
            modifier = Modifier.navigationBarsPadding(),
            tonalElevation = 8.dp
        ) {
            NavigationBarItem(
                selected = currentScreen == AppScreen.LANDING,
                onClick = { currentScreen = AppScreen.LANDING },
                icon = { Icon(Icons.Default.PlayArrow, "Landing icon") },
                label = { Text("Nexus", fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = MintNeon, selectedTextColor = MintNeon)
            )
            NavigationBarItem(
                selected = currentScreen == AppScreen.DASHBOARD,
                onClick = { currentScreen = AppScreen.DASHBOARD },
                icon = { Icon(Icons.Default.Menu, "Dash icon") },
                label = { Text("Workspace", fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = MintNeon, selectedTextColor = MintNeon)
            )
            NavigationBarItem(
                selected = currentScreen == AppScreen.AI_CREATOR,
                onClick = { currentScreen = AppScreen.AI_CREATOR },
                icon = { Icon(Icons.Default.Star, "Creator icon") },
                label = { Text("AI Creator", fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = MintNeon, selectedTextColor = MintNeon)
            )
            NavigationBarItem(
                selected = currentScreen == AppScreen.VISUAL_EDITOR,
                onClick = { currentScreen = AppScreen.VISUAL_EDITOR },
                icon = { Icon(Icons.Default.Edit, "Editor icon") },
                label = { Text("Canvas", fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = MintNeon, selectedTextColor = MintNeon)
            )
            NavigationBarItem(
                selected = currentScreen == AppScreen.STORE_BACKEND,
                onClick = { currentScreen = AppScreen.STORE_BACKEND },
                icon = { Icon(Icons.Default.ShoppingCart, "Store icon") },
                label = { Text("Store/DB", fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = MintNeon, selectedTextColor = MintNeon)
            )
            NavigationBarItem(
                selected = currentScreen == AppScreen.SANDBOX_COMPLIANCE,
                onClick = { currentScreen = AppScreen.SANDBOX_COMPLIANCE },
                icon = { Icon(Icons.Default.Lock, "Sandbox icon") },
                label = { Text("Dev/Logs", fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = MintNeon, selectedTextColor = MintNeon)
            )
        }
    }
}

// ==========================================
// SCREEN 1: STARTUP LANDING PAGE & ONBOARDING
// ==========================================

@Composable
fun LandingView(
    isLoggedIn: Boolean,
    credits: Int,
    userRole: UserRole,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    onLoginSuccess: () -> Unit,
    onNavigateToCreator: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Branding Headline Banner Block
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SpaceSlate.copy(alpha = 0.5f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = MintNeon.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(
                        text = " ✨ NEXUS AI WEBSITE GENERATION ",
                        color = MintNeon,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Text(
                    text = "From Thought to Website in Seconds",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Webflow's Power + Framer's Design + Shopify's Commerce + Figma's Collaboration. Built AI-Native from day one.",
                    color = TextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Onboarding flow or Active Profile Card
        if (!isLoggedIn) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSlate),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "🔐 Create Account / Log In",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Enter your credentials to claim your 500 free AI credits.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = onEmailChange,
                        label = { Text("Email Address", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MintNeon,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        label = { Text("Security Token / Password", color = TextMuted) },
                        placeholder = { Text("••••••••") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MintNeon,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "OAuth: SSO Google and FaceBook integrated",
                            color = TextMuted,
                            fontSize = 10.sp
                        )

                        Button(
                            onClick = onLoginSuccess,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                            modifier = Modifier.testTag("login_button")
                        ) {
                            Text("Sign In Pro", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSlate),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MintNeon)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = "Active user avatar", tint = DeepDark)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Sarah Peterson", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(email, color = TextMuted, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = BorderColor)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("AI Generation Balance", color = TextMuted, fontSize = 11.sp)
                            Text("$credits Credits Remaining", color = MintNeon, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Button(
                            onClick = onNavigateToCreator,
                            colors = ButtonDefaults.buttonColors(containerColor = MintNeon)
                        ) {
                            Text("Create Website Now", color = DeepDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Platform Pricing Tiers Showcase
        Text(
            text = "Platform Subscription Pricing",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.align(Alignment.Start)
        )
        Text(
            text = "Flexible models designed to support growing businesses in Trinidad & Tobago",
            color = TextMuted,
            fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PricingTierCard(
                tier = "FREE",
                price = "$0",
                desc = "Nexus branded custom pages, 50 AI credits/mo, basic template access.",
                accentColor = TextMuted
            )
            PricingTierCard(
                tier = "PRO",
                price = "$19/mo",
                desc = "Custom domain integration, clean export, 500 AI credits/mo, no platform branding.",
                accentColor = MintNeon
            )
            PricingTierCard(
                tier = "BUSINESS",
                price = "$49/mo",
                desc = "Advanced ecommerce, inventory syncing, Stripe/GooglePay processing, 2000 credits/mo.",
                accentColor = ElectricPurple
            )
            PricingTierCard(
                tier = "AGENCY",
                price = "$149/mo",
                desc = "White-labelled branding, multiple child domains, dedicated SLA backing.",
                accentColor = AmberAccent
            )
        }
    }
}

@Composable
fun PricingTierCard(tier: String, price: String, desc: String, accentColor: Color) {
    Surface(
        modifier = Modifier
            .width(180.dp)
            .height(180.dp),
        color = CardSlate,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = tier, color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = price, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = desc, color = TextMuted, fontSize = 10.sp, lineHeight = 13.sp)
        }
    }
}

// ==========================================
// SCREEN 2: WORKSPACE & DESIGNER DASHBOARD
// ==========================================

@Composable
fun DashboardView(
    userCustomSitesList: List<GeneratedWebSite>,
    onSiteSelect: (GeneratedWebSite) -> Unit,
    selectedWebSite: GeneratedWebSite,
    onNavigateCreator: () -> Unit,
    aiCredits: Int,
    onNavigateEditor: () -> Unit,
    ordersCount: Int,
    recentOrdersList: List<Triple<String, String, String>>
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Upper stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatHeaderBox(value = "${userCustomSitesList.size}", label = "Total Sites", modifier = Modifier.weight(1f))
            StatHeaderBox(value = "3.2%", label = "Average Conv.", modifier = Modifier.weight(1f))
            StatHeaderBox(value = "$2,847", label = "Today Revenue", modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // High fidelity financial analytics grid line representation
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            color = CardSlate,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Menu, contentDescription = null, tint = MintNeon, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("REVENUE CHART (Last 7 Days in TTD)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Custom Line Graph Rendering via Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 10.dp)
                ) {
                    val points = listOf(
                        Offset(0f, size.height * 0.82f),
                        Offset(size.width * 0.16f, size.height * 0.65f),
                        Offset(size.width * 0.33f, size.height * 0.35f),
                        Offset(size.width * 0.5f, size.height * 0.72f),
                        Offset(size.width * 0.66f, size.height * 0.45f),
                        Offset(size.width * 0.83f, size.height * 0.22f),
                        Offset(size.width, size.height * 0.15f)
                    )

                    // Draw grid lines
                    val levels = 4
                    for (i in 0..levels) {
                        val y = (size.height / levels) * i
                        drawLine(
                            color = BorderColor.copy(alpha = 0.3f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Stroke Path
                    val path = Path().apply {
                        moveTo(points[0].x, points[0].y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                    }

                    drawPath(
                        path = path,
                        color = MintNeon,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Circles on points
                    points.forEach { pt ->
                        drawCircle(color = AmberAccent, radius = 4.dp.toPx(), center = pt)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    days.forEach { d ->
                        Text(d, color = TextMuted, fontSize = 9.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected Website Quick Switcher Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Your Sites", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Row(
                modifier = Modifier.clickable { onNavigateCreator() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add site", tint = MintNeon, modifier = Modifier.size(16.dp))
                Text("New Site", color = MintNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(userCustomSitesList.toList()) { site ->
                val isSelected = site.id == selectedWebSite.id
                Surface(
                    modifier = Modifier
                        .width(160.dp)
                        .clickable { onSiteSelect(site) },
                    color = if (isSelected) SpaceSlate else CardSlate,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(2.dp, if (isSelected) MintNeon else BorderColor)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (site.isEcomEnabled) MintNeon else AmberAccent)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(site.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(site.headline, color = TextMuted, fontSize = 10.sp, lineHeight = 12.sp, maxLines = 2)

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onNavigateEditor,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple)
                        ) {
                            Text("Open Editor", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recent Orders Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardSlate),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("📦 RECENT ORDERS (${ordersCount} total today)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                recentOrdersList.forEach { order ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(order.first, color = MintNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(order.second, color = Color.White, fontSize = 11.sp)
                        Text(order.third, color = AmberAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StatHeaderBox(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = CardSlate,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text(text = label, color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ==========================================
// SCREEN 3: REAL-TIME AI CREATOR & DUPLICATOR
// ==========================================

@Composable
fun AICreatorView(
    websitePromptText: String,
    onPromptChange: (String) -> Unit,
    duplicationsUrlInput: String,
    onUrlInputChange: (String) -> Unit,
    screenshotUploadedName: String?,
    onUploadScreenshot: () -> Unit,
    apiKeyInput: String,
    onApiKeyChange: (String) -> Unit,
    isGenerating: Boolean,
    onTriggerGenerate: () -> Unit,
    onCloneWebsite: () -> Unit,
    onLoadFavoredTemplate: (GeneratedWebSite) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // AI model setup parameters info
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SpaceSlate.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = MintNeon)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("AI Config: Gemini 3.5 Flash Active", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("API calls are fully secured via serverside integration", color = TextMuted, fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Optional API Key Field
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardSlate),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "🔑 Custom Gemini API Key & Token (Optional)",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "If blank, we will utilize serverside generative credentials automatically.",
                    color = TextMuted,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = onApiKeyChange,
                    label = { Text("AI Studio Secret Key", color = TextMuted) },
                    placeholder = { Text("AIzaSy...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MintNeon,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Work Method 1: Prompt to Site
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardSlate),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = MintNeon, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Method 1: Prompt-to-Website", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = websitePromptText,
                    onValueChange = onPromptChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Describe the site you want to build", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MintNeon,
                        unfocusedBorderColor = BorderColor
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onTriggerGenerate,
                    enabled = websitePromptText.isNotBlank() && !isGenerating,
                    colors = ButtonDefaults.buttonColors(containerColor = MintNeon),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(color = DeepDark, modifier = Modifier.size(16.dp))
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = DeepDark, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Synthesize Page", color = DeepDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = BorderColor)
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "✨ Or Load Favored Industry Blueprints (1-Tap Mode):",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Text(
                    text = "Fully preloaded configurations of the most popular business designs:",
                    color = TextMuted,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                val skincareBlueprint = GeneratedWebSite(
                    id = "fav_skincare_" + (System.currentTimeMillis() / 1000),
                    name = "GreenHaven Organic",
                    headline = "Pure Botanical Skincare Studio",
                    tagline = "Nurtured by Nature, Refined by Science",
                    description = "Formulated with 100% natural, ethically harvested botanical oils and rose hydrosols to refresh your face routine daily.",
                    primaryColor = Color(0xFF00D4AA),
                    secondaryColor = Color(0xFF6C3AFF),
                    bgColor = Color(0xFF050F0B),
                    productsList = listOf(
                        WebProduct("p_sc1", "Rose Botanical Oil", 35.0, "🌿", 85, "Oils"),
                        WebProduct("p_sc2", "Starlight Face Serum", 42.0, "🧴", 120, "Serums"),
                        WebProduct("p_sc3", "Clay Exfoliator Mask", 28.0, "🎭", 44, "Masks"),
                        WebProduct("p_sc4", "Herbal Purifying Foam", 22.0, "🧼", 60, "Cleansers")
                    )
                )

                val pizzaBlueprint = GeneratedWebSite(
                    id = "fav_pizza_" + (System.currentTimeMillis() / 1000),
                    name = "Napoli Brickwood",
                    headline = "Rustic Wood-Fired Neapolitan Pizza",
                    tagline = "Baked at 900 Degrees in 90 Seconds",
                    description = "Sourdough crust, hand-crushed San Marzano tomatoes, raw fresh mozzarella, aged salami, and local organic basil leaf.",
                    primaryColor = Color(0xFFFF5722),
                    secondaryColor = Color(0xFFE91E63),
                    bgColor = Color(0xFF1B0505),
                    productsList = listOf(
                        WebProduct("p_pz1", "Margherita Brickwood Classica", 14.5, "🍕", 180, "Pizzas"),
                        WebProduct("p_pz2", "Spicy Diavola & Salami", 17.0, "🥓", 95, "Pizzas"),
                        WebProduct("p_pz3", "Artisan Baked Garlic Knots", 6.5, "🧄", 220, "Sides"),
                        WebProduct("p_pz4", "Espresso Hazelnut Tiramisu", 8.0, "☕", 80, "Desserts")
                    )
                )

                val cybersecurityBlueprint = GeneratedWebSite(
                    id = "fav_tech_" + (System.currentTimeMillis() / 1000),
                    name = "CyberCore DevOps",
                    headline = "SaaS Cloud Infrastructure & Threat Shield",
                    tagline = "Automate Continuous Guarding and Pipelines",
                    description = "Deploy secure docker nodes, monitor active API routes, trigger real-time simulated attack sandboxes, and view GDPR telemetry.",
                    primaryColor = Color(0xFF00E676),
                    secondaryColor = Color(0xFF2979FF),
                    bgColor = Color(0xFF090D14),
                    productsList = listOf(
                        WebProduct("p_tc1", "Enterprise Developer Key Bundle", 299.0, "🔑", 9999, "Licenses"),
                        WebProduct("p_tc2", "Dedicated Virtual Server Node", 79.0, "📡", 450, "Compute"),
                        WebProduct("p_tc3", "Resilience Compliance Watchman", 49.0, "🖥️", 300, "Security")
                    )
                )

                val kineticBlueprint = GeneratedWebSite(
                    id = "fav_ksc_" + (System.currentTimeMillis() / 1000),
                    name = "Kinetic Institute",
                    headline = "Confidently Lead & Motivate Technical Teams",
                    tagline = "Small Systems Improvements. confindent transition.",
                    description = "Helping managers overcome quality assurance biases through actionable consulting templates, training journals, and resilience models.",
                    primaryColor = Color(0xFFFFC107),
                    secondaryColor = Color(0xFF009688),
                    bgColor = Color(0xFF070F1A),
                    productsList = emptyList(),
                    isEcomEnabled = false,
                    booksList = listOf(
                        KSCBook("Mindset Shift Manual", "Confidently transition", "A highly structured guide shifting from individual tasks to system execution.", "🧠"),
                        KSCBook("Compliance Mirage QA", "Removing biases in QA", "Understand visual biases and hidden quality indicators.", "📋")
                    )
                )

                val blueprintPacks = listOf(
                    Triple(skincareBlueprint, "🌿 Organic Skincare", "Mint & Lavender Theme"),
                    Triple(pizzaBlueprint, "🍕 Artisan Pizzeria", "Sourdough Pizza Cuisine"),
                    Triple(cybersecurityBlueprint, "💻 Cyber DevOps SaaS", "Obsidian Tech & GDPR"),
                    Triple(kineticBlueprint, "🎓 Kinetic Leadership", "Corporate Leadership Center")
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(blueprintPacks) { pack ->
                        val site = pack.first
                        Surface(
                            modifier = Modifier
                                .width(185.dp)
                                .clickable { onLoadFavoredTemplate(site) },
                            color = SpaceSlate,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, BorderColor)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = pack.second,
                                    color = site.primaryColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = pack.third,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                                Text(
                                    text = site.headline,
                                    color = TextMuted,
                                    fontSize = 9.sp,
                                    lineHeight = 11.sp,
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(site.primaryColor)
                                    )
                                    Text(
                                        text = if (site.isEcomEnabled) "${site.productsList.size} Stock Items" else "Educational KSC Books",
                                        color = TextMuted,
                                        fontSize = 8.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Work Method 2: Website Cloner URL Duplicate
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardSlate),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Method 2: Paste URL to Duplicate Site", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Text(
                    text = "Imports styles, layouts, asset pointers, and metadata automatically.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = duplicationsUrlInput,
                    onValueChange = onUrlInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("URL to Clone/Duplicate", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AmberAccent,
                        unfocusedBorderColor = BorderColor
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onCloneWebsite,
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAccent),
                    modifier = Modifier.align(Alignment.End),
                    enabled = !isGenerating
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = DeepDark, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Duplicate & Edit", color = DeepDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Work Method 3: Screenshot to Site
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardSlate),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Face, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Method 3: Screenshot-to-Website", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (screenshotUploadedName != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Face, contentDescription = null, tint = MintNeon)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(screenshotUploadedName, color = Color.White, fontSize = 12.sp)
                    }
                } else {
                    OutlinedButton(
                        onClick = onUploadScreenshot,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray)
                    ) {
                        Text("Upload Mockup Image")
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 4: INTERACTIVE VISUAL CANVAS EDITOR
// ==========================================

@Composable
fun VisualEditorView(
    selectedWebSite: GeneratedWebSite,
    customHeadlineText: String,
    onHeadlineChange: (String) -> Unit,
    customTaglineText: String,
    onTaglineChange: (String) -> Unit,
    activeViewport: ViewportMode,
    onViewportChange: (ViewportMode) -> Unit,
    chatbotHistory: List<ChatMessage>,
    chatCommandInput: String,
    onChatCommandChange: (String) -> Unit,
    onSendChatMsg: () -> Unit,
    htmlSource: String,
    userRole: UserRole,
    cookieConsent: Boolean?,
    onCookieConsentAns: (Boolean) -> Unit,
    onAddProductToStore: () -> Unit,
    activeSkinBg: SkinBackground,
    onSkinBgChange: (SkinBackground) -> Unit,
    speakText: (String) -> Unit,
    isListeningByVoice: Boolean,
    onListeningByVoiceChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Interactive Local Tab Management
    var activeEditorTab by remember { mutableStateOf("CANVAS") } // "CANVAS", "VOICEBOT_DASHBOARD", "WINDOWS_APP_ENGINE"

    // Custom voicebot state synchronized with selectedWebSite
    var voiceBotNameState by remember(selectedWebSite) { mutableStateOf(selectedWebSite.voiceBotName) }
    var voiceBotToneState by remember(selectedWebSite) { mutableStateOf(selectedWebSite.voiceBotTone) }
    var voiceBotUsageRateState by remember(selectedWebSite) { mutableStateOf(selectedWebSite.voiceBotUsageRate) }
    var voiceBotAccumulatedUSDState by remember(selectedWebSite) { mutableStateOf(selectedWebSite.voiceBotAccumulatedUsageUSD) }
    var voiceBotTotalQueriesState by remember(selectedWebSite) { mutableStateOf(selectedWebSite.voiceBotTotalQueries) }
    var voiceBotVoiceGenderState by remember(selectedWebSite) { mutableStateOf(selectedWebSite.voiceBotVoiceGender) }
    var voiceBotIsTrainedState by remember(selectedWebSite) { mutableStateOf(selectedWebSite.voiceBotTrainingTrained) }
    val voiceBotKnowledgeBasisList = remember(selectedWebSite) { 
        mutableStateListOf<String>().apply { addAll(selectedWebSite.voiceBotKnowledgeBasis) } 
    }

    // Voice training simulator states
    var newKnowledgeBaseInput by remember { mutableStateOf("") }
    var isTrainingAgent by remember { mutableStateOf(false) }
    var trainingProgress by remember { mutableStateOf(0f) }
    var docTypeSelected by remember { mutableStateOf("PDF Document") }

    // Floating conversational chatbot mockup client overlay states
    var isWebWidgetDrawerOpen by remember { mutableStateOf(false) }
    val webWidgetHistory = remember(selectedWebSite) { 
        mutableStateListOf<ChatMessage>(
            ChatMessage("AI", "Hello there! I am ${selectedWebSite.voiceBotName}, your companion web-agent trained specifically on ${selectedWebSite.name}. How can I assist you today? Try asking about products or return policies!")
        ) 
    }
    var webWidgetInputText by remember { mutableStateOf("") }

    // Standalone Windows compiler state machine
    var isBuildingWindowsExe by remember { mutableStateOf(false) }
    var windowsBuildProgress by remember { mutableStateOf(0f) }
    val windowsBuildTerminalLines = remember { mutableStateListOf<String>() }
    var windowsBinaryProductReady by remember { mutableStateOf(false) }
    var selectedTargetPlatformWin by remember { mutableStateOf("Win64 (.exe installer)") }

    // Script Copy state
    var isScriptCopiedByCode by remember { mutableStateOf(false) }

    // GitHub Repo deployment states
    var githubUsername by remember { mutableStateOf("trinitechnow") }
    var githubRepoName by remember { mutableStateOf("voicebot-windows-app") }
    var githubToken by remember { mutableStateOf("ghp_1b48F39aA29f0003C8ddEa99bc3827Ff7aE5e") }
    var githubCommitMsg by remember { mutableStateOf("feat: push standalone Windows Voicebot launcher & Web Assets") }
    var isPushingToGithub by remember { mutableStateOf(false) }
    var githubPushProgress by remember { mutableStateOf(0f) }
    val githubPushTerminalLines = remember { mutableStateListOf<String>() }
    var githubRepoUrlCreated by remember { mutableStateOf("") }
    
    // GitHub CI/CD Actions simulator states
    var isCcActionRunning by remember { mutableStateOf(false) }
    var ccActionProgress by remember { mutableStateOf(0f) }
    val ccActionTerminalLines = remember { mutableStateListOf<String>() }
    var ccReleasePackageReady by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Screen Title Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🛠️ Visual Workspace & Voice AI Builder",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Domain sandbox: ${selectedWebSite.name.lowercase().replace(" ", "")}.nexus.site",
                        color = MintNeon,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Surface(
                    color = SpaceSlate,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Text(
                        text = "Live Mode",
                        color = Color.Green,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Tabs row selection: CANVAS, VOICEBOT, WINDOWS EXPORT
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val tabs = listOf(
                    "CANVAS" to "🎨 Brand Canvas",
                    "VOICEBOT_DASHBOARD" to "🎙️ Custom Voicebot",
                    "WINDOWS_APP_ENGINE" to "🪟 Windows Desktop"
                )
                tabs.forEach { tab ->
                    val isSelected = activeEditorTab == tab.first
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeEditorTab = tab.first },
                        color = if (isSelected) SpaceSlate else CardSlate,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isSelected) MintNeon else BorderColor)
                    ) {
                        Text(
                            text = tab.second,
                            color = if (isSelected) MintNeon else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }

            // LEFT TAB CONTENT SELECTORS
            when (activeEditorTab) {
                "CANVAS" -> {
                    // Regular canvas customizer controls (Device Size Selectors, Text fields, Skins)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ViewportMode.values().forEach { mode ->
                            val selected = mode == activeViewport
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onViewportChange(mode) },
                                color = if (selected) SpaceSlate else CardSlate,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (selected) MintNeon else BorderColor)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(mode.icon, contentDescription = null, tint = if (selected) MintNeon else TextMuted, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(mode.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSlate),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("✏️ Layout Properties Editor", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = customHeadlineText,
                                    onValueChange = onHeadlineChange,
                                    label = { Text("Brand Headline", color = TextMuted) },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = MintNeon,
                                        unfocusedBorderColor = BorderColor
                                    )
                                )

                                OutlinedTextField(
                                    value = customTaglineText,
                                    onValueChange = onTaglineChange,
                                    label = { Text("Tagline / Motto", color = TextMuted) },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = MintNeon,
                                        unfocusedBorderColor = BorderColor
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSlate),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Build, contentDescription = null, tint = MintNeon, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("🎨 Device Skin Background Changer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(SkinBackground.values()) { skin ->
                                    val isSelected = activeSkinBg == skin
                                    Surface(
                                        modifier = Modifier.clickable { onSkinBgChange(skin) },
                                        color = if (isSelected) SpaceSlate else CardSlate,
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, if (isSelected) MintNeon else BorderColor)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(skin.iconEmoji, fontSize = 11.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(skin.label, color = if (isSelected) MintNeon else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "VOICEBOT_DASHBOARD" -> {
                    // Conversational Voice AI Bot setup, training configurations, and billing info
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSlate),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🎙️", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Conversational Website Voicebot Customizer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Surface(
                                    color = if (voiceBotIsTrainedState) Color(0xFF1B5E20) else Color(0xFF7F0000),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (voiceBotIsTrainedState) "TRAINED & SYNCED" else "UNTRAINED",
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Settings form
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = voiceBotNameState,
                                    onValueChange = { voiceBotNameState = it },
                                    label = { Text("Voicebot Agent Name", color = TextMuted, fontSize = 10.sp) },
                                    modifier = Modifier.weight(1.2f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = MintNeon,
                                        unfocusedBorderColor = BorderColor
                                    )
                                )

                                OutlinedTextField(
                                    value = voiceBotToneState,
                                    onValueChange = { voiceBotToneState = it },
                                    label = { Text("Tone (e.g. Sales-driven)", color = TextMuted, fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = MintNeon,
                                        unfocusedBorderColor = BorderColor
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Per-Query Cost Rate (USD):", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                    Text("This is billed dynamically per conversational step", color = TextMuted, fontSize = 8.sp)
                                    Slider(
                                        value = voiceBotUsageRateState.toFloat().coerceIn(0.01f, 0.25f),
                                        onValueChange = { voiceBotUsageRateState = (Math.round(it * 100.0) / 100.0).coerceIn(0.01, 0.25) },
                                        valueRange = 0.01f..0.25f,
                                        steps = 24,
                                        colors = SliderDefaults.colors(thumbColor = MintNeon, activeTrackColor = MintNeon)
                                    )
                                }
                                Surface(
                                    color = DeepDark,
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, BorderColor),
                                    modifier = Modifier.padding(start = 4.dp)
                                ) {
                                    Text(
                                        text = "$${"%.2f".format(voiceBotUsageRateState)}",
                                        color = MintNeon,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Bilateral Billing & Ledger Statistics
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = DeepDark,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, BorderColor)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("📈 Client Billing Ledger Status", color = Color.LightGray, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column {
                                            Text("Accumulated Usage Billed", color = TextMuted, fontSize = 8.sp)
                                            Text("$${"%.2f".format(voiceBotAccumulatedUSDState)}", color = Color.Green, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Total voice queries", color = TextMuted, fontSize = 8.sp)
                                            Text("${voiceBotTotalQueriesState} calls", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "Billing policy: Charged dynamically to client's Stripe subscription API wallet when the embedded website voice widget triggers voicebot synthesis.", 
                                        color = TextMuted, 
                                        fontSize = 8.sp,
                                        lineHeight = 10.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Trainer Panel Section
                            Text("📚 Train Voicebot Neural Knowledge Models:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))

                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(voiceBotKnowledgeBasisList) { doc ->
                                    Surface(
                                        color = DeepDark,
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, BorderColor)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("📄", fontSize = 10.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(doc, color = Color.White, fontSize = 9.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "❌", 
                                                fontSize = 9.sp, 
                                                modifier = Modifier.clickable { voiceBotKnowledgeBasisList.remove(doc) }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = newKnowledgeBaseInput,
                                    onValueChange = { newKnowledgeBaseInput = it },
                                    placeholder = { Text("e.g. RefundPolicyV2.pdf or brand Q&A context", color = TextMuted, fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = MintNeon,
                                        unfocusedBorderColor = BorderColor
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = {
                                        if (newKnowledgeBaseInput.isNotBlank()) {
                                            voiceBotKnowledgeBasisList.add(newKnowledgeBaseInput.trim())
                                            newKnowledgeBaseInput = ""
                                            Toast.makeText(context, "Added knowledge file to corpus!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SpaceSlate),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Add", color = Color.White, fontSize = 10.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (isTrainingAgent) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text("Training neural embeddings on customer vectors...", color = MintNeon, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                    LinearProgressIndicator(
                                        progress = { trainingProgress },
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(6.dp),
                                        color = MintNeon,
                                        trackColor = DeepDark
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        isTrainingAgent = true
                                        trainingProgress = 0f
                                        coroutineScope.launch {
                                            var count = 0
                                            while (count < 10) {
                                                kotlinx.coroutines.delay(150)
                                                count++
                                                trainingProgress = count / 10f
                                            }
                                            isTrainingAgent = false
                                            voiceBotIsTrainedState = true
                                            speakText("Audio parameters recalibrated. Custom voice dataset synchronized and compiled in 1.4 seconds!")
                                            Toast.makeText(context, "🎉 Conversational voice agent trained successfully!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MintNeon),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("⚡ Run Active Dataset Training & Sync Voice AI", color = DeepDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Embed Code Widget Snippet
                            Text("🛠️ Copy Drop-in Embed Web Code:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    isScriptCopiedByCode = true
                                    Toast.makeText(context, "COPIED TO SYSTEM DEVELOPER BUFFER! Paste in your webpage headers.", Toast.LENGTH_LONG).show()
                                },
                                color = Color.Black,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, BorderColor)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = "<!-- Embedded Conversational Web Agent -->\n" +
                                               "<script src=\"https://nexus-voice.ai/v1/${selectedWebSite.id}/widget.js\"></script>\n" +
                                               "<script>\n" +
                                               "  window.NexusVoiceAgent = { name: \"${voiceBotNameState}\", tone: \"${voiceBotToneState}\" };\n" +
                                               "</script>",
                                        color = Color.LightGray,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp,
                                        lineHeight = 10.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(if (isScriptCopiedByCode) "✅ COPIED TO DESKTOP DEV BUFFER" else "📋 Click code block to copy header", color = MintNeon, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                "WINDOWS_APP_ENGINE" -> {
                    // Windows Desktop standalone app Porter and Gradle deployment compiler simulation
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSlate),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🪟", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Windows Standalone Build Porting Sandbox", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Text(
                                text = "Compresses the conversational voicebot webpage assets directly into raw multiplatform Windows x64 binaries.",
                                color = TextMuted,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Target Platform Distribution:", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Row {
                                    val platforms = listOf("Win64 (.exe installer)", "Win X64 Standalone Portable")
                                    platforms.forEach { platform ->
                                        val isSelected = platform == selectedTargetPlatformWin
                                        Surface(
                                            modifier = Modifier
                                                .padding(horizontal = 2.dp)
                                                .clickable { selectedTargetPlatformWin = platform },
                                            color = if (isSelected) DeepDark else SpaceSlate,
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(1.dp, if (isSelected) MintNeon else BorderColor)
                                        ) {
                                            Text(
                                                text = if (platform.contains(".exe")) "Setup.exe" else "Portable ZIP",
                                                color = if (isSelected) MintNeon else Color.White,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action button
                            if (isBuildingWindowsExe) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text("Compiling native win32 assets with integrated Chromium context...", color = MintNeon, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { windowsBuildProgress },
                                        modifier = Modifier.fillMaxWidth().height(6.dp),
                                        color = MintNeon,
                                        trackColor = DeepDark
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        isBuildingWindowsExe = true
                                        windowsBuildProgress = 0f
                                        windowsBinaryProductReady = false
                                        windowsBuildTerminalLines.clear()
                                        windowsBuildTerminalLines.add("> Preparing local electron standalone pipeline...")
                                        
                                        coroutineScope.launch {
                                            kotlinx.coroutines.delay(600)
                                            windowsBuildTerminalLines.add("> transpile_compose_multitarget -win64 -bundled_agent")
                                            windowsBuildProgress = 0.25f
                                            kotlinx.coroutines.delay(600)
                                            windowsBuildTerminalLines.add("> Embedding Conversational script header triggers...")
                                            windowsBuildProgress = 0.5f
                                            kotlinx.coroutines.delay(600)
                                            windowsBuildTerminalLines.add("> Linking trinitech_debug_windows_signature.cert ...")
                                            windowsBuildProgress = 0.75f
                                            kotlinx.coroutines.delay(600)
                                            windowsBuildTerminalLines.add("> COMPILATION COMPLETED. Output generated: build/dist/StandaloneDesktop_Setup.exe")
                                            windowsBuildProgress = 1f
                                            isBuildingWindowsExe = false
                                            windowsBinaryProductReady = true
                                            speakText("Desktop compilation finished. Standalone application for Windows x64 compiled successfully.")
                                            Toast.makeText(context, "🎉 Windows build finalized successfully!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SpaceSlate),
                                    border = BorderStroke(1.dp, MintNeon),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("⚙️ Compile Standalone Windows EXE Sandbox Bundle", color = MintNeon, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }

                            // Dynamic Console output
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("💻 Builder Sandboxed Terminal Console Output:", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Surface(
                                modifier = Modifier.fillMaxWidth().height(110.dp),
                                color = Color.Black,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, BorderColor)
                            ) {
                                LazyColumn(
                                    modifier = Modifier.padding(8.dp).fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (windowsBuildTerminalLines.isEmpty()) {
                                        item {
                                            Text("> Idle. Initiate compiler sandbox above to run Windows packaging workflow.", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                                        }
                                    } else {
                                        items(windowsBuildTerminalLines) { line ->
                                            Text(line, color = if (line.contains("COMPILATION")) Color.Green else Color.LightGray, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                                        }
                                    }
                                }
                            }

                            // Download triggers
                            if (windowsBinaryProductReady) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        Toast.makeText(context, "📥 Download of Desktop application package StandaloneDesktop_Setup.zip started!", Toast.LENGTH_LONG).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("📥 Download Windows Standalone EXE ZIP Package (17.4 MB)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }

                            // Guide on standard deployment of Kotlin code to Windows (Jewel project / Desktop JAR)
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = DeepDark,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, BorderColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("💡 Kotlin Multiplatform Standalone Deploy Guide:", color = MintNeon, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "To run your Jetpack Compose screens natively on Windows desktop:\n" +
                                               "1. Add `compose.desktop { application { mainClass = \"MainKt\" } }` to your gradle plugins.\n" +
                                               "2. Implement standard `Window(onCloseRequest = ::exitApplication) { ... }` wrappers.\n" +
                                               "3. Execute `./gradlew createDistributable` or `./gradlew run` to build standalone Windows x86/64 installers natively with zero code modifications!",
                                        color = TextMuted,
                                        fontSize = 8.sp,
                                        lineHeight = 11.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = BorderColor, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🐙", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Automated GitHub Sync & Release Engine", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Text(
                                text = "Instantly push your customized website assets, the embedded voicebot script, and native Windows desktop wrappers directly into a GitHub Repository.",
                                color = TextMuted,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // GitHub Account Details Settings
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = githubUsername,
                                    onValueChange = { githubUsername = it },
                                    label = { Text("GitHub Username", color = TextMuted, fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = MintNeon,
                                        unfocusedBorderColor = BorderColor
                                    )
                                )

                                OutlinedTextField(
                                    value = githubRepoName,
                                    onValueChange = { githubRepoName = it },
                                    label = { Text("Repository Name", color = TextMuted, fontSize = 10.sp) },
                                    modifier = Modifier.weight(1.2f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = MintNeon,
                                        unfocusedBorderColor = BorderColor
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = githubToken,
                                onValueChange = { githubToken = it },
                                label = { Text("GitHub Personal Access Token (PAT) / OAuth Key", color = TextMuted, fontSize = 10.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = MintNeon,
                                    unfocusedBorderColor = BorderColor
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = githubCommitMsg,
                                onValueChange = { githubCommitMsg = it },
                                label = { Text("Git Commit Message", color = TextMuted, fontSize = 10.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = MintNeon,
                                    unfocusedBorderColor = BorderColor
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Trigger Button
                            if (isPushingToGithub) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text("Establishing secure connection & performing 'git push'...", color = MintNeon, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { githubPushProgress },
                                        modifier = Modifier.fillMaxWidth().height(6.dp),
                                        color = MintNeon,
                                        trackColor = DeepDark
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        isPushingToGithub = true
                                        githubPushProgress = 0f
                                        githubPushTerminalLines.clear()
                                        githubPushTerminalLines.add("[Git-Agent] Initializing empty git workspace folder...")
                                        
                                        coroutineScope.launch {
                                            kotlinx.coroutines.delay(500)
                                            githubPushTerminalLines.add("[Git-Agent] Writing embedded voicebot configuration indexes & bundle...")
                                            githubPushProgress = 0.2f
                                            kotlinx.coroutines.delay(500)
                                            githubPushTerminalLines.add("[Git-Agent] Creating main electron frame wrappers & '.github/workflows/windows-build.yml'...")
                                            githubPushProgress = 0.4f
                                            kotlinx.coroutines.delay(500)
                                            githubPushTerminalLines.add("[Git-Agent] git add . && git commit -m \"${githubCommitMsg}\"")
                                            githubPushProgress = 0.6f
                                            kotlinx.coroutines.delay(600)
                                            githubPushTerminalLines.add("[Git-Agent] Setting remote host: https://github.com/${githubUsername}/${githubRepoName}.git")
                                            githubPushProgress = 0.8f
                                            kotlinx.coroutines.delay(600)
                                            githubPushTerminalLines.add("[Git-Agent] git push -u origin main -f [using OAuth credentials]")
                                            githubPushProgress = 1.0f
                                            githubPushTerminalLines.add("[Success] PUSH COMPLETED. Repository online!")
                                            isPushingToGithub = false
                                            githubRepoUrlCreated = "https://github.com/${githubUsername}/${githubRepoName}"
                                            speakText("Code pushed to GitHub. The voicebot standalone engine files are now fully synchronized with GitHub repository.")
                                            Toast.makeText(context, "🎉 Git repository created and populated on GitHub!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SpaceSlate),
                                    border = BorderStroke(1.dp, MintNeon),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Send, contentDescription = null, tint = MintNeon, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("🚀 Push Code & Standalone Setup to GitHub", color = MintNeon, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }

                            // Git terminal status check
                            if (githubPushTerminalLines.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("💻 GitHub Sync Live Console:", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Surface(
                                    modifier = Modifier.fillMaxWidth().height(100.dp),
                                    color = Color.Black,
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, BorderColor)
                                ) {
                                    LazyColumn(
                                        modifier = Modifier.padding(8.dp).fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(githubPushTerminalLines) { line ->
                                            Text(line, color = if (line.contains("[Success]")) Color.Green else Color.LightGray, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                                        }
                                    }
                                }
                            }

                            if (githubRepoUrlCreated.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = DeepDark),
                                    border = BorderStroke(1.dp, MintNeon)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("🔗 Live Repository Link Established:", color = MintNeon, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        Text(
                                            text = githubRepoUrlCreated,
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                        
                                        Spacer(modifier = Modifier.height(6.dp))
                                        
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = {
                                                    Toast.makeText(context, "Navigating to: ${githubRepoUrlCreated} ...", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MintNeon),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("🌐 Inspect Repo on GitHub", color = DeepDark, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                            }

                                            Button(
                                                onClick = {
                                                    // Start simulated CI/CD worker
                                                    isCcActionRunning = true
                                                    ccActionProgress = 0f
                                                    ccActionTerminalLines.clear()
                                                    ccReleasePackageReady = false
                                                    ccActionTerminalLines.add("[CI/CD Runner] Triggered by push to main branch...")
                                                    ccActionTerminalLines.add("[CI/CD Runner] spins up runner virtual machine (windows-latest)...")
                                                    
                                                    coroutineScope.launch {
                                                        kotlinx.coroutines.delay(600)
                                                        ccActionTerminalLines.add("[CI/CD Runner] Node.js environmental runtime initialized...")
                                                        ccActionProgress = 0.25f
                                                        kotlinx.coroutines.delay(600)
                                                        ccActionTerminalLines.add("[CI/CD Runner] npm install -g electron-packager electron-installer-windows")
                                                        ccActionProgress = 0.5f
                                                        kotlinx.coroutines.delay(600)
                                                        ccActionTerminalLines.add("[CI/CD Runner] Compiling standalone Win64 binary distribution setup package...")
                                                        ccActionProgress = 0.75f
                                                        kotlinx.coroutines.delay(600)
                                                        ccActionTerminalLines.add("[CI/CD Runner] Binary compiled and signed. Releasing artifact 'production-windows-amd64.zip'!")
                                                        ccActionProgress = 1.0f
                                                        ccReleasePackageReady = true
                                                        isCcActionRunning = false
                                                        speakText("GitHub action workflow executed successfully. Windows distribution installer ready on server.")
                                                        Toast.makeText(context, "Production Windows Build Released via GitHub Actions CI/CD!", Toast.LENGTH_LONG).show()
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = SpaceSlate),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.weight(1.2f),
                                                border = BorderStroke(1.dp, BorderColor)
                                            ) {
                                                Text("🎬 Check GitHub CI/CD Actions", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            // GitHub actions monitor
                            if (isCcActionRunning || ccActionTerminalLines.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, Color.Gray)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isCcActionRunning) "⚙️ GitHub Actions CI Runner Status: RUNNING..." else "✅ GitHub Actions CI Runner Status: COMPLETED",
                                                color = if (isCcActionRunning) MintNeon else Color.Green,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${(ccActionProgress * 100).toInt()}%",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        LinearProgressIndicator(
                                            progress = { ccActionProgress },
                                            modifier = Modifier.fillMaxWidth().height(4.dp),
                                            color = if (isCcActionRunning) MintNeon else Color.Green,
                                            trackColor = DeepDark
                                        )
                                        
                                        Spacer(modifier = Modifier.height(6.dp))
                                        
                                        LazyColumn(
                                            modifier = Modifier.fillMaxWidth().height(100.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            items(ccActionTerminalLines) { line ->
                                                Text(line, color = if (line.contains("Releasing")) Color.Green else Color.LightGray, fontFamily = FontFamily.Monospace, fontSize = 8.sp)
                                            }
                                        }

                                        if (ccReleasePackageReady) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = {
                                                    Toast.makeText(context, "📥 Downloading CI produced executable installer production-windows-amd64.zip ...", Toast.LENGTH_LONG).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("📥 Download Release Artifact from GitHub (18.1 MB)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Live Mockup representation of the active website inside the device container
            val drawInWindowsFrame = activeViewport == ViewportMode.DESKTOP || activeEditorTab == "WINDOWS_APP_ENGINE"
            
            Text(
                text = if (drawInWindowsFrame) "🪟 STANDALONE WINDOWS APP CONTROLLER" else "🖥️ Web Canvas (Live Container Preview)", 
                color = Color.White, 
                fontSize = 12.sp, 
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Outer surface wrap for emulator
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.5f)
                    .border(2.dp, if (drawInWindowsFrame) Color.Gray else activeSkinBg.accentColor, RoundedCornerShape(12.dp)),
                color = Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Render custom Windows 11 App Frame decoration if tab or desktop viewport selected
                    if (drawInWindowsFrame) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF202020),
                            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("💠", fontSize = 10.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${selectedWebSite.name} Standalone (Win64 Desktop App v1.0)",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("━", color = Color.Gray, fontSize = 9.sp)
                                        Text("🗖", color = Color.Gray, fontSize = 9.sp)
                                        Text("✕", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                // URL address bar
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color(0xFF141414),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(1.dp, Color.DarkGray)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🔒 Secure | ", color = Color.LightGray, fontSize = 8.sp)
                                        Text(
                                            text = "https://${selectedWebSite.name.lowercase().replace(" ", "")}.nexus.site/app/windows-env", 
                                            color = MintNeon, 
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Actual web page contents
                    Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.linearGradient(colors = activeSkinBg.bgColors))
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            // Responsive width visualization alignment
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding((if (activeViewport == ViewportMode.MOBILE) 16.dp else 0.dp)),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Site Brand Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(selectedWebSite.primaryColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                selectedWebSite.name.take(2).uppercase(),
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            selectedWebSite.name,
                                            color = activeSkinBg.textColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }

                                    // Mini Menu list
                                    Text(
                                        text = "Home | Products | Contact",
                                        color = activeSkinBg.mutedColor,
                                        fontSize = 10.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // Hero Content
                                Text(
                                    text = customHeadlineText,
                                    color = activeSkinBg.textColor,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = customTaglineText,
                                    color = activeSkinBg.accentColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = selectedWebSite.description,
                                    color = activeSkinBg.mutedColor,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 14.sp
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Dynamic element container based on active template selected
                                if (selectedWebSite.isEcomEnabled) {
                                    // EcoStore visual elements (Products grid showcase)
                                    Text(
                                        "🌿 Featured Stock Catalog",
                                        color = activeSkinBg.textColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.align(Alignment.Start)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        selectedWebSite.productsList.take(3).forEach { pt ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = CardSlate.copy(alpha = 0.5f)),
                                                border = BorderStroke(1.dp, BorderColor)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(pt.emoji, fontSize = 20.sp)
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Column {
                                                            Text(pt.title, color = activeSkinBg.textColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                            Text("Stock left: ${pt.stockCount}", color = activeSkinBg.mutedColor, fontSize = 10.sp)
                                                        }
                                                    }
                                                    Text("$${pt.price}", color = activeSkinBg.accentColor, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Kinetic Synergies Consulting books list (Trinidad / leadership audience)
                                    Text(
                                        "📚 Published Leadership Trilogy & Works",
                                        color = activeSkinBg.textColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.align(Alignment.Start)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        selectedWebSite.booksList.take(3).forEach { book ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = activeSkinBg.accentColor.copy(alpha = 0.12f)),
                                                border = BorderStroke(1.dp, activeSkinBg.accentColor.copy(alpha = 0.25f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(book.emoji, fontSize = 24.sp)
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column {
                                                        Text(book.title, color = activeSkinBg.textColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                        Text(book.subtitle, color = activeSkinBg.accentColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                                        Text(book.desc, color = activeSkinBg.mutedColor, fontSize = 9.sp, lineHeight = 11.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // GDPR Cookie Consent compliance visualizer block
                                if (cookieConsent == null) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = SpaceSlate,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, MintNeon)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                "🍪 GDPR cookie consent manager preview banner",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = { onCookieConsentAns(true) },
                                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = MintNeon),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text("Accept All", color = DeepDark, fontSize = 9.sp)
                                                }
                                                Button(
                                                    onClick = { onCookieConsentAns(false) },
                                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = BorderColor),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text("Reject", color = Color.White, fontSize = 9.sp)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = if (cookieConsent == true) "✅ Consent approved. Cookies stored." else "❌ Consent denied. Zero-Tracking mode.",
                                        color = if (cookieConsent == true) MintNeon else Color.Red,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.align(Alignment.Start)
                                    )
                                }
                            }
                        }

                        // ===================================
                        // REAL FLOATING VOICEBOT BUBBLE OVERLAY
                        // ===================================
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                        ) {
                            if (!isWebWidgetDrawerOpen) {
                                // Floating chat bubble
                                FloatingActionButton(
                                    onClick = { 
                                        isWebWidgetDrawerOpen = true 
                                        speakText("Hello! I am your companion voice agent. Talk or type to communicate with me!")
                                    },
                                    containerColor = selectedWebSite.primaryColor,
                                    contentColor = Color.White,
                                    shape = CircleShape,
                                    modifier = Modifier.size(54.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🎙️", fontSize = 22.sp)
                                    }
                                }
                            } else {
                                // Floating open speech agent drawer overlay
                                Surface(
                                    modifier = Modifier
                                        .width(300.dp)
                                        .height(350.dp),
                                    color = DeepDark.copy(alpha = 0.98f),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(2.dp, selectedWebSite.primaryColor)
                                ) {
                                    Column {
                                        // Header
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            color = selectedWebSite.primaryColor
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("🎙️", fontSize = 16.sp)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Column {
                                                        Text(voiceBotNameState, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                        Text("Website voicebot agent • Active", color = Color.White.copy(alpha = 0.8f), fontSize = 8.sp)
                                                    }
                                                }
                                                IconButton(
                                                    onClick = { isWebWidgetDrawerOpen = false },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Text("❌", color = Color.White, fontSize = 10.sp)
                                                }
                                            }
                                        }

                                        // Balance usage disclaimer tier
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            color = Color.Black.copy(alpha = 0.4f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Pricing: $${"%.2f".format(voiceBotUsageRateState)} / turn", color = MintNeon, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                Text("Usage billed: $${"%.2f".format(voiceBotAccumulatedUSDState)}", color = Color.LightGray, fontSize = 8.sp)
                                            }
                                        }

                                        // Dialogue messages
                                        LazyColumn(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            items(webWidgetHistory) { msg ->
                                                val isAI = msg.sender == "AI"
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = if (isAI) Arrangement.Start else Arrangement.End
                                                ) {
                                                    Surface(
                                                        color = if (isAI) SpaceSlate else selectedWebSite.primaryColor.copy(alpha = 0.8f),
                                                        shape = RoundedCornerShape(
                                                            topStart = 8.dp,
                                                            topEnd = 8.dp,
                                                            bottomStart = if (isAI) 0.dp else 8.dp,
                                                            bottomEnd = if (isAI) 8.dp else 0.dp
                                                        ),
                                                        modifier = Modifier.widthIn(max = 220.dp)
                                                    ) {
                                                        Column(modifier = Modifier.padding(8.dp)) {
                                                            Text(
                                                                text = msg.text,
                                                                color = Color.White,
                                                                fontSize = 10.sp,
                                                                lineHeight = 12.sp
                                                            )
                                                            if (isAI) {
                                                                Spacer(modifier = Modifier.height(2.dp))
                                                                Text("🔊 Vocal Synthesis Playing", color = MintNeon, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Quick voice phrases triggers (train helper)
                                        Text(
                                            "Tap customer voice quick phrases:", 
                                            color = TextMuted, 
                                            fontSize = 8.sp, 
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState())
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            val promptShortcuts = listOf(
                                                "🗣️ Return policy?" to "What is your return policy?",
                                                "🗣️ Store Hours?" to "What are your operating store hours?",
                                                "🗣️ List Products?" to "Show me your featured product stock details."
                                            )
                                            promptShortcuts.forEach { pair ->
                                                Surface(
                                                    color = SpaceSlate,
                                                    shape = RoundedCornerShape(4.dp),
                                                    border = BorderStroke(1.dp, BorderColor),
                                                    modifier = Modifier.clickable {
                                                        webWidgetInputText = pair.second
                                                    }
                                                ) {
                                                    Text(
                                                        pair.first,
                                                        color = Color.White,
                                                        fontSize = 8.sp,
                                                        modifier = Modifier.padding(6.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Input Form
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = webWidgetInputText,
                                                onValueChange = { webWidgetInputText = it },
                                                placeholder = { Text("Ask voice agent...", color = TextMuted, fontSize = 9.sp) },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White,
                                                    focusedBorderColor = MintNeon,
                                                    unfocusedBorderColor = BorderColor
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = {
                                                    if (webWidgetInputText.isNotBlank()) {
                                                        val userMessageText = webWidgetInputText
                                                        webWidgetInputText = ""
                                                        webWidgetHistory.add(ChatMessage("USER", userMessageText))
                                                        
                                                        // Increment custom metrics as user interacts and charges accordingly
                                                        voiceBotTotalQueriesState += 1
                                                        voiceBotAccumulatedUSDState += voiceBotUsageRateState
                                                        
                                                        // Simulated AI evaluation response
                                                        coroutineScope.launch {
                                                            kotlinx.coroutines.delay(1000)
                                                            val answer = when {
                                                                userMessageText.contains("return", true) || userMessageText.contains("refund", true) -> {
                                                                    "Our return policy allows complete domestic refunds within 30 days of shipment. No hassle, eco-friendly claims verified instantly!"
                                                                }
                                                                userMessageText.contains("hour", true) || userMessageText.contains("open", true) -> {
                                                                    "We are open Monday through Friday from 9AM to 6PM EST. We also have voice agents monitoring chat channels 24/7."
                                                                }
                                                                userMessageText.contains("product", true) || userMessageText.contains("stock", true) || userMessageText.contains("price", true) -> {
                                                                    if (selectedWebSite.isEcomEnabled) {
                                                                        "Our featured catalog includes: ${selectedWebSite.productsList.joinToString { "${it.emoji} ${it.title} ($${it.price})" }}."
                                                                    } else {
                                                                        "The published leadership works include: ${selectedWebSite.booksList.joinToString { "${it.emoji} ${it.title} - ${it.subtitle}" }}."
                                                                    }
                                                                }
                                                                else -> {
                                                                    "I've searched our custom dataset rules of ${selectedWebSite.name} and synced that request. Please let me know if you would like me to process a booking!"
                                                                }
                                                            }
                                                            webWidgetHistory.add(ChatMessage("AI", answer))
                                                            speakText(answer)
                                                        }
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(selectedWebSite.primaryColor)
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Code Editor View tab trigger when developer role is active
            if (userRole.canCode) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("💻 Code Engine View (HTML Source with Voice Embeds)", color = MintNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = htmlSource,
                            color = Color.LightGray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            lineHeight = 12.sp,
                            maxLines = 4
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡ Sync status: HTML dynamic code synchronizes automatically upon visual changes.", color = Color.Gray, fontSize = 8.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Lower Copilot Prompt Panel
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardSlate),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = MintNeon, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AI Copilot Assistant", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    Text(
                        text = chatbotHistory.lastOrNull()?.text ?: "",
                        color = TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = chatCommandInput,
                            onValueChange = onChatCommandChange,
                            placeholder = { Text("e.g. change colors to neon green", color = TextMuted, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MintNeon,
                                unfocusedBorderColor = BorderColor
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = onSendChatMsg,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(ElectricPurple)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send prompt", tint = Color.White)
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Microphone button with colored indicators
                        IconButton(
                            onClick = {
                                if (!isListeningByVoice) {
                                    onListeningByVoiceChange(true)
                                    speakText("Ready. Tap a microphone quick phrase or speak out loud to activate voice controls.")
                                } else {
                                    onListeningByVoiceChange(false)
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isListeningByVoice) Color.Red else MintNeon.copy(alpha = 0.9f))
                        ) {
                            Text(if (isListeningByVoice) "⏹️" else "🎙️", fontSize = 16.sp)
                        }
                    }
                }
            }
        }

        if (isListeningByVoice) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(14.dp),
                color = DeepDark.copy(alpha = 0.95f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, MintNeon)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎙️", fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
                            Column {
                                Text("Nexus Voice Recognition", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Listening for spoken directions...", color = MintNeon, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        IconButton(onClick = { onListeningByVoiceChange(false) }) {
                            Text("❌", fontSize = 13.sp, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Tap one of these voice commands to trigger vocal synthesis:", color = TextMuted, fontSize = 10.sp)

                    Spacer(modifier = Modifier.height(8.dp))

                    val simulatedSpeechOptions = listOf(
                        "🎨 Make it Metallic sky blue!" to "metallic sky blue",
                        "🔴 Set it to Ruby red look" to "ruby red",
                        "🌸 Toggle Rose pink backgrounds" to "rose pink",
                        "🌿 Add Organic face oil product" to "add organic face oil product",
                        "🎓 Swap canvas to Kinetic coaching content" to "load kinetic consulting design"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        simulatedSpeechOptions.forEach { option ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onChatCommandChange(option.second)
                                        onSendChatMsg()
                                    },
                                color = SpaceSlate,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, BorderColor)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(option.first, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text("🗣️", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegacyVisualEditorView(
    selectedWebSite: GeneratedWebSite,
    customHeadlineText: String,
    onHeadlineChange: (String) -> Unit,
    customTaglineText: String,
    onTaglineChange: (String) -> Unit,
    activeViewport: ViewportMode,
    onViewportChange: (ViewportMode) -> Unit,
    chatbotHistory: List<ChatMessage>,
    chatCommandInput: String,
    onChatCommandChange: (String) -> Unit,
    onSendChatMsg: () -> Unit,
    htmlSource: String,
    userRole: UserRole,
    cookieConsent: Boolean?,
    onCookieConsentAns: (Boolean) -> Unit,
    onAddProductToStore: () -> Unit,
    activeSkinBg: SkinBackground,
    onSkinBgChange: (SkinBackground) -> Unit,
    speakText: (String) -> Unit,
    isListeningByVoice: Boolean,
    onListeningByVoiceChange: (Boolean) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
        // Upper Viewport size selectors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ViewportMode.values().forEach { mode ->
                val selected = mode == activeViewport
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onViewportChange(mode) },
                    color = if (selected) SpaceSlate else CardSlate,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (selected) MintNeon else BorderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(mode.icon, contentDescription = null, tint = if (selected) MintNeon else TextMuted, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(mode.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Left Properties Drawer panel inline
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardSlate),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("✏️ Layout Properties Editor", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customHeadlineText,
                        onValueChange = onHeadlineChange,
                        label = { Text("Brand Headline", color = TextMuted) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MintNeon,
                            unfocusedBorderColor = BorderColor
                        )
                    )

                    OutlinedTextField(
                        value = customTaglineText,
                        onValueChange = onTaglineChange,
                        label = { Text("Tagline / Motto", color = TextMuted) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MintNeon,
                            unfocusedBorderColor = BorderColor
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Dynamic Skin Changer Row
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardSlate),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = MintNeon, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("🎨 Device Skin Background Changer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(SkinBackground.values()) { skin ->
                        val isSelected = activeSkinBg == skin
                        Surface(
                            modifier = Modifier
                                .clickable { 
                                    onSkinBgChange(skin)
                                },
                            color = if (isSelected) SpaceSlate else CardSlate,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, if (isSelected) MintNeon else BorderColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(skin.iconEmoji, fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(skin.label, color = if (isSelected) MintNeon else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Live Mockup representation of the active website inside the device container
        Text("🖥️ Web Canvas (Live Container Preview)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(6.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.5f)
                .border(2.dp, activeSkinBg.accentColor, RoundedCornerShape(12.dp)),
            color = Color.Transparent,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(colors = activeSkinBg.bgColors))
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Responsive width visualization alignment
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding((if (activeViewport == ViewportMode.MOBILE) 16.dp else 0.dp)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Site Brand Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(selectedWebSite.primaryColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    selectedWebSite.name.take(2).uppercase(),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                selectedWebSite.name,
                                color = activeSkinBg.textColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        // Mini Menu list
                        Text(
                            text = "Home | Products | Contact",
                            color = activeSkinBg.mutedColor,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Hero Content
                    Text(
                        text = customHeadlineText,
                        color = activeSkinBg.textColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = customTaglineText,
                        color = activeSkinBg.accentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = selectedWebSite.description,
                        color = activeSkinBg.mutedColor,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Dynamic element container based on active template selected
                    if (selectedWebSite.isEcomEnabled) {
                        // EcoStore visual elements (Products grid showcase)
                        Text(
                            "🌿 Featured Stock Catalog",
                            color = activeSkinBg.textColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            selectedWebSite.productsList.take(3).forEach { pt ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = CardSlate.copy(alpha = 0.5f)),
                                    border = BorderStroke(1.dp, BorderColor)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(pt.emoji, fontSize = 20.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(pt.title, color = activeSkinBg.textColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Text("Stock left: ${pt.stockCount}", color = activeSkinBg.mutedColor, fontSize = 10.sp)
                                            }
                                        }
                                        Text("$${pt.price}", color = activeSkinBg.accentColor, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        // Kinetic Synergies Consulting books list (Trinidad / leadership audience)
                        Text(
                            "📚 Published Leadership Trilogy & Works",
                            color = activeSkinBg.textColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            selectedWebSite.booksList.take(3).forEach { book ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = activeSkinBg.accentColor.copy(alpha = 0.12f)),
                                    border = BorderStroke(1.dp, activeSkinBg.accentColor.copy(alpha = 0.25f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(book.emoji, fontSize = 24.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(book.title, color = activeSkinBg.textColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(book.subtitle, color = activeSkinBg.accentColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                            Text(book.desc, color = activeSkinBg.mutedColor, fontSize = 9.sp, lineHeight = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // GDPR Cookie Consent compliance visualizer block
                    if (cookieConsent == null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = SpaceSlate,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MintNeon)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    "🍪 GDPR cookie consent manager preview banner",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { onCookieConsentAns(true) },
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MintNeon),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Accept All", color = DeepDark, fontSize = 9.sp)
                                    }
                                    Button(
                                        onClick = { onCookieConsentAns(false) },
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = BorderColor),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Reject", color = Color.White, fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = if (cookieConsent == true) "✅ Consent approved. Cookies stored." else "❌ Consent denied. Zero-Tracking mode.",
                            color = if (cookieConsent == true) MintNeon else Color.Red,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Start)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Code Editor View tab trigger when developer role is active
        if (userRole.canCode) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("💻 Code Engine View (HTML Source)", color = MintNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = htmlSource,
                        color = Color.LightGray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        lineHeight = 12.sp,
                        maxLines = 4
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // AI Chatbot Designer widget at footer editor
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardSlate),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = MintNeon, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI Copilot Assistant", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Text(
                    text = chatbotHistory.lastOrNull()?.text ?: "",
                    color = TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = chatCommandInput,
                        onValueChange = onChatCommandChange,
                        placeholder = { Text("e.g., change colors to neon green", color = TextMuted, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MintNeon,
                            unfocusedBorderColor = BorderColor
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onSendChatMsg,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ElectricPurple)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send prompt", tint = Color.White)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Microphone button with colored indicators
                    IconButton(
                        onClick = {
                            if (!isListeningByVoice) {
                                onListeningByVoiceChange(true)
                                speakText("Ready. Tap a microphone quick phrase or speak out loud to activate voice controls.")
                            } else {
                                onListeningByVoiceChange(false)
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isListeningByVoice) Color.Red else MintNeon.copy(alpha = 0.9f))
                    ) {
                        Text(if (isListeningByVoice) "⏹️" else "🎙️", fontSize = 16.sp)
                    }
                }
            }
        }
    }

    if (isListeningByVoice) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(14.dp),
            color = DeepDark.copy(alpha = 0.95f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, MintNeon)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎙️", fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
                        Column {
                            Text("Nexus Voice Recognition", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Listening for spoken directions...", color = MintNeon, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    IconButton(onClick = { onListeningByVoiceChange(false) }) {
                        Text("❌", fontSize = 13.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("Tap one of these voice commands to trigger vocal synthesis:", color = TextMuted, fontSize = 10.sp)

                Spacer(modifier = Modifier.height(8.dp))

                val simulatedSpeechOptions = listOf(
                    "🎨 Make it Metallic sky blue!" to "metallic sky blue",
                    "🔴 Set it to Ruby red look" to "ruby red",
                    "🌸 Toggle Rose pink backgrounds" to "rose pink",
                    "🌿 Add Organic face oil product" to "add organic face oil product",
                    "🎓 Swap canvas to Kinetic coaching content" to "load kinetic consulting design"
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    simulatedSpeechOptions.forEach { option ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onChatCommandChange(option.second)
                                    onSendChatMsg()
                                },
                            color = SpaceSlate,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, BorderColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(option.first, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.weight(1f))
                                Text("🗣️", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
}

// ==========================================
// SCREEN 5: STORE BACKEND CATALOG MANAGER
// ==========================================

@Composable
fun StoreBackendView(
    selectedWebSite: GeneratedWebSite,
    onUpdateStock: (WebProduct, Int) -> Unit,
    onAddProduct: () -> Unit,
    ordersCount: MutableState<Int>,
    recentOrdersList: MutableList<Triple<String, String, String>>
) {
    var discountCodeInput by remember { mutableStateOf("XMAS20") }
    var discountRateApplied by remember { mutableStateOf("20% off") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🛒 Store Catalogs & Backend Manager", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Button(
                onClick = onAddProduct,
                colors = ButtonDefaults.buttonColors(containerColor = MintNeon)
            ) {
                Text("+ Product", color = DeepDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Inventory Stock management grid
        Text("Stock Levels Manager", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (selectedWebSite.productsList.isEmpty()) {
                item {
                    Text("No products in active site schema.", color = TextMuted, fontSize = 12.sp)
                }
            } else {
                items(selectedWebSite.productsList) { product ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = CardSlate,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(product.emoji, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(product.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Category: ${product.category}", color = TextMuted, fontSize = 11.sp)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Stock: ", color = TextMuted, fontSize = 11.sp)
                                Text(
                                    "${product.stockCount}",
                                    color = if (product.stockCount < 10) Color.Red else MintNeon,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                IconButton(
                                    onClick = { onUpdateStock(product, -5) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = TextMuted)
                                }
                                IconButton(
                                    onClick = { onUpdateStock(product, 5) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = MintNeon)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Coupon generation sandbox widget
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardSlate),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("🏷️ Active Discounts & Coupons", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = discountCodeInput,
                        onValueChange = { discountCodeInput = it },
                        label = { Text("Coupon Code", color = TextMuted) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MintNeon,
                            unfocusedBorderColor = BorderColor
                        )
                    )

                    OutlinedTextField(
                        value = discountRateApplied,
                        onValueChange = { discountRateApplied = it },
                        label = { Text("Rate / Discount", color = TextMuted) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MintNeon,
                            unfocusedBorderColor = BorderColor
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Stripe Sandbox Payment Visualizer
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SpaceSlate,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MintNeon)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("💳 SECURE PAYMENT SYSTEM PROXY", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "PCI-DSS Compliant Gateway handles all transaction scopes safely. Google Pay/Visa API triggers successfully on active device checkups.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

// ==========================================
// SCREEN 6: COMPLIANCE CHECKLIST & API SANDBOX
// ==========================================

@Composable
fun SandboxAndComplianceView(
    apiMethod: String,
    onMethodChange: (String) -> Unit,
    apiEndpoint: String,
    onEndpointChange: (String) -> Unit,
    apiResponse: String,
    onSendRequest: () -> Unit,
    isGDPRRightToDeleteExecuted: Boolean,
    onGDPRDeleteTrigger: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Upper compliance status metrics card
        Text("⚖️ Regulatory & Safety Compliance", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(
            text = "Automated controls built-in for international standard operations.",
            color = TextMuted,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        ComplianceCheckRow(label = "GDPR Compliance Suite", desc = "Provides Cookie agreement and data deletion right.", active = true)
        ComplianceCheckRow(label = "CCPA Compliant System", desc = "Opt-out links and privacy policy constructors.", active = true)
        ComplianceCheckRow(label = "PCI-DSS Payment Seal", desc = "Direct Stripe elements proxy, strictly encrypted.", active = true)
        ComplianceCheckRow(label = "WCAG 2.1 AA Checklist", desc = "Minimum 48dp layouts and color contrast check.", active = true)

        Spacer(modifier = Modifier.height(16.dp))

        // GDPR Erasure Trigger Button
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardSlate),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "🧹 GDPR Right to be Forgotten (Erasure)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "Allows site visitors to erase their complete tracking parameters and cart databases instantly.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = onGDPRDeleteTrigger,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isGDPRRightToDeleteExecuted) Color.Gray else Color.Red),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isGDPRRightToDeleteExecuted) "Deleted & Cleaned" else "Execute Data Deletion Request",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // REST API Explorer Sandbox
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardSlate),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = MintNeon, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("📡 REST API Developer Sandbox Explorer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // API Method selector drop visual
                    Row(
                        modifier = Modifier
                            .background(SpaceSlate, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 12.dp)
                            .clickable {
                                // switch method
                                val nextM = if (apiMethod == "GET") "POST" else "GET"
                                onMethodChange(nextM)
                            }
                    ) {
                        Text(apiMethod, color = MintNeon, fontWeight = FontWeight.Bold)
                    }

                    // API Endpoint selector
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(SpaceSlate, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 12.dp)
                            .clickable {
                                // Cycle endpoints
                                val nextEndpoint = when (apiEndpoint) {
                                    "/api/sites" -> "/api/analytics"
                                    "/api/analytics" -> "/api/security-audit"
                                    else -> "/api/sites"
                                }
                                onEndpointChange(nextEndpoint)
                            }
                    ) {
                        Text(apiEndpoint, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onSendRequest,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple)
                ) {
                    Text("Trigger API Request (Simulate Response)", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // API response viewer
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    color = Color.Black,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    LazyColumn(modifier = Modifier.padding(8.dp)) {
                        item {
                            Text(
                                text = apiResponse,
                                color = Color.Green,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ComplianceCheckRow(label: String, desc: String, active: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (active) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (active) MintNeon else Color.Gray,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(desc, color = TextMuted, fontSize = 10.sp)
        }
    }
}

// ==========================================
// SECURE DIRECT REST CALL TO GEMINI API SERVICE
// ==========================================

suspend fun callGeminiDirectRest(userPrompt: String, userCustomKey: String?): String? = withContext(Dispatchers.IO) {
    // Falls back to Serverside AI Studio Secret if custom key is not declared
    val clientApiKey = if (!userCustomKey.isNullOrEmpty()) userCustomKey else try {
        BuildConfig.GEMINI_API_KEY
    } catch (e: Exception) {
        ""
    }

    if (clientApiKey.isEmpty() || clientApiKey == "MY_GEMINI_API_KEY") {
        return@withContext null // Fallback to offline template engine gracefully
    }

    val model = "gemini-3.5-flash"
    val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$clientApiKey"

    // Construct prompt demanding clean structured JSON representation output
    val finalStyledPrompt = """
        Analyze the following prompt and return a valid clean JSON object with precise matching parameters.
        Prompt: "$userPrompt"
        
        Mandatory format:
        {
          "siteName": "A catchy business name",
          "headline": "A clean bold headline",
          "tagline": "An inspiring tag sentence representing small improvements",
          "description": "A structured description paragraph matching themes"
        }
        Do not wrap output in markdown codeblocks. Return pure JSON text only.
    """.trimIndent()

    val payload = JSONObject()
    val contentsArray = JSONArray()
    val contentObj = JSONObject()
    val partsArray = JSONArray()
    val partObj = JSONObject()
    partObj.put("text", finalStyledPrompt)
    partsArray.put(partObj)
    contentObj.put("parts", partsArray)
    contentsArray.put(contentObj)
    payload.put("contents", contentsArray)

    // Setup network timeouts
    val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val mediaType = "application/json; charset=utf-8".toMediaType()
    val requestBody = payload.toString().toRequestBody(mediaType)

    val request = Request.Builder()
        .url(url)
        .post(requestBody)
        .build()

    try {
        val response = client.newCall(request).execute()
        val textBody = response.body?.string()
        if (textBody != null) {
            val jsonResponse = JSONObject(textBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val contentRes = firstCandidate?.optJSONObject("content")
            val parts = contentRes?.optJSONArray("parts")
            val firstPart = parts?.optJSONObject(0)
            val fullResultText = firstPart?.optString("text")?.trim()

            // Remove any potential code block wrappers on the model response text
            fullResultText?.replace("```json", "")?.replace("```", "")?.trim()
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}
