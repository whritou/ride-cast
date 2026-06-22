package com.example.cyclistweather.data.route

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface RouteDao {
    @Transaction
    @Query("SELECT * FROM routes ORDER BY isFavorite DESC, createdAtEpochMillis DESC")
    suspend fun getRoutesWithPoints(): List<RouteWithPoints>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoute(route: RouteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPoints(points: List<RoutePointEntity>)

    @Query("DELETE FROM route_points WHERE routeId = :routeId")
    suspend fun deletePointsForRoute(routeId: String)

    @Query("DELETE FROM routes WHERE id = :routeId")
    suspend fun deleteRoute(routeId: String)

    @Query("UPDATE routes SET name = :name WHERE id = :routeId")
    suspend fun renameRoute(routeId: String, name: String)

    @Query("UPDATE routes SET isFavorite = :isFavorite WHERE id = :routeId")
    suspend fun setFavorite(routeId: String, isFavorite: Boolean)

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
