package com.example.sd_smart_parking_app.ui.screens.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sd_smart_parking_app.data.model.ParkingNote
import com.example.sd_smart_parking_app.ui.theme.BackgroundLightGray
import com.example.sd_smart_parking_app.ui.theme.BackgroundWhite
import com.example.sd_smart_parking_app.ui.theme.CornerRadius
import com.example.sd_smart_parking_app.ui.theme.Spacing
import com.example.sd_smart_parking_app.ui.theme.Typography
import com.example.sd_smart_parking_app.ui.theme.WarningYellow
import com.example.sd_smart_parking_app.viewmodel.ParkingNotesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkingNotesScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: ParkingNotesViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ParkingNotesViewModel(context) as T
            }
        }
    )

    val uiState by viewModel.uiState.collectAsState()
    var newNoteText by remember { mutableStateOf("") }
    var selectedFloor by remember { mutableStateOf(0) }

    LaunchedEffect(uiState.submitSuccess) {
        if (uiState.submitSuccess) {
            newNoteText = ""
            viewModel.resetSubmitSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Parking Notes",
                        style = Typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WarningYellow
                ),
                actions = {
                    FilterChip(
                        selected = uiState.showOnlyMyNotes,
                        onClick = { viewModel.toggleShowOnlyMyNotes() },
                        label = {
                            Text(
                                text = if (uiState.showOnlyMyNotes) "My notes" else "All notes",
                                style = Typography.bodySmall
                            )
                        },
                        modifier = Modifier.padding(end = Spacing.sm)
                    )
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundWhite)
                .padding(paddingValues)
        ) {
            // Lista de notas
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = WarningYellow)
                    }
                }
                uiState.notes.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            Text(text = "💬", fontSize = 48.sp)
                            Text(
                                text = "No notes yet",
                                style = Typography.headlineSmall
                            )
                            Text(
                                text = "Be the first to leave a note!",
                                style = Typography.bodySmall
                            )
                        }
                    }
                }
                else -> {
                    val displayedNotes = if (uiState.showOnlyMyNotes) {
                        uiState.notes.filter { it.userEmail == viewModel.getCurrentUserEmail() }
                    } else {
                        uiState.notes
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        item { Spacer(modifier = Modifier.height(Spacing.sm)) }
                        items(displayedNotes) { note ->
                            ParkingNoteItem(
                                note = note,
                                isCurrentUser = note.userEmail == viewModel.getCurrentUserEmail(),
                                timeAgo = viewModel.formatTimestamp(note.timestamp)
                            )
                        }
                        item { Spacer(modifier = Modifier.height(Spacing.sm)) }
                    }
                }
            }

            // Input para nueva nota
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = CornerRadius.lg, topEnd = CornerRadius.lg),
                colors = CardDefaults.cardColors(containerColor = BackgroundLightGray),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(Spacing.md)) {

                    // Selector de piso
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        modifier = Modifier.padding(bottom = Spacing.sm)
                    ) {
                        Text(
                            text = "Floor:",
                            style = Typography.bodySmall,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        listOf(0, 1, 2, 3, 4).forEach { floor ->
                            FilterChip(
                                selected = selectedFloor == floor,
                                onClick = { selectedFloor = floor },
                                label = {
                                    Text(
                                        text = if (floor == 0) "All" else "$floor",
                                        style = Typography.bodySmall
                                    )
                                }
                            )
                        }
                    }

                    // Campo de texto y botón enviar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        OutlinedTextField(
                            value = newNoteText,
                            onValueChange = { newNoteText = it },
                            placeholder = {
                                Text(
                                    text = "Write a note about the parking...",
                                    style = Typography.bodySmall
                                )
                            },
                            modifier = Modifier.weight(1f),
                            maxLines = 3,
                            shape = RoundedCornerShape(CornerRadius.md)
                        )

                        IconButton(
                            onClick = {
                                if (newNoteText.isNotBlank()) {
                                    viewModel.addNote(newNoteText, selectedFloor)
                                }
                            },
                            enabled = newNoteText.isNotBlank() && !uiState.isSubmitting,
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = if (newNoteText.isNotBlank()) WarningYellow else Color.Gray,
                                    shape = CircleShape
                                )
                        ) {
                            if (uiState.isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send note",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ParkingNoteItem(
    note: ParkingNote,
    isCurrentUser: Boolean,
    timeAgo: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) WarningYellow.copy(alpha = 0.2f) else BackgroundLightGray
        ),
        shape = RoundedCornerShape(CornerRadius.md)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = if (isCurrentUser) WarningYellow else Color.Gray.copy(alpha = 0.3f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = note.userName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            style = Typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrentUser) Color.Black else Color.DarkGray
                        )
                    }

                    Column {
                        Text(
                            text = if (isCurrentUser) "You" else note.userName,
                            style = Typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (note.floor > 0) {
                            Text(
                                text = "Floor ${note.floor}",
                                style = Typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (note.isLocal) {
                        Text(
                            text = "⏳",
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = timeAgo,
                        style = Typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text = note.message,
                style = Typography.bodyMedium
            )
        }
    }
}