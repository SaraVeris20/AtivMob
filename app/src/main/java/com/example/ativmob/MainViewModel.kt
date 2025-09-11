package com.example.ativmob

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ativmob.ui.theme.ThemeManager
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Modelos de dados para API do IBGE
@Serializable
data class Estado(
    val id: Int,
    val sigla: String,
    val nome: String,
    val regiao: Regiao
)

@Serializable
data class Regiao(
    val id: Int,
    val sigla: String,
    val nome: String
)

// UiState integrado combinando ambas as funcionalidades
data class MainUiState(
    // Funcionalidades do segundo ViewModel (geolocalização + saudações)
    val currentLocation: LocationData? = null,
    val isLoadingLocation: Boolean = false,
    val locationError: String? = null,
    val greeting: String = "Android",

    // Funcionalidades do primeiro ViewModel (Estados IBGE)
    val displayTitle: String = "Bem-vindo ao AtivMob!",
    val isLoading: Boolean = false,
    val error: String? = null,
    val estados: List<Estado> = emptyList()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // Gerenciadores do segundo ViewModel
    private val locationManager = LocationManager(application)
    private val themeManager = ThemeManager(application)

    // Cliente HTTP do primeiro ViewModel
    private val client: HttpClient
        get() = HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                    isLenient = true
                })
            }
        }

    private val ibgeApiUrl = "https://servicodados.ibge.gov.br/api/v1/localidades/estados"

    // StateFlow para gerenciar o estado da UI
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // Flow para tema dark/light
    val isDarkTheme = themeManager.isDarkTheme

    // ========== FUNCIONALIDADES DE SAUDAÇÃO ==========
    fun updateGreeting(newGreeting: String) {
        _uiState.value = _uiState.value.copy(
            greeting = newGreeting,
            displayTitle = "Olá, $newGreeting! Explore as funcionalidades abaixo."
        )
    }

    // ========== FUNCIONALIDADES DE GEOLOCALIZAÇÃO ==========
    fun getCurrentLocation() {
        if (!locationManager.hasLocationPermission()) {
            _uiState.value = _uiState.value.copy(
                locationError = "Permissão de localização necessária"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingLocation = true,
                locationError = null
            )

            locationManager.getCurrentLocation()
                .onSuccess { location ->
                    _uiState.value = _uiState.value.copy(
                        currentLocation = location,
                        isLoadingLocation = false,
                        locationError = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingLocation = false,
                        locationError = error.message ?: "Erro ao obter localização"
                    )
                }
        }
    }

    fun clearLocationError() {
        _uiState.value = _uiState.value.copy(locationError = null)
    }

    // ========== FUNCIONALIDADES DE TEMA ==========
    fun toggleTheme() {
        viewModelScope.launch {
            val currentTheme = isDarkTheme
            // Obtém o valor atual do tema do Flow
            val currentThemeValue = currentTheme.firstOrNull() ?: false
            // Inverte o tema atual
            themeManager.setDarkTheme(!currentThemeValue)
        }
    }

    // ========== FUNCIONALIDADES DOS ESTADOS IBGE ==========
    fun fetchStatesData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                displayTitle = "Buscando dados dos estados..."
            )

            try {
                val states: List<Estado> = client.get(ibgeApiUrl).body()

                if (states.isNotEmpty()) {
                    val stateNames = states.take(3).joinToString(", ") { it.nome }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        displayTitle = "✅ ${states.size} estados carregados: $stateNames...",
                        estados = states,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        displayTitle = "⚠️ Nenhum estado encontrado",
                        estados = emptyList(),
                        error = "Nenhum dado retornado pela API do IBGE"
                    )
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro de conexão: ${e.localizedMessage ?: "Erro desconhecido"}",
                    displayTitle = "❌ Erro ao buscar dados do IBGE",
                    estados = emptyList()
                )
            }
        }
    }

    // ========== LIMPEZA DE RECURSOS ==========
    override fun onCleared() {
        super.onCleared()
        client.close()
    }
}

// ========== CLASSES AUXILIARES NECESSÁRIAS ==========

// LocationData (se não existir no projeto)
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null,
    val timestamp: Long = System.currentTimeMillis()
)

// LocationManager (estrutura básica se não existir)
class LocationManager(private val application: Application) {
    fun hasLocationPermission(): Boolean {
        // Implementar verificação de permissões
        return true // Placeholder
    }

    suspend fun getCurrentLocation(): Result<LocationData> {
        // Implementar lógica de obtenção de localização
        return Result.success(
            LocationData(
                latitude = -21.5590, // Coordenadas de exemplo (Varginha, MG)
                longitude = -45.4394,
                accuracy = 10.0f
            )
        )
    }
}

// ThemeManager (estrutura básica se não existir)
class ThemeManager(private val application: Application) {
    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: Flow<Boolean> = _isDarkTheme.asStateFlow()

    fun setDarkTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
        // Implementar persistência local (SharedPreferences)
    }
}