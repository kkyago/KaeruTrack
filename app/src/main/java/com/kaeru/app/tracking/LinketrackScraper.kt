package com.kaeru.app.data.scraper

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeoutException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private class LinketrackHtmlInterface(private val onHtmlReceived: (String?) -> Unit) {
    @JavascriptInterface
    fun success(html: String) {
        onHtmlReceived(html)
    }

    @JavascriptInterface
    fun notFound() {
        onHtmlReceived(null)
    }

    @JavascriptInterface
    fun log(msg: String) {
        Log.d("LINKETRACK", msg)
    }
}

class LinketrackWebViewScraper(private val context: Context) {

    suspend fun fetchHtml(trackingCode: String): String? {
        return suspendCancellableCoroutine { continuation ->
            var hasResumed = false
            var webView: WebView? = null
            val handler = Handler(Looper.getMainLooper())
            var checkRunnable: Runnable? = null
            var timeoutRunnable: Runnable? = null

            fun cleanup() {
                handler.post {
                    checkRunnable?.let { handler.removeCallbacks(it) }
                    timeoutRunnable?.let { handler.removeCallbacks(it) }
                    webView?.apply {
                        stopLoading()
                        clearHistory()
                        removeAllViews()
                        destroy()
                    }
                    webView = null
                }
            }

            continuation.invokeOnCancellation {
                cleanup()
            }

            handler.post {
                try {
                    webView = WebView(context.applicationContext).apply {
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                            userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
                        }

                        addJavascriptInterface(LinketrackHtmlInterface { html ->
                            if (!hasResumed && continuation.isActive) {
                                hasResumed = true
                                cleanup()
                                continuation.resume(html)
                            }
                        }, "LinketrackInterface")
                    }

                    val jsCode = """
                        (function() {
                            if (window.hasSentData) return;
                            
                            var items = document.getElementsByClassName('evento-collection');
                            var bodyText = document.body.innerText || "";
                            
                            if (items.length > 0) {
                                window.hasSentData = true;
                                window.LinketrackInterface.success(document.documentElement.outerHTML);
                            } else if (bodyText.includes("Objeto não encontrado") || bodyText.includes("Código inválido") || bodyText.includes("Aguardando postagem pelo remetente")) {
                                window.hasSentData = true;
                                window.LinketrackInterface.notFound();
                            }
                        })();
                    """.trimIndent()

                    webView?.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            view?.evaluateJavascript(jsCode, null)
                        }
                    }

                    val url = "https://linketrack.com/track?codigo=$trackingCode"
                    val headers = mapOf("Referer" to "https://www.google.com/")
                    webView?.loadUrl(url, headers)

                    checkRunnable = object : Runnable {
                        var count = 0
                        override fun run() {
                            if (hasResumed || count > 15) return
                            webView?.evaluateJavascript(jsCode, null)
                            count++
                            handler.postDelayed(this, 2000)
                        }
                    }.also { handler.postDelayed(it, 3000) }

                    timeoutRunnable = Runnable {
                        if (!hasResumed && continuation.isActive) {
                            hasResumed = true
                            cleanup()
                            Log.e("LINKETRACK", "Timeout da WebView")
                            continuation.resumeWithException(TimeoutException("Linketrack WebView demorou muito para responder"))
                        }
                    }.also { handler.postDelayed(it, 30000) }

                } catch (e: Exception) {
                    Log.e("LINKETRACK", "erro crítico: ${e.message}")
                    if (!hasResumed && continuation.isActive) {
                        hasResumed = true
                        cleanup()
                        continuation.resumeWithException(e)
                    }
                }
            }
        }
    }
}