package com.segundoserrano.supertask.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.segundoserrano.supertask.MainActivity
import com.segundoserrano.supertask.R
import com.segundoserrano.supertask.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.*

object NotificationHelper {

    private const val CHANNEL_ID = "task_reminders"
    private const val CHANNEL_NAME = "Task Reminders"
    private const val NOTIFICATION_ID = 1001
    private const val ALARM_REQUEST_CODE = 1001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = context.getString(R.string.notification_channel_description)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleDailyNotification(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, DailyTaskReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Calcular la próxima ejecución a las 6:00 AM
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 6)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Si ya pasaron las 6 AM hoy, programar para mañana
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // Cancelar alarma anterior si existe
        alarmManager.cancel(pendingIntent)

        // Programar alarma
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Para Android 6.0+ usar setExactAndAllowWhileIdle
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            Log.d("NotificationHelper", "Alarma programada para: ${calendar.time}")
        } catch (e: SecurityException) {
            Log.e("NotificationHelper", "No se pudo programar alarma exacta", e)
        }
    }

    fun cancelDailyNotification(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailyTaskReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        Log.d("NotificationHelper", "Alarma cancelada")
    }

    suspend fun showTaskNotification(context: Context) {
        val database = AppDatabase.getDatabase(context)
        val today = LocalDate.now()

        // Obtener tareas de hoy Y atrasadas
        val todayTasks = withContext(Dispatchers.IO) {
            database.taskDao().getPendingTasksForDate(today)
        }

        val overdueTasks = withContext(Dispatchers.IO) {
            database.taskDao().getOverdueTasks(today)
        }

        val todayCount = todayTasks.size
        val overdueCount = overdueTasks.size
        val totalTasks = todayCount + overdueCount

        // Solo mostrar notificación si hay tareas
        if (totalTasks == 0) {
            Log.d("NotificationHelper", "No hay tareas pendientes, no se mostrará notificación")
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val contentText = buildNotificationText(context, todayCount, overdueCount)
        val title = buildNotificationTitle(context, todayCount, overdueCount)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)

        Log.d("NotificationHelper", "Notificación mostrada: $totalTasks tareas")
    }

    private fun buildNotificationTitle(context: Context, todayCount: Int, overdueCount: Int): String {
        val totalTasks = todayCount + overdueCount
        return when {
            totalTasks == 1 -> context.getString(R.string.notification_title_singular)
            else -> context.getString(R.string.notification_title_plural, totalTasks)
        }
    }

    private fun buildNotificationText(context: Context, todayCount: Int, overdueCount: Int): String {
        return when {
            todayCount > 0 && overdueCount > 0 -> {
                context.getString(R.string.notification_body_both, todayCount, overdueCount)
            }
            todayCount > 0 && overdueCount == 0 -> {
                if (todayCount == 1) {
                    context.getString(R.string.notification_body_today_singular)
                } else {
                    context.getString(R.string.notification_body_today_plural, todayCount)
                }
            }
            todayCount == 0 && overdueCount > 0 -> {
                if (overdueCount == 1) {
                    context.getString(R.string.notification_body_overdue_singular)
                } else {
                    context.getString(R.string.notification_body_overdue_plural, overdueCount)
                }
            }
            else -> {
                context.getString(R.string.notification_body_default)
            }
        }
    }

    // Función de prueba
    fun showTestNotification(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Test Notification")
            .setContentText("Las notificaciones están funcionando correctamente!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}