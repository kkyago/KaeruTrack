package com.kaeru.app.tracking

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CainiaoResponse(
    @SerialName("module") val modules: List<CainiaoModule>?,
    @SerialName("success") val success: Boolean?
)
@Serializable
data class CainiaoModule(
    @SerialName("mailNo") val code: String?,
    @SerialName("detailList") val details: List<CainiaoDetail>?
)
@Serializable
data class CainiaoDetail(
    @SerialName("timeStr") val dateString: String?,
    @SerialName("standerdDesc") val statusDescription: String?,
    @SerialName("desc") val altDescription: String?
)