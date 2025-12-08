# Echo Android - Plan de Desarrollo de App Móvil

## Resumen Ejecutivo

Este documento define la arquitectura, stack tecnológico y plan de implementación para la aplicación Android nativa de Echo Music Server. La app se conectará al backend existente (150+ endpoints REST) y ofrecerá una experiencia de streaming de música premium.

---

## Stack Tecnológico

### Core
| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| **Kotlin** | 1.9.x | Lenguaje principal (100%) |
| **Jetpack Compose** | 1.6.x | UI declarativa moderna |
| **Material 3** | 1.2.x | Design system |
| **Gradle KTS** | 8.x | Build system |
| **Min SDK** | 26 (Android 8.0) | Soporte ~95% dispositivos |
| **Target SDK** | 34 (Android 14) | Última versión |

### Arquitectura & DI
| Tecnología | Propósito |
|------------|-----------|
| **Clean Architecture** | Separación de capas (Domain/Data/Presentation) |
| **MVVM + MVI** | Patrón de presentación |
| **Hilt** | Inyección de dependencias |
| **Multi-module** | Modularización por features |

### Networking
| Tecnología | Propósito |
|------------|-----------|
| **Retrofit 2** | Cliente HTTP REST |
| **OkHttp 4** | HTTP client + interceptors |
| **Kotlinx Serialization** | JSON parsing |
| **Ktor Client** | SSE (Server-Sent Events) |

### Persistencia
| Tecnología | Propósito |
|------------|-----------|
| **Room** | Base de datos local SQLite |
| **DataStore** | Preferencias y tokens |
| **Encrypted DataStore** | Almacenamiento seguro |

### Async & Reactive
| Tecnología | Propósito |
|------------|-----------|
| **Coroutines** | Programación asíncrona |
| **Flow** | Streams reactivos |
| **StateFlow/SharedFlow** | UI state management |

### Media
| Tecnología | Propósito |
|------------|-----------|
| **Media3 (ExoPlayer)** | Reproducción de audio |
| **MediaSession** | Controles de sistema |
| **Notification** | Media notification |

### Imágenes
| Tecnología | Propósito |
|------------|-----------|
| **Coil 3** | Carga de imágenes (Compose-native) |

### Testing
| Tecnología | Propósito |
|------------|-----------|
| **JUnit 5** | Unit tests |
| **MockK** | Mocking |
| **Turbine** | Testing de Flows |
| **Compose Testing** | UI tests |

---

## Arquitectura

