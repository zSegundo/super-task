package com.segundoserrano.supertask.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.segundoserrano.supertask.data.Group
import com.segundoserrano.supertask.data.Task
import kotlinx.coroutines.delay

@Composable
fun TaskCard(
    task: Task,
    group: Group?,
    showGroupLabel: Boolean = true,
    onComplete: () -> Unit,
    onClick: () -> Unit
) {
    var isCompleting by remember { mutableStateOf(false) }
    var showCheckmark by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(true) }

    LaunchedEffect(isCompleting) {
        if (isCompleting) {
            showCheckmark = true
            delay(500)
            isVisible = false
            delay(400)
            onComplete()
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        exit = fadeOut(tween(300)) + shrinkVertically(tween(400))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isCompleting) { onClick() }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (!isCompleting) {
                        isCompleting = true
                    }
                },
                modifier = Modifier.size(28.dp),
                enabled = !isCompleting
            ) {
                Icon(
                    imageVector = if (showCheckmark) {
                        Icons.Filled.CheckCircle
                    } else {
                        Icons.Outlined.Circle
                    },
                    contentDescription = if (showCheckmark) "Completed" else "Not completed",
                    tint = if (showCheckmark) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (isCompleting) TextDecoration.LineThrough else TextDecoration.None,
                modifier = Modifier.weight(1f)
            )

            if (showGroupLabel) group?.let {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(
                            color = Color(android.graphics.Color.parseColor(it.colorHex)),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = it.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}
