package com.neonforge.agent.services

import android.app.Service
import android.content.Intent
import android.os.IBinder

class OverlayMonitorService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
