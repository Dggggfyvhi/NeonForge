package com.neonforge.agent.net

import android.content.Context
import android.content.SharedPreferences
import java.net.HttpURLConnection
import java.net.URL

object AgentClient {
    fun agnesSync(ctx: Context, user: String, system: String): String {
        val sp: SharedPreferences = ctx.getSharedPreferences("nf", Context.MODE_PRIVATE)
        val key = sp.getString("agnes_key", "").orEmpty()
        val model = sp.getString("agnes_model", "agnes-3.0-flash").orEmpty()
        return httpPost("https://sapiens-ai.com/api/v1/chat/completions", key, model, user, system)
    }

    fun glmSync(ctx: Context, user: String, system: String): String {
        val sp: SharedPreferences = ctx.getSharedPreferences("nf", Context.MODE_PRIVATE)
        val key = sp.getString("glm_key", "").orEmpty()
        val model = sp.getString("glm_model", "glm-4").orEmpty()
        return httpPost("https://open.bigmodel.cn/api/paas/v4/chat/completions", key, model, user, system)
    }

    private fun httpPost(url: String, key: String, model: String, user: String, system: String): String {
        if (key.isEmpty()) return "no api key configured"
        val payload = """{"model":"$model","messages":[{"role":"system","content":"$system"},{"role":"user","content":"$user"}]}"""
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $key")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.outputStream.write(payload.toByteArray())
            val in = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
            val raw = in.bufferedReader().readText()
            conn.disconnect()
            raw
        } catch (t: Throwable) {
            "request failed: ${t.message}"
        }
    }
}
