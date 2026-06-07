package com.segundoserrano.supertask.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.segundoserrano.supertask.R
import com.segundoserrano.supertask.data.Group
import com.segundoserrano.supertask.viewmodel.TaskViewModel
import kotlinx.coroutines.launch

@Composable
fun ManageGroupsScreen(
    viewModel: TaskViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val allGroups by viewModel.allGroups.collectAsState()
    val scope = rememberCoroutineScope()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var groupToDelete by remember { mutableStateOf<Group?>(null) }
    var taskCountForDelete by remember { mutableStateOf(0) }
    var showNewGroupDialog by remember { mutableStateOf(false) }
    var groupToEdit by remember { mutableStateOf<Group?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onNavigateBack != null) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = if (onNavigateBack == null) 20.dp else 4.dp)
                    .size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.manage_groups),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 28.dp, bottom = 16.dp)
            )
            if (onNavigateBack != null) {
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onNavigateBack) {
                    Text(
                        text = stringResource(R.string.done),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Lista de grupos
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            items(allGroups, key = { it.id }) { group ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(group.colorHex)))
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = group.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            var currentTaskCount by remember { mutableStateOf(0) }
                            LaunchedEffect(group.id) {
                                currentTaskCount = viewModel.countPendingTasksByGroup(group.id)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.tasks_count, currentTaskCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Pin
                        IconButton(
                            onClick = {
                                if (group.isPinned) viewModel.unpinGroup()
                                else viewModel.pinGroup(group.id)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = stringResource(
                                    if (group.isPinned) R.string.unpin_group else R.string.pin_group
                                ),
                                tint = if (group.isPinned)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Editar
                        IconButton(
                            onClick = { groupToEdit = group },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.edit_group),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Eliminar
                        IconButton(
                            onClick = {
                                scope.launch {
                                    groupToDelete = group
                                    taskCountForDelete = viewModel.countPendingTasksByGroup(group.id)
                                    showDeleteDialog = true
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }

        Button(
            onClick = { showNewGroupDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.create_new_group),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }

    // Delete Dialog
    if (showDeleteDialog && groupToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_group_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.delete_group_message, taskCountForDelete))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        groupToDelete?.let { viewModel.deleteGroup(it, deleteTasksToo = true) }
                        showDeleteDialog = false
                        groupToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.delete_group_and_tasks))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Edit Group Dialog
    groupToEdit?.let { group ->
        val availableColors = listOf(
            "#4A90E2", "#FF8C42", "#9B59B6", "#E91E63",
            "#FFC107", "#00BCD4", "#F44336", "#4CAF50"
        )
        var editName by remember(group.id) { mutableStateOf(group.name) }
        var editColor by remember(group.id) { mutableStateOf(group.colorHex) }

        AlertDialog(
            onDismissRequest = { groupToEdit = null },
            title = { Text(stringResource(R.string.edit_group)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text(stringResource(R.string.name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Color",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableColors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(color)))
                                    .clickable { editColor = color },
                                contentAlignment = Alignment.Center
                            ) {
                                if (editColor == color) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editName.isNotBlank()) {
                            viewModel.updateGroup(group.copy(name = editName, colorHex = editColor))
                            groupToEdit = null
                        }
                    },
                    enabled = editName.isNotBlank()
                ) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { groupToEdit = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // New Group Dialog
    if (showNewGroupDialog) {
        var groupName by remember { mutableStateOf("") }
        val availableColors = listOf(
            "#4A90E2", "#FF8C42", "#9B59B6", "#E91E63",
            "#FFC107", "#00BCD4", "#F44336", "#4CAF50"
        )
        var selectedColor by remember { mutableStateOf(availableColors[0]) }

        AlertDialog(
            onDismissRequest = { showNewGroupDialog = false },
            title = { Text(stringResource(R.string.create_new_group)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text(stringResource(R.string.name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Color",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableColors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(color)))
                                    .clickable { selectedColor = color },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedColor == color) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (groupName.isNotBlank()) {
                            viewModel.insertGroup(
                                Group(name = groupName, colorHex = selectedColor, position = allGroups.size)
                            )
                            showNewGroupDialog = false
                        }
                    },
                    enabled = groupName.isNotBlank()
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewGroupDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
