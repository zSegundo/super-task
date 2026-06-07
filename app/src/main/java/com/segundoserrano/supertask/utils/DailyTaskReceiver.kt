package com.segundoserrano.supertask.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.segundoserrano.supertask.data.AppDatabase
import com.segundoserrano.supertask.data.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DailyTaskReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("DailyTaskReceiver", "Receiver ejecutándose...")

        // Usar goAsync() para permitir operaciones asíncronas
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("DailyTaskReceiver", "Iniciando procesamiento de tareas...")

                // 1. Generar instancias de tareas recurrentes
                val database = AppDatabase.getDatabase(context)
                val taskDao = database.taskDao()
                val groupDao = database.groupDao()
                val repository = TaskRepository(taskDao, groupDao)

                Log.d("DailyTaskReceiver", "Generando tareas recurrentes...")
                repository.generateRecurringTaskInstances()
                Log.d("DailyTaskReceiver", "Tareas recurrentes generadas exitosamente")

                // 2. Mostrar notificaciones si están habilitadas
                val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)

                if (notificationsEnabled) {
                    Log.d("DailyTaskReceiver", "Mostrando notificaciones...")
                    NotificationHelper.showTaskNotification(context)
                    Log.d("DailyTaskReceiver", "Notificaciones mostradas")
                } else {
                    Log.d("DailyTaskReceiver", "Notificaciones deshabilitadas, solo se generaron tareas")
                }

                // 3. Re-programar la próxima alarma (necesario para Android 12+)
                NotificationHelper.scheduleDailyNotification(context)

            } catch (e: Exception) {
                Log.e("DailyTaskReceiver", "Error procesando tareas diarias", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}