### Clean Architecture - Capas

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │   Screens   │  │ ViewModels  │  │   UI Components     │  │
│  │  (Compose)  │  │   (MVI)     │  │  (Composables)      │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      DOMAIN LAYER                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │  Use Cases  │  │   Models    │  │   Repository        │  │
│  │             │  │  (Entities) │  │   Interfaces        │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                       DATA LAYER                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ Repository  │  │   Remote    │  │      Local          │  │
│  │   Impl      │  │ DataSource  │  │   DataSource        │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
│                         │                    │              │
│                         ▼                    ▼              │
│                   ┌──────────┐        ┌──────────┐          │
│                   │ Retrofit │        │   Room   │          │
│                   │   API    │        │ Database │          │
│                   └──────────┘        └──────────┘          │
└─────────────────────────────────────────────────────────────┘
```

### MVI Pattern (Model-View-Intent)

```kotlin
// Estado inmutable
data class AlbumsState(
    val albums: List<Album> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// Eventos/Intents del usuario
sealed class AlbumsIntent {
    object LoadAlbums : AlbumsIntent()
    object RefreshAlbums : AlbumsIntent()
    data class SearchAlbums(val query: String) : AlbumsIntent()
}

// Side effects (navegación, toasts, etc.)
sealed class AlbumsEffect {
    data class NavigateToAlbum(val albumId: String) : AlbumsEffect()
    data class ShowError(val message: String) : AlbumsEffect()
}
```

---

## Estructura de Módulos

```
echo-android/
├── app/                          # Módulo principal (Application)
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   └── kotlin/
│   │       └── com/echo/android/
│   │           ├── EchoApp.kt           # Application class
│   │           ├── MainActivity.kt       # Single Activity
│   │           └── navigation/
│   │               └── EchoNavGraph.kt   # Navigation Compose
│   └── build.gradle.kts
│
├── core/                         # Módulos core compartidos
│   ├── common/                   # Utilidades comunes
│   │   └── src/main/kotlin/
│   │       └── com/echo/core/common/
│   │           ├── di/                   # Hilt modules comunes
│   │           ├── util/                 # Extensions, helpers
│   │           └── result/               # Result wrapper
│   │
│   ├── network/                  # Configuración de red
│   │   └── src/main/kotlin/
│   │       └── com/echo/core/network/
│   │           ├── di/NetworkModule.kt
│   │           ├── ApiClient.kt
│   │           ├── AuthInterceptor.kt
│   │           ├── TokenAuthenticator.kt
│   │           └── NetworkMonitor.kt
│   │
│   ├── database/                 # Room database
│   │   └── src/main/kotlin/
│   │       └── com/echo/core/database/
│   │           ├── EchoDatabase.kt
│   │           ├── di/DatabaseModule.kt
│   │           └── converters/
│   │
│   ├── datastore/                # DataStore preferences
│   │   └── src/main/kotlin/
│   │       └── com/echo/core/datastore/
│   │           ├── UserPreferences.kt
│   │           ├── AuthTokenStore.kt
│   │           └── di/DataStoreModule.kt
│   │
│   ├── ui/                       # Design system compartido
│   │   └── src/main/kotlin/
│   │       └── com/echo/core/ui/
│   │           ├── theme/
│   │           │   ├── Theme.kt
│   │           │   ├── Color.kt
│   │           │   └── Typography.kt
│   │           └── components/
│   │               ├── EchoButton.kt
│   │               ├── EchoCard.kt
│   │               ├── AlbumCover.kt
│   │               ├── TrackItem.kt
│   │               └── LoadingState.kt
│   │
│   └── media/                    # Media3 / ExoPlayer
│       └── src/main/kotlin/
│           └── com/echo/core/media/
│               ├── EchoPlayer.kt
│               ├── PlaybackService.kt
│               ├── MediaNotificationManager.kt
│               ├── QueueManager.kt
│               └── di/MediaModule.kt
│
├── feature/                      # Feature modules
│   ├── auth/
│   │   ├── data/
│   │   │   ├── api/AuthApi.kt
│   │   │   ├── repository/AuthRepositoryImpl.kt
│   │   │   └── dto/
│   │   ├── domain/
│   │   │   ├── model/User.kt
│   │   │   ├── repository/AuthRepository.kt
│   │   │   └── usecase/
│   │   │       ├── LoginUseCase.kt
│   │   │       ├── LogoutUseCase.kt
│   │   │       └── RefreshTokenUseCase.kt
│   │   └── presentation/
│   │       ├── login/
│   │       │   ├── LoginScreen.kt
│   │       │   ├── LoginViewModel.kt
│   │       │   └── LoginState.kt
│   │       └── firstlogin/
│   │
│   ├── home/
│   │   ├── data/
│   │   ├── domain/
│   │   └── presentation/
│   │       ├── HomeScreen.kt
│   │       ├── HomeViewModel.kt
│   │       └── components/
│   │           ├── RecentAlbumsSection.kt
│   │           ├── TopPlayedSection.kt
│   │           └── WaveMixSection.kt
│   │
│   ├── albums/
│   │   ├── data/
│   │   │   ├── api/AlbumsApi.kt
│   │   │   ├── repository/AlbumsRepositoryImpl.kt
│   │   │   ├── dto/AlbumDto.kt
│   │   │   └── mapper/AlbumMapper.kt
│   │   ├── domain/
│   │   │   ├── model/Album.kt
│   │   │   ├── repository/AlbumsRepository.kt
│   │   │   └── usecase/
│   │   │       ├── GetAlbumsUseCase.kt
│   │   │       ├── GetAlbumDetailUseCase.kt
│   │   │       └── GetAlbumTracksUseCase.kt
│   │   └── presentation/
│   │       ├── list/
│   │       │   ├── AlbumsScreen.kt
│   │       │   └── AlbumsViewModel.kt
│   │       └── detail/
│   │           ├── AlbumDetailScreen.kt
│   │           └── AlbumDetailViewModel.kt
│   │
│   ├── artists/
│   │   ├── data/
│   │   ├── domain/
│   │   └── presentation/
│   │
│   ├── tracks/
│   │   ├── data/
│   │   ├── domain/
│   │   └── presentation/
│   │
│   ├── player/                   # Mini player + Full player
│   │   ├── data/
│   │   ├── domain/
│   │   │   └── usecase/
│   │   │       ├── PlayTrackUseCase.kt
│   │   │       ├── PauseUseCase.kt
│   │   │       ├── SkipNextUseCase.kt
│   │   │       ├── SkipPreviousUseCase.kt
│   │   │       └── SeekToUseCase.kt
│   │   └── presentation/
│   │       ├── miniplayer/
│   │       │   └── MiniPlayer.kt
│   │       ├── fullplayer/
│   │       │   ├── FullPlayerScreen.kt
│   │       │   └── FullPlayerViewModel.kt
│   │       └── queue/
│   │           └── QueueScreen.kt
│   │
│   ├── playlists/
│   │   ├── data/
│   │   ├── domain/
│   │   └── presentation/
│   │
│   ├── search/
│   │   ├── data/
│   │   ├── domain/
│   │   └── presentation/
│   │       ├── SearchScreen.kt
│   │       ├── SearchViewModel.kt
│   │       └── components/
│   │           ├── SearchBar.kt
│   │           └── SearchResults.kt
│   │
│   ├── radio/
│   │   ├── data/
│   │   │   ├── api/RadioApi.kt
│   │   │   ├── sse/RadioMetadataSSE.kt
│   │   │   └── repository/RadioRepositoryImpl.kt
│   │   ├── domain/
│   │   └── presentation/
│   │
│   ├── recommendations/          # Wave Mix
│   │   ├── data/
│   │   ├── domain/
│   │   └── presentation/
│   │
│   ├── social/
│   │   ├── data/
│   │   │   ├── api/SocialApi.kt
│   │   │   └── sse/ListeningStreamSSE.kt
│   │   ├── domain/
│   │   └── presentation/
│   │       ├── friends/
│   │       ├── activity/
│   │       └── listening/
│   │
│   ├── profile/
│   │   ├── data/
│   │   ├── domain/
│   │   └── presentation/
│   │       ├── ProfileScreen.kt
│   │       └── settings/
│   │
│   └── offline/                  # Descargas y modo offline
│       ├── data/
│       │   ├── download/DownloadManager.kt
│       │   └── repository/OfflineRepositoryImpl.kt
│       ├── domain/
│       │   └── usecase/
│       │       ├── DownloadTrackUseCase.kt
│       │       ├── DownloadAlbumUseCase.kt
│       │       └── GetOfflineTracksUseCase.kt
│       └── presentation/
│
└── build-logic/                  # Convention plugins
    └── convention/
        └── src/main/kotlin/
            ├── AndroidApplicationConventionPlugin.kt
            ├── AndroidLibraryConventionPlugin.kt
            ├── AndroidComposeConventionPlugin.kt
            └── AndroidFeatureConventionPlugin.kt
```

---

## Modelos de Datos (Domain Layer)

### User
```kotlin
data class User(
    val id: String,
    val username: String,
    val name: String?,
    val isAdmin: Boolean,
    val hasAvatar: Boolean,
    val mustChangePassword: Boolean,
    val createdAt: Instant?
)
```

### Album
```kotlin
data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String,
    val coverUrl: String?,
    val year: Int?,
    val totalTracks: Int,
    val duration: Duration?,
    val genres: List<String>,
    val addedAt: Instant
)
```

### Artist
```kotlin
data class Artist(
    val id: String,
    val name: String,
    val albumCount: Int,
    val songCount: Int,
    val biography: String?,
    val imageUrl: String?
)
```

### Track
```kotlin
data class Track(
    val id: String,
    val title: String,
    val artistId: String?,
    val artistName: String?,
    val albumId: String?,
    val albumName: String?,
    val trackNumber: Int?,
    val duration: Duration?,
    val bitRate: Int?,
    val suffix: String?,
    val coverUrl: String?,
    // Offline
    val isDownloaded: Boolean = false,
    val localPath: String? = null
)
```

### Playlist
```kotlin
data class Playlist(
    val id: String,
    val name: String,
    val description: String?,
    val duration: Duration,
    val songCount: Int,
    val ownerId: String,
    val isPublic: Boolean,
    val coverUrls: List<String>, // Primeras 4 carátulas
    val createdAt: Instant
)
```

### PlaybackState
```kotlin
data class PlaybackState(
    val currentTrack: Track?,
    val isPlaying: Boolean,
    val position: Duration,
    val duration: Duration,
    val queue: List<Track>,
    val queueIndex: Int,
    val shuffleMode: ShuffleMode,
    val repeatMode: RepeatMode
)

