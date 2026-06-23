package com.example.cyclistweather.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.example.cyclistweather.domain.model.AppLanguage
import com.example.cyclistweather.domain.model.MapWeatherFilter
import com.example.cyclistweather.domain.model.ThemeMode
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class DataStoreAppSettingsRepository(
    context: Context
) : AppSettingsRepository {
    private val dataStore = context.applicationContext.appSettingsDataStore

    override val settings: Flow<AppSettingsSnapshot> = dataStore.data.catch {
        emit(AppSettingsSnapshot())
    }

    override suspend fun setAverageSpeedKmh(value: Double) {
        dataStore.updateData { settings ->
            settings.copy(
                averageSpeedKmh = value.takeIf { it > 0.0 } ?: DEFAULT_AVERAGE_SPEED_KMH,
                averageSpeedConfigured = true
            )
        }
    }

    override suspend fun setSelectedMapFilter(filter: MapWeatherFilter) {
        dataStore.updateData { settings ->
            settings.copy(selectedMapFilterName = filter.name)
        }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.updateData { settings ->
            settings.copy(themeModeName = mode.name)
        }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        dataStore.updateData { settings ->
            settings.copy(languageName = language.name)
        }
    }

    override suspend fun setDepartureNotificationsEnabled(enabled: Boolean) {
        dataStore.updateData { settings ->
            settings.copy(departureNotificationsEnabled = enabled)
        }
    }

    override suspend fun setRouteWeatherSummary(summary: RouteWeatherSummarySnapshot) {
        dataStore.updateData { settings ->
            settings.copy(
                routeWeatherSummaries = settings.routeWeatherSummaries + (summary.routeId to summary)
            )
        }
    }
}

private val Context.appSettingsDataStore: DataStore<AppSettingsSnapshot> by dataStore(
    fileName = "app_settings.json",
    serializer = AppSettingsSerializer
)

private object AppSettingsSerializer : Serializer<AppSettingsSnapshot> {
    override val defaultValue: AppSettingsSnapshot = AppSettingsSnapshot()

    override suspend fun readFrom(input: InputStream): AppSettingsSnapshot {
        return try {
            Json.decodeFromString(
                deserializer = AppSettingsSnapshot.serializer(),
                string = input.readBytes().decodeToString()
            )
        } catch (_: SerializationException) {
            defaultValue
        } catch (_: IllegalArgumentException) {
            defaultValue
        }
    }

    override suspend fun writeTo(
        t: AppSettingsSnapshot,
        output: OutputStream
    ) {
        output.write(
            Json.encodeToString(
                serializer = AppSettingsSnapshot.serializer(),
                value = t
            ).encodeToByteArray()
        )
    }
}
