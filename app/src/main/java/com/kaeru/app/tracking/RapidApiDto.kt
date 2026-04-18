package com.kaeru.app.tracking

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RapidApiResponse(
    @SerialName("correios_object") val correiosObject: CorreiosObject?
)
@Serializable
data class CorreiosObject(
    @SerialName("codObjeto") val code: String?,
    @SerialName("eventos") val events: List<RapidApiEvent>?
)
@Serializable
data class RapidApiEvent(
    @SerialName("descricao") val description: String?,
    @SerialName("dtHrCriado") val createdAt: CreatedAt?,
    @SerialName("unidade") val unit: UnitObject?,
    @SerialName("unidadeDestino") val destination: UnitObject?
)
@Serializable
data class CreatedAt(
    @SerialName("date") val dateIso: String?
)
@Serializable
data class UnitObject(
    @SerialName("tipo") val type: String?,
    @SerialName("endereco") val address: AddressObject?
)
@Serializable
data class AddressObject(
    @SerialName("cidade") val city: String?,
    @SerialName("uf") val state: String?
)