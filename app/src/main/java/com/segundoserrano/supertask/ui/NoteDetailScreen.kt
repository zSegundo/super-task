package com.segundoserrano.supertask.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.segundoserrano.supertask.R
import com.segundoserrano.supertask.data.Note
import com.segundoserrano.supertask.data.NoteImage
import com.segundoserrano.supertask.viewmodel.NoteViewModel
import com.segundoserrano.supertask.viewmodel.TaskViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    viewModel: NoteViewModel,
    taskViewModel: TaskViewModel,
    noteId: Long?,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allGroups by taskViewModel.allGroups.collectAsState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedGroupId by remember { mutableStateOf<Long?>(null) }
    var loadedNote by remember { mutableStateOf<Note?>(null) }
    var existingImages by remember { mutableStateOf<List<NoteImage>>(emptyList()) }
    var showGroupPicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showImageSourceDialog by remember { mutableStateOf(false) }

    // Camera temp file – created before launching the camera
    var cameraFile by remember { mutableStateOf<File?>(null) }

    val imageCount = existingImages.size

    // Load note data when editing
    LaunchedEffect(noteId) {
        if (noteId != null) {
            viewModel.getNoteById(noteId)?.let { note ->
                loadedNote = note
                title = note.title
                description = note.description
                selectedGroupId = note.groupId
            }
            viewModel.getImagesForNote(noteId).collect { images ->
                existingImages = images
            }
        }
    }

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val targetNoteId = noteId ?: run {
                    // Save the note first to get an ID
                    val note = Note(
                        title = title,
                        description = description,
                        groupId = selectedGroupId
                    )
                    val newId = viewModel.saveNote(note)
                    loadedNote = note.copy(id = newId)
                    newId
                }
                viewModel.saveImageFromUri(targetNoteId, it)?.let { img ->
                    existingImages = existingImages + img
                }
            }
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val file = cameraFile ?: return@rememberLauncherForActivityResult
            scope.launch {
                val targetNoteId = noteId ?: run {
                    val note = Note(
                        title = title,
                        description = description,
                        groupId = selectedGroupId
                    )
                    val newId = viewModel.saveNote(note)
                    loadedNote = note.copy(id = newId)
                    newId
                }
                viewModel.saveTempCameraImage(targetNoteId, file)?.let { img ->
                    existingImages = existingImages + img
                }
                cameraFile = null
            }
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val tempFile = viewModel.createTempCameraFile()
            cameraFile = tempFile
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            cameraLauncher.launch(uri)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_note_title)) },
            text = { Text(stringResource(R.string.delete_note_message)) },
            confirmButton = {
                TextButton(onClick = {
                    loadedNote?.let { viewModel.deleteNote(it) }
                    showDeleteDialog = false
                    onNavigateBack()
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text(stringResource(R.string.add_image)) },
            text = { Text(stringResource(R.string.add_image_source)) },
            confirmButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                }) { Text(stringResource(R.string.image_source_camera)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                    galleryLauncher.launch("image/*")
                }) { Text(stringResource(R.string.image_source_gallery)) }
            }
        )
    }

    if (showGroupPicker) {
        AlertDialog(
            onDismissRequest = { showGroupPicker = false },
            title = { Text(stringResource(R.string.group)) },
            text = {
                Column {
                    // No group option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedGroupId = null
                                showGroupPicker = false
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = selectedGroupId == null,
                            onClick = { selectedGroupId = null; showGroupPicker = false }
                        )
                        Text(stringResource(R.string.no_group))
                    }
                    allGroups.forEach { group ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedGroupId = group.id
                                    showGroupPicker = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = selectedGroupId == group.id,
                                onClick = { selectedGroupId = group.id; showGroupPicker = false }
                            )
                            Text(group.name)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (noteId == null) stringResource(R.string.new_note)
                               else stringResource(R.string.edit_note),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (loadedNote != null || title.isNotBlank() || description.isNotBlank()) {
                        val shareChooserTitle = stringResource(R.string.share_note_chooser)
                        IconButton(onClick = {
                            val text = buildString {
                                if (title.isNotBlank()) {
                                    append(title)
                                    append("\n\n")
                                }
                                append(description)
                            }.trim()

                            val imageUris = existingImages.mapNotNull { img ->
                                runCatching {
                                    FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        java.io.File(img.imagePath)
                                    )
                                }.getOrNull()
                            }

                            val intent = if (imageUris.isNotEmpty()) {
                                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                    type = "image/*"
                                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(imageUris))
                                    putExtra(Intent.EXTRA_TEXT, text)
                                    if (title.isNotBlank()) putExtra(Intent.EXTRA_SUBJECT, title)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                            } else {
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                    if (title.isNotBlank()) putExtra(Intent.EXTRA_SUBJECT, title)
                                }
                            }
                            context.startActivity(Intent.createChooser(intent, shareChooserTitle))
                        }) {
                            Icon(
                                Icons.Filled.Share,
                                contentDescription = shareChooserTitle
                            )
                        }
                    }
                    if (loadedNote != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                val note = if (loadedNote != null) {
                                    loadedNote!!.copy(
                                        title = title,
                                        description = description,
                                        groupId = selectedGroupId
                                    )
                                } else {
                                    Note(
                                        title = title,
                                        description = description,
                                        groupId = selectedGroupId
                                    )
                                }
                                viewModel.saveNote(note)
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = stringResource(R.string.save_note)
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text(stringResource(R.string.note_title_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(10.dp)
            )

            // Group selector
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showGroupPicker = true },
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = allGroups.find { it.id == selectedGroupId }?.name
                            ?: stringResource(R.string.no_group),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text(stringResource(R.string.note_description_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp),
                minLines = 6,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(10.dp)
            )

            // Images section
            if (existingImages.isNotEmpty() || imageCount < 4) {
                Text(
                    text = stringResource(R.string.note_images),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                NoteImagesGrid(
                    images = existingImages,
                    canAddMore = imageCount < 4,
                    onAddImage = { showImageSourceDialog = true },
                    onDeleteImage = { image -> viewModel.deleteImage(image) }
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun NoteImagesGrid(
    images: List<NoteImage>,
    canAddMore: Boolean,
    onAddImage: () -> Unit,
    onDeleteImage: (NoteImage) -> Unit
) {
    val cellSize = 100.dp

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        images.forEach { image ->
            Box(
                modifier = Modifier
                    .size(cellSize)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        RoundedCornerShape(8.dp)
                    )
            ) {
                AsyncImage(
                    model = File(image.imagePath),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Delete button
                IconButton(
                    onClick = { onDeleteImage(image) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp)
                        .padding(4.dp)
                        .background(
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                            RoundedCornerShape(50)
                        )
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        if (canAddMore) {
            Box(
                modifier = Modifier
                    .size(cellSize)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onAddImage() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.AddPhotoAlternate,
                    contentDescription = stringResource(R.string.add_image),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