enum class ShuffleMode { OFF, ON }
enum class RepeatMode { OFF, ONE, ALL }
```

---

## Configuración de Red

### Base URL y Autenticación

```kotlin
// AuthInterceptor.kt
class AuthInterceptor @Inject constructor(
    private val tokenStore: AuthTokenStore
) : Interceptor {
    override fun intercept(chain: Chain): Response {
        val token = runBlocking { tokenStore.accessToken.first() }
        val request = chain.request().newBuilder()
            .apply {
                token?.let { addHeader("Authorization", "Bearer $it") }
            }
            .build()
        return chain.proceed(request)
    }
}

// TokenAuthenticator.kt - Refresh automático
class TokenAuthenticator @Inject constructor(
    private val tokenStore: AuthTokenStore,
    private val authApi: AuthApi
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.code == 401) {
            val refreshToken = runBlocking { tokenStore.refreshToken.first() }
            val newTokens = runBlocking {
                authApi.refresh(RefreshRequest(refreshToken))
            }
            runBlocking { tokenStore.saveTokens(newTokens) }
            return response.request.newBuilder()
                .header("Authorization", "Bearer ${newTokens.accessToken}")
                .build()
        }
        return null
    }
}
```

### Stream Token para Audio

```kotlin
// El streaming de audio usa tokens separados (no JWT)
interface StreamTokenApi {
    @POST("stream-token/generate")
    suspend fun generateToken(): StreamTokenResponse

