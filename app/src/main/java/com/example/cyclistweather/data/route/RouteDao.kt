package com.example.cyclistweather.data.route

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface RouteDao {
    @Transaction
    @Query("SELECT * FROM routes ORDER BY createdAtEpochMillis DESC")
    suspend fun getRoutesWithPoints(): List<RouteWithPoints>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoute(route: RouteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPoints(points: List<RoutePointEntity>)

    @Query("DELETE FROM route_points WHERE routeId = :routeId")
    suspend fun deletePointsForRoute(routeId: String)

    @Query("DELETE FROM routes WHERE id = :routeId")
    suspend fun deleteRoute(routeId: String)

    @Query("SELECT COUNT(*) FROM routes")
    suspend fun routeCount(): Int

    @Transaction
    suspend fun upsertRouteWithPoints(
        route: RouteEntity,
        points: List<RoutePointEntity>
    ) {
        upsertRoute(route)
        deletePointsForRoute(route.id)
        upsertPoints(points)
    }
}
