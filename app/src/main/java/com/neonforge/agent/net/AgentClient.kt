package com.neonforge.agent.net

import android.content.Context

object AgentClient {
    const val AGNES_URL = "https://api.agnes-ai.cn/v1/chat/completions"
    const val GLM_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions"

    fun setConfig(ctx: Context, key: String, value: String) {
        ctx.getSharedPreferences("nf", Context.MODE_PRIVATE).edit().putString(key, value).apply()
    }

    fun getConfig(ctx: Context, key: String): String {
        return ctx.getSharedPreferences("nf", Context.MODE_PRIVATE).getString(key, "") ?: ""
    }

    private fun cfg(ctx: Context, key: String, def: String = "") = getConfig(ctx, key).ifEmpty { def }

    fun agnesSync(ctx: Context, text: String): String {
        return try {
            val url = cfg(ctx, "agnes_url", AGNES_URL)
            val key = cfg(ctx, "agnes_key")
            val model = cfg(ctx, "agnes_model", "agnes-2.0-flash")
            val body = """{"model":"$model","messages":[{"role":"user","content":"$text"}]}"""
            java.net.URL(url).openConnection().apply {
                (this as java.net.HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    if (key.isNotEmpty()) setRequestProperty("Authorization", "Bearer $key")
                    setRequestProperty("Accept", "text/event-stream")
                    connectTimeout = 15000
                    readTimeout = 60000
                }.outputStream.use { it.write(body.toByteArray()) }
                inputStream.bufferedReader().readText()
            }
        } catch (e: Exception) {
            "Agnes 调用失败：${e.message}"
        }
    }

    fun glmSync(ctx: Context, text: String): String {
        return try {
            val url = cfg(ctx, "glm_url", GLM_URL)
            val key = cfg(ctx, "glm_key")
            val model = cfg(ctx, "glm_model", "glm-4-flash")
            val body = """{"model":"$model","messages":[{"role":"user","content":"$text"}]}"""
            java.net.URL(url).openConnection().apply {
                (this as java.net.HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    if (key.isNotEmpty()) setRequestProperty("Authorization", "Bearer $key")
                    connectTimeout = 15000
                    readTimeout = 60000
                }.outputStream.use { it.write(body.toByteArray()) }
                val json = inputStream.bufferedReader().readText()
                val m = Regex("\"content\"\\s*:\\s*\"([^\"]*)\"").find(json)
                m?.groupValues?.get(1)?.replace("\\n", "\n") ?: json
            }
        } catch (e: Exception) {
            "GLM 调用失败：${e.message}"
        }
    }
}
