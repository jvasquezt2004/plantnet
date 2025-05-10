package com.example.plantnet.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.plantnet.data.api.RetrofitClient
import com.example.plantnet.data.model.PlantResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.io.FileOutputStream

class PlantRepository {
    private val apiService = RetrofitClient.plantNetApiService
    
    /**
     * Identifica una planta a partir de una imagen
     * @param context Contexto de la aplicación
     * @param bitmap Imagen de la planta capturada
     * @param apiKey Clave API de PlantNet
     * @return Lista de resultados de identificación o null si ocurrió un error
     */
    suspend fun identifyPlant(
        context: Context,
        bitmap: Bitmap,
        apiKey: String
    ): Result<List<PlantResult>> = withContext(Dispatchers.IO) {
        try {
            // Convertir el bitmap a un archivo temporal
            val file = bitmapToFile(context, bitmap)
            
            // Usar una string directa para el tipo MIME
            val requestBody = RequestBody.create(null, file)
            val imagePart = MultipartBody.Part.createFormData("images", file.name, requestBody)
            
            // Imprimir información de depuración
            println("Enviando solicitud a PlantNet con API key: $apiKey")
            println("Archivo temporal: ${file.absolutePath}")
            
            // Hacer la solicitud a la API
            val response = apiService.identifyPlant(
                images = listOf(imagePart),
                apiKey = apiKey
            )
            
            // Imprimir información de respuesta
            println("Respuesta de PlantNet - Código: ${response.code()}")
            println("Respuesta de PlantNet - Mensaje: ${response.message()}")
            
            if (response.isSuccessful) {
                response.body()?.let {
                    // Eliminar el archivo temporal
                    file.delete()
                    return@withContext Result.success(it.results)
                } ?: run {
                    println("Respuesta vacía del servidor")
                    return@withContext Result.failure(Exception("No se recibieron resultados de identificación"))
                }
            } else if (response.code() == 404) {
                // Manejar caso específico de "Species not found"
                println("Planta no identificada (404): La especie no fue encontrada")
                file.delete() // Eliminar archivo temporal
                return@withContext Result.failure(Exception("No se pudo identificar la planta. Intenta con otra foto o desde un ángulo diferente."))
            } else {
                val errorBody = response.errorBody()?.string() ?: "No hay cuerpo de error"
                println("Error en la respuesta: $errorBody")
                file.delete() // Eliminar archivo temporal
                return@withContext Result.failure(Exception("Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            println("Excepción durante la identificación: ${e.message}")
            e.printStackTrace()
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Convierte un bitmap a un archivo temporal en el directorio cache
     */
    private fun bitmapToFile(context: Context, bitmap: Bitmap): File {
        val tempFile = File(context.cacheDir, "plant_image_${System.currentTimeMillis()}.jpg")
        val fos = FileOutputStream(tempFile)
        
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
        fos.flush()
        fos.close()
        
        return tempFile
    }
}
