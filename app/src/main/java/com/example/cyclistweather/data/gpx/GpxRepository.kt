package com.example.cyclistweather.data.gpx

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.cyclistweather.data.route.LocalRouteStore
import com.example.cyclistweather.domain.model.ImportedRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface GpxRepository {
    suspend fun importGpx(uri: Uri): ImportedRoute
    suspend fun importBundledSampleRoute(): ImportedRoute
    suspend fun getRoutes(): List<ImportedRoute>
    suspend fun getRoute(routeId: String): ImportedRoute?
    suspend fun deleteRoute(routeId: String)
}

class LocalGpxRepository(
    private val context: Context,
    private val parser: GpxParser = GpxParser(),
    private val routeStore: LocalRouteStore = LocalRouteStore(context)
) : GpxRepository {
    override suspend fun importGpx(uri: Uri): ImportedRoute = withContext(Dispatchers.IO) {
        val routeName = displayNameFor(uri)
        val route = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            parser.parse(routeName, inputStream)
        } ?: throw GpxParseException("Unable to open the selected GPX file.")

        routeStore.upsertRoute(route)
        route
    }

    override suspend fun importBundledSampleRoute(): ImportedRoute = withContext(Dispatchers.IO) {
        val existingRoutes = routeStore.loadRoutes()
        var selectedRoute: ImportedRoute? = null

        BUNDLED_TEST_ROUTES.forEach { testRoute ->
            val route = existingRoutes.firstOrNull { it.name == testRoute.displayName }
                ?: context.assets.open(testRoute.assetName).use { inputStream ->
                    parser.parse(testRoute.fileName, inputStream)
                }.also { route ->
                    routeStore.upsertRoute(route)
                }

            if (testRoute.openAfterImport) {
                selectedRoute = route
            }
        }

        selectedRoute ?: routeStore.loadRoutes().first()
    }

    override suspend fun getRoutes(): List<ImportedRoute> = withContext(Dispatchers.IO) {
        routeStore.loadRoutes()
    }

    override suspend fun getRoute(routeId: String): ImportedRoute? = withContext(Dispatchers.IO) {
        routeStore.loadRoutes().firstOrNull { it.id == routeId }
    }

    override suspend fun deleteRoute(routeId: String) = withContext(Dispatchers.IO) {
        routeStore.deleteRoute(routeId)
    }

    private fun displayNameFor(uri: Uri): String {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                return cursor.getString(nameIndex)
            }
        }

        return uri.lastPathSegment?.substringAfterLast('/') ?: "Imported route.gpx"
    }

    private companion object {
        val BUNDLED_TEST_ROUTES = listOf(
            BundledTestRoute(
                assetName = "sample_han_river_loop.gpx",
                fileName = "Demo Han River Loop.gpx",
                displayName = "Demo Han River Loop",
                openAfterImport = false
            ),
            BundledTestRoute(
                assetName = "test_france.gpx",
                fileName = "Test France.gpx",
                displayName = "Test France",
                openAfterImport = true
            )
        )
    }
}

private data class BundledTestRoute(
    val assetName: String,
    val fileName: String,
    val displayName: String,
    val openAfterImport: Boolean
)