    @GET("stream-token")
    suspend fun getToken(): StreamTokenResponse
}

// URL de streaming: /tracks/{id}/stream?token={streamToken}
```

---

## Reproducción de Audio (Media3)

### PlaybackService
```kotlin
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var exoPlayer: ExoPlayer
    @Inject lateinit var queueManager: QueueManager

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setCallback(EchoMediaSessionCallback())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        super.onDestroy()
    }
}

// ExoPlayer config para streaming
@Module
@InstallIn(SingletonComponent::class)
object MediaModule {
    @Provides
    @Singleton
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        audioAttributes: AudioAttributes
    ): ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(audioAttributes, true)
        .setHandleAudioBecomingNoisy(true)
        .build()
}
```

### Streaming URL Builder
```kotlin
object StreamUrlBuilder {
    fun buildStreamUrl(baseUrl: String, trackId: String, token: String): String {
        return "$baseUrl/tracks/$trackId/stream?token=$token"
    }
}
```

---

## Navegación (Jetpack Compose Navigation)

```kotlin
@Composable
fun EchoNavGraph(
    navController: NavHostController,
    startDestination: String = EchoDestinations.HOME
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Auth
        composable(EchoDestinations.LOGIN) {
            LoginScreen(onLoginSuccess = { navController.navigate(EchoDestinations.HOME) })
        }
        composable(EchoDestinations.FIRST_LOGIN) {
            FirstLoginScreen(onComplete = { navController.navigate(EchoDestinations.HOME) })
        }

        // Main
        composable(EchoDestinations.HOME) { HomeScreen(navController) }
        composable(EchoDestinations.SEARCH) { SearchScreen(navController) }
        composable(EchoDestinations.LIBRARY) { LibraryScreen(navController) }

        // Albums
        composable(EchoDestinations.ALBUMS) { AlbumsScreen(navController) }
        composable(
            route = "${EchoDestinations.ALBUM_DETAIL}/{albumId}",
            arguments = listOf(navArgument("albumId") { type = NavType.StringType })
        ) { backStackEntry ->
            AlbumDetailScreen(
                albumId = backStackEntry.arguments?.getString("albumId") ?: "",
                navController = navController
            )
        }

        // Artists
        composable(EchoDestinations.ARTISTS) { ArtistsScreen(navController) }
        composable("${EchoDestinations.ARTIST_DETAIL}/{artistId}") { ... }

        // Playlists
        composable(EchoDestinations.PLAYLISTS) { PlaylistsScreen(navController) }
        composable("${EchoDestinations.PLAYLIST_DETAIL}/{playlistId}") { ... }

        // Radio
        composable(EchoDestinations.RADIO) { RadioScreen(navController) }

        // Social
        composable(EchoDestinations.SOCIAL) { SocialScreen(navController) }

        // Profile
        composable(EchoDestinations.PROFILE) { ProfileScreen(navController) }
        composable(EchoDestinations.SETTINGS) { SettingsScreen(navController) }

        // Full Player (bottom sheet o pantalla completa)
        composable(EchoDestinations.FULL_PLAYER) { FullPlayerScreen(navController) }
        composable(EchoDestinations.QUEUE) { QueueScreen(navController) }
    }
}

