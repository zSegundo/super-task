package com.segundoserrano.supertask.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.segundoserrano.supertask.R
import com.segundoserrano.supertask.data.RecurrenceType
import com.segundoserrano.supertask.data.Task
import com.segundoserrano.supertask.viewmodel.TaskViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTaskScreen(
    viewModel: TaskViewModel,
    taskId: Long?,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allGroups by viewModel.allGroups.collectAsState()

    var taskTitle by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedGroupId by remember { mutableStateOf<Long?>(
        if (taskId == null) viewModel.selectedGroupId.value ?: allGroups.firstOrNull()?.id else null
    ) }
    var selectedRecurrence by remember { mutableStateOf(RecurrenceType.NEVER) }
    var customInterval by remember { mutableStateOf(1) }
    var recurrenceEndDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showGroupPicker by remember { mutableStateOf(false) }
    var showRecurrencePicker by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var loadedTask by remember { mutableStateOf<Task?>(null) }

    // Cargar tarea si estamos editando
    LaunchedEffect(taskId) {
        taskId?.let {
            viewModel.getTaskById(it)?.let { task ->
                loadedTask = task
                taskTitle = task.title
                selectedDate = task.dueDate
                selectedGroupId = task.groupId
                selectedRecurrence = task.recurrenceType
                customInterval = task.recurrenceInterval
                recurrenceEndDate = task.recurrenceEndDate
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (taskId == null) {
                            stringResource(R.string.new_task)
                        } else {
                            stringResource(R.string.edit_task)
                        },
                        style = MaterialTheme.typography.displayMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = onNavigateBack,
                        enabled = !isLoading
                    ) {
                        Text(
                            text = stringResource(R.string.back),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Título de la tarea
            OutlinedTextField(
                value = taskTitle,
                onValueChange = { taskTitle = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(R.string.task_title_hint),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                textStyle = MaterialTheme.typography.displayMedium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.background,
                    unfocusedBorderColor = MaterialTheme.colorScheme.background,
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background
                ),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Due Date
            SettingItem(
                icon = Icons.Default.CalendarToday,
                label = stringResource(R.string.date),
                value = selectedDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                onClick = { showDatePicker = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Group
            SettingItem(
                icon = Icons.Default.Folder,
                label = stringResource(R.string.group),
                value = allGroups.find { it.id == selectedGroupId }?.name
                    ?: stringResource(R.string.group_personal),
                onClick = { showGroupPicker = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Recurrence
            SettingItem(
                icon = Icons.Default.Refresh,
                label = stringResource(R.string.recurrence),
                value = getRecurrenceLabel(context, selectedRecurrence),
                onClick = { showRecurrencePicker = true }
            )

            // Campo de intervalo personalizado
            if (selectedRecurrence == RecurrenceType.CUSTOM) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = stringResource(R.string.repeat_every),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    // Botón -
                    IconButton(
                        onClick = { if (customInterval > 1) customInterval-- },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Número
                    Text(
                        text = customInterval.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // Botón +
                    IconButton(
                        onClick = { if (customInterval < 365) customInterval++ },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (customInterval == 1)
                            stringResource(R.string.day)
                        else
                            stringResource(R.string.days),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Fecha de finalización (para todas las recurrencias excepto NEVER)
            if (selectedRecurrence != RecurrenceType.NEVER) {
                Spacer(modifier = Modifier.height(16.dp))

                SettingItem(
                    icon = Icons.Default.Event,
                    label = stringResource(R.string.end_date),
                    value = recurrenceEndDate?.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                        ?: stringResource(R.string.no_end_date),
                    onClick = { showEndDatePicker = true }
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Save / Delete Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Delete Button (solo al editar)
                if (taskId != null) {
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(56.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                            )
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete_task_title),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Save Button
                Button(
                    onClick = {
                        if (taskTitle.isNotBlank()) {
                            isLoading = true
                            scope.launch {
                                val task = Task(
                                    id = taskId ?: 0,
                                    title = taskTitle,
                                    dueDate = selectedDate,
                                    groupId = selectedGroupId,
                                    recurrenceType = selectedRecurrence,
                                    recurrenceInterval = if (selectedRecurrence == RecurrenceType.CUSTOM) customInterval else 1,
                                    recurrenceEndDate = if (selectedRecurrence != RecurrenceType.NEVER) recurrenceEndDate else null
                                )
                                if (taskId == null) {
                                    viewModel.insertTask(task)
                                } else {
                                    viewModel.updateTask(task)
                                }
                                onNavigateBack()
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    enabled = taskTitle.isNotBlank() && !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.save_task),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        val isRecurring = loadedTask?.recurrenceType != RecurrenceType.NEVER
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_task_title)) },
            text = {
                Text(
                    stringResource(
                        if (isRecurring) R.string.delete_recurring_task_message
                        else R.string.delete_task_message
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        loadedTask?.let { viewModel.deleteTask(it) }
                        showDeleteDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toEpochDay() * 24 * 60 * 60 * 1000
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Group Picker Dialog
    if (showGroupPicker) {
        AlertDialog(
            onDismissRequest = { showGroupPicker = false },
            title = { Text(stringResource(R.string.group)) },
            text = {
                Column {
                    allGroups.forEach { group ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedGroupId = group.id
                                    showGroupPicker = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedGroupId == group.id,
                                onClick = {
                                    selectedGroupId = group.id
                                    showGroupPicker = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = group.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGroupPicker = false }) {
                    Text(stringResource(R.string.done))
                }
            }
        )
    }

    // Recurrence Picker Dialog
    if (showRecurrencePicker) {
        AlertDialog(
            onDismissRequest = { showRecurrencePicker = false },
            title = { Text(stringResource(R.string.recurrence)) },
            text = {
                Column {
                    RecurrenceType.values().forEach { type ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedRecurrence = type
                                    showRecurrencePicker = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedRecurrence == type,
                                onClick = {
                                    selectedRecurrence = type
                                    showRecurrencePicker = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = getRecurrenceLabel(context, type))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRecurrencePicker = false }) {
                    Text(stringResource(R.string.done))
                }
            }
        )
    }

    // End Date Picker Dialog
    if (showEndDatePicker) {
        val endDatePickerState = rememberDatePickerState(
            initialSelectedDateMillis = (recurrenceEndDate ?: selectedDate.plusMonths(3))
                .toEpochDay() * 24 * 60 * 60 * 1000
        )

        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDatePickerState.selectedDateMillis?.let { millis ->
                        recurrenceEndDate = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                    }
                    showEndDatePicker = false
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        recurrenceEndDate = null
                        showEndDatePicker = false
                    }) {
                        Text(stringResource(R.string.clear))
                    }
                    TextButton(onClick = { showEndDatePicker = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        ) {
            DatePicker(state = endDatePickerState)
        }
    }
}

@Composable
private fun SettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }

        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun getRecurrenceLabel(context: android.content.Context, type: RecurrenceType): String {
    return when (type) {
        RecurrenceType.NEVER -> context.getString(R.string.recurrence_never)
        RecurrenceType.DAILY -> context.getString(R.string.recurrence_daily)
        RecurrenceType.WEEKLY -> context.getString(R.string.recurrence_weekly)
        RecurrenceType.MONTHLY -> context.getString(R.string.recurrence_monthly)
        RecurrenceType.CUSTOM -> context.getString(R.string.recurrence_custom)
    }
}