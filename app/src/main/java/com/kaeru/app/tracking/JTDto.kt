package com.kaeru.app.tracking

import com.google.gson.annotations.SerializedName

data class JTExpressResponse(
    @SerializedName("data") val data: JTExpressData?,
    @SerializedName("succ") val success: Boolean?,
    @SerializedName("msg") val message: String?
)

data class JTExpressData(
    @SerializedName("keyword") val code: String?,
    @SerializedName("details") val details: List<JTExpressDetail>?
)

data class JTExpressDetail(
    @SerializedName("scanTime") val dateString: String?,
    @SerializedName("status") val statusTitle: String?,
    @SerializedName("customerTracking") val description: String?
)