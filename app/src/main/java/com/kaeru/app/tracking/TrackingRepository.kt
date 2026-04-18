package com.kaeru.app.tracking

import android.content.Context
import com.google.gson.Gson
import com.kaeru.app.R
import com.kaeru.app.data.scraper.LinketrackWebViewScraper
import com.kaeru.app.tracking.utils.TrackingCarrier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import okhttp3.CookieJar
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class TrackingRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .cookieJar(object : CookieJar {
            private val cookieStore = mutableMapOf<String, List<Cookie>>()
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                cookieStore[url.host] = cookies
            }
            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return cookieStore[url.host] ?: listOf()
            }
        })
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    fun isCarrierSupported(code: String): Boolean {
        return TrackingCarrier.fromCode(code) != TrackingCarrier.UNKNOWN
    }
    suspend fun trackPackage(code: String, forceRefresh: Boolean = false, carrier: String = "Auto", cpf: String? = null): TrackingResponse? {
        val cleanCode = code.trim().uppercase()

        if (!forceRefresh) {
            val cached = TrackingCache.get(cleanCode)
            if (cached != null) {
                return cached
            }
        }

        val result = when (carrier) {
            context.getString(R.string.carrier_correios) -> trackViaLinketrackWebView(cleanCode)
            context.getString(R.string.carrier_loggi) -> trackViaLoggi(cleanCode)
            context.getString(R.string.carrier_shopee_xpress) -> trackViaSpx(cleanCode)
            context.getString(R.string.carrier_cainiao) -> trackCainiaoFree(cleanCode)
            context.getString(R.string.carrier_anjun) -> trackViaAnjun(cleanCode)
            context.getString(R.string.carrier_melhor_envio) -> trackMelhorRastreioFree(cleanCode)
            context.getString(R.string.carrier_total_express) -> trackViaTotalExpress(cleanCode)
            context.getString(R.string.carrier_jt) -> trackViaJTExpress(cleanCode, cpf)
            else -> {
                val carrier = TrackingCarrier.fromCode(cleanCode)
                return when (carrier) {
                    TrackingCarrier.LOGGI -> trackViaLoggi(cleanCode)
                    TrackingCarrier.CAINIAO -> trackCainiaoFree(cleanCode)
                    TrackingCarrier.MELHOR_ENVIO -> trackMelhorRastreioFree(cleanCode)
                    TrackingCarrier.ANJUN -> trackViaAnjun(cleanCode)
                    TrackingCarrier.SHOPEE -> trackViaSpx(cleanCode)
                    TrackingCarrier.TOTAL_EXPRESS -> trackViaTotalExpress(cleanCode)
                    TrackingCarrier.CORREIOS -> trackViaLinketrackWebView(cleanCode)
                    TrackingCarrier.JT_EXPRESS -> trackViaJTExpress(cleanCode, cpf)
                    TrackingCarrier.UNKNOWN -> null
                }
            }
        }

        if (result != null && !result.events.isNullOrEmpty()) {
            TrackingCache.save(cleanCode, result)
        }

        return result
    }

    private suspend fun trackViaSpx(code: String): TrackingResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://spx.com.br/shipment/order/open/order/get_order_info?spx_tn=$code&language_code=pt"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .build()
                val response = client.newCall(request).execute()
                val bodyString = response.body?.string() ?: return@withContext null
                val rawData = gson.fromJson(bodyString, SpxResponse::class.java)
                val records = rawData.data?.slsTrackingInfo?.records ?: return@withContext null
                val uiEvents = records.map { record ->
                    val dateObj = java.util.Date(record.actualTime * 1000L)
                    val sdfDate = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                    val sdfTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    val d = sdfDate.format(dateObj)
                    val t = sdfTime.format(dateObj)
                    val stat = record.buyerDescription.ifBlank { record.description }.trim()
                    val loc = record.currentLocation.ifBlank { "Em Trânsito" } ?: "Em Trânsito"

                    TrackingEvent(
                        status = loc,
                        date = d,
                        time = t,
                        location = stat,
                        subStatus = null
                    )
                }
                return@withContext TrackingResponse(tracking_code = code, events = uiEvents)
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        }
    }

    suspend fun trackViaJTExpress(code: String, cpf: String?): TrackingResponse? {
        return withContext(Dispatchers.IO) {
            try {
                if (cpf.isNullOrBlank()) {
                    return@withContext null
                }
                val url = "https://official.jtjms-br.com/official/logisticsTracking/v2/getDetailByWaybillNo"
                val appId = "3B29A9C5728BF3E1DB0C4D66B79748B7"
                val key = "94bbcac67ab47c736d530efe3e1dc358"
                val time = System.currentTimeMillis().toString()
                val nonce = Math.random().toString()
                val jsonPayload = """{"cpf":"$cpf","langType":"PT","waybillNo":"$code"}"""
                val rawString = appId + time + nonce + jsonPayload + key
                val sign = rawString.md5().uppercase()
                val body = jsonPayload.toRequestBody("application/json".toMediaType())
                val request = okhttp3.Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("appId", appId)
                    .addHeader("key", key)
                    .addHeader("timestamp", time)
                    .addHeader("nonce", nonce)
                    .addHeader("sign", sign)
                    .addHeader("langType", "PT")
                    .addHeader("clientSource", "web")
                    .addHeader("timezone", "GMT-0300")
                    .addHeader("Origin", "https://www.jtexpress.com.br")
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
                val response = client.newCall(request).execute()
                val rawData = gson.fromJson(response.body?.string(), JTExpressResponse::class.java)
                if (rawData?.success != true || rawData.data?.details == null) {
                    return@withContext null
                }
                val uiEvents = rawData.data.details.map { detail ->
                    val parts = (detail.dateString ?: "").split(" ")
                    val d = try {
                        val s = parts.getOrElse(0) { "" }.split("-")
                        "${s[2]}/${s[1]}/${s[0]}"
                    } catch(e: Exception) {
                        parts.getOrElse(0) { "" }
                    }
                    var loc = "Nacional"
                    var stat = detail.statusTitle ?: "Status atualizado"
                    var desc = detail.description ?: ""
                    val m = Regex("^\\[(.*?)\\](.*)").find(stat)
                    if (m != null) {
                        loc = m.groupValues[1].trim()
                        stat = m.groupValues[2].trim().removePrefix("-").trim()
                    }

                    TrackingEvent(
                        status = stat,
                        date = d,
                        time = parts.getOrElse(1) { "" },
                        location = desc,
                        subStatus = null
                    )
                }

                return@withContext TrackingResponse(tracking_code = code, events = uiEvents)

            } catch (e: Exception) {
                throw e
            }
        }
    }

    private suspend fun trackViaTotalExpress(code: String): TrackingResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://totalconecta.totalexpress.com.br/mfe-rastreio/api/order-data?awb=$code&language=pt"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .addHeader("Accept", "application/json, text/plain, */*")
                    .addHeader("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7")
                    .addHeader("Sec-Ch-Ua", "\"Chromium\";v=\"122\", \"Not(A:Brand\";v=\"24\", \"Google Chrome\";v=\"122\"")
                    .addHeader("Sec-Ch-Ua-Mobile", "?0")
                    .addHeader("Sec-Ch-Ua-Platform", "\"Windows\"")
                    .addHeader("Sec-Fetch-Dest", "empty")
                    .addHeader("Sec-Fetch-Mode", "cors")
                    .addHeader("Sec-Fetch-Site", "same-site")
                    .addHeader("Referer", "https://totalconecta.totalexpress.com.br/mfe-rastreio/")
                    .build()
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                if (!response.isSuccessful) {
                    return@withContext null
                }
                if (body.isNullOrBlank()) {
                    return@withContext null
                }
                val rawData = gson.fromJson(body, TotalExpressResponse::class.java)
                val layouts = rawData.data?.layouts
                if (layouts.isNullOrEmpty()) {
                    return@withContext null
                }
                val events = mutableListOf<TrackingEvent>()

                layouts.forEach { layout ->
                    layout.etapas?.forEach { etapa ->
                        etapa.listaStatus?.forEach { status ->
                            val dateParts = status.data?.split("-")
                            val formattedDate = if (dateParts != null && dateParts.size == 3) {
                                "${dateParts[2]}/${dateParts[1]}/${dateParts[0]}"
                            } else {
                                status.data ?: ""
                            }

                            events.add(
                                TrackingEvent(
                                    status = status.statusDescricao ?: "Atualização",
                                    date = formattedDate,
                                    time = status.hora ?: "",
                                    location = status.mensagemEvaTraducao?.mensagemEva ?: "Total Express",
                                    subStatus = null
                                )
                            )
                        }
                    }
                }

                if (events.isEmpty()) {
                    return@withContext null
                }
                return@withContext TrackingResponse(tracking_code = code, events = events.reversed())

            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        }
    }

    private suspend fun trackViaLinketrackWebView(code: String): TrackingResponse? {
        val cleanCode = code.substringBefore("|").trim()
        val scraper = LinketrackWebViewScraper(context)
        val html = scraper.fetchHtml(cleanCode)
        if (html.isNullOrBlank()) {
            return null
        }
        return LinketrackParser.parseHtml(html, cleanCode)
    }

    private suspend fun trackViaLoggi(code: String): TrackingResponse? {
        val scraper = LoggiScraper(context)
        val html = scraper.fetchHtml(code)
        if (html.isNullOrBlank()) return null
        return parseLoggiHtml(html, code)
    }

    private fun parseLoggiHtml(html: String, code: String): TrackingResponse? {
        val events = mutableListOf<TrackingEvent>()
        try {
            val doc = Jsoup.parse(html)
            val steps = doc.select("div.MuiStep-root")
            if (steps.isEmpty()) return null
            for (step in steps) {
                val dateRaw = step.select(".MuiTypography-overline").text().trim()
                var status = step.select(".MuiTypography-subtitleLarge").text().trim()
                if (status.isEmpty()) status = step.select(".MuiTypography-subtitleMedium").text().trim()
                var location = step.select(".MuiTypography-bodyTextMedium").text().trim()
                if (location.startsWith("-")) location = location.removePrefix("-").trim()
                if (status.isNotEmpty()) {
                    events.add(TrackingEvent(status = status, date = dateRaw, time = "", location = location.ifBlank { "Loggi" }, subStatus = null))
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        if (events.isEmpty()) return null
        return TrackingResponse(tracking_code = code, events = events)
    }

    private suspend fun trackViaAnjun(code: String): TrackingResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://anjunexpress.com/trackPackage?tracking=$code"
                val doc = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(30000).get()
                val items = doc.select(".progress-item")
                if (items.isEmpty()) return@withContext null
                val events = items.map { item ->
                    val parts = item.select(".progress-time").text().trim().split(" ")
                    TrackingEvent(status = item.select(".progress-title").text().trim().ifBlank { "Status" }, date = parts.getOrNull(0) ?: "", time = parts.getOrNull(1) ?: "", location = item.select(".progress-desc").text().trim().ifBlank { "Em trânsito" }, subStatus = null)
                }.toMutableList()
                return@withContext TrackingResponse(tracking_code = code, events = events)
            } catch (e: Exception) {
                throw e
            }
        }
    }

    private suspend fun trackCainiaoFree(code: String): TrackingResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url("https://global.cainiao.com/global/detail.json?mailNos=$code&lang=pt-BR").build()
                val response = client.newCall(request).execute()
                val rawData = gson.fromJson(response.body?.string(), CainiaoResponse::class.java)
                val packageData = rawData.modules?.firstOrNull() ?: return@withContext null
                val uiEvents = packageData.details?.map { detail ->
                    val parts = (detail.dateString ?: "").split(" ")
                    val d = try { val s = parts.getOrElse(0){""}.split("-"); "${s[2]}/${s[1]}/${s[0]}" } catch(e:Exception){parts.getOrElse(0){""}}
                    var loc = "Internacional"; var stat = detail.statusDescription ?: detail.altDescription ?: "Status"
                    val m = Regex("^\\[(.*?)\\](.*)").find(stat)
                    if(m!=null){ loc=m.groupValues[1].trim(); stat=m.groupValues[2].trim().removePrefix("-").trim() }
                    TrackingEvent(status=stat, date=d, time=parts.getOrElse(1){""}, location=loc, subStatus=null)
                }
                return@withContext TrackingResponse(tracking_code = code, events = uiEvents)
            } catch (e: Exception) {
                throw e
            }
        }
    }

    private suspend fun trackMelhorRastreioFree(code: String): TrackingResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val body = """{"operationName":"searchParcel","variables":{"tracker":{"trackingCode":"$code","type":"melhorenvio"}},"query":"mutation searchParcel(${"$"}tracker:TrackerSearchInput!){result:searchParcel(tracker:${"$"}tracker){trackingEvents{title createdAt from description}}}"}""".toRequestBody("application/json".toMediaType())
                val req = Request.Builder().url("https://melhor-rastreio-api.melhorrastreio.com.br/graphql").post(body).header("User-Agent","Mozilla/5.0").build()
                val res = client.newCall(req).execute()
                val data = gson.fromJson(res.body?.string(), MelhorEnvioResponse::class.java)
                val evs = data?.data?.result?.events?.map {
                    val iso = it.createdAt?:""; val p = if(iso.contains("T")) iso.split("T") else listOf(iso,"")
                    val d = try{val s=p[0].split("-"); "${s[2]}/${s[1]}/${s[0]}"}catch(e:Exception){p[0]}
                    TrackingEvent(status=it.title?:"", date=d, time=p[1].take(5), location=it.fromLocation?:"", subStatus=null)
                }?.reversed() ?: return@withContext null
                return@withContext TrackingResponse(tracking_code = code, events = evs)
            } catch(e:Exception){
                throw e
            }
        }
    }
}

fun String.md5(): String {
    val bytes = MessageDigest.getInstance("MD5").digest(this.toByteArray())
    return bytes.joinToString("") { "%02X".format(it) }
}