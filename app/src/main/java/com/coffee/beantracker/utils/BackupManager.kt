package com.coffee.beantracker.utils

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.room.withTransaction
import com.coffee.beantracker.data.CoffeeBean
import com.coffee.beantracker.data.CoffeeBeanDatabase
import com.coffee.beantracker.data.DeductRecord
import com.coffee.beantracker.data.GreenBean
import com.coffee.beantracker.bridge.RoastConsumeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 数据备份/恢复（纯本地，zip 方案）：
 * - 导出：四张表全量 JSON 打包 zip，经 MediaStore 直写 Download 目录（Android 10+ 免弹窗）
 * - 导入：兼容 zip / 裸 json 两种格式；按 ID 合并（已有 ID 覆盖、新 ID 插入），绝不删除现有数据
 * 幂等键表 roast_consumes 一并备份，保证与烤豆互联的幂等状态不丢失
 */
object BackupManager {

    /** 备份文件格式版本（结构变更时递增） */
    private const val FORMAT_VERSION = 1
    /** zip 包内 json 的固定文件名 */
    const val ENTRY_NAME = "beanbag_backup.json"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Serializable
    data class BackupBundle(
        val formatVersion: Int = FORMAT_VERSION,
        val exportedAt: Long,
        val coffeeBeans: List<CoffeeBean>,
        val greenBeans: List<GreenBean>,
        val deductRecords: List<DeductRecord>,
        val roastConsumes: List<RoastConsumeEntity>,
    )

    /** 生成备份 JSON（全表，事务内快照一致） */
    suspend fun exportAll(context: Context): String = withContext(Dispatchers.IO) {
        val db = CoffeeBeanDatabase.getDatabase(context)
        val bundle = db.withTransaction {
            BackupBundle(
                exportedAt = System.currentTimeMillis(),
                coffeeBeans = db.coffeeBeanDao().getAllBeansOnce(),
                greenBeans = db.greenBeanDao().getAllGreenBeansOnce(),
                deductRecords = db.deductRecordDao().getAllOnce(),
                roastConsumes = db.roastConsumeDao().getAllOnce(),
            )
        }
        json.encodeToString(bundle)
    }

    /** json → zip 字节流 */
    fun packZip(jsonText: String): ByteArray = try {
        val bos = java.io.ByteArrayOutputStream()
        ZipOutputStream(bos).use { zos ->
            zos.putNextEntry(ZipEntry(ENTRY_NAME))
            zos.write(jsonText.toByteArray())
            zos.closeEntry()
        }
        bos.toByteArray()
    } catch (_: Exception) {
        byteArrayOf()
    }

    /** 推荐文件名：beanbag_backup_MMdd-HHmm.zip */
    fun suggestedFileName(): String {
        val f = SimpleDateFormat("MMdd-HHmm", Locale.US)
        return "beanbag_backup_${f.format(Date())}.zip"
    }

    /**
     * 直写公共 Download 目录（Android 10+ MediaStore，无弹窗无权限；9 以下落 App 外部分区）。
     * @return 人读保存位置；null = 失败
     */
    fun saveToDownloads(context: Context, filename: String, data: ByteArray): String? {
        return try {
        if (Build.VERSION.SDK_INT >= 29) {
            val values = android.content.ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, filename)
                put(MediaStore.Downloads.MIME_TYPE, "application/zip")
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return null
            context.contentResolver.openOutputStream(uri)?.use { it.write(data) } ?: return null
            "Download/$filename"
        } else {
            val dir = java.io.File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "").apply { mkdirs() }
            java.io.File(dir, filename).writeBytes(data)
            "App外部分区/$filename"
        }
        } catch (_: Exception) {
            null
        }
    }

    /** zip 字节流 → json 文本；输入若是裸 json（PK 魔数判断）则原样返回 */
    fun unpackZip(data: ByteArray): String? = try {
        if (data.size >= 4 && data[0] == 'P'.code.toByte() && data[1] == 'K'.code.toByte()) {
            var text: String? = null
            ZipInputStream(data.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null && text == null) {
                    if (!entry.isDirectory) text = zis.readBytes().decodeToString()
                    entry = zis.nextEntry
                }
            }
            text
        } else {
            data.decodeToString().takeIf { it.trimStart().startsWith("{") }
        }
    } catch (_: Exception) {
        null
    }

    /** 从 JSON 文本按 ID 合并导入。@return Triple(熟豆数, 生豆数, 流水数) */
    suspend fun importFrom(context: Context, jsonText: String): Result<Triple<Int, Int, Int>> =
        withContext(Dispatchers.IO) {
            try {
                val bundle = json.decodeFromString<BackupBundle>(jsonText)
                if (bundle.formatVersion > FORMAT_VERSION) {
                    return@withContext Result.failure(
                        IllegalStateException("备份文件版本过新（${bundle.formatVersion} > $FORMAT_VERSION），请先升级 App")
                    )
                }
                val db = CoffeeBeanDatabase.getDatabase(context)
                var nBean = 0; var nGreen = 0; var nDeduct = 0
                db.withTransaction {
                    bundle.coffeeBeans.forEach { db.coffeeBeanDao().insertBean(it); nBean++ }
                    bundle.greenBeans.forEach { db.greenBeanDao().insert(it); nGreen++ }
                    bundle.deductRecords.forEach { db.deductRecordDao().insert(it); nDeduct++ }
                    bundle.roastConsumes.forEach { db.roastConsumeDao().insert(it) }
                }
                Result.success(Triple(nBean, nGreen, nDeduct))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /** SAF URI 读入（zip/json 自适应）并导入 */
    suspend fun importFrom(context: Context, uri: Uri): Result<Triple<Int, Int, Int>> =
        withContext(Dispatchers.IO) {
            try {
                val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@withContext Result.failure(IllegalStateException("无法读取文件"))
                val text = unpackZip(raw)
                    ?: return@withContext Result.failure(IllegalStateException("文件格式无法识别（需要 beanbag zip 或 json）"))
                importFrom(context, text)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /** 导入前的友好错误提示（避免暴露序列化细节） */
    fun friendlyError(e: Throwable): String = when {
        e.message?.contains("version") == true -> e.message ?: "版本不兼容"
        else -> "文件内容不是有效的豆袋备份"
    }
}
