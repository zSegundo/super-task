package com.segundoserrano.supertask.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import android.content.Intent
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.segundoserrano.supertask.R
import com.segundoserrano.supertask.data.Group
import com.segundoserrano.supertask.data.Task
import com.segundoserrano.supertask.ui.components.FilterChip
import com.segundoserrano.supertask.ui.components.TaskCard
import com.segundoserrano.supertask.ui.theme.LightBorderColor
import com.segundoserrano.supertask.viewmodel.SettingsViewModel
import com.segundoserrano.supertask.viewmodel.TaskFilter
import com.segundoserrano.supertask.viewmodel.TaskSection
import com.segundoserrano.supertask.viewmodel.TaskViewModel
import androidx.compose.material.icons.filled.CheckCircle
import java.time.LocalTime

@Composable
fun HomeScreen(
    viewModel: TaskViewModel,
    onNavigateToNewTask: (Long?) -> Unit
) {
    val context = LocalContext.current
    val settingsViewModel: SettingsViewModel = viewModel()
    val organizedTasks by viewModel.organizedTasks.collectAsState()
    val allGroups by viewModel.allGroups.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val selectedGroupId by viewModel.selectedGroupId.collectAsState()
    val userName by settingsViewModel.userName.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icono SuperTask como bullet
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(28.dp)
                                .alignByBaseline()
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        // Saludo
                        Text(
                            text = getGreeting(context, userName),
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Botón Compartir (solo si hay tareas visibles)
                    if (organizedTasks.isNotEmpty()) {
                        val shareChooserTitle = stringResource(R.string.share_tasks_chooser)
                        IconButton(onClick = {
                            val text = buildShareText(context, organizedTasks)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(intent, shareChooserTitle))
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = shareChooserTitle,
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                    }
                }


                Spacer(modifier = Modifier.height(24.dp))

                // Filtros
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Filtros por cada grupo (el pineado primero)
                    val sortedGroups = allGroups.sortedByDescending { it.isPinned }
                    sortedGroups.forEach { group ->
                        FilterChip(
                            label = group.name,
                            selected = selectedFilter == TaskFilter.ByGroup && selectedGroupId == group.id,
                            onClick = { viewModel.setSelectedGroup(group.id) }
                        )
                    }
                }
            }

            // Lista de tareas
            if (organizedTasks.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.no_tasks),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.add_first_task),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            } else
            {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    // Today
                    organizedTasks[TaskSection.Today]?.let { tasks ->
                        if (tasks.isNotEmpty()) {
                            item(key = "header_today") {
                                SectionHeader(
                                    title = stringResource(R.string.section_today),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            item(key = "section_today") {
                                TaskSectionCard(
                                    tasks = tasks,
                                    allGroups = allGroups,
                                    showGroupLabel = selectedGroupId == null,
                                    onComplete = { task -> viewModel.completeTask(task) },
                                    onClick = { taskId -> onNavigateToNewTask(taskId) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    // Overdue
                    organizedTasks[TaskSection.Overdue]?.let { tasks ->
                        if (tasks.isNotEmpty()) {
                            item(key = "header_overdue") {
                                SectionHeader(
                                    title = stringResource(R.string.section_overdue),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            item(key = "section_overdue") {
                                TaskSectionCard(
                                    tasks = tasks,
                                    allGroups = allGroups,
                                    showGroupLabel = selectedGroupId == null,
                                    onComplete = { task -> viewModel.completeTask(task) },
                                    onClick = { taskId -> onNavigateToNewTask(taskId) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    // Tomorrow
                    organizedTasks[TaskSection.Tomorrow]?.let { tasks ->
                        if (tasks.isNotEmpty()) {
                            item(key = "header_tomorrow") {
                                SectionHeader(
                                    title = stringResource(R.string.section_tomorrow),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            item(key = "section_tomorrow") {
                                TaskSectionCard(
                                    tasks = tasks,
                                    allGroups = allGroups,
                                    showGroupLabel = selectedGroupId == null,
                                    onComplete = { task -> viewModel.completeTask(task) },
                                    onClick = { taskId -> onNavigateToNewTask(taskId) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    // Upcoming
                    organizedTasks[TaskSection.Upcoming]?.let { tasks ->
                        if (tasks.isNotEmpty()) {
                            item(key = "header_upcoming") {
                                SectionHeader(
                                    title = stringResource(R.string.section_upcoming),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            item(key = "section_upcoming") {
                                TaskSectionCard(
                                    tasks = tasks,
                                    allGroups = allGroups,
                                    showGroupLabel = selectedGroupId == null,
                                    onComplete = { task -> viewModel.completeTask(task) },
                                    onClick = { taskId -> onNavigateToNewTask(taskId) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    // Espaciado final para el FAB
                    item(key = "bottom_spacer") {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { onNavigateToNewTask(null) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add task",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun TaskSectionCard(
    tasks: List<Task>,
    allGroups: List<Group>,
    showGroupLabel: Boolean,
    onComplete: (Task) -> Unit,
    onClick: (Long?) -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (!isDarkTheme) BorderStroke(1.dp, LightBorderColor) else null
    ) {
        Column {
            tasks.forEachIndexed { index, task ->
                key(task.id) {
                    TaskCard(
                        task = task,
                        group = allGroups.find { it.id == task.groupId },
                        showGroupLabel = showGroupLabel,
                        onComplete = { onComplete(task) },
                        onClick = { onClick(task.id) }
                    )
                    if (index < tasks.size - 1) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    color: androidx.compose.ui.graphics.Color
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = color,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

private fun buildShareText(
    context: android.content.Context,
    organizedTasks: Map<TaskSection, List<Task>>
): String {
    val sb = StringBuilder()
    sb.appendLine("✅ SuperTask")
    sb.appendLine()

    val sections = listOf(
        TaskSection.Today    to "📅 ${context.getString(R.string.section_today)}",
        TaskSection.Overdue  to "⚠️ ${context.getString(R.string.section_overdue)}",
        TaskSection.Tomorrow to "📅 ${context.getString(R.string.section_tomorrow)}",
        TaskSection.Upcoming to "📅 ${context.getString(R.string.section_upcoming)}"
    )

    sections.forEach { (section, label) ->
        val tasks = organizedTasks[section]
        if (!tasks.isNullOrEmpty()) {
            sb.appendLine(label)
            tasks.forEach { task -> sb.appendLine("• ${task.title}") }
            sb.appendLine()
        }
    }

    return sb.toString().trimEnd()
}

private fun getGreeting(context: android.content.Context, userName: String): String {
    val hour = LocalTime.now().hour
    val greeting = when (hour) {
        in 0..11 -> context.getString(R.string.good_morning)
        in 12..17 -> context.getString(R.string.good_afternoon)
        else -> context.getString(R.string.good_evening)
    }
    return "$greeting, $userName"
}