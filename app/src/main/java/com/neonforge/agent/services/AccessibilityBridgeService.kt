package com.neonforge.agent.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AccessibilityBridgeService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        // 可在这里保存全局引用、启动轮询、通知状态等
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 可扩展：记录窗口变化、控件变化，为后续视觉/操作决策提供上下文
    }

    override fun onInterrupt() {
    }

    override fun onGlobalAction(action: Int) {
        // 可扩展：处理全局动作（返回、主页等）
    }

    companion object {
        var instance: AccessibilityBridgeService? = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}
