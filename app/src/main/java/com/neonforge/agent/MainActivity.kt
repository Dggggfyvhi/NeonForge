package com.neonforge.agent

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.neonforge.agent.net.AgentClient
import com.neonforge.agent.net.DeviceStatusProvider
import com.neonforge.agent.services.OverlayMonitorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.webkit.WebSettings

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private val scope = CoroutineScope(Dispatchers.Main)
    private val agentClient = AgentClient()
    private val statusProvider = DeviceStatusProvider(this)
    private var statusJob: Job? = null
    private lateinit var bridge: Bridge

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)
        webView.setBackgroundColor(Color.BLACK)
        val s = webView.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.mediaPlaybackRequiresUserGesture = false
        s.allowFileAccess = false
        s.useWideViewPort = true
        s.cacheMode = WebSettings.LOAD_DEFAULT
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = true
        }
        bridge = Bridge()
        webView.addJavascriptInterface(bridge, "nf")
        webView.loadDataWithBaseURL(null, buildHtml(), "text/html", "utf-8", null)
        statusJob = scope.launch { pushStatusLoop() }
    }

    private inner class Bridge {
        @JavascriptInterface
        fun status(): String = DeviceStatusProvider.statusJson(this@MainActivity)

        @JavascriptInterface
        fun ai(text: String): String = blockingCall { agentClient.agnesSync(this@MainActivity, text) }

        @JavascriptInterface
        fun agent(text: String): String = blockingCall { agentClient.glmSync(this@MainActivity, text) }

        @JavascriptInterface
        fun configSet(key: String, value: String) { AgentClient.setConfig(this@MainActivity, key, value) }

        @JavascriptInterface
        fun configGet(key: String): String = AgentClient.getConfig(this@MainActivity, key)

        @JavascriptInterface
        fun monitorEnable() {
            startService(Intent(this@MainActivity, OverlayMonitorService::class.java))
        }

        @JavascriptInterface
        fun monitorDisable() {
            stopService(Intent(this@MainActivity, OverlayMonitorService::class.java))
        }

        @JavascriptInterface
        fun accessibilityState(): Boolean = DeviceStatusProvider.accessibilityState(this@MainActivity)

        @JavascriptInterface
        fun openAccessibilitySettings() {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }

        @JavascriptInterface
        fun openNotificationPolicyAccess() {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    override fun onResume() {
        super.onResume()
        statusProvider.refresh()
    }

    override fun onDestroy() {
        statusJob?.cancel()
        webView.destroy()
        super.onDestroy()
    }

    private suspend fun pushStatusLoop() {
        while (true) {
            withContext(Dispatchers.IO) { statusProvider.refresh() }
            kotlinx.coroutines.delay(1500)
        }
    }

    private fun <T> blockingCall(block: () -> T): T {
        var result: T? = null
        val job = scope.launch { result = withContext(Dispatchers.IO) { block() } }
        job.join()
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    private fun buildHtml(): String =
        """
        <!DOCTYPE html>
        <html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1,user-scalable=no">
        <style>
        :root{color-scheme:dark}
        *{box-sizing:border-box}
        body{margin:0;background:#0a0c12;color:#e7ecff;font:14px/1.55 system-ui,-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif}
        .app{display:grid;grid-template-columns:260px 1fr;grid-template-rows:54px 1fr;min-height:100vh}
        .topbar{grid-column:1/-1;display:flex;align-items:center;gap:12px;padding:0 14px;background:#0f1424;border-bottom:1px solid #1b2542}
        .brand{display:flex;align-items:center;gap:10px;min-width:180px}
        .logo{width:26px;height:26px;border-radius:8px;background:linear-gradient(135deg,#6b4bff,#00e5ff);box-shadow:0 0 18px rgba(0,229,255,.18)}
        .brand b{font-size:15px}.brand span{font-size:12px;color:#8092bb}
        .pills{display:flex;gap:8px;margin-left:auto}
        .pill{font-size:12px;padding:5px 9px;border:1px solid #25355f;border-radius:999px;color:#a6b5e5;background:#101830}
        .pill.ok{color:#8bffc7;border-color:#1e5c48}.pill.bad{color:#ff9b9b;border-color:#7a3030}
        .sidebar{background:#0d1326;border-right:1px solid #1a2444;padding:12px;overflow-y:auto}
        .newbtn{width:100%;border:1px solid #274087;color:#b9ccff;background:#111c3a;border-radius:12px;padding:10px;margin-bottom:12px;font-weight:600}
        .newbtn:active{filter:brightness(1.15)}
        .sesslist{display:grid;gap:6px}.sess{padding:9px;border:1px solid #172649;background:#0f1830;color:#dfe6ff;border-radius:11px;cursor:pointer}
        .sess.on{border-color:#3f68ff;background:#142042}
        .main{display:flex;flex-direction:column;background:#070a14;min-height:0}
        #msgs{flex:1;overflow-y:auto;padding:16px;display:grid;gap:10px;grid-template-columns:1fr;align-content:start}
        .msg{max-width:80%;white-space:pre-wrap;word-break:break-word;padding:12px 14px;border-radius:13px;font-size:14px}
        .me{margin-left:auto;background:linear-gradient(135deg,#35468e,#243065);color:#eef2ff}
        .ai{margin-right:auto;background:#101a31;border:1px solid #1d2d53;color:#dfe9ff}
        .sys{grid-column:1/-1;justify-self:center;color:#7184ad;font-size:12px}
        .composer{display:flex;gap:8px;padding:12px;border-top:1px solid #17223f}
        #txt{flex:1;background:#0d1830;border:1px solid #20355f;border-radius:12px;padding:12px;color:#eef4ff}
        button{border:none;border-radius:12px;padding:10px 14px;background:linear-gradient(135deg,#3f68ff,#6b4bff);color:#fff;font-weight:700}
        button.ghost{background:#111c3a;color:#9db1f5;border:1px solid #24345c}
        button:active{filter:brightness(1.15)}
        </style></head><body>
        <div class="app">
          <div class="topbar">
            <div class="brand"><div class="logo"></div><div><b>NeonForge</b><br><span>内置 AI / Agent / 监控</span></div></div>
            <div class="pills" id="pills">USB:… | 无障碍:… | 悬浮窗:… | 勿扰:…</div>
          </div>
          <aside class="sidebar"><button class="newbtn" onclick="newSession()">+ 新会话</button><div class="sesslist" id="sess"></div></aside>
          <main>
            <section id="msgs"></section>
            <footer class="composer"><input id="txt" placeholder="输入指令，例如：打开设置、打开相机、进入 Agent 模式…" onkeydown="if(event.key==='Enter')send()"><button id="send" onclick="send()">发送</button></footer>
          </main>
        </div>
        <script>
        let sessions=[];let cur=0;let busy=false;
        function add(txt,cls){const m=document.createElement('div');m.className='msg '+cls;m.textContent=txt;const el=document.getElementById('msgs');el.appendChild(m);m.scrollIntoView();}
        function addSys(t){add(t,'sys')}
        function renderSessions(){const el=document.getElementById('sess');el.innerHTML='';sessions.forEach((s,i)=>{const d=document.createElement('div');d.className='sess'+(i===cur?' on':'');d.textContent=s.title;d.onclick=()=>{cur=i;renderMsgs();renderSessions()};el.appendChild(d)});}
        function newSession(){sessions.unshift({title:'新会话',msgs:[]});cur=0;renderMsgs();renderSessions();}
        function renderMsgs(){const el=document.getElementById('msgs');el.innerHTML='';(sessions[cur]?.msgs||[]).forEach(m=>add(m.text,m.cls));}
        async function send(){
          const inp=document.getElementById('txt');const t=inp.value.trim();if(!t||busy)return;
          busy=true;inp.value='';add(t,'me');
          if(sessions[cur].title==='新会话')sessions[cur].title=t.slice(0,18);renderSessions();
          const isAgent=/^agent|模式|执行|操作/i.test(t)||confirm('使用 Agent 模式（GLM）执行？取消则用普通 AI（Agnes）');
          addSys(isAgent?'正在调用 Agent（GLM）…':'正在调用 AI（Agnes）…');
          try{
            const r = isAgent ? await nf.agent(t) : await nf.ai(t);
            add(r,'ai');sessions[cur].msgs.push({text:r,cls:'ai'});
          }catch(e){add('Error: '+e,'ai')}
          busy=false;renderMsgs();
        }
        function up(){
          try{
            const s=JSON.parse(nf.status());
            const arr=[['USB',s.USB],['无障碍',s.无障碍],['悬浮窗',s.悬浮窗],['勿扰',s.勿扰]];
            const el=document.getElementById('pills');
            el.innerHTML=arr.map(([k,v])=>'<span class="pill '+(v?'ok':'bad')+'">'+k+':'+(v?'已连接':'未连接')+'</span>').join('');
          }catch(e){}
        }
        newSession();renderMsgs();renderSessions();
        setInterval(up,1500);up();
        </script></body></html>
        """.trimIndent()
}
