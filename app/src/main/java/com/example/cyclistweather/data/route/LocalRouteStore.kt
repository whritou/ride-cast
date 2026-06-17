package com.example.cyclistweather.data.route

import android.content.Context
import com.example.cyclistweather.data.local.CyclistWeatherDatabase
import com.example.cyclistweather.domain.model.ImportedRoute
import com.example.cyclistweather.domain.model.RoutePoint
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

class LocalRouteStore(
    context: Context,
    private val routeDao: RouteDao = CyclistWeatherDatabase.getInstance(context).routeDao()
) {
    private val legacyRouteFile = File(context.filesDir, LEGACY_ROUTE_FILE_NAME)

    suspend fun loadRoutes(): List<ImportedRoute> {
        migrateLegacyRoutesIfNeeded()
        return routeDao.getRoutesWithPoints()
            .mapNotNull { it.toDomain() }
            .sortedByDescending { it.createdAtEpochMillis }
    }

    suspend fun upsertRoute(route: ImportedRoute) {
        routeDao.upsertRouteWithPoints(
            route = route.toRouteEntity(),
            points = route.toRoutePointEntities()
        )
    }

    suspend fun deleteRoute(routeId: String) {
        routeDao.deleteRoute(routeId)
    }

    private suspend fun migrateLegacyRoutesIfNeeded() {
        if (!legacyRouteFile.exists() || routeDao.routeCount() > 0) {
            return
        }

        readLegacyRoutes().forEach { route ->
            upsertRoute(route)
        }
        legacyRouteFile.renameTo(File(legacyRouteFile.parentFile, "$LEGACY_ROUTE_FILE_NAME.migrated"))
    }

    private fun readLegacyRoutes(): List<ImportedRoute> {
        return runCatching {
            val array = JSONArray(legacyRouteFile.readText())
            buildList {
                for (index in 0 until array.length()) {
                    decodeRoute(array.getJSONObject(index))?.let(::add)
                }
            }
        }.getOrElse {
            emptyList()
        }
    }

    private fun decodeRoute(json: JSONObject): ImportedRoute? {
        val pointsArray = json.optJSONArray("points") ?: return null
        val points = buildList {
            for (index in 0 until pointsArray.length()) {
                val pointJson = pointsArray.optJSONObject(index) ?: continue
                add(
                    RoutePoint(
                        latitude = pointJson.optDouble("latitude"),
                        longitude = pointJson.optDouble("longitude"),
                        elevationMeters = pointJson.optionalDouble("elevationMeters"),
                        distanceFromStartMeters = pointJson.optDouble("distanceFromStartMeters")
                    )
                )
            }
        }

        if (points.size < 2) {
            return null
        }

        return ImportedRoute(
            id = json.optString("id"),
            name = json.optString("name", "Imported route"),
            points = points,
            totalDistanceMeters = json.optDouble("totalDistanceMeters"),
            totalElevationGainMeters = json.optionalDouble("totalElevationGainMeters"),
            createdAtEpochMillis = json.optLong("createdAtEpochMillis")
        )
    }

    private fun JSONObject.optionalDouble(name: String): Double? {
        return if (has(name) && !isNull(name)) {
            optDouble(name)
        } else {
            null
        }
    }

    private companion object {
        const val LEGACY_ROUTE_FILE_NAME = "routes.json"
    }
}
