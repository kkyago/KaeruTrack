package com.kaeru.app.tracking

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JTExpressResponse(
    @SerialName("data") val data: JTExpressData?,
    @SerialName("succ") val success: Boolean?,
    @SerialName("msg") val message: String?
)

@Serializable
data class JTExpressData(
    @SerialName("keyword") val code: String?,
    @SerialName("details") val details: List<JTExpressDetail>?
)

@Serializable
data class JTExpressDetail(
    @SerialName("scanTime") val dateString: String?,
    @SerialName("status") val statusTitle: String?,
    @SerialName("customerTracking") val description: String?
)