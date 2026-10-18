package com.neonforge.agent.net

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager

class DeviceStatusProvider(private val context: Context) {
    private val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

    fun snapshot(): Map<String, Any> {
        val usb = usbDebuggingEnabled()
        val net = hasInternet()
        val acc = accessibilityEnabled()
        val overlay = canDrawOverlays()

        return mapOf(
            "usbConnected" to usb,
            "networkOnline" to net,
            "accessibilityEnabled" to acc,
            "overlayAvailable" to overlay,
            "bridgeMode" to "apk_builtin",
            "runtime" to "android",
            "androidSdk" to Build.VERSION.SDK_INT,
            "appName" to "NeonForge"
        )
    }

    fun refresh() {
        // 预留：可在这里做轮询、缓存、通知等扩展
    }

    private fun usbDebuggingEnabled(): Boolean {
        return try {
            val value = Settings.Global.getInt(context.contentResolver, "adb_enabled", 0)
            value == 1
        } catch (e: Exception) {
            false
        }
    }

    private fun hasInternet(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun accessibilityEnabled(): Boolean {
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityService.INFO_TYPE_ALL)
        return enabledServices.any {
            it.serviceInfo.packageName == context.packageName
        }
    }

    private fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(context)
    }
}
