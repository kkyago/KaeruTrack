package com.kaeru.app.tracking

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TotalExpressResponse(
    @SerialName("data") val data: TotalExpressData?
)
@Serializable
data class TotalExpressData(
    @SerialName("layouts") val layouts: List<TotalExpressLayout>?
)
@Serializable
data class TotalExpressLayout(
    @SerialName("etapas") val etapas: List<TotalExpressEtapa>?
)
@Serializable
data class TotalExpressEtapa(
    @SerialName("listaStatus") val listaStatus: List<TotalExpressStatus>?
)
@Serializable
data class TotalExpressStatus(
    @SerialName("statusDescricao") val statusDescricao: String?,
    @SerialName("data") val data: String?,
    @SerialName("hora") val hora: String?,
    @SerialName("mensagemEvaTraducao") val mensagemEvaTraducao: TotalExpressMensagem?
)
@Serializable
data class TotalExpressMensagem(
    @SerialName("mensagemEva") val mensagemEva: String?
)