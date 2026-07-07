package com.kaeru.app.tracking

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CainiaoResponse(
    @SerialName("module") val modules: List<CainiaoModule>? = null,
    @SerialName("success") val success: Boolean? = null
)

@Serializable
data class CainiaoModule(
    @SerialName("mailNo") val code: String? = null,
    @SerialName("detailList") val details: List<CainiaoDetail>? = null
)

@Serializable
data class CainiaoDetail(
    @SerialName("timeStr") val dateString: String? = null,
    @SerialName("standerdDesc") val statusDescription: String? = null,
    @SerialName("desc") val altDescription: String? = null
)