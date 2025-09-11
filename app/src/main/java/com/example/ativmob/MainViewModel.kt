package com.example.ativmob.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ativmob.location.LocationData
import com.example.ativmob.location.LocationManager
import com.example.ativmob.ui.theme.ThemeManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class MainUiState(
    val currentLocation: LocationData? = null,
    val isLoadingLocation: Boolean = false,
    val locationError: String? = null,
    val greeting: String = "Android"
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val locationManager = LocationManager(application)
    private val themeManager = ThemeManager(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val isDarkTheme = themeManager.isDarkTheme

    fun updateGreeting(newGreeting: String) {
        _uiState.value = _uiState.value.copy(greeting = newGreeting)
    }

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

    fun toggleTheme() {
        viewModelScope.launch {
            val currentTheme = isDarkTheme
            // Obtém o valor atual do tema do Flow
            val currentThemeValue = currentTheme.firstOrNull() ?: false
            // Inverte o tema atual
            themeManager.setDarkTheme(!currentThemeValue)
        }
    }
    fun clearLocationError() {
        _uiState.value = _uiState.value.copy(locationError = null)
    }
}