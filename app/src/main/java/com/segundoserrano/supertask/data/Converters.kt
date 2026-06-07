package com.segundoserrano.supertask.data

import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? {
        return date?.toString()
    }

    @TypeConverter
    fun toLocalDate(dateString: String?): LocalDate? {
        return dateString?.let { LocalDate.parse(it) }
    }

    @TypeConverter
    fun fromRecurrenceType(type: RecurrenceType): String {
        return type.name
    }

    @TypeConverter
    fun toRecurrenceType(typeString: String): RecurrenceType {
        return RecurrenceType.valueOf(typeString)
    }
}