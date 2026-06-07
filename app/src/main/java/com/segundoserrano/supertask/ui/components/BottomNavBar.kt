package com.segundoserrano.supertask.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.segundoserrano.supertask.R
import com.segundoserrano.supertask.Screen

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val labelResId: Int
)

@Composable
fun BottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        BottomNavItem(
            route = Screen.Home.route,
            icon = Icons.Filled.CheckCircle,
            labelResId = R.string.nav_tasks
        ),
        BottomNavItem(
            route = Screen.Calendar.route,
            icon = Icons.Filled.CalendarToday,
            labelResId = R.string.nav_calendar
        ),
        BottomNavItem(
            route = Screen.Groups.route,
            icon = Icons.Filled.Folder,
            labelResId = R.string.nav_groups
        ),
        BottomNavItem(
            route = Screen.Notes.route,
            icon = Icons.Filled.Note,
            labelResId = R.string.nav_notes
        ),
        BottomNavItem(
            route = Screen.Settings.route,
            icon = Icons.Filled.Settings,
            labelResId = R.string.nav_settings
        )
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = stringResource(item.labelResId)
                    )
                },
                label = {
                    Text(
                        text = stringResource(item.labelResId),
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}