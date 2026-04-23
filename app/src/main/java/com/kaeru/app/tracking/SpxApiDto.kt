package com.kaeru.app.tracking

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpxResponse(
    @SerialName("data") val data: SpxData?
)
@Serializable
data class SpxData(
    @SerialName("sls_tracking_info") val slsTrackingInfo: SpxTrackingInfo?
)
@Serializable
data class SpxTrackingInfo(
    @SerialName("records") val records: List<SpxRecord>?
)
@Serializable
data class SpxRecord(
    @SerialName("description") val description: String,
    @SerialName("buyer_description") val buyerDescription: String,
    @SerialName("actual_time") val actualTime: Long,
    @SerialName("milestone_name") val currentLocation: String,
)
@Serializable
data class SpxLocation(
    @SerialName("milestone_name") val locationName: String?
)