package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.ReaderFontFamily
import com.example.data.model.ReaderSettings
import com.example.data.model.ReaderTheme
import org.json.JSONObject

/**
 * JSON message passed from the embedded JS engine (reader-engine.js) to Kotlin
 * via [ReaderJsInterface.postMessage].
 */
data class EngineMessage(val name: String, val data: JSONObject)

/**
 * JS -> Kotlin bridge. The JS engine calls `AndroidBridge.postMessage(json)`.
 */
class ReaderJsInterface(
    private val onMessage: (EngineMessage) -> Unit
) {
    @JavascriptInterface
    fun postMessage(json: String) {
        try {
            val obj = JSONObject(json)
            val name = obj.optString("name", "")
            val data = obj.optJSONObject("data") ?: JSONObject()
            onMessage(EngineMessage(name, data))
        } catch (_: Exception) {
        }
    }
}

/**
 * Holds the live [WebView] and offers a typed command API that is serialized
 * into `readerEngine.*` JavaScript calls.
 */
class ReaderEngine {
    @Volatile
    var webView: WebView? = null

    var onReady: (() -> Unit)? = null
    var onLoaded: ((title: String, author: String) -> Unit)? = null
    var onRelocate: ((fraction: Float, cfi: String, location: String, tocLabel: String) -> Unit)? = null
    var onSelection: ((text: String, cfi: String, index: Int) -> Unit)? = null
    var onTap: ((x: Float, width: Float) -> Unit)? = null
    var onToc: ((entries: List<TocEntry>) -> Unit)? = null

    data class TocEntry(val label: String, val href: String)

    private fun eval(js: String) {
        webView?.post { webView?.evaluateJavascript(js, null) }
    }

    private fun call(method: String, vararg jsonArgs: String) {
        eval("window.readerEngine && readerEngine.$method(${jsonArgs.joinToString(",")})")
    }

    fun load(base64: String) {
        val escaped = JSONObject.quote(base64)
        call("load", escaped)
    }

    fun setSettings(settings: ReaderSettings) {
        val theme = settings.themeChoice
        val json = JSONObject().apply {
            put("spacing", settings.lineHeightMultiplier.toDouble())
            put("justify", settings.justifyText)
            put("hyphenate", settings.hyphenation)
            put("fontFamily", fontFamilyCss(settings.fontChoice))
            put("fontSize", settings.fontSizeSp.toDouble())
            put("lineHeight", settings.lineHeightMultiplier.toDouble())
            put("textColor", theme.textColorHexString())
            put("backgroundColor", theme.backgroundColorHexString())
            put("themeColor", theme.accentColorHexString())
            put("maxInlineSize", settings.marginChoice.maxInlineSizePx)
        }
        call("setSettings", json.toString())
    }

    fun next() = call("next")
    fun prev() = call("prev")
    fun goToFraction(fraction: Float) = call("goToFraction", fraction.toString())
    fun goTo(target: String) = call("goTo", JSONObject.quote(target))
    fun addAnnotation(cfi: String, color: String, note: String) =
        call("addAnnotation", JSONObject.quote(cfi), JSONObject.quote(color), JSONObject.quote(note))
    fun removeAnnotation(cfi: String) = call("removeAnnotation", JSONObject.quote(cfi))
    fun clearSelection() = call("clearSelection")
    fun requestToc() = eval(
        "window.readerEngine && window.AndroidBridge.postMessage(" +
            "JSON.stringify({name:'toc', data:{entries: readerEngine.getToc()}}))"
    )

    fun handleMessage(message: EngineMessage) {
        when (message.name) {
            "ready" -> onReady?.invoke()
            "loaded" -> onLoaded?.invoke(
                message.data.optString("title", ""),
                message.data.optString("author", "")
            )
            "relocate" -> onRelocate?.invoke(
                message.data.optDouble("fraction", 0.0).toFloat(),
                message.data.optString("cfi", ""),
                message.data.optString("location", ""),
                message.data.optString("tocLabel", "")
            )
            "selection" -> onSelection?.invoke(
                message.data.optString("text", ""),
                message.data.optString("cfi", ""),
                message.data.optInt("index", 0)
            )
            "tap" -> onTap?.invoke(
                message.data.optDouble("x", 0.0).toFloat(),
                message.data.optDouble("width", 0.0).toFloat()
            )
            "toc" -> {
                val arr = message.data.optJSONArray("entries") ?: return
                val entries = ArrayList<TocEntry>()
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    entries.add(TocEntry(o.optString("label"), o.optString("href")))
                }
                onToc?.invoke(entries)
            }
        }
    }

    companion object {
        private fun fontFamilyCss(font: ReaderFontFamily): String = when (font) {
            ReaderFontFamily.SERIF -> "Georgia, 'Times New Roman', serif"
            ReaderFontFamily.SANS -> "'Helvetica Neue', Arial, sans-serif"
            ReaderFontFamily.MONOSPACE -> "'Courier New', monospace"
            ReaderFontFamily.CURSIVE -> "'Segoe Script', cursive"
        }

        private fun ReaderTheme.textColorHexString() =
            "#%06X".format(textColorHex and 0xFFFFFF)
        private fun ReaderTheme.backgroundColorHexString() =
            "#%06X".format(backgroundColorHex and 0xFFFFFF)
        private fun ReaderTheme.accentColorHexString() =
            "#%06X".format(accentColorHex and 0xFFFFFF)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ReaderWebView(
    engine: ReaderEngine,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(AndroidColor.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.setSupportZoom(false)
                settings.displayZoomControls = false
                settings.builtInZoomControls = false
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.defaultTextEncodingName = "utf-8"
                addJavascriptInterface(ReaderJsInterface(engine::handleMessage), "AndroidBridge")
                webViewClient = WebViewClient()
                engine.webView = this
                loadUrl("file:///android_asset/reader.html")
            }
        },
        update = { webView ->
            if (engine.webView !== webView) engine.webView = webView
        },
        onRelease = {
            engine.webView = null
        }
    )
}
