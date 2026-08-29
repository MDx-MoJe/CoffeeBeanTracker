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
        if (!printerMgr.isBluetoothAvailable()) { toast(context.getString(R.string.bluetooth_not_supported_msg)); return }
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
                .setTitle(R.string.printer_not_paired)
                .setMessage(R.string.printer_not_paired_msg)
                .setPositiveButton(R.string.close, null)
                .show()
            return
        }
        val labels = paired.map { (devE, likely) ->
            val name = try { devE.name ?: context.getString(R.string.unknown_device) } catch (_: Throwable) { context.getString(R.string.unknown_device) }
            val flag = if (likely) "  ✨" else ""
            "$name$flag  [${devE.address}]"
        }
        MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_CoffeeBean_Dialog)
            .setTitle(R.string.printer_choose_title)
            .setSingleChoiceItems(labels.toTypedArray(), 0, null)
            .setCancelable(true)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.print_start) { dlg, _ ->
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
            ?: run { toast(context.getString(R.string.printer_lifecycle_missing)); return }

        var dlg: AlertDialog? = null
        val msgView = TextView(context).apply {
            text = context.getString(R.string.connecting_to, device.address)
            setPadding(56, 48, 56, 24)
            textSize = 15f
        }
        dlg = MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_CoffeeBean_Dialog)
            .setTitle(R.string.print_bean_label)
            .setView(msgView)
            .setCancelable(false)
            .setNegativeButton(R.string.cancel, null)
            .create().also { it.show() }

        scope.launch {
            val conn = printerMgr.connect(device.address)
            if (conn.isFailure) {
                msgView.text = context.getString(R.string.connect_failed, conn.exceptionOrNull()?.message ?: context.getString(R.string.unknown_error))
                dlg?.setCancelable(true)
                dlg?.getButton(AlertDialog.BUTTON_NEGATIVE)?.text = context.getString(R.string.close)
                return@launch
            }
            msgView.text = context.getString(R.string.connected_generating, copies)
            // 改：调用 buildCoffeeBeanLabelPacket 返回 Pair<Phase1, Phase2>
            val labelPacket = withContext(Dispatchers.Default) {
                TscLabelBuilder.buildCoffeeBeanLabelPacket(context, bean, dateFormat, pendingProtocol)
            }
            msgView.text = context.getString(R.string.sending_label_data, labelPacket.first.size, labelPacket.second.size, copies)
            val printed = printerMgr.printLabel(labelPacket, copies)
            // 再保底等待 3000ms 再关连接
            withContext(Dispatchers.IO) { Thread.sleep(3000) }
            printerMgr.disconnect()
            if (printed.isSuccess) {
                msgView.text = context.getString(R.string.print_done_protocol, pendingProtocol.name)
                dlg?.setCancelable(true)
                dlg?.getButton(AlertDialog.BUTTON_NEGATIVE)?.text = context.getString(R.string.close)
                toast(context.getString(R.string.printer_done_copies, copies))
            } else {
                msgView.text = context.getString(R.string.send_failed, printed.exceptionOrNull()?.message ?: context.getString(R.string.unknown))
                dlg?.setCancelable(true)
                dlg?.getButton(AlertDialog.BUTTON_NEGATIVE)?.text = context.getString(R.string.close)
            }
        }
    }

    private fun toast(msg: String) {
        ToastCustom.show(context, msg, android.widget.Toast.LENGTH_SHORT)
    }
}
