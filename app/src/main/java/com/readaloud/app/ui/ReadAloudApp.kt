package com.readaloud.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.readaloud.app.Routes
import com.readaloud.app.model.MessageLanguage
import com.readaloud.app.model.ReadAloudMessage
import com.readaloud.app.model.SpeechPreference
import com.readaloud.app.speech.VoiceAvailability

@Composable
fun ReadAloudApp(
    initialRoute: String
) {
    val viewModel: ReadAloudViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var route by remember { mutableStateOf(initialRoute) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshRuntimeChecks()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(initialRoute) {
        route = initialRoute
    }
    LaunchedEffect(uiState.settings.onboardingComplete) {
        if (!uiState.settings.onboardingComplete) {
            route = Routes.ONBOARDING
        } else if (route == Routes.ONBOARDING) {
            route = Routes.ANALYSIS
        }
    }

    ReadAloudTheme {
        when (route) {
            Routes.SETTINGS -> SettingsScreen(
                state = uiState,
                viewModel = viewModel,
                onBack = { route = Routes.ANALYSIS }
            )
            Routes.ONBOARDING -> OnboardingScreen(
                state = uiState,
                viewModel = viewModel,
                onDone = { route = Routes.ANALYSIS }
            )
            else -> AnalysisScreen(
                state = uiState,
                viewModel = viewModel,
                onSettings = { route = Routes.SETTINGS }
            )
        }
    }
}

@Composable
private fun ReadAloudTheme(content: @Composable () -> Unit) {
    val colors = androidx.compose.material3.lightColorScheme(
        primary = Color(0xFF23615A),
        onPrimary = Color.White,
        secondary = Color(0xFF7A5C28),
        tertiary = Color(0xFF7A3E55),
        background = Color(0xFFF7FAF8),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFE2E8E4),
        onSurface = Color(0xFF18211F),
        onSurfaceVariant = Color(0xFF44504C)
    )
    MaterialTheme(
        colorScheme = colors,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnalysisScreen(
    state: ReadAloudUiState,
    viewModel: ReadAloudViewModel,
    onSettings: () -> Unit
) {
    val messages = state.analysis.screenAnalysis.messages
    val listState = rememberLazyListState()
    val currentId = state.speech.currentMessageId

    LaunchedEffect(currentId, state.settings.highlightSpeaking, messages) {
        if (state.settings.highlightSpeaking && currentId != null) {
            val index = messages.indexOfFirst { it.id == currentId }
            if (index >= 0) listState.animateScrollToItem(index)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("ReadAloud") },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (messages.isNotEmpty()) {
                PlaybackBar(
                    isPaused = state.speech.isPaused,
                    onPlayAll = { viewModel.playAll(messages) },
                    onPause = { viewModel.pause() },
                    onResume = { viewModel.resume() },
                    onStop = { viewModel.stop() }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            when {
                state.analysis.isAnalyzing -> CenterMessage("Analyzing visible messages...")
                messages.isEmpty() -> EmptyAnalysis(
                    accessibilityEnabled = state.accessibilityEnabled,
                    onSample = { viewModel.loadSampleMessages() }
                )
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(messages, key = { _, message -> message.id }) { _, message ->
                        MessageCard(
                            message = message,
                            showProcessed = state.settings.showProcessedText,
                            speaking = state.settings.highlightSpeaking && message.id == currentId,
                            onSpeak = { viewModel.speak(message) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageCard(
    message: ReadAloudMessage,
    showProcessed: Boolean,
    speaking: Boolean,
    onSpeak: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (speaking) Color(0xFFE9F4EF) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (speaking) 3.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = message.originalText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                AnimatedVisibility(visible = speaking) {
                    Text(
                        text = "Speaking...",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                AnimatedVisibility(visible = showProcessed && message.processedText != message.originalText) {
                    Column(Modifier.padding(top = 8.dp)) {
                        Text(
                            text = "Speak as",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = message.processedText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            IconButton(
                onClick = onSpeak,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (speaking) Icons.Default.GraphicEq else Icons.Default.VolumeUp,
                    contentDescription = "Speak message",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PlaybackBar(
    isPaused: Boolean,
    onPlayAll: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = onPlayAll, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Text("Play All", modifier = Modifier.padding(start = 8.dp))
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = if (isPaused) onResume else onPause,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null
                    )
                    Text(if (isPaused) "Resume" else "Pause", Modifier.padding(start = 6.dp))
                }
                OutlinedButton(
                    onClick = onStop,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Text("Stop", Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyAnalysis(
    accessibilityEnabled: Boolean,
    onSample: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (accessibilityEnabled) {
                "Open a chat and activate ReadAloud."
            } else {
                "Set up ReadAloud to read visible messages."
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onSample) {
            Text("Preview with sample messages")
        }
    }
}

@Composable
private fun CenterMessage(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    state: ReadAloudUiState,
    viewModel: ReadAloudViewModel,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SettingsGroup("Activation") {
                    SwitchRow(
                        title = "Double volume press",
                        checked = state.settings.doubleVolumeEnabled,
                        onCheckedChange = viewModel::setDoubleVolumeEnabled
                    )
                    SliderRow(
                        title = "Double press interval",
                        value = state.settings.doublePressIntervalMs.toFloat(),
                        range = 300f..1200f,
                        valueLabel = "${state.settings.doublePressIntervalMs} ms",
                        onValueChange = { viewModel.setDoublePressInterval(it.toLong()) }
                    )
                }
            }
            item {
                SettingsGroup("Speech") {
                    PreferenceButtons(
                        selected = state.settings.speechPreference,
                        onSelected = viewModel::setSpeechPreference
                    )
                    SliderRow(
                        title = "Speech speed",
                        value = state.settings.speechSpeed,
                        range = 0.5f..1.8f,
                        valueLabel = "%.1fx".format(state.settings.speechSpeed),
                        onValueChange = viewModel::setSpeechSpeed
                    )
                    SliderRow(
                        title = "Pitch",
                        value = state.settings.pitch,
                        range = 0.5f..1.8f,
                        valueLabel = "%.1fx".format(state.settings.pitch),
                        onValueChange = viewModel::setPitch
                    )
                }
            }
            item {
                SettingsGroup("Processing") {
                    SwitchRow("Translation", state.settings.translationEnabled, viewModel::setTranslationEnabled)
                    SwitchRow("Roman-language conversion", state.settings.romanConversionEnabled, viewModel::setRomanConversionEnabled)
                    SwitchRow("OCR fallback", state.settings.ocrFallbackEnabled, viewModel::setOcrFallbackEnabled)
                }
            }
            item {
                SettingsGroup("Interface") {
                    SwitchRow("Show processed text", state.settings.showProcessedText, viewModel::setShowProcessedText)
                    SwitchRow("Highlight speaking message", state.settings.highlightSpeaking, viewModel::setHighlightSpeaking)
                }
            }
            item {
                SettingsGroup("Offline Voices") {
                    VoiceRows(state.speech.voices)
                }
            }
        }
    }
}

@Composable
private fun OnboardingScreen(
    state: ReadAloudUiState,
    viewModel: ReadAloudViewModel,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    "ReadAloud",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Your messages are processed on your device. ReadAloud does not upload your messages or screenshots.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            item {
                SetupStep("1", "Enable Accessibility Service") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusPill(ready = state.accessibilityEnabled)
                        Spacer(Modifier.weight(1f))
                        OutlinedButton(onClick = {
                            safeStart(context, viewModel.openAccessibilitySettings())
                        }) {
                            Text("Open")
                        }
                    }
                }
            }
            item {
                SetupStep("2", "Check offline voices") {
                    VoiceRows(state.speech.voices)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        OutlinedButton(onClick = { viewModel.refreshVoices() }) {
                            Text("Refresh")
                        }
                        OutlinedButton(onClick = {
                            safeStart(context, viewModel.openVoiceInstall())
                        }) {
                            Text("Install voices")
                        }
                    }
                }
            }
            item {
                SetupStep("3", "Test speech") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { viewModel.testVoice(MessageLanguage.HINDI) }) {
                            Text("Hindi")
                        }
                        OutlinedButton(onClick = { viewModel.testVoice(MessageLanguage.GUJARATI) }) {
                            Text("Gujarati")
                        }
                        OutlinedButton(onClick = { viewModel.testVoice(MessageLanguage.ENGLISH) }) {
                            Text("English")
                        }
                    }
                }
            }
            item {
                SetupStep("4", "Preferred language") {
                    PreferenceButtons(
                        selected = state.settings.speechPreference,
                        onSelected = viewModel::setSpeechPreference
                    )
                }
            }
            item {
                SetupStep("5", "Activation") {
                    Text(
                        "Press a volume button twice quickly. Use the Accessibility button or Quick Settings tile if your device blocks volume-key activation.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                Button(
                    onClick = {
                        viewModel.completeOnboarding()
                        onDone()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start")
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun SetupStep(
    number: String,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    SettingsGroup("$number. $title", content)
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SliderRow(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onValueChange: (Float) -> Unit
) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, modifier = Modifier.weight(1f))
            Text(valueLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range
        )
    }
}

@Composable
private fun PreferenceButtons(
    selected: SpeechPreference,
    onSelected: (SpeechPreference) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            PreferenceButton("Automatic", SpeechPreference.AUTOMATIC, selected, onSelected, Modifier.weight(1f))
            PreferenceButton("Hindi", SpeechPreference.HINDI, selected, onSelected, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            PreferenceButton("Gujarati", SpeechPreference.GUJARATI, selected, onSelected, Modifier.weight(1f))
            PreferenceButton("English", SpeechPreference.ENGLISH, selected, onSelected, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PreferenceButton(
    label: String,
    value: SpeechPreference,
    selected: SpeechPreference,
    onSelected: (SpeechPreference) -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected == value) {
        Button(onClick = { onSelected(value) }, modifier = modifier.height(44.dp)) {
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    } else {
        OutlinedButton(onClick = { onSelected(value) }, modifier = modifier.height(44.dp)) {
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun VoiceRows(voices: List<VoiceAvailability>) {
    val displayed = voices.ifEmpty {
        listOf(
            VoiceAvailability(java.util.Locale("hi", "IN"), "Hindi voice", false),
            VoiceAvailability(java.util.Locale("gu", "IN"), "Gujarati voice", false),
            VoiceAvailability(java.util.Locale.ENGLISH, "English voice", false)
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        displayed.forEach { voice ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(voice.label, modifier = Modifier.weight(1f))
                if (voice.availableOffline) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Available",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Available", color = MaterialTheme.colorScheme.primary)
                } else {
                    Text("Install needed", color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
private fun StatusPill(ready: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (ready) Color(0xFFE2F2EA) else Color(0xFFFFF5DD)
    ) {
        Text(
            text = if (ready) "Enabled" else "Not enabled",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = if (ready) Color(0xFF23615A) else Color(0xFF7A5C28)
        )
    }
}

private fun safeStart(context: Context, intent: android.content.Intent) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
    }
}
