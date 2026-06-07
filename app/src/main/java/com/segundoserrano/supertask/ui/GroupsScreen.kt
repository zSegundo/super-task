package com.segundoserrano.supertask.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import com.segundoserrano.supertask.ui.theme.LightBorderColor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
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
import com.segundoserrano.supertask.ui.components.TaskCard
import com.segundoserrano.supertask.viewmodel.TaskViewModel
import kotlinx.coroutines.launch

@Composable
fun GroupsScreen(
    viewModel: TaskViewModel,
    onNavigateToManageGroups: () -> Unit
) {
    val allGroups by viewModel.allGroups.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedGroup by remember { mutableStateOf<Group?>(null) }
    var taskCounts by remember { mutableStateOf<Map<Long, Int>>(emptyMap()) }

    // Calcular conteos de tareas por grupo
    LaunchedEffect(allGroups) {
        val counts = mutableMapOf<Long, Int>()
        allGroups.forEach { group ->
            counts[group.id] = viewModel.countPendingTasksByGroup(group.id)
        }
        taskCounts = counts
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.groups),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            IconButton(onClick = onNavigateToManageGroups) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Manage groups",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        if (selectedGroup == null) {
            // Vista de todos los grupos
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(allGroups) { group ->
                    GroupCard(
                        group = group,
                        taskCount = taskCounts[group.id] ?: 0,
                        onClick = { selectedGroup = group }
                    )
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        } else {
            // Vista de tareas del grupo seleccionado
            Column(modifier = Modifier.fillMaxSize()) {
                // Header del grupo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedGroup = null }
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(selectedGroup!!.colorHex)))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = selectedGroup!!.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "← ${stringResource(R.string.filter_all)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Tareas del grupo
                val groupTasks = allTasks.filter {
                    it.groupId == selectedGroup!!.id && !it.isCompleted
                }

                if (groupTasks.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_tasks),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(groupTasks) { task ->
                            TaskCard(
                                task = task,
                                group = selectedGroup,
                                onComplete = { viewModel.completeTask(task) },
                                onClick = { }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupCard(
    group: Group,
    taskCount: Int,
    onClick: () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (!isDarkTheme) {
            BorderStroke(1.dp, LightBorderColor)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(group.colorHex)))
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Group info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.tasks_count, taskCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}