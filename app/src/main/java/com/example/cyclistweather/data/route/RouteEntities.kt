package com.example.cyclistweather.data.route

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.cyclistweather.domain.model.ImportedRoute
import com.example.cyclistweather.domain.model.RoutePoint

@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey val id: String,
    val name: String,
    val totalDistanceMeters: Double,
    val totalElevationGainMeters: Double?,
    val createdAtEpochMillis: Long
)

@Entity(
    tableName = "route_points",
    primaryKeys = ["routeId", "pointIndex"],
    foreignKeys = [
        ForeignKey(
            entity = RouteEntity::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("routeId")]
)
data class RoutePointEntity(
    val routeId: String,
    val pointIndex: Int,
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Double?,
    val distanceFromStartMeters: Double
)

data class RouteWithPoints(
    @Embedded val route: RouteEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "routeId"
    )
    val points: List<RoutePointEntity>
) {
    fun toDomain(): ImportedRoute? {
        val sortedPoints = points.sortedBy { it.pointIndex }
        if (sortedPoints.size < 2) {
            return null
        }

        return ImportedRoute(
            id = route.id,
            name = route.name,
            points = sortedPoints.map { point ->
                RoutePoint(
                    latitude = point.latitude,
                    longitude = point.longitude,
                    elevationMeters = point.elevationMeters,
                    distanceFromStartMeters = point.distanceFromStartMeters
                )
            },
            totalDistanceMeters = route.totalDistanceMeters,
            totalElevationGainMeters = route.totalElevationGainMeters,
            createdAtEpochMillis = route.createdAtEpochMillis
        )
    }
}

fun ImportedRoute.toRouteEntity(): RouteEntity {
    return RouteEntity(
        id = id,
        name = name,
        totalDistanceMeters = totalDistanceMeters,
        totalElevationGainMeters = totalElevationGainMeters,
        createdAtEpochMillis = createdAtEpochMillis
    )
}

fun ImportedRoute.toRoutePointEntities(): List<RoutePointEntity> {
    return points.mapIndexed { index, point ->
        RoutePointEntity(
            routeId = id,
            pointIndex = index,
            latitude = point.latitude,
            longitude = point.longitude,
            elevationMeters = point.elevationMeters,
            distanceFromStartMeters = point.distanceFromStartMeters
        )
    }
}
