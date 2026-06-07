package com.segundoserrano.supertask.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.segundoserrano.supertask.R
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign

@Composable
fun SplashScreen(
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current
    var startAnimation by remember { mutableStateOf(false) }

    // Animación del ícono
    val iconScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iconScale"
    )

    val iconAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "iconAlpha"
    )

    // Animación del texto
    val textAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1200, delayMillis = 500),
        label = "textAlpha"
    )

    // Animación de la barra de progreso
    val progressAnimation by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 4000, delayMillis = 800),
        label = "progress"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(5000) // Duración total del splash
        onNavigateToHome()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Ícono con fondo circular
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(iconScale)
                    .alpha(iconAlpha)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.background
                            )
                        ),
                        shape = RoundedCornerShape(30.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Super Task Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(70.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Título "Super Task"
            Text(
                text = context.getString(R.string.splash_title),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.alpha(textAlpha)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtítulo con palabra destacada
            val subtitleText = context.getString(R.string.splash_subtitle)
            val parts = subtitleText.split("effortlessly", "sin esfuerzo")
            Text(
                text = buildAnnotatedString {
                    append(parts[0])
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        if (subtitleText.contains("effortlessly")) {
                            append("effortlessly")
                        } else {
                            append("sin esfuerzo")
                        }
                    }
                    append(".")
                },
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.alpha(textAlpha)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Texto descriptivo
            val descriptionText = context.getString(R.string.splash_description)
            val boldWords = listOf("tasks", "tareas", "groups", "grupos", "calendar", "calendario", "notifications", "notificaciones")

            Text(
                text = buildAnnotatedString {
                    var currentText = descriptionText
                    var lastIndex = 0

                    boldWords.forEach { word ->
                        val index = currentText.indexOf(word, lastIndex, ignoreCase = true)
                        if (index != -1) {
                            append(currentText.substring(lastIndex, index))
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(currentText.substring(index, index + word.length))
                            }
                            lastIndex = index + word.length
                        }
                    }
                    append(currentText.substring(lastIndex))
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier
                    .padding(horizontal = 40.dp)
                    .alpha(textAlpha),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Barra de progreso
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(4.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(2.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressAnimation)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(2.dp)
                        )
                )
            }
            // Versión - NUEVO
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = stringResource(R.string.version, "2026.04.26.00"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                modifier = Modifier.alpha(textAlpha),
                textAlign = TextAlign.Center
            )
        }
    }
}