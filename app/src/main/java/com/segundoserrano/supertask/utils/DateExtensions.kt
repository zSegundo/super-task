package com.segundoserrano.supertask.utils

import android.content.Context
import com.segundoserrano.supertask.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

fun LocalDate.toRelativeDateString(context: Context): String {
    val today = LocalDate.now()
    val daysUntil = ChronoUnit.DAYS.between(today, this).toInt()

    return when (daysUntil) {
        -1 -> context.getString(R.string.date_yesterday)
        0 -> context.getString(R.string.date_today)
        1 -> context.getString(R.string.date_tomorrow)
        in 2..6 -> {
            // Mostrar día de la semana para los próximos 5 días
            this.format(DateTimeFormatter.ofPattern("EEEE"))
        }
        else -> {
            // Para fechas más lejanas, mostrar formato normal
            this.format(DateTimeFormatter.ofPattern("MMM dd"))
        }
    }
}