object EchoDestinations {
    const val LOGIN = "login"
    const val FIRST_LOGIN = "first_login"
    const val HOME = "home"
    const val SEARCH = "search"
    const val LIBRARY = "library"
    const val ALBUMS = "albums"
    const val ALBUM_DETAIL = "album"
    const val ARTISTS = "artists"
    const val ARTIST_DETAIL = "artist"
    const val PLAYLISTS = "playlists"
    const val PLAYLIST_DETAIL = "playlist"
    const val RADIO = "radio"
    const val SOCIAL = "social"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val FULL_PLAYER = "player"
    const val QUEUE = "queue"
}
```

---

## Plan de Implementación por Fases

### Fase 1: Fundamentos (2-3 semanas de trabajo)
1. **Setup del proyecto**
   - Crear proyecto Android con Compose
   - Configurar multi-module con Convention Plugins
   - Setup de Hilt, Retrofit, Room

2. **Core modules**
   - `:core:network` - API client, interceptors
   - `:core:database` - Room setup
   - `:core:datastore` - Auth tokens, preferences
   - `:core:ui` - Theme, componentes básicos

3. **Autenticación**
   - Login screen
   - Token management (access + refresh)
   - Auth state global

### Fase 2: Biblioteca Musical (2-3 semanas)
1. **Álbumes**
   - Listado con grid/list toggle
   - Búsqueda
   - Detalle de álbum con tracks
   - Carátulas con Coil

2. **Artistas**
   - Listado
   - Detalle con álbumes

3. **Tracks**
   - Listado
   - Búsqueda

### Fase 3: Player de Audio (2-3 semanas)
1. **Media3 Setup**
   - ExoPlayer configuration
   - PlaybackService
   - MediaSession

2. **UI del Player**
   - Mini player (bottom bar)
   - Full player screen
   - Queue management
   - Controles (play/pause, skip, seek)

3. **Features avanzadas**
   - Background playback
   - Notification controls
   - Lock screen controls
   - Audio focus handling

### Fase 4: Playlists & Search (1-2 semanas)
1. **Playlists**
   - CRUD de playlists
   - Añadir/quitar tracks
   - Reordenar

2. **Búsqueda Global**
   - Search bar
   - Resultados unificados (albums, artists, tracks)
   - Historial de búsqueda

### Fase 5: Home & Recomendaciones (1-2 semanas)
1. **Home Screen**
   - Álbumes recientes
   - Top played
   - Recently played
   - Featured album

2. **Wave Mix**
   - Playlists personalizadas
   - Artist playlists
   - Genre playlists

### Fase 6: Radio (1-2 semanas)
1. **Radio Stations**
   - Listado por categorías
   - Búsqueda
   - Favoritas

2. **Radio Player**
   - Streaming
   - Metadatos en tiempo real (SSE)

### Fase 7: Social (1-2 semanas)
1. **Friends**
   - Lista de amigos
   - Solicitudes
   - Búsqueda de usuarios

2. **Activity**
   - Listening now (SSE)
   - Activity feed

### Fase 8: Perfil & Settings (1 semana)
1. **Profile**
   - Avatar
   - Editar perfil

2. **Settings**
   - Tema (light/dark)
   - Idioma
   - Calidad de streaming
   - Privacidad

### Fase 9: Offline Mode (2-3 semanas)
1. **Downloads**
   - Descargar tracks
   - Descargar álbumes
   - Gestión de storage

2. **Offline Playback**
   - Detectar conectividad
   - Fallback a local

### Fase 10: Polish & Optimization (1-2 semanas)
1. **Performance**
   - Lazy loading
   - Image caching
   - Memory optimization

2. **UX Polish**
   - Animaciones
   - Transiciones
   - Edge cases

---

## APIs a Implementar

### Prioridad Alta (Core)
```kotlin
interface AuthApi {
    @POST("auth/login") suspend fun login(body: LoginRequest): AuthResponse
    @POST("auth/refresh") suspend fun refresh(body: RefreshRequest): AuthResponse
    @GET("auth/me") suspend fun getCurrentUser(): User
}

