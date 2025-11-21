package org.fossify.notes.attack

import android.database.ContentObserver
import android.os.Handler

// 无法感知 Adb 截图
class ScreenshotObserver(handler: Handler?) : ContentObserver(handler) {
    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        // 在此处理文件夹内容变化，例如截图
        // 可以进一步判断文件路径来确认是否为截图
        println("Screenshot detected!")
    }
}

