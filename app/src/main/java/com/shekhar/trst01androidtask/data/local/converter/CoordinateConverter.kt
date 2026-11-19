package com.shekhar.trst01androidtask.data.local.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class LatLngPoint(
    val latitude: Double,
    val longitude: Double
)

class CoordinateConverter {
    private val gson = Gson()
    private val listType = object : TypeToken<List<LatLngPoint>>() {}.type

    @TypeConverter
    fun fromCoordinateList(coordinates: List<LatLngPoint>?): String? {
        if (coordinates == null) return null
        return gson.toJson(coordinates, listType)
    }

    @TypeConverter
    fun toCoordinateList(value: String?): List<LatLngPoint> {
        if (value.isNullOrBlank()) {
            return emptyList()
        }
        return try {
            val list: List<LatLngPoint>? = gson.fromJson(value, listType)
            list ?: emptyList()
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse stored coordinates JSON", e)
        }
    }
}
