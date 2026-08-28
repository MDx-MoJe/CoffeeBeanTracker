package com.coffee.beantracker.utils

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewTreeObserver

object GlassBlurHelper {

    private const val DEFAULT_BLUR_RADIUS = 25f
    private const val UPDATE_INTERVAL_MS = 80L

    fun applyGlassBlur(target: View, radius: Float = DEFAULT_BLUR_RADIUS) {
        val activity = target.context as? Activity ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val handler = Handler(Looper.getMainLooper())
        var blurStarted = false

        target.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                target.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (!blurStarted && target.width > 0 && target.height > 0) {
                    blurStarted = true
                    handler.post(blurRunnable(target, radius, handler))
                }
            }
        })

        target.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {}
            override fun onViewDetachedFromWindow(v: View) {
                handler.removeCallbacksAndMessages(null)
            }
        })
    }

    private fun blurRunnable(target: View, radius: Float, handler: Handler): Runnable {
        return object : Runnable {
            override fun run() {
                captureAndApplyGlass(target)
                if (target.isAttachedToWindow && target.isShown) {
                    handler.postDelayed(this, UPDATE_INTERVAL_MS)
                }
            }
        }
    }

    private fun captureAndApplyGlass(target: View) {
        if (!target.isShown || target.width == 0 || target.height == 0) return

        val activity = target.context as? Activity ?: return

        val bitmap = Bitmap.createBitmap(target.width, target.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val decorView = activity.window.decorView

        val rect = Rect()
        target.getGlobalVisibleRect(rect)
        canvas.save()
        canvas.translate(-rect.left.toFloat(), -rect.top.toFloat())
        decorView.draw(canvas)
        canvas.restore()

        target.post {
            target.background = BitmapDrawable(target.resources, bitmap)
            target.alpha = 0.92f
        }
    }

    fun applyBlurToView(view: View, radius: Float = 4f) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        try {
            view.setRenderEffect(
                android.graphics.RenderEffect.createBlurEffect(
                    radius, radius, android.graphics.Shader.TileMode.CLAMP
                )
            )
        } catch (_: Exception) {
        }
    }

    fun clearBlur(view: View) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        try {
            view.setRenderEffect(null)
        } catch (_: Exception) {
        }
    }

    fun tintWithFrostGlass(bitmap: Bitmap, isDark: Boolean): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(bitmap, 0f, 0f, Paint())
        val tintPaint = Paint().apply {
            color = if (isDark) 0x801D1D1D.toInt() else 0x80FFFFFF.toInt()
        }
        canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), tintPaint)
        return result
    }
}
