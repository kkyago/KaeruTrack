package com.kaeru.app.tracking

data class TrackingResponse(
    val tracking_code: String?,
    val events: List<TrackingEvent>?,
    var carrier: String = "Auto"
)

data class TrackingEvent(
    val date: String?,
    val time: String?,
    val location: String?,
    val status: String?,
    val subStatus: List<String>?
)