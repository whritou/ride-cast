# RideCast

**Plan with weather, ride with confidence.**

RideCast is an Android app that overlays live weather forecasts onto your GPS routes. Import a GPX file, pick a departure time, and see wind, rain, temperature, and air quality at every point along your ride — with a departure optimizer that finds the best window in the next 7 days.

---

## Features

- **Route weather overlay** — wind speed & direction, temperature, feels-like, precipitation, cloud cover, and ride score painted along the route on an interactive map
- **Best departure optimizer** — scores every hour over the next 7 days and recommends the optimal window; a 24-hour quality strip makes trade-offs visible at a glance
- **Hazard banners** — 16 hazard types (thunderstorms, ice, dense fog, extreme heat, very high UV, night riding, …) deduplicated and surfaced as actionable warnings
- **Conditions grid** — UV index, humidity, air quality (European AQI), visibility, cloud cover, and daylight breakdown for the full route
- **GPX import & export** — import from any GPX-capable device or app; export back with weather annotations
- **Departure reminders** — schedule a notification up to 1 hour before your chosen start time
- **Stale-while-revalidate** — cached data stays visible during a background refresh; pull-to-refresh available anywhere in the detail screen
- **Skeleton loading** — shimmer placeholders match the real layout so the screen never jumps
- **Offline support** — last-fetched data is displayed with a freshness indicator (Live / Cached / Offline)
- **Full localization** — English, French, German, Spanish; in-app language switcher in Settings
- **Light / dark / system theme**

## Screenshots

<!-- Add screenshots here once available -->

## Tech stack

| Layer | Library / approach |
|---|---|
| UI | Jetpack Compose + Material 3 (BOM 2026.02.01) |
| Architecture | MVI — `StateFlow<UiState>` driven, single `ViewModel` per screen |
| DI | Manual `AppContainer` — no reflection, fast cold starts |
| Networking | Ktor 3 (Android engine + kotlinx.serialization) |
| Weather data | [Open-Meteo](https://open-meteo.com/) — free, no API key required |
| Map | MapLibre Android SDK 11 |
| Persistence | Room 2.8 (weather cache) + DataStore (settings) |
| Background work | WorkManager (departure reminder scheduling) |
| Kotlin | 2.2 · KSP 2.2 · coroutines + Flow |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 36 |

## Architecture overview

```
presentation/
  app/          ← root state, navigation, snackbar coordination
  route/        ← route library screen
  routeweather/ ← detail screen (overview + departure planner tabs)
  map/          ← full-screen map + legend + weather overlays
  settings/     ← theme, language, speed, reminders
  components/   ← shared composables (IconStat, StatTile, …)

domain/
  model/        ← pure data + enums (@StringRes on all display enums)
  usecase/      ← ScoreRideUseCase, OptimizeDepartureTimeUseCase, …

data/
  weather/      ← OpenMeteoWeatherRepository → Room cache
  route/        ← Room-backed route store
  gpx/          ← GpxParser + GpxExporter
  settings/     ← DataStoreAppSettingsRepository

ui/theme/       ← RideWeatherColors, Spacing/Radius/Sizes tokens,
                   RouteFirstComponents, SkeletonComponents
```

**Key conventions:**
- Domain layer is `Context`-free. Display strings are carried as `@StringRes Int` through state and resolved only in `@Composable` functions.
- Theme-aware colors are read via `LocalRideWeatherColors` composition local; never accessed in `DrawScope` directly.
- `RouteWeatherActions` collapses all route-weather callbacks into a single `@Stable` interface to prevent spurious recomposition.

## Getting started

### Prerequisites

- Android Studio Meerkat (2024.3) or later
- JDK 11+
- Android SDK platform 36

### Build

```bash
# Debug APK
./gradlew :app:assembleDebug

# Run unit tests
./gradlew :app:testDebugUnitTest

# Install on connected device / emulator
./gradlew :app:installDebug
```

No API key is needed — the app uses [Open-Meteo](https://open-meteo.com/), which is free and open-source.

### First run

1. Tap **Import GPX** to load a route from your device, or tap **Demo route** to explore with a built-in sample.
2. The app fetches a 7-day hourly forecast for every point along the route.
3. Open the **Plan departure** tab to see the best departure window and adjust your speed.

## Localization

String resources live in `app/src/main/res/values*/strings.xml`. All four locales (`en`, `fr`, `de`, `es`) are complete. The in-app language switcher in **Settings → Language** overrides the system locale without restarting the Activity.

To add a new locale, create `values-XX/strings.xml` and copy the keys from `values/strings.xml`.

## Weather data

Forecasts and air-quality data are provided by **[Open-Meteo](https://open-meteo.com/)** under the [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/) license. No account or API key is required. The app queries:

- `api.open-meteo.com` — hourly forecast (wind, temperature, precipitation, UV, cloud cover, visibility)
- `air-quality-api.open-meteo.com` — European AQI

Data is cached in Room and served stale while a background refresh runs.

## License

This project is for personal use. All rights reserved.
