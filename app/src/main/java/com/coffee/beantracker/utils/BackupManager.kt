package com.coffee.beantracker.utils

import android.content.Context
import android.net.Uri
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

/**
 * 数据备份/恢复（纯本地）：
 * - 导出：四张表全量 JSON 写入用户选择的位置（SAF），文件名含日期
 * - 导入：按 ID 合并（已有 ID 覆盖、新 ID 插入），绝不删除现有数据
 * 幂等键表 roast_consumes 也一并备份，保证与烤豆互联的幂等状态不丢失
 */
object BackupManager {

    /** 备份文件格式版本（结构变更时递增） */
    private const val FORMAT_VERSION = 1
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

    /** 推荐文件名：beanbag_backup_MMdd-HHmm.json */
    fun suggestedFileName(): String {
        val f = SimpleDateFormat("MMdd-HHmm", Locale.US)
        return "beanbag_backup_${f.format(Date())}.json"
    }

    /**
     * 从 URI 读入备份并按 ID 合并导入。
     * @return Pair(导入熟豆数, 导入生豆数) 等统计
     */
    suspend fun importFrom(context: Context, uri: Uri): Result<Triple<Int, Int, Int>> =
        withContext(Dispatchers.IO) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.use {
                    it.bufferedReader().readText()
                } ?: return@withContext Result.failure(IllegalStateException("无法读取文件"))

                val bundle = json.decodeFromString<BackupBundle>(text)

                // 格式版本校验（向下兼容：只接受 <= 当前版本的文件）
                if (bundle.formatVersion > FORMAT_VERSION) {
                    return@withContext Result.failure(
                        IllegalStateException("备份文件版本过新（${bundle.formatVersion} > $FORMAT_VERSION），请先升级 App")
                    )
                }

                val db = CoffeeBeanDatabase.getDatabase(context)
                var nBean = 0; var nGreen = 0; var nDeduct = 0
                db.withTransaction {
                    bundle.coffeeBeans.forEach {
                        db.coffeeBeanDao().insertBean(it); nBean++
                    }
                    bundle.greenBeans.forEach {
                        db.greenBeanDao().insert(it); nGreen++
                    }
                    bundle.deductRecords.forEach {
                        db.deductRecordDao().insert(it); nDeduct++
                    }
                    bundle.roastConsumes.forEach {
                        db.roastConsumeDao().insert(it)
                    }
                }
                Result.success(Triple(nBean, nGreen, nDeduct))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
