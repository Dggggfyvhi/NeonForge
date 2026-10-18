package com.neonforge.agent.net

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.provider.Settings
import org.json.JSONObject

object DeviceStatusProvider {
    fun batteryJson(ctx: Context): String {
        val o = JSONObject()
        val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        o.put("level", bm.level)
        o.put("scale", bm.scale)
        o.put("plugged", bm.plugged)
        o.put("health", bm.health)
        return o.toString()
    }

    fun usbJson(ctx: Context): String {
        val o = JSONObject()
        val powerManager = ctx.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        o.put("charging", powerManager.isInteractive)
        return o.toString()
    }

    fun accessibilityJson(ctx: Context): String {
        val o = JSONObject()
        o.put("enabled", Settings.Secure.getString(ctx.contentResolver, "enabled_accessibility_services")?.contains("com.neonforge.agent/.services.AccessibilityBridgeService") == true)
        return o.toString()
    }

    fun overlayJson(ctx: Context): String {
        val o = JSONObject()
        o.put("granted", ctx.checkSelfPermission(android.Manifest.permission.SYSTEM_ALERT_WINDOW) == PackageManager.PERMISSION_GRANTED)
        return o.toString()
    }
}
