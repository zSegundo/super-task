package com.segundoserrano.supertask

import android.app.Application
import android.app.AlarmManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.segundoserrano.supertask.utils.LanguageManager
import com.segundoserrano.supertask.utils.NotificationHelper

class SuperTaskApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Aplicar idioma guardado
        LanguageManager.applyLanguage(this)

        // Crear canal de notificaciones
        NotificationHelper.createNotificationChannel(this)

        // Programar notificaciones diarias
        NotificationHelper.scheduleDailyNotification(this)

        // Para Android 12+: verificar permiso de alarmas exactas
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w("SuperTaskApp", "No se puede programar alarmas exactas. El usuario debe habilitarlo manualmente.")
            }
        }
    }
}