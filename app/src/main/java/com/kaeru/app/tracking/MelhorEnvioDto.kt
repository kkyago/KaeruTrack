package com.kaeru.app.tracking

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MelhorEnvioResponse(
    @SerialName("data") val data: MelhorEnvioData?
)
@Serializable
data class MelhorEnvioData(
    @SerialName("result") val result: MelhorEnvioResult?
)
@Serializable
data class MelhorEnvioResult(
    @SerialName("trackingEvents") val events: List<MelhorEnvioEvent>?
)
@Serializable
data class MelhorEnvioEvent(
    @SerialName("createdAt") val createdAt: String?,
    @SerialName("title") val title: String?,
    @SerialName("from") val fromLocation: String?,
    @SerialName("description") val description: String?
)