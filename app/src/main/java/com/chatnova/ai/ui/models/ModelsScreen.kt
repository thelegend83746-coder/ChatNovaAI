package com.chatnova.ai.ui.models

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chatnova.ai.domain.model.AiModel
import com.chatnova.ai.ui.components.EmptyStateView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelsScreen(
    viewModel: ModelsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Models", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::refreshModels,
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Input
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                placeholder = { Text("Search models by name or provider...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModelFilter.values().forEach { filter ->
                    FilterChip(
                        selected = uiState.currentFilter == filter,
                        onClick = { viewModel.onFilterSelected(filter) },
                        label = {
                            Text(
                                when (filter) {
                                    ModelFilter.ALL -> "All Models"
                                    ModelFilter.FREE -> "Free (0 Cost)"
                                    ModelFilter.VISION -> "Vision / Multimodal"
                                    ModelFilter.CODING -> "Coding Models"
                                }
                            )
                        }
                    )
                }
            }

            if (uiState.filteredModels.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Hub,
                    title = "No models found",
                    subtitle = "Try changing search keywords or refresh from OpenRouter",
                    actionButtonText = "Refresh Models",
                    onActionClick = viewModel::refreshModels
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = uiState.filteredModels,
                        key = { it.id }
                    ) { model ->
                        ModelItemCard(
                            model = model,
                            isDefault = model.id == uiState.defaultModelId,
                            onSetDefault = { viewModel.setDefaultModel(model.id) }
                        )
                    }
                }
            }

            // Error Snackbar
            if (uiState.errorMessage != null) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = viewModel::clearErrorMessage) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(uiState.errorMessage ?: "")
                }
            }
        }
    }
}

@Composable
private fun ModelItemCard(
    model: AiModel,
    isDefault: Boolean,
    onSetDefault: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = model.id,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isDefault) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Active Default", color = MaterialTheme.colorScheme.primary) },
                        icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    )
                } else {
                    OutlinedButton(
                        onClick = onSetDefault,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Set Default", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (model.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = model.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text("${model.contextLength / 1000}k Context") }
                )

                if (model.isFree) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Free", color = MaterialTheme.colorScheme.primary) }
                    )
                } else {
                    AssistChip(
                        onClick = {},
                        label = { Text("$${String.format("%.2f", model.promptPrice)}/M tokens") }
                    )
                }

                if (model.hasVision) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Vision") },
                        leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                }
            }
        }
    }
}
