package com.example.plantnet.data.model

import com.google.gson.annotations.SerializedName

/**
 * Respuesta de la API PlantNet
 */
data class PlantNetResponse(
    val results: List<PlantResult>,
    val remainingIdentificationRequests: Int = 0,
    val language: String = "es",
    val version: String = "",
    val query: Query = Query()
)

/**
 * Información sobre la consulta realizada
 */
data class Query(
    val project: String = "all",
    val images: List<String> = emptyList(),
    val organs: List<String> = emptyList(),
    val includeRelatedImages: Boolean = false
)

/**
 * Modelo que representa el resultado de la identificación de una planta
 */
data class PlantResult(
    val score: Float,
    val species: Species,
    val gbif: Gbif? = null,
    val images: List<ResultImage> = emptyList()
)

/**
 * Información sobre la especie de planta
 */
data class Species(
    @SerializedName("scientificNameWithoutAuthor")
    val scientificNameWithoutAuthor: String,
    @SerializedName("scientificNameAuthorship")
    val scientificNameAuthorship: String,
    val commonNames: List<String> = emptyList(),
    val family: Taxonomy,
    val genus: Taxonomy
)

/**
 * Taxonomía básica para una planta
 */
data class Taxonomy(
    @SerializedName("scientificNameWithoutAuthor")
    val scientificNameWithoutAuthor: String,
    @SerializedName("scientificNameAuthorship")
    val scientificNameAuthorship: String
)

/**
 * Información adicional de GBIF (Global Biodiversity Information Facility)
 */
data class Gbif(
    val id: String = ""
)

/**
 * Información sobre una imagen de resultado
 */
data class ResultImage(
    val organ: String = "",
    val author: String = "",
    val license: String = "",
    val date: DateInfo = DateInfo(),
    val url: ImageUrl = ImageUrl(),
    val citation: String = ""
)

/**
 * Información de fecha
 */
data class DateInfo(
    val timestamp: Long = 0,
    val string: String = ""
)

/**
 * URLs de imágenes en diferentes tamaños
 */
data class ImageUrl(
    val o: String = "",  // Original
    val m: String = "",  // Medium
    val s: String = ""   // Small
)
