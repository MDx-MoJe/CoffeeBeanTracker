package com.coffee.beantracker.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.text.LineBreaker
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.coffee.beantracker.data.CoffeeBean
import com.coffee.beantracker.data.RoastLevel
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object TscLabelBuilder {

    enum class PrintProtocol {
        ESC_POS,
        TSPL,
        TRY_BOTH
    }

    private const val DOTS_PER_MM = 8

    const val LABEL_WIDTH_MM = 50
    const val LABEL_HEIGHT_MM = 50
    const val GAP_MM = 2

    const val LABEL_WIDTH_DOT = LABEL_WIDTH_MM * DOTS_PER_MM   // 400
    const val LABEL_HEIGHT_DOT = LABEL_HEIGHT_MM * DOTS_PER_MM  // 400

    private const val PAD = 24
    private const val USABLE_W = LABEL_WIDTH_DOT - PAD * 2

    private const val TITLE_SIZE = 40f
    private const val KEY_SIZE = 22f
    private const val VALUE_SIZE = 22f
    private const val FLAVOR_SIZE = 20f
    private const val ROW_GAP = 8
    private const val SECTION_GAP = 10

    private val ASCII: Charset by lazy { Charset.forName("ISO-8859-1") }

    fun buildCoffeeBeanLabelPacket(
        bean: CoffeeBean,
        dateFormat: SimpleDateFormat,
        protocol: PrintProtocol = PrintProtocol.ESC_POS
    ): Pair<ByteArray, ByteArray> {
        val bmp = renderLabelBitmap(bean, dateFormat)
        val out = when (protocol) {
            PrintProtocol.ESC_POS -> buildEscPosTwoPhase(bmp)
            PrintProtocol.TSPL    -> buildTsplBitmapCommands(bmp) to ByteArray(0)
            PrintProtocol.TRY_BOTH -> {
                val (a, b) = buildEscPosTwoPhase(bmp)
                val t = buildTsplBitmapCommands(bmp)
                val combinedA = ByteArray(a.size + t.size)
                System.arraycopy(a, 0, combinedA, 0, a.size)
                System.arraycopy(t, 0, combinedA, a.size, t.size)
                combinedA to b
            }
        }
        bmp.recycle()
        return out
    }

    @Deprecated("use buildCoffeeBeanLabelPacket for two-phase send")
    fun buildCoffeeBeanLabelBytes(
        bean: CoffeeBean,
        dateFormat: SimpleDateFormat,
        protocol: PrintProtocol = PrintProtocol.ESC_POS
    ): ByteArray {
        val (a, b) = buildCoffeeBeanLabelPacket(bean, dateFormat, protocol)
        val out = ByteArray(a.size + b.size)
        System.arraycopy(a, 0, out, 0, a.size)
        System.arraycopy(b, 0, out, a.size, b.size)
        return out
    }

    private fun renderLabelBitmap(bean: CoffeeBean, sdf: SimpleDateFormat): Bitmap {
        val bmp = Bitmap.createBitmap(LABEL_WIDTH_DOT, LABEL_HEIGHT_DOT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK; typeface = Typeface.DEFAULT_BOLD; textSize = TITLE_SIZE
        }
        val keyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK; typeface = Typeface.DEFAULT_BOLD; textSize = KEY_SIZE
        }
        val valuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK; typeface = Typeface.DEFAULT; textSize = VALUE_SIZE
        }
        val flavorPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK; typeface = Typeface.DEFAULT; textSize = FLAVOR_SIZE
        }
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 2f
        }

        var y = PAD
        val titleLayout = buildStaticLayout(bean.name.ifBlank { "咖啡豆" }, titlePaint, USABLE_W)
        drawLayoutAt(canvas, titleLayout, PAD.toFloat(), y.toFloat())
        y += titleLayout.height + 8
        canvas.drawLine(PAD.toFloat(), y.toFloat(), (LABEL_WIDTH_DOT - PAD).toFloat(), y.toFloat(), dividerPaint)
        y += 10
        val millisPerDay = TimeUnit.DAYS.toMillis(1)
        y = drawKv(canvas, "烘焙深度:  ", resolveRoast(bean), keyPaint, valuePaint, PAD, y, USABLE_W); y += ROW_GAP
        y = drawKv(canvas, "处理方式:  ", bean.processMethod.ifBlank { "-" }, keyPaint, valuePaint, PAD, y, USABLE_W); y += ROW_GAP
        y = drawKv(canvas, "产地:       ", bean.origin.ifBlank { "-" }, keyPaint, valuePaint, PAD, y, USABLE_W); y += ROW_GAP
        if (bean.developmentTime.isNotBlank()) {
            y = drawKv(canvas, "发展时间:  ", bean.developmentTime, keyPaint, valuePaint, PAD, y, USABLE_W); y += ROW_GAP
        }
        y = drawSection(canvas, "风味描述:  ", bean.flavorNotes.ifBlank { "-" }, keyPaint, flavorPaint, PAD, y, USABLE_W); y += SECTION_GAP
        val roastDateStr = sdf.format(Date(bean.roastDate))
        val bestCal = Calendar.getInstance().apply { timeInMillis = bean.roastDate + bean.bestBeforeDays * millisPerDay }
        val bestDateStr = sdf.format(bestCal.time)
        val restDaysValue = bean.restDays
        y = drawKv(canvas, "烘焙日期:  ", roastDateStr, keyPaint, valuePaint, PAD, y, USABLE_W); y += ROW_GAP
        y = drawKv(canvas, "赏味期限:  ", bestDateStr, keyPaint, valuePaint, PAD, y, USABLE_W); y += ROW_GAP
        y = drawKv(canvas, "养豆时间:  ", "$restDaysValue 天", keyPaint, valuePaint, PAD, y, USABLE_W)
        return bmp
    }

    private fun resolveRoast(bean: CoffeeBean): String {
        return try { RoastLevel.valueOf(bean.roastLevel).displayName }
               catch (_: Throwable) { bean.roastLevel.ifBlank { "-" } }
    }

    private fun drawLayoutAt(cv: Canvas, layout: StaticLayout, x: Float, y: Float) {
        val save = cv.save()
        try { cv.translate(x, y); layout.draw(cv) } finally { cv.restoreToCount(save) }
    }

    private fun drawKv(cv: Canvas, key: String, value: String, kp: TextPaint, vp: TextPaint,
                       pad: Int, y0: Int, usableW: Int): Int {
        val keyWidth = measureTextWidth(kp, key)
        val valueWidth = (usableW - keyWidth).coerceAtLeast(usableW * 2f / 5f)
        val keyLayout = buildStaticLayout(key, kp, usableW)
        val valueLayout = buildStaticLayout(value, vp, valueWidth.toInt())
        val totalH = maxOf(keyLayout.height, valueLayout.height)
        drawLayoutAt(cv, keyLayout, pad.toFloat(), y0.toFloat())
        drawLayoutAt(cv, valueLayout, pad + keyWidth, y0.toFloat())
        return y0 + totalH
    }

    private fun drawSection(cv: Canvas, key: String, value: String, kp: TextPaint, vp: TextPaint,
                            pad: Int, y0: Int, usableW: Int): Int {
        val keyLayout = buildStaticLayout(key, kp, usableW)
        drawLayoutAt(cv, keyLayout, pad.toFloat(), y0.toFloat())
        var y = y0 + keyLayout.height + 2
        val valueLayout = buildStaticLayout(value, vp, usableW)
        drawLayoutAt(cv, valueLayout, (pad + 16).toFloat(), y.toFloat())
        y += valueLayout.height
        return y
    }

    private fun measureTextWidth(paint: Paint, text: String): Float {
        val rect = android.graphics.Rect()
        paint.getTextBounds(text, 0, text.length, rect)
        return rect.width().toFloat() + 4f
    }

    @Suppress("DEPRECATION")
    private fun buildStaticLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(20))
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .setBreakStrategy(LineBreaker.BREAK_STRATEGY_SIMPLE)
                .build()
        } else {
            StaticLayout(text, paint, width.coerceAtLeast(20), Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false)
        }
    }

    private fun buildTsplBitmapCommands(src: Bitmap): ByteArray {
        val out = ByteArrayOutputStream(4096 + src.width / 8 * src.height + 128)
        out.write(ByteArray(3) { 0x18 })
        out.write(ByteArray(5) { 0x00 })
        out.write(byteArrayOf(0x1B, 0x40))
        out.write(ByteArray(5) { 0x00 })
        fun t(line: String) { out.write(line.toByteArray(ASCII)); out.write(0x0D); out.write(0x0A) }
        t("SIZE " + LABEL_WIDTH_MM + " mm," + LABEL_HEIGHT_MM + " mm")
        t("GAP " + GAP_MM + " mm,0 mm")
        t("SPEED 3"); t("DENSITY 8"); t("DIRECTION 1,0"); t("REFERENCE 0,0"); t("CLS")
        val rowBytes = (src.width + 7) / 8
        val packed = bitmapToPacked1bpp(src)
        out.write("BITMAP 0,0,".toByteArray(ASCII))
        out.write(rowBytes.toString().toByteArray(ASCII))
        out.write(",".toByteArray(ASCII))
        out.write(src.height.toString().toByteArray(ASCII))
        out.write(",0,".toByteArray(ASCII))
        out.write(packed); out.write(0x0D); out.write(0x0A)
        t("PRINT 1,1"); t("HOME"); t("SET TEAR ON")
        out.write(ByteArray(16) { 0x00 })
        out.write(0x0D); out.write(0x0A); out.write(0x0D); out.write(0x0A)
        return out.toByteArray()
    }

    private fun buildEscPosTwoPhase(src: Bitmap): Pair<ByteArray, ByteArray> {
        val packed = bitmapToPacked1bpp(src)
        val rowBytes = (src.width + 7) / 8   // 50
        val height = src.height              // 400
        val xl = (rowBytes and 0xFF).toByte()
        val xh = ((rowBytes shr 8) and 0xFF).toByte()
        val yl = (height and 0xFF).toByte()         // 0x90 = 144
        val yh = ((height shr 8) and 0xFF).toByte()  // 0x01 = 256

        // ===== PHASE 1: 纯位图打印（不切任何标签/间隙模式，避免 MCU 状态混乱）=====
        val a = ByteArrayOutputStream(256 + packed.size)
        a.write(byteArrayOf(0x1B, 0x40))                              // ESC @ 初始化
        a.write(byteArrayOf(0x1B, 0x61, 0x00))                        // ESC a 0 左对齐
        a.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00))                  // GS v 0 0 位图
        a.write(byteArrayOf(xl, xh, yl, yh))                          // xL xH=50 ; yL yH=400 (50mm)
        a.write(packed)

        // ===== PHASE 2: 3 秒后再发（等位图物理走完停在差 95mm 处）=====
        // 策略：完全放弃所有传感器/间隙对齐命令，纯步进电机精确走 760 dots = 95 mm
        // GS J n (0x1D 0x4A n) 单字节最多 255 dots，分三段：
        //   255 + 255 + 250 = 760 dots = 95 mm  （用户实测差 95 mm 到撕纸位）
        val b = ByteArrayOutputStream(64)
        b.write(ByteArray(8) { 0x00 })                                  // 分界 padding
        b.write(byteArrayOf(0x1B, 0x40))                                // ESC @ 清命令解析器
        b.write(byteArrayOf(0x1D, 0x4A, 0xFF.toByte()))                 // GS J 255
        b.write(byteArrayOf(0x1D, 0x4A, 0xFF.toByte()))                 // GS J 255  = 510 dots
        b.write(byteArrayOf(0x1D, 0x4A, 0xFA.toByte()))                 // GS J 250  = 760 dots = 95mm
        b.write(ByteArray(16) { 0x00 })                                 // 串口填充推动

        return a.toByteArray() to b.toByteArray()
    }

    private fun bitmapToPacked1bpp(src: Bitmap): ByteArray {
        val w = src.width
        val h = src.height
        val rowBytes = (w + 7) / 8
        val out = ByteArray(rowBytes * h)
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        var i = 0
        for (y in 0 until h) {
            var bitIdx = 0
            var curByte = 0
            for (x in 0 until w) {
                val color = pixels[y * w + x]
                val r = (color shr 16) and 0xff
                val g = (color shr 8) and 0xff
                val b = color and 0xff
                val lum = (0.2126 * r + 0.7152 * g + 0.0722 * b).toInt()
                val isBlack = lum < 160
                if (isBlack) curByte = curByte or (1 shl (7 - bitIdx))
                bitIdx++
                if (bitIdx == 8) {
                    out[i++] = curByte.toByte()
                    curByte = 0
                    bitIdx = 0
                }
            }
            if (bitIdx != 0) out[i++] = curByte.toByte()
        }
        return out
    }
}