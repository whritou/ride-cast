package com.example.cyclistweather.data.weather

import com.example.cyclistweather.domain.model.WeatherPoint
import org.json.JSONArray
import org.json.JSONObject

internal object WeatherPointJsonCodec {
    fun encode(points: List<WeatherPoint>): String {
        return JSONArray().also { array ->
            points.forEach { point ->
                array.put(
                    JSONObject()
                        .put("latitude", point.latitude)
                        .put("longitude", point.longitude)
                        .put("timeEpochMillis", point.timeEpochMillis)
                        .put("temperatureCelsius", point.temperatureCelsius)
                        .put("apparentTemperatureCelsius", point.apparentTemperatureCelsius ?: JSONObject.NULL)
                        .put("windSpeedKmh", point.windSpeedKmh)
                        .put("windDirectionDegrees", point.windDirectionDegrees)
                        .put("windGustKmh", point.windGustKmh ?: JSONObject.NULL)
                        .put("precipitationProbabilityPercent", point.precipitationProbabilityPercent ?: JSONObject.NULL)
                        .put("precipitationMm", point.precipitationMm ?: JSONObject.NULL)
                        .put("cloudCoverPercent", point.cloudCoverPercent ?: JSONObject.NULL)
                        .put("weatherCode", point.weatherCode)
                        .put("relativeHumidityPercent", point.relativeHumidityPercent ?: JSONObject.NULL)
                        .put("uvIndex", point.uvIndex ?: JSONObject.NULL)
                        .put("visibilityMeters", point.visibilityMeters ?: JSONObject.NULL)
                        .put("europeanAqi", point.europeanAqi ?: JSONObject.NULL)
                        .put("isDay", point.isDay?.let { if (it) 1 else 0 } ?: JSONObject.NULL)
                )
            }
        }.toString()
    }

    fun decode(payloadJson: String): List<WeatherPoint> {
        return runCatching {
            val array = JSONArray(payloadJson)
            buildList {
                for (index in 0 until array.length()) {
                    val json = array.optJSONObject(index) ?: continue
                    add(
                        WeatherPoint(
                            latitude = json.optDouble("latitude"),
                            longitude = json.optDouble("longitude"),
                            timeEpochMillis = json.optLong("timeEpochMillis"),
                            temperatureCelsius = json.optDouble("temperatureCelsius"),
                            apparentTemperatureCelsius = json.optionalDouble("apparentTemperatureCelsius"),
                            windSpeedKmh = json.optDouble("windSpeedKmh"),
                            windDirectionDegrees = json.optDouble("windDirectionDegrees"),
                            windGustKmh = json.optionalDouble("windGustKmh"),
                            precipitationProbabilityPercent = json.optionalInt("precipitationProbabilityPercent"),
                            precipitationMm = json.optionalDouble("precipitationMm"),
                            cloudCoverPercent = json.optionalInt("cloudCoverPercent"),
                            weatherCode = json.optInt("weatherCode"),
                            relativeHumidityPercent = json.optionalInt("relativeHumidityPercent"),
                            uvIndex = json.optionalDouble("uvIndex"),
                            visibilityMeters = json.optionalDouble("visibilityMeters"),
                            europeanAqi = json.optionalInt("europeanAqi"),
                            isDay = json.optionalInt("isDay")?.let { it == 1 }
                        )
                    )
                }
            }
        }.getOrElse {
            emptyList()
        }
    }

    private fun JSONObject.optionalDouble(name: String): Double? {
        return if (has(name) && !isNull(name)) {
            optDouble(name)
        } else {
            null
        }
    }

    private fun JSONObject.optionalInt(name: String): Int? {
        return if (has(name) && !isNull(name)) {
            optInt(name)
        } else {
            null
        }
    }
}
