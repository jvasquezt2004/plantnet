package com.example.plantnet.ui.viewmodel

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.plantnet.data.api.RetrofitClient
import com.example.plantnet.data.model.PlantResult
import com.example.plantnet.data.repository.PlantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlantViewModel : ViewModel() {
    private val repository = PlantRepository()
    
    // Estado para la identificación de plantas
    private val _identificationState = MutableStateFlow<IdentificationState>(IdentificationState.Initial)
    val identificationState: StateFlow<IdentificationState> = _identificationState.asStateFlow()
    
    // Estado para los resultados de identificación
    private val _identificationResults = MutableStateFlow<List<PlantResult>>(emptyList())
    val identificationResults: StateFlow<List<PlantResult>> = _identificationResults.asStateFlow()
    
    /**
     * Identifica una planta a partir de una imagen usando la API key fija
     */
    fun identifyPlant(context: Context, imageBitmap: ImageBitmap) {
        _identificationState.value = IdentificationState.Loading
        
        viewModelScope.launch {
            try {
                // Convertir ImageBitmap a Android Bitmap
                val bitmap = imageBitmap.asAndroidBitmap()
                
                repository.identifyPlant(context, bitmap, RetrofitClient.API_KEY).fold(
                    onSuccess = { results ->
                        if (results.isNotEmpty()) {
                            _identificationResults.value = results
                            _identificationState.value = IdentificationState.Success(results)
                        } else {
                            _identificationState.value = IdentificationState.Error("No se encontraron resultados para esta imagen")
                        }
                    },
                    onFailure = { error ->
                        _identificationState.value = IdentificationState.Error("Error: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                _identificationState.value = IdentificationState.Error("Error: ${e.message}")
            }
        }
    }
    
    /**
     * Resetea el estado de identificación
     */
    fun resetIdentificationState() {
        _identificationState.value = IdentificationState.Initial
        _identificationResults.value = emptyList()
    }
}

/**
 * Estados posibles para el proceso de identificación de plantas
 */
sealed class IdentificationState {
    object Initial : IdentificationState()
    object Loading : IdentificationState()
    data class Success(val results: List<PlantResult>) : IdentificationState()
    data class Error(val message: String) : IdentificationState()
}
