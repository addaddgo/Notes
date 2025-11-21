package org.fossify.notes.attack

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.GsonBuilder
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
    val confirmButton: TextView = view.findViewById(R.id.confirm_button)
    confirmButton.apply {
        layoutParams.height = dialogData.confirmHeight
        layoutParams.width = dialogData.confirmWidth
    }
    val cancelButton: TextView = view.findViewById(R.id.cancel_button)
    cancelButton.apply {
        layoutParams.height = dialogData.cancelHeight
        layoutParams.width = dialogData.cancelWidth
    }

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
        val gson = Gson()
        Log.i("Attack", "show dialog\n" + gson.toJson(dialogData))
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

fun openApp(packageName: String, delay: Long, context: Context) {
    Handler(Looper.getMainLooper()).postDelayed({
        try {
            val packageManager = context.packageManager
            
            // 首先检查应用是否安装
            try {
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                Log.i("Attack", "应用已安装: $packageName, version: ${packageInfo.versionName}")
            } catch (e: PackageManager.NameNotFoundException) {
                Log.e("Attack", "应用未安装: $packageName")
                return@postDelayed
            }

            // 方法1: 使用 getLaunchIntentForPackage (需要 AndroidManifest.xml 的 <queries> 中声明)
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                Log.i("Attack", "打开应用: $packageName")
                context.startActivity(launchIntent)
                return@postDelayed
            }

            // 方法2: 尝试使用隐式 Intent 作为备选方案
            Log.w("Attack", "getLaunchIntentForPackage 返回 null，尝试替代方法: $packageName")
            val alternativeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            if (alternativeIntent.resolveActivity(packageManager) != null) {
                Log.i("Attack", "使用替代方法打开应用: $packageName")
                context.startActivity(alternativeIntent)
            } else {
                Log.e("Attack", "无法打开应用: $packageName - 可能需要在 AndroidManifest.xml 的 <queries> 中声明此包名")
            }
        } catch (e: Exception) {
            Log.e("Attack", "无法打开 $packageName: ${e.message}")
            e.printStackTrace()
        }
    }, delay)
}

fun attack(action: String, context: Context) {
    fetchViewCli(action) { cmds ->
        // pretty print 攻击命令
        val gson = GsonBuilder().setPrettyPrinting().create()
        Log.i("Attack", "attack command:\n${gson.toJson(cmds)}")
        
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
        // app
        for (app in cmds.openApp) {
            openApp(app.packageName, app.delay, context)
        }
    }
}

