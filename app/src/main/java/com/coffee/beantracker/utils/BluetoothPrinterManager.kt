package com.coffee.beantracker.utils

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

class BluetoothPrinterManager(private val context: Context) {

    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        private val PRINTER_NAME_KEYWORDS = listOf(
            "HPRT", "T260", "HM-T", "M300", "A300", "汉印", "汉码", "印条",
            "Xprinter", "Jiabo", "TSC", "Printer", "Label"
        )

        fun hasBluetoothPermissions(ctx: Context): Boolean {
            val dangerous = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
            }
            return dangerous
        }

        fun requiredRuntimePermissions(): Array<String> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
            } else {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH)
            }
        }
    }

    private var socket: BluetoothSocket? = null
    private var outStream: OutputStream? = null
    private var connectedMac: String? = null

    fun isBluetoothAvailable(): Boolean {
        val mgr = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        return mgr?.adapter != null
    }

    fun isBluetoothEnabled(): Boolean {
        val mgr = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        return mgr?.adapter?.isEnabled == true
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<Pair<BluetoothDevice, Boolean>> {
        if (!hasBluetoothPermissions(context)) return emptyList()
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?)?.adapter ?: return emptyList()
        val bonded = adapter.bondedDevices ?: return emptyList()
        return bonded.map { dev ->
            val name = try { dev.name ?: "" } catch (_: Throwable) { "" }
            val score = PRINTER_NAME_KEYWORDS.any { kw -> name.contains(kw, ignoreCase = true) }
            dev to score
        }.sortedWith(compareByDescending<Pair<BluetoothDevice, Boolean>> { it.second }.thenBy { it.first.address })
            .map { it.first to it.second }
    }

    val isConnected: Boolean get() = socket?.isConnected == true && connectedMac != null
    val currentConnectedMac: String? get() = connectedMac

    @SuppressLint("MissingPermission")
    suspend fun connect(mac: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (isConnected && connectedMac == mac) return@withContext Result.success(Unit)
            disconnect()
            val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?)?.adapter
                ?: return@withContext Result.failure(IllegalStateException("蓝牙未初始化"))
            val device = try { adapter.getRemoteDevice(mac) } catch (t: Throwable) {
                return@withContext Result.failure(IllegalArgumentException("无效的蓝牙地址: $mac"))
            }
            val sock = try {
                device.createRfcommSocketToServiceRecord(SPP_UUID)
            } catch (t: Throwable) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType as Class<Int>)
                    m.invoke(device, 1) as BluetoothSocket
                } catch (t2: Throwable) {
                    return@withContext Result.failure(t2.cause ?: t2)
                }
            }
            try { sock.connect() } catch (t: Throwable) {
                try { sock.close() } catch (_: Throwable) { }
                return@withContext Result.failure(t)
            }
            socket = sock
            outStream = sock.outputStream
            connectedMac = mac
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try { outStream?.flush() } catch (_: Throwable) { }
        try { outStream?.close() } catch (_: Throwable) { }
        try { socket?.close() } catch (_: Throwable) { }
        outStream = null
        socket = null
        connectedMac = null
    }

    suspend fun sendData(bytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val os = outStream ?: return@withContext Result.failure(IOException("未连接到打印机"))
            // 分块：512B/块，间隔 20ms，尾块 flush 后等 50ms
            var off = 0
            while (off < bytes.size) {
                val chunk = minOf(512, bytes.size - off)
                os.write(bytes, off, chunk)
                os.flush()
                off += chunk
                if (off < bytes.size) Thread.sleep(20) else Thread.sleep(50)
            }
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun printLabel(packet: Pair<ByteArray, ByteArray>, copies: Int = 1): Result<Unit> {
        if (copies <= 0) return Result.success(Unit)
        var last: Result<Unit> = Result.success(Unit)
        repeat(copies) { idx ->
            // Phase 1：设置间隙标签模式 + 位图打印 50mm
            val r1 = sendData(packet.first)
            if (r1.isFailure) { last = r1; return@repeat }
            // 位图处理等待 3000ms：让 MCU 真正走完 400 行到间隙附近
            Thread.sleep(3000)
            // Phase 2：分界 + GS FF 触发间隙对齐撕纸位
            val r2 = sendData(packet.second)
            if (r2.isFailure) { last = r2; return@repeat }
            // 份与份之间 1200ms：等 GS FF 走完到下一张对齐位置
            if (idx < copies - 1) Thread.sleep(1200)
        }
        // Phase 2 发送完毕后总等待 3500ms：让命令彻底执行
        Thread.sleep(3500)
        return last
    }
}
