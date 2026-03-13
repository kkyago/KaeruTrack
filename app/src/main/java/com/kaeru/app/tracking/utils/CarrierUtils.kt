package com.kaeru.app.tracking.utils

import com.kaeru.app.R

enum class TrackingCarrier(val nameRes: Int) {
    LOGGI(R.string.carrier_loggi) {
        override fun matches(code: String) = listOf("MZ", "LJ", "LOG", "GJP").any { code.startsWith(it) } && code.length >= 6
    },
    CAINIAO(R.string.carrier_cainiao) {
        override fun matches(code: String) = listOf("NN", "LP").any { code.startsWith(it) } && code.length >= 12
    },
    MELHOR_ENVIO(R.string.carrier_melhor_envio) {
        override fun matches(code: String) = code.startsWith("ME") && code.length >= 12
    },
    ANJUN(R.string.carrier_anjun) {
        override fun matches(code: String) = code.startsWith("AJ") && code.length >= 12
    },
    SHOPEE(R.string.carrier_shopee_xpress) {
        override fun matches(code: String) = (code.startsWith("SPX") || (code.startsWith("BR") && !code.endsWith("BR"))) && code.length >= 12
    },
    TOTAL_EXPRESS(R.string.carrier_total_express) {
        override fun matches(code: String) = code.startsWith("TX") || code.endsWith("TX")
    },
    JT_EXPRESS(R.string.carrier_jt) {
        override fun matches(code: String) = code.startsWith("JT") || (code.all { it.isDigit() } && code.length >= 10)
    },
    CORREIOS(R.string.carrier_correios) {
        override fun matches(code: String) = code.matches(Regex("[A-Z]{2}[0-9]{9}BR")) || (listOf("AD", "AB", "AN").any { code.startsWith(it) } && code.length >= 12)
    },
    UNKNOWN(R.string.unknown) {
        override fun matches(code: String) = false
    };

    abstract fun matches(code: String): Boolean

    companion object {
        fun fromCode(code: String): TrackingCarrier {
            val clean = code.trim().uppercase()
            return entries.find { it.matches(clean) } ?: UNKNOWN
        }
    }
}