package com.coffee.beantracker.bridge

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.coffee.beantracker.data.CoffeeBeanDatabase
import com.coffee.beantracker.data.DeductRecord
import com.coffee.beantracker.data.CoffeeBean
import com.coffee.beantracker.data.GreenBean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * 豆袋对外互联接口（ContentProvider）。
 *
 * 供烤豆 (RoastCurve) 跨应用调用：
 *   - 查询生豆批次列表：query() → vnd.android.cursor.item/vnd.coffee.beantracker.green_bean
 *     返回列：_id / name / remainingGrams
 *   - 烘焙消耗扣减：call("consume", beanId, grams) → 幂等，同一 roastId 只扣一次；
 *     成功后同时在「做一杯」扣减记录里留一条 ROAST 类型流水。
 *
 * 权限：com.coffee.beantracker.permission.BRIDGE（自定义普通权限，签名级隔离不可用——双 App 证书不同）。
 */
class BeanBridgeProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.coffee.beantracker.bridge"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/green_beans")

        const val METHOD_CONSUME = "consume"
        /** 熟豆入库：只建熟豆记录，不碰生豆库存（补录场景：生豆已扣过） */
        const val METHOD_ADD_ROASTED = "add_roasted"
        /** call(extras) 键值约定（烤豆端按此传参） */
        const val EXTRA_ROAST_ID = "roast_id"      // String，幂等键：一炉一个唯一 ID
        const val EXTRA_GREEN_BEAN_ID = "bean_id"  // Long，生豆批次 id
        const val EXTRA_GRAMS = "grams"            // Double，本次消耗克重（= 入豆重）
        const val EXTRA_RESULT = "result"          // 返回 Bundle 键："ok"/"err"
        const val EXTRA_MESSAGE = "message"        // 返回 Bundle 键：错误说明或剩余克重描述
        // —— 熟豆入库参数 ——
        const val EXTRA_BEAN_NAME = "bean_name"        // String，熟豆名称
        const val EXTRA_ROASTED_GRAMS = "roasted_grams" // Double，熟豆克重（入库量）
        const val EXTRA_ROAST_LEVEL = "roast_level"    // String?，烘焙度（可空）
        const val EXTRA_ROAST_DATE = "roast_date"      // Long，烘焙日期 epoch 毫秒

        private const val CODE_GREEN_BEANS = 1
        private const val CODE_GREEN_BEAN_ITEM = 2
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var matcher: UriMatcher

    override fun onCreate(): Boolean {
        matcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "green_beans", CODE_GREEN_BEANS)
            addURI(AUTHORITY, "green_beans/#", CODE_GREEN_BEAN_ITEM)
        }
        return true
    }

    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?
    ): Cursor? {
        when (matcher.match(uri)) {
            CODE_GREEN_BEANS -> {
                val db = CoffeeBeanDatabase.getDatabase(context!!)
                val all = runBlocking { db.greenBeanDao().getAllGreenBeansOnce() }
                val cursor = MatrixCursor(arrayOf("_id", "name", "remainingGrams"))
                for (b in all) {
                    cursor.addRow(arrayOf(b.id, b.name, b.remainingGrams))
                }
                return cursor
            }
            else -> throw IllegalArgumentException("未知 URI: $uri")
        }
    }

    override fun call(method: String, arg: String?, extras: android.os.Bundle?): android.os.Bundle {
        if (method != METHOD_CONSUME && method != METHOD_ADD_ROASTED) {
            throw IllegalArgumentException("不支持的方法: $method")
        }
        val ex = extras ?: throw IllegalArgumentException("缺少参数")

        val roastId = ex.getString(EXTRA_ROAST_ID)
            ?: throw IllegalArgumentException("缺少 $EXTRA_ROAST_ID")
        require(roastId.isNotBlank()) { "roast_id 不能为空" }

        val result = android.os.Bundle()
        val ctx = context!!

        val outcome = runBlocking(scope.coroutineContext) {
            try {
                if (method == METHOD_ADD_ROASTED) addRoastedInternal(ctx, ex)
                else consumeInternal(ctx, roastId, ex)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        outcome.fold(
            onSuccess = { msg ->
                result.putString(EXTRA_RESULT, "ok")
                result.putString(EXTRA_MESSAGE, msg)
            },
            onFailure = { e ->
                result.putString(EXTRA_RESULT, "err")
                result.putString(EXTRA_MESSAGE, e.message ?: e.javaClass.simpleName)
            },
        )
        return result
    }

    /**
     * 幂等扣减逻辑：
     *  1. 查幂等表，同 roastId 已成功 → 直接返回 ok（重复推送不重复扣）
     *  2. 校验生豆存在且库存充足
     *  3. 扣减 + 写「做一杯」式 ROAST 流水 + 记录幂等键
     *  三步按顺序执行（Provider call 由 Binder 主线程驱动，天然单线程串行；
     *  幂等表的唯一索引做并发兜底，重复推送直接被拦截）
     */
    private suspend fun consumeInternal(
        ctx: android.content.Context,
        roastId: String,
        ex: android.os.Bundle,
    ): Result<String> {
        val db = CoffeeBeanDatabase.getDatabase(ctx)

        val beanId = ex.getLong(EXTRA_GREEN_BEAN_ID, -1L)
        val grams = ex.getDouble(EXTRA_GRAMS, 0.0)
            .let { if (it.isNaN()) 0.0 else it.coerceIn(0.0, 100000.0) }
        require(beanId > 0) { "bean_id 无效" }

        // 1) 幂等检查：重复推送不重复扣、不重复入库
        if (db.roastConsumeDao().existsRoast(roastId)) {
            val bean = db.greenBeanDao().getById(beanId)
            return Result.success("已同步过（幂等跳过）：该批次剩余 ${bean?.remainingGrams ?: -1.0}g")
        }

        // 2) 库存校验
        val bean = db.greenBeanDao().getById(beanId)
            ?: return Result.failure(IllegalStateException("生豆批次不存在"))
        if (bean.remainingGrams < grams) {
            return Result.failure(IllegalStateException("库存不足：剩 ${bean.remainingGrams}g，需 ${grams}g"))
        }

        // 3) 扣生豆
        val stockBefore = bean.remainingGrams
        val stockAfter = stockBefore - grams
        db.greenBeanDao().updateRemainingGrams(beanId, stockAfter)

        // 4) 熟豆入库：同名累加，不存在则新建（见 addRoastedStock）
        val roastedMsg = addRoastedStock(db, ex, fallbackName = "${bean.name}（自烘）")

        db.deductRecordDao().insert(
            DeductRecord(
                beanId = 0L, // ROAST 流水指向生豆批次，不做熟豆关联
                beanName = "[烘焙] ${bean.name}",
                gramsDeducted = grams,
                stockBefore = stockBefore,
                stockAfter = stockAfter,
                brewType = "ROAST",
            )
        )
        db.roastConsumeDao().insert(RoastConsumeEntity(roastId = roastId, greenBeanId = beanId))

        return Result.success("已扣生豆 ${grams}g（剩 ${stockAfter}g）；$roastedMsg")
    }

    /**
     * 只入熟豆，不碰生豆库存（METHOD_ADD_ROASTED，补录场景：生豆已扣过）。
     * 幂等键同表，前缀区分，重复推送不重复入库。
     */
    private suspend fun addRoastedInternal(
        ctx: android.content.Context,
        ex: android.os.Bundle,
    ): Result<String> {
        val db = CoffeeBeanDatabase.getDatabase(ctx)
        val roastId = ex.getString(EXTRA_ROAST_ID).orEmpty()
        if (db.roastConsumeDao().existsRoast("roasted-$roastId")) {
            return Result.success("该炉熟豆已入库过，本次幂等跳过")
        }
        val msg = addRoastedStock(db, ex, fallbackName = "自烘焙豆")
        db.roastConsumeDao().insert(
            RoastConsumeEntity(roastId = "roasted-$roastId", greenBeanId = -1L)
        )
        return Result.success(msg)
    }

    /**
     * 熟豆入库共用实现：同名熟豆累加库存，不存在则新建。
     * @return 人读结果描述
     */
    private suspend fun addRoastedStock(
        db: CoffeeBeanDatabase,
        ex: android.os.Bundle,
        fallbackName: String,
    ): String {
        val name = ex.getString(EXTRA_BEAN_NAME)?.trim().takeUnless { it.isNullOrEmpty() } ?: fallbackName
        val roastedGrams = ex.getDouble(EXTRA_ROASTED_GRAMS, 0.0)
            .let { if (it.isNaN() || it <= 0.0) 0.0 else it.coerceAtMost(100000.0) }
        if (roastedGrams <= 0.0) return "熟豆重未提供，未入库"
        val roastLevel = ex.getString(EXTRA_ROAST_LEVEL).orEmpty()
        val roastDate = ex.getLong(EXTRA_ROAST_DATE, System.currentTimeMillis())

        val existing = db.coffeeBeanDao().getByNameOnce(name)
        if (existing != null) {
            val newStock = existing.stockGrams + roastedGrams
            db.coffeeBeanDao().updateStock(existing.id, newStock)
            return "熟豆「$name」累加 ${roastedGrams}g，现有 ${newStock}g"
        } else {
            db.coffeeBeanDao().insert(
                CoffeeBean(
                    name = name,
                    roastDate = roastDate,
                    restDays = 7,
                    bestBeforeDays = 30,
                    stockGrams = roastedGrams,
                    roastLevel = roastLevel,
                )
            )
            return "熟豆「$name」新建入库 ${roastedGrams}g"
        }
    }

    // ===== 未实现的方法：本 Provider 为只读+定点写入设计 =====
    override fun getType(uri: Uri): String? =
        when (matcher.match(uri)) {
            CODE_GREEN_BEANS -> "vnd.android.cursor.dir/vnd.coffee.beantracker.green_bean"
            CODE_GREEN_BEAN_ITEM -> "vnd.android.cursor.item/vnd.coffee.beantracker.green_bean"
            else -> null
        }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(
        uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?
    ): Int = 0
}
