package com.kaeru.app.data.scraper

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeoutException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private class LoggiHtmlInterface(private val onHtmlReceived: (String?) -> Unit) {
    @JavascriptInterface
    fun success(html: String) {
        Log.d("LOGGI_DEBUG", "[INTERFACE] JS chamou success()! Retornando HTML.")
        onHtmlReceived(html)
    }

    @JavascriptInterface
    fun notFound() {
        Log.d("LOGGI_DEBUG", "[INTERFACE] JS chamou notFound()! Retornando NULL.")
        onHtmlReceived(null)
    }

    @JavascriptInterface
    fun log(msg: String) {
        Log.d("LOGGI_DEBUG", "[JS_SPY] $msg")
    }
}

class LoggiScraper(private val context: Context) {

    suspend fun fetchHtml(trackingCode: String): String? {
        Log.d("LOGGI_DEBUG", "--------------------------------------------------")
        Log.d("LOGGI_DEBUG", "Iniciando rastreio Loggi para o código: $trackingCode")

        return suspendCancellableCoroutine { continuation ->
            var hasResumed = false
            var webView: WebView? = null
            val handler = Handler(Looper.getMainLooper())
            var checkRunnable: Runnable? = null
            var timeoutRunnable: Runnable? = null

            fun cleanup() {
                Log.d("LOGGI_DEBUG", "Executando cleanup da WebView...")
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
                Log.w("LOGGI_DEBUG", "Coroutine cancelada externamente (WorkManager ou ViewModel).")
                cleanup()
            }

            handler.post {
                try {
                    webView = WebView(context.applicationContext).apply {
                        layoutParams = android.view.ViewGroup.LayoutParams(1080, 1920)

                        resumeTimers()
                        onResume()

                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            val defaultUA = WebSettings.getDefaultUserAgent(context.applicationContext)
                            userAgentString = defaultUA
                            mediaPlaybackRequiresUserGesture = false
                            loadsImagesAutomatically = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                Log.w("LOGGI_CONSOLE", "${consoleMessage?.message()} -- linha ${consoleMessage?.lineNumber()}")
                                return true
                            }
                        }

                        addJavascriptInterface(LoggiHtmlInterface { html ->
                            if (!hasResumed && continuation.isActive) {
                                hasResumed = true
                                cleanup()
                                continuation.resume(html)
                            }
                        }, "LoggiInterface")
                    }

                    val jsCode = """
                        (function() {
                            if (window.hasSentData) return;
                            
                            var bodyText = document.body.innerText || "";
                            
                            var achouDados = bodyText.includes("Pedido criado") || 
                                             bodyText.includes("Entregue") || 
                                             bodyText.includes("Saiu para entrega") || 
                                             bodyText.includes("Em trânsito");
                            
                            if (achouDados) {
                                window.LoggiInterface.log("GATILHO: Dados de rastreio apareceram na tela!");
                                window.hasSentData = true;
                                window.LoggiInterface.success(document.documentElement.outerHTML);
                            } else if (bodyText.includes("Pacote não encontrado") || bodyText.includes("não encontramos") || bodyText.includes("Ops!")) {
                                window.LoggiInterface.log("GATILHO: Pacote inexistente detectado.");
                                window.hasSentData = true;
                                window.LoggiInterface.notFound();
                            } else {
                                var preview = bodyText.substring(0, 150).replace(/\n/g, ' ');
                                window.LoggiInterface.log("Aguardando carregamento... Tela: " + preview);
                            }
                        })();
                    """.trimIndent()

                    webView?.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            Log.d("LOGGI_DEBUG", "onPageFinished chamado. URL carregada: $url")
                            view?.evaluateJavascript(jsCode, null)
                        }

                        override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                            Log.e("LOGGI_DEBUG", "onReceivedError: $description (Code: $errorCode) no URL: $failingUrl")
                        }
                    }

                    val url = "https://app.loggi.com/rastreador/$trackingCode/historico"
                    Log.d("LOGGI_DEBUG", "Mandando carregar a URL: $url")
                    webView?.loadUrl(url)

                    checkRunnable = object : Runnable {
                        var count = 0
                        override fun run() {
                            if (hasResumed || count > 15) return
                            Log.d("LOGGI_DEBUG", "Loop de checagem rodando... Tentativa nº ${count + 1}/15")
                            webView?.evaluateJavascript(jsCode, null)
                            count++
                            handler.postDelayed(this, 2000)
                        }
                    }.also { handler.postDelayed(it, 3000) }

                    timeoutRunnable = Runnable {
                        if (!hasResumed && continuation.isActive) {
                            hasResumed = true
                            Log.e("LOGGI_DEBUG", "❌ TEMPO ESGOTADO! O código rodou por 30s e a tela não mudou.")
                            cleanup()
                            continuation.resumeWithException(TimeoutException("Loggi WebView demorou muito para responder"))
                        }
                    }.also { handler.postDelayed(it, 30000) }

                } catch (e: Exception) {
                    Log.e("LOGGI_DEBUG", "Erro critico na main thread: ${e.message}")
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