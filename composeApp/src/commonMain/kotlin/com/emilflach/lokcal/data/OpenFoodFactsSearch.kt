package com.emilflach.lokcal.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class OpenFoodFactsSearch(
    private val client: HttpClient = defaultClient
) {
    suspend fun search(query: String): List<OffItem> {
        if (query.isBlank()) return emptyList()
        // If the query is a GTIN-13 (EAN-13) barcode, use the product endpoint (single item)
        val isGtin13 = query.length == 13 && query.all { it.isDigit() }
        if (isGtin13) {
            val productUrl = "https://world.openfoodfacts.net/api/v3/product/$query"
            val resp: OffProductResponse = client.get(productUrl) {
                accept(ContentType.Application.Json)
                timeout { requestTimeoutMillis = 10000 }
            }.body()
            val item = resp.product?.toOffItemOrNull()
            return item?.let { listOf(it) } ?: emptyList()
        }

        // Fallback to search endpoint for non-barcode queries (list)
        val url = "https://world.openfoodfacts.org/cgi/search.pl"
        val resp: OffResponse = client.get(url) {
            url {
                parameters.append("search_terms", query)
                parameters.append("search_simple", "1")
                parameters.append("json", "1")
            }
            accept(ContentType.Application.Json)
            timeout {
                requestTimeoutMillis = 10000
            }
        }.body()
        return resp.products.orEmpty().mapNotNull { it.toOffItemOrNull() }
    }

    companion object {
        private val defaultClient by lazy {
            HttpClient {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
                install(Logging) {
                    level = LogLevel.INFO
                }
            }
        }
    }
}

@Serializable
private data class OffResponse(
    val products: List<OffProduct>? = null
)

@Serializable
private data class OffProductResponse(
    val product: OffProduct? = null
)

@Serializable
private data class OffProduct(
    @SerialName("product_name") val productName: String? = null,
    @SerialName("product_name_nl") val productNameNl: String? = null,
    @SerialName("_id") val id: String? = null,
    @SerialName("code") val code: String? = null,
    @SerialName("url") val url: String? = null,
    @SerialName("selected_images") val selectedImages: SelectedImages? = null,
    @SerialName("nutriments") val nutriments: Nutriments? = null,
    @SerialName("serving_quantity") val servingQuantity: Double? = null,
)

@Serializable
private data class SelectedImages(
    val front: ImageSizes? = null,
    val nutrition: ImageSizes? = null,
    val ingredients: ImageSizes? = null,
)

@Serializable
private data class ImageSizes(
    val small: Map<String, String>? = null,
    val thumb: Map<String, String>? = null,
    val display: Map<String, String>? = null,
)

@Serializable
private data class Nutriments(
    @SerialName("energy-kcal_100g") val energyKcalPer100g: Double? = null,
)

data class OffItem(
    val name: String,
    val gtin13: String?,
    val energyKcalPer100g: Double?,
    val servingSize: Double?,
    val productUrl: String?,
    val imageUrl: String?,
    val dutchName: String?,
)

private fun OffProduct.toOffItemOrNull(): OffItem? {
    val name = productName ?: productNameNl ?: return null
    val imageUrl = selectedImages?.let { sel ->
        sel.front?.small?.values?.firstOrNull()
            ?: sel.front?.thumb?.values?.firstOrNull()
            ?: sel.front?.display?.values?.firstOrNull()
    }
    return OffItem(
        name = name,
        gtin13 = id ?: code,
        energyKcalPer100g = nutriments?.energyKcalPer100g ?: 0.0,
        servingSize = servingQuantity,
        productUrl = url,
        imageUrl = imageUrl,
        dutchName = productNameNl
    )
}