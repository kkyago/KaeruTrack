package com.kaeru.app.tracking

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MagalogResponse(
    @SerialName("tracking") val tracking: MagalogTracking? = null
)

@Serializable
data class MagalogShipping(
    @SerialName("code") val code: String? = null,
    @SerialName("tenant") val tenant: MagalogTenant? = null,
    @SerialName("delivery") val delivery: MagalogDelivery? = null
)

@Serializable
data class MagalogTenant(
    @SerialName("name") val name: String? = null
)

@Serializable
data class MagalogDelivery(
    @SerialName("expected_date") val expectedDate: String? = null,
    @SerialName("received_by") val receivedBy: String? = null
)

@Serializable
data class MagalogTracking(
    @SerialName("steps") val steps: List<MagalogStep>? = null
)

@Serializable
data class MagalogSummaryResponse(
    @SerialName("external_code") val externalCode: String? = null,
    @SerialName("external_grouper_code") val externalGrouperCode: String? = null
)

@Serializable
data class MagalogStep(
    @SerialName("occurred_at") val occurredAt: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("details") val details: String? = null
)