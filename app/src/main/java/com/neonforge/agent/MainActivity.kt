package com.neonforge.agent

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.neonforge.agent.net.AgentClient
import com.neonforge.agent.net.DeviceStatusProvider
import com.neonforge.agent.services.OverlayMonitorService
import kotlinx.coroutines.*
import org.json.JSONObject
import android.accessibilityservice.AccessibilityService

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private val scope = CoroutineScope(Dispatchers.IO)

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        webView.addJavascriptInterface(jsBridge(), "NF")
        webView.webViewClient = object : WebViewClient() {}
        webView.loadDataWithBaseURL(null, buildHtml(), "text/html", "utf-8", null)
        ensurePermission(Manifest.permission.SYSTEM_ALERT_WINDOW)
    }

    private fun ensurePermission(p: String) {
        if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(p), 1)
        }
    }

    private fun jsBridge() = NFJS

    inner class NFJS {
        @JavascriptInterface
        fun status(): String {
            val o = JSONObject()
            o.put("battery", DeviceStatusProvider.batteryJson(this@MainActivity))
            o.put("usb", DeviceStatusProvider.usbJson(this@MainActivity))
            o.put("accessibility", DeviceStatusProvider.accessibilityJson(this@MainActivity))
            o.put("overlay", DeviceStatusProvider.overlayJson(this@MainActivity))
            return o.toString()
        }

        @JavascriptInterface
        fun agnes(user: String, system: String?): String {
            val job = scope.launch {
                AgentClient.agnesSync(this@MainActivity, user, system ?: "You are NeonForge.").let {
                    runOnUiThread {
                        webView.evaluateJavascript("NF.onResult('$it')", null)
                    }
                }
            }
            return job.joinAndReturn()
        }

        @JavascriptInterface
        fun glm(user: String, system: String?): String {
            val job = scope.launch {
                AgentClient.glmSync(this@MainActivity, user, system ?: "You are NeonForge Agent.").let {
                    runOnUiThread {
                        webView.evaluateJavascript("NF.onResult('$it')", null)
                    }
                }
            }
            return job.joinAndReturn()
        }
    }

    private fun Coroutine.joinAndReturn(): String {
        // Blocking JS bridge thread is acceptable here because we launch in IO scope and then evaluate UI
        val latch = java.util.concurrent.CountDownLatch(1)
        val holder = java.util.concurrent.atomic.AtomicReference("")
        this.invokeOnCompletion {
            holder.set(it ?: "")
            latch.countDown()
        }
        latch.await()
        return holder.get()
    }

    private fun buildHtml(): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
        <meta charset="utf-8"/>
        <style>
        :root{color-scheme:dark}
        body{margin:0;background:#0a0c12;color:#e7ecff;font:14px/1.5 system-ui,sans-serif}
        .wrap{max-width:860px;margin:0 auto;padding:18px}
        h1{font-size:22px;margin:0 0 8px}
        .card{border:1px solid #1d2438;border-radius:14px;background:#0f1220;padding:14px;margin:12px 0}
        .status{color:#8ee3ff}
        textarea{width:100%;height:84px;background:#080a12;color:#eaf6ff;border:1px solid #243056;border-radius:10px;padding:10px}
        button{background:#00f0ff;color:#001018;border:none;border-radius:10px;padding:10px 14px;margin-top:8px}
        .msg{white-space:pre-wrap;color:#dfe6ff;padding:8px;border-radius:8px;background:#101424;margin-top:8px}
        </style>
        </head>
        <body>
        <div class="wrap">
          <h1>NeonForge</h1>
          <div class="card status" id="status">loading</div>
          <div class="card">
            <textarea id="prompt">开始任务</textarea>
            <button onclick="ask('agnes')">Agnes</button>
            <button onclick="ask('glm')">GLM Agent</button>
            <div id="out"></div>
          </div>
        </div>
        <script>
        function ask(kind){
          const p=document.getElementById('prompt').value;
          document.getElementById('out').innerHTML='<div class="msg">waiting...</div>';
          if(kind==='agnes') NF.agnes(p, null); else NF.glm(p, null);
        }
        NF.onResult=function(text){
          document.getElementById('out').innerHTML='<div class="msg">'+text.replace(/</g,'<').replace(/>/g,'>')+'</div>';
        };
        function refresh(){ document.getElementById('status').textContent=NF.status(); setInterval(refresh,3000); }
        refresh();
        </script>
        </body>
        </html>
        """.trimIndent()
    }
}
