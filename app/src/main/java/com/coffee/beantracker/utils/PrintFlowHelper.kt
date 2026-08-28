package com.coffee.beantracker.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.coffee.beantracker.R
import com.coffee.beantracker.data.CoffeeBean
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import com.coffee.beantracker.utils.ToastCustom

class PrintFlowHelper private constructor(
    private val context: android.content.Context,
    private val anchorView: View?,
    private val printerMgr: BluetoothPrinterManager,
    private val permissionLauncher: ActivityResultLauncher<Array<String>>?,
    private val enableBtLauncher: ActivityResultLauncher<Intent>?,
    private val askActivityForPermission: (() -> Unit)?
) {

    private var pendingBean: CoffeeBean? = null
    private var pendingCopies = 1
    private var pendingProtocol: TscLabelBuilder.PrintProtocol = TscLabelBuilder.PrintProtocol.ESC_POS
    private var dateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    companion object {
        fun from(
            fragment: Fragment,
            printerMgr: BluetoothPrinterManager,
            permissionLauncher: ActivityResultLauncher<Array<String>>,
            enableBtLauncher: ActivityResultLauncher<Intent>
        ): PrintFlowHelper {
            return PrintFlowHelper(
                context = fragment.requireContext(),
                anchorView = fragment.view,
                printerMgr = printerMgr,
                permissionLauncher = permissionLauncher,
                enableBtLauncher = enableBtLauncher,
                askActivityForPermission = null
            )
        }

        fun from(
            activity: Activity,
            printerMgr: BluetoothPrinterManager,
            askForPermission: () -> Unit,
            enableBtLauncher: ActivityResultLauncher<Intent>
        ): PrintFlowHelper {
            return PrintFlowHelper(
                context = activity,
                anchorView = activity.findViewById(android.R.id.content),
                printerMgr = printerMgr,
                permissionLauncher = null,
                enableBtLauncher = enableBtLauncher,
                askActivityForPermission = askForPermission
            )
        }
    }

    fun requestBluetoothPermissionsThenPrint(bean: CoffeeBean, copies: Int = 1, protocol: TscLabelBuilder.PrintProtocol = TscLabelBuilder.PrintProtocol.ESC_POS) {
        pendingBean = bean
        pendingCopies = copies.coerceAtLeast(1)
        pendingProtocol = protocol
        if (!printerMgr.isBluetoothAvailable()) { toast("此设备不支持蓝牙"); return }
        if (!printerMgr.isBluetoothEnabled()) {
            val i = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBtLauncher?.launch(i); return
        }
        if (!BluetoothPrinterManager.hasBluetoothPermissions(context)) {
            when {
                permissionLauncher != null -> permissionLauncher.launch(BluetoothPrinterManager.requiredRuntimePermissions())
                askActivityForPermission != null -> askActivityForPermission.invoke()
            }; return
        }
        onReadyToChooseDevice()
    }

    fun onPermissionsGranted() {
        if (!printerMgr.isBluetoothEnabled()) {
            val i = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBtLauncher?.launch(i); return
        }
        onReadyToChooseDevice()
    }

    fun onBluetoothEnabled() {
        if (!BluetoothPrinterManager.hasBluetoothPermissions(context)) {
            when {
                permissionLauncher != null -> permissionLauncher.launch(BluetoothPrinterManager.requiredRuntimePermissions())
                askActivityForPermission != null -> askActivityForPermission.invoke()
            }; return
        }
        onReadyToChooseDevice()
    }

    @SuppressLint("MissingPermission")
    private fun onReadyToChooseDevice() {
        val bean = pendingBean ?: return
        val paired = printerMgr.getPairedDevices()
        if (paired.isEmpty()) {
            MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_CoffeeBean_Dialog)
                .setTitle("未发现已配对的标签机")
                .setMessage("请先打开手机「设置 → 蓝牙」，搜索并与打印机完成配对（配对 PIN 通常为 0000 或 1234）。配对成功后重新点击打印即可。设备名通常包含：HPRT / T260 / HM-T")
                .setPositiveButton("知道了", null)
                .show()
            return
        }
        val labels = paired.map { (devE, likely) ->
            val name = try { devE.name ?: "(未知设备)" } catch (_: Throwable) { "(未知设备)" }
            val flag = if (likely) "  ✨" else ""
            "$name$flag  [${devE.address}]"
        }
        MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_CoffeeBean_Dialog)
            .setTitle("选择要打印的蓝牙设备")
            .setSingleChoiceItems(labels.toTypedArray(), 0, null)
            .setCancelable(true)
            .setNegativeButton("取消", null)
            .setPositiveButton("开始打印") { dlg, _ ->
                val which = (dlg as AlertDialog).listView.checkedItemPosition
                if (which < 0 || which >= paired.size) return@setPositiveButton
                val device = paired[which].first
                startConnectAndPrint(device)
            }
            .show()
    }

    private fun startConnectAndPrint(device: BluetoothDevice) {
        val bean = pendingBean ?: return
        val copies = pendingCopies
        val scope = (context as? androidx.activity.ComponentActivity)?.lifecycleScope
            ?: (context as? androidx.fragment.app.FragmentActivity)?.lifecycleScope
            ?: run { toast("缺少 LifecycleScope 支持"); return }

        var dlg: AlertDialog? = null
        val msgView = TextView(context).apply {
            text = "正在连接 [${device.address}] ..."
            setPadding(56, 48, 56, 24)
            textSize = 15f
        }
        dlg = MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_CoffeeBean_Dialog)
            .setTitle("打印咖啡豆标签")
            .setView(msgView)
            .setCancelable(false)
            .setNegativeButton("取消", null)
            .create().also { it.show() }

        scope.launch {
            val conn = printerMgr.connect(device.address)
            if (conn.isFailure) {
                msgView.text = "连接失败，请确认打印机已开机并处于可配对状态。原因: " + (conn.exceptionOrNull()?.message ?: "未知错误")
                dlg?.setCancelable(true)
                dlg?.getButton(AlertDialog.BUTTON_NEGATIVE)?.text = "关闭"
                return@launch
            }
            msgView.text = "连接成功，正在生成标签 (1/$copies) ..."
            // 改：调用 buildCoffeeBeanLabelPacket 返回 Pair<Phase1, Phase2>
            val labelPacket = withContext(Dispatchers.Default) {
                TscLabelBuilder.buildCoffeeBeanLabelPacket(bean, dateFormat, pendingProtocol)
            }
            msgView.text = "发送数据到打印机 (A=${labelPacket.first.size} B=${labelPacket.second.size}, $copies 份) ..."
            val printed = printerMgr.printLabel(labelPacket, copies)
            // 再保底等待 3000ms 再关连接
            withContext(Dispatchers.IO) { Thread.sleep(3000) }
            printerMgr.disconnect()
            if (printed.isSuccess) {
                msgView.text = "打印完成（协议：${pendingProtocol.name}）。Phase1=间隙模式+位图，Phase2=GS FF间隙对齐撕纸位。若GS FF不生效则兜底120LF。"
                dlg?.setCancelable(true)
                dlg?.getButton(AlertDialog.BUTTON_NEGATIVE)?.text = "关闭"
                toast("打印已发送 ($copies 份)")
            } else {
                msgView.text = "发送失败: " + (printed.exceptionOrNull()?.message ?: "未知")
                dlg?.setCancelable(true)
                dlg?.getButton(AlertDialog.BUTTON_NEGATIVE)?.text = "关闭"
            }
        }
    }

    private fun toast(msg: String) {
        ToastCustom.show(context, msg, android.widget.Toast.LENGTH_SHORT)
    }
}
