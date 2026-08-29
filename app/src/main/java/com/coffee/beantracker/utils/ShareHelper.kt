package com.coffee.beantracker.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import com.coffee.beantracker.utils.ToastCustom

object ShareHelper {

    private data class Backup(val original: Bitmap, val view: ImageView)

    /**
     * 从任意 Drawable 中提取 Bitmap（处理多层包装）
     */
    private fun extractBitmap(iv: ImageView): Bitmap? {
        var d: Drawable? = iv.drawable
        // 解开可能的包装层
        while (d != null) {
            when (d) {
                is BitmapDrawable -> return d.bitmap
                // RoundedBitmapDrawable 也通过反射处理，避免 import 依赖
                else -> {
                    // 尝试反射获取内部 drawable（Coil 的 ScaleDrawable 等）
                    try {
                        val field = d.javaClass.getDeclaredField("drawable")
                        field.isAccessible = true
                        d = field.get(d) as? Drawable
                    } catch (_: Exception) {
                        return null
                    }
                }
            }
        }
        return null
    }

    /**
     * 递归将 View 树中所有 HARDWARE 位图替换为软件副本
     */
    private fun swapHardwareBitmaps(view: View): List<Backup> {
        val backups = mutableListOf<Backup>()
        if (view is ImageView) {
            val bitmap = extractBitmap(view)
            if (bitmap != null && bitmap.config == Bitmap.Config.HARDWARE) {
                val softwareCopy = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                if (softwareCopy != null) {
                    backups.add(Backup(bitmap, view))
                    view.setImageBitmap(softwareCopy)
                }
            }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                backups.addAll(swapHardwareBitmaps(view.getChildAt(i)))
            }
        }
        // 处理 background
        val bg = view.background
        if (bg is BitmapDrawable && bg.bitmap?.config == Bitmap.Config.HARDWARE) {
            val softwareCopy = bg.bitmap.copy(Bitmap.Config.ARGB_8888, false)
            if (softwareCopy != null) {
                view.background = BitmapDrawable(view.resources, softwareCopy)
            }
        }
        return backups
    }

    private fun restoreBackups(backups: List<Backup>) {
        for (b in backups) {
            b.view.setImageBitmap(b.original)
        }
    }

    /**
     * 将 View 转为 Bitmap —— 兼容硬件加速位图
     */
    fun captureView(view: View): Bitmap {
        var w = view.width
        var h = view.height
        if (w <= 0 || h <= 0) {
            val parentWidth = (view.parent as? ViewGroup)?.width
                ?: (view.resources.displayMetrics.widthPixels -
                    (view.resources.displayMetrics.density * 28).toInt())
            view.measure(
                View.MeasureSpec.makeMeasureSpec(parentWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
            w = view.measuredWidth
            h = view.measuredHeight
        }

        // 1) 递归替换所有 HARDWARE 位图
        val backups = swapHardwareBitmaps(view)

        // 2) 切换到软件渲染层
        val origLayer = view.layerType
        view.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        view.buildLayer()

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(0xFFFFFFFF.toInt())
        view.draw(canvas)

        // 3) 恢复
        view.setLayerType(origLayer, null)
        restoreBackups(backups)
        return bitmap
    }

    /**
     * 将卡片 View 保存为图片并分享
     */
    fun shareViewAsImage(context: Context, view: View, title: String) {
        try {
            val bitmap = captureView(view)
            val cacheDir = File(context.cacheDir, "shared_images").apply { if (!exists()) mkdirs() }
            val imageFile = File(cacheDir, "coffee_card_" + System.currentTimeMillis() + ".png")
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bitmap.recycle()

            val uri: Uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".fileprovider",
                imageFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TITLE, title)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, title))
        } catch (t: Throwable) {
            t.printStackTrace()
            ToastCustom.show(context, context.getString(com.coffee.beantracker.R.string.share_failed, t.message ?: context.getString(com.coffee.beantracker.R.string.unknown_error)), android.widget.Toast.LENGTH_SHORT)
        }
    }
}