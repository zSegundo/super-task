package com.segundoserrano.supertask.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import com.segundoserrano.supertask.ui.theme.LightBorderColor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.segundoserrano.supertask.R
import com.segundoserrano.supertask.data.BackupData
import com.segundoserrano.supertask.viewmodel.SettingsViewModel
import com.segundoserrano.supertask.viewmodel.TaskViewModel
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    taskViewModel: TaskViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val userName by viewModel.userName.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val language by viewModel.language.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()

    var showNameDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showConflictDialog by remember { mutableStateOf(false) }
    var pendingBackup by remember { mutableStateOf<BackupData?>(null) }
    var conflictCount by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val json = taskViewModel.exportData()
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            }
            Toast.makeText(context, context.getString(R.string.export_success), Toast.LENGTH_SHORT).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val json = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
            } ?: return@launch
            val backup = try {
                Gson().fromJson(json, BackupData::class.java)
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.import_invalid_file), Toast.LENGTH_SHORT).show()
                return@launch
            }
            val conflicts = taskViewModel.getConflictingTaskIds(backup)
            if (conflicts.isEmpty()) {
                taskViewModel.importData(backup, false)
                Toast.makeText(context, context.getString(R.string.import_success, backup.tasks.size), Toast.LENGTH_SHORT).show()
            } else {
                pendingBackup = backup
                conflictCount = conflicts.size
                showConflictDialog = true
            }
        }
    }

    // Función para abrir URL en el navegador
    fun openUrl(url: String) {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
        context.startActivity(intent)
    }

    // Función para compartir la app con imagen
    fun shareApp() {
        try {
            val appUrl = "https://play.google.com/store/apps/details?id=com.segundoserrano.supertask"
            val shareText = context.getString(R.string.share_app_text, appUrl)

            // Cargar la imagen del drawable
            val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.share_banner)

            // Guardar la imagen temporalmente
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "share_banner.png")
            val fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.close()

            // Obtener URI usando FileProvider
            val imageUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            // Crear el intent para compartir
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_app_subject))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(
                Intent.createChooser(shareIntent, context.getString(R.string.share_app_title))
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: compartir solo texto si falla la imagen
            val appUrl = "https://play.google.com/store/apps/details?id=com.segundoserrano.supertask"
            val shareText = context.getString(R.string.share_app_text, appUrl)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_app_subject))
                putExtra(Intent.EXTRA_TEXT, shareText)
            }

            context.startActivity(
                Intent.createChooser(shareIntent, context.getString(R.string.share_app_title))
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // NAME Section
        SectionHeader(stringResource(R.string.name))
        SettingItem(
            icon = Icons.Default.Person,
            label = stringResource(R.string.name),
            value = userName,
            onClick = { showNameDialog = true }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // THEME Section
        SectionHeader(stringResource(R.string.theme))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ThemeButton(
                label = stringResource(R.string.theme_normal),
                selected = !isDarkTheme,
                onClick = { viewModel.setDarkTheme(false) },
                modifier = Modifier.weight(1f)
            )
            ThemeButton(
                label = stringResource(R.string.theme_dark),
                selected = isDarkTheme,
                onClick = { viewModel.setDarkTheme(true) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // NOTIFICATIONS Section
        SectionHeader(stringResource(R.string.notifications))
        SettingItemWithSwitch(
            icon = Icons.Default.Notifications,
            label = stringResource(R.string.push_notifications),
            checked = notificationsEnabled,
            onCheckedChange = { viewModel.setNotificationsEnabled(it) }
        )

        // Botón de prueba de notificaciones
        if (notificationsEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Button(
                    onClick = {
                        com.segundoserrano.supertask.utils.NotificationHelper.showTestNotification(context)
                        val intent = Intent(context, com.segundoserrano.supertask.utils.DailyTaskReceiver::class.java)
                        context.sendBroadcast(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Notification | Alarm")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // GENERAL Section
        SectionHeader(stringResource(R.string.general))
        SettingItem(
            icon = Icons.Default.Language,
            label = stringResource(R.string.language),
            value = if (language == "en") {
                stringResource(R.string.language_english)
            } else {
                stringResource(R.string.language_spanish)
            },
            onClick = { showLanguageDialog = true }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // DATA Section
        SectionHeader(stringResource(R.string.data_section))
        SettingItem(
            icon = Icons.Default.Upload,
            label = stringResource(R.string.export_tasks),
            value = "",
            onClick = {
                exportLauncher.launch("supertask_${LocalDate.now()}.json")
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingItem(
            icon = Icons.Default.Download,
            label = stringResource(R.string.import_tasks),
            value = "",
            onClick = {
                importLauncher.launch(arrayOf("application/json", "*/*"))
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // SUPPORT Section
        SectionHeader(stringResource(R.string.support))
        SettingItem(
            icon = Icons.Default.PrivacyTip,
            label = stringResource(R.string.privacy_policy),
            value = "",
            onClick = { openUrl("https://zzerrano.com/privacy_policy.html") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Share App
        SettingItem(
            icon = Icons.Default.Share,
            label = stringResource(R.string.share_app),
            value = "",
            onClick = { shareApp() }
        )

        Spacer(modifier = Modifier.height(40.dp))
    }

    // Conflict Dialog
    if (showConflictDialog) {
        AlertDialog(
            onDismissRequest = { showConflictDialog = false },
            title = { Text(stringResource(R.string.import_conflict_title)) },
            text = { Text(stringResource(R.string.import_conflict_message, conflictCount)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingBackup?.let { backup ->
                        taskViewModel.importData(backup, true)
                        Toast.makeText(context, context.getString(R.string.import_success, backup.tasks.size), Toast.LENGTH_SHORT).show()
                    }
                    showConflictDialog = false
                }) { Text(stringResource(R.string.import_overwrite)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingBackup?.let { backup ->
                        taskViewModel.importData(backup, false)
                        Toast.makeText(context, context.getString(R.string.import_success, backup.tasks.size - conflictCount), Toast.LENGTH_SHORT).show()
                    }
                    showConflictDialog = false
                }) { Text(stringResource(R.string.import_keep_mine)) }
            }
        )
    }

    // Name Dialog
    if (showNameDialog) {
        var newName by remember { mutableStateOf(userName) }

        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text(stringResource(R.string.name)) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.setUserName(newName)
                            showNameDialog = false
                        }
                    },
                    enabled = newName.isNotBlank()
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Language Dialog
    if (showLanguageDialog) {
        var tempLanguage by remember { mutableStateOf(language) }

        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.language)) },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                tempLanguage = "en"
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = tempLanguage == "en",
                            onClick = {
                                tempLanguage = "en"
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(R.string.language_english))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                tempLanguage = "es"
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = tempLanguage == "es",
                            onClick = {
                                tempLanguage = "es"
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(R.string.language_spanish))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (tempLanguage != language) {
                        // Guardar el idioma
                        viewModel.setLanguage(tempLanguage)
                        showLanguageDialog = false

                        // NO necesitas recreate() porque AppCompatDelegate lo maneja automáticamente
                        // El cambio se aplicará inmediatamente
                    } else {
                        showLanguageDialog = false
                    }
                }) {
                    Text(stringResource(R.string.done))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@Composable
private fun SettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = if (!isDarkTheme) {
            BorderStroke(1.dp, LightBorderColor)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            if (value.isNotEmpty()) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingItemWithSwitch(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = if (!isDarkTheme) {
            BorderStroke(1.dp, LightBorderColor)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@Composable
private fun ThemeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge
        )
    }
}