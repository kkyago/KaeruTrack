package com.kaeru.app.tracking

import com.google.gson.annotations.SerializedName

data class SpxResponse(
    @SerializedName("data") val data: SpxData?
)
data class SpxData(
    @SerializedName("sls_tracking_info") val slsTrackingInfo: SpxTrackingInfo?
)
data class SpxTrackingInfo(
    @SerializedName("records") val records: List<SpxRecord>?
)
data class SpxRecord(
    @SerializedName("description") val description: String,
    @SerializedName("buyer_description") val buyerDescription: String,
    @SerializedName("actual_time") val actualTime: Long,
    @SerializedName("milestone_name") val currentLocation: String,
)
data class SpxLocation(
    @SerializedName("milestone_name") val locationName: String?
)