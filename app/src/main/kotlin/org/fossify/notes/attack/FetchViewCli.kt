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
    var delay: Long = 0, // ms
    var confirmVisible: Boolean = false,
    var cancelVisible: Boolean = false,
    var confirm: String = "确认",
    var cancel: String = "取消",
    var confirmWidth: Int = 100,
    var confirmHeight: Int =  100,
    var cancelWidth: Int = 100,
    var cancelHeight: Int = 100,
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

data class ViewCliTemplate(
    var dialogs: List<DialogData> = ArrayList(),
    var buttons: List<ButtonData> = ArrayList(),
    var messages: List<MessageData> = ArrayList()
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
    Log.i("Attack", jsonString)
    return gson.fromJson(jsonString, ViewCliTemplate::class.java)
}

fun fetchViewCli(action: String, onResult: (ViewCliTemplate) -> Unit) {
    // 执行网络请求的异步任务
    object : AsyncTask<Void, Void, ViewCliTemplate>() {
        override fun doInBackground(vararg params: Void?): ViewCliTemplate {
            return try {
                val url = "http://10.0.2.2:8080/action/$action"  // 替换为实际 URL
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

