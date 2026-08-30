package com.chatnova.ai.ui.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chatnova.ai.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    conversationId: String?,
    onNavigateToHistory: () -> Unit,
    onNavigateToModels: () -> Unit,
    onNavigateToApiKey: () -> Unit,
    onNavigateToInstructions: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    var showModelMenu by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showAttachmentSheet by remember { mutableStateOf(false) }

    // Launchers
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri -> viewModel.addAttachmentFromUri(context, uri) }
    }

    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri -> viewModel.addAttachmentFromUri(context, uri) }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.addAttachmentFromUri(context, it) }
    }

    LaunchedEffect(conversationId) {
        viewModel.loadConversation(conversationId)
    }

    // Auto scroll on new messages or generation
    LaunchedEffect(uiState.messages.size, uiState.messages.lastOrNull()?.content?.length) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showModelMenu = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            val activeModel = uiState.availableModels.find { it.id == uiState.selectedModelId }
                            val modelDisplayName = activeModel?.name ?: uiState.selectedModelId.substringAfterLast('/')

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = modelDisplayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Model",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                if (activeModel?.isFree == true) {
                                    Text(
                                        text = "Free Model • ${activeModel.contextLength / 1000}k ctx",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Model Selector Dropdown
                        DropdownMenu(
                            expanded = showModelMenu,
                            onDismissRequest = { showModelMenu = false }
                        ) {
                            uiState.availableModels.take(8).forEach { model ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(model.name, fontWeight = FontWeight.Medium)
                                            Text(
                                                "${if (model.isFree) "Free" else "Paid"} • ${model.contextLength / 1000}k context",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.onSelectModel(model.id)
                                        showModelMenu = false
                                    },
                                    leadingIcon = {
                                        if (model.id == uiState.selectedModelId) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Browse All Models...") },
                                onClick = {
                                    showModelMenu = false
                                    onNavigateToModels()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.ViewList, contentDescription = null)
                                }
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.startNewChat() }) {
                        Icon(
                            imageVector = Icons.Default.AddComment,
                            contentDescription = "New Chat",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Box {
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                        }

                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Chat History") },
                                onClick = {
                                    showOptionsMenu = false
                                    onNavigateToHistory()
                                },
                                leadingIcon = { Icon(Icons.Default.History, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("All Models") },
                                onClick = {
                                    showOptionsMenu = false
                                    onNavigateToModels()
                                },
                                leadingIcon = { Icon(Icons.Default.Hub, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("API Key Configuration") },
                                onClick = {
                                    showOptionsMenu = false
                                    onNavigateToApiKey()
                                },
                                leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Custom Instructions") },
                                onClick = {
                                    showOptionsMenu = false
                                    onNavigateToInstructions()
                                },
                                leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                onClick = {
                                    showOptionsMenu = false
                                    onNavigateToSettings()
                                },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            MessageComposer(
                text = uiState.inputText,
                onTextChanged = viewModel::onInputTextChanged,
                attachments = uiState.pendingAttachments,
                onRemoveAttachment = viewModel::removeAttachment,
                onAttachClick = { showAttachmentSheet = true },
                onSendClick = viewModel::sendMessage,
                onStopClick = viewModel::stopGeneration,
                isGenerating = uiState.isGenerating,
                sendOnEnter = uiState.sendOnEnter
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.messages.isEmpty()) {
                // Empty State with Starter Prompts
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterVertically,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "ChatNova AI",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Powered by OpenRouter & Ox Alpha (GLM-5.3)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Suggestion Chips
                    val suggestions = listOf(
                        "Explain Ox Alpha (GLM-5.3) architecture",
                        "Write a Kotlin Jetpack Compose animation",
                        "Optimize a database query with indexing",
                        "Analyze an attached file or code snippet"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestions.forEach { prompt ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                onClick = {
                                    viewModel.onInputTextChanged(prompt)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = prompt,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(
                        items = uiState.messages,
                        key = { it.id }
                    ) { message ->
                        MessageItemView(
                            message = message,
                            enableMarkdown = uiState.enableMarkdown,
                            showTimestamps = uiState.showTimestamps,
                            onRetry = viewModel::retryLastMessage,
                            onDelete = { viewModel.deleteMessage(message.id) }
                        )
                    }
                }
            }

            // Error Banner
            if (uiState.errorMessage != null) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = viewModel::clearErrorMessage) {
                            Text("Dismiss", color = MaterialTheme.colorScheme.inversePrimary)
                        }
                    }
                ) {
                    Text(uiState.errorMessage ?: "")
                }
            }

            // Attachment Picker Modal Sheet
            if (showAttachmentSheet) {
                AttachmentPickerSheet(
                    onPickImages = { imagePickerLauncher.launch("image/*") },
                    onPickVideos = { videoPickerLauncher.launch("video/*") },
                    onPickDocuments = { docPickerLauncher.launch("*/*") },
                    onDismiss = { showAttachmentSheet = false }
                )
            }
        }
    }
}