interface AlbumsApi {
    @GET("albums") suspend fun getAlbums(@Query params): PaginatedResponse<Album>
    @GET("albums/recent") suspend fun getRecentAlbums(): List<Album>
    @GET("albums/top-played") suspend fun getTopPlayed(): List<Album>
    @GET("albums/{id}") suspend fun getAlbum(@Path id: String): Album
    @GET("albums/{id}/tracks") suspend fun getAlbumTracks(@Path id: String): List<Track>
}

interface TracksApi {
    @GET("tracks") suspend fun getTracks(@Query params): PaginatedResponse<Track>
    @GET("tracks/search/{query}") suspend fun search(@Path query: String): List<Track>
}

interface StreamTokenApi {
    @POST("stream-token/generate") suspend fun generate(): StreamToken
    @GET("stream-token") suspend fun getToken(): StreamToken
}
```

### Prioridad Media
```kotlin
interface ArtistsApi { ... }
interface PlaylistsApi { ... }
interface PlayTrackingApi { ... }
interface RecommendationsApi { ... }
```

### Prioridad Baja
```kotlin
interface RadioApi { ... }
interface SocialApi { ... }
interface InteractionsApi { ... }
```

---

## Consideraciones Técnicas

### Seguridad
- Tokens almacenados en EncryptedSharedPreferences
- Certificate pinning para producción
- ProGuard/R8 para ofuscación

### Performance
- Paginación con Paging 3
- Image caching agresivo con Coil
- Prefetch de tracks siguiente/anterior
- Background data sync

### Offline
- Room para caché de metadatos
- WorkManager para descargas
- NetworkCallback para detectar conectividad

### Testing
- Unit tests para UseCases y ViewModels
- Integration tests para Repositories
- UI tests con Compose Testing

### CI/CD
- GitHub Actions para builds
- Fastlane para releases
- Firebase App Distribution para beta

---

## Dependencias (build.gradle.kts)

```kotlin
// Core Android
implementation("androidx.core:core-ktx:1.12.0")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

// Compose
implementation(platform("androidx.compose:compose-bom:2024.02.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.ui:ui-graphics")
implementation("androidx.compose.material3:material3")
implementation("androidx.activity:activity-compose:1.8.2")
implementation("androidx.navigation:navigation-compose:2.7.7")

// Hilt
implementation("com.google.dagger:hilt-android:2.50")
kapt("com.google.dagger:hilt-compiler:2.50")
implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

// Networking
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

// Room
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")

// DataStore
implementation("androidx.datastore:datastore-preferences:1.0.0")

// Media3
implementation("androidx.media3:media3-exoplayer:1.2.1")
implementation("androidx.media3:media3-session:1.2.1")
implementation("androidx.media3:media3-ui:1.2.1")

// Images
implementation("io.coil-kt:coil-compose:2.5.0")

// SSE (Ktor)
implementation("io.ktor:ktor-client-android:2.3.8")
implementation("io.ktor:ktor-client-content-negotiation:2.3.8")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

// Testing
testImplementation("junit:junit:4.13.2")
testImplementation("io.mockk:mockk:1.13.9")
testImplementation("app.cash.turbine:turbine:1.0.0")
androidTestImplementation("androidx.compose.ui:ui-test-junit4")
```

---

## Resumen

Este plan proporciona una base sólida para desarrollar una app Android profesional y mantenible. La arquitectura Clean Architecture + MVI garantiza:

- **Testabilidad**: Cada capa se puede testear independientemente
- **Escalabilidad**: Fácil añadir nuevas features como módulos
- **Mantenibilidad**: Código organizado y predecible
- **Rendimiento**: Optimizado para streaming de audio

El stack tecnológico es 100% moderno y sigue las mejores prácticas de Android en 2024.
