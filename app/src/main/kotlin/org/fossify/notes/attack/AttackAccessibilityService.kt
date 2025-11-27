package org.fossify.notes.attack

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.util.Log

/**
 * 占位的无障碍服务，用于在设置中显示并可手动开启。
 * 当前未拦截事件，仅用于引导用户授权。
 */
class AttackAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i("AttackAccService", "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op: 目前不做事件处理
    }

    override fun onInterrupt() {
        // No-op
    }
}
