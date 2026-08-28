package com.coffee.beantracker.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object ImageHelper {

    private const val IMAGE_DIR = "bean_images"
    private const val BG_IMAGE_DIR = "bean_backgrounds"

    fun saveImage(context: Context, uri: Uri, isBackground: Boolean = false): String? {
        return try {
            val dirName = if (isBackground) BG_IMAGE_DIR else IMAGE_DIR
            val dir = File(context.filesDir, dirName).apply { if (!exists()) mkdirs() }
            val fileName = "${UUID.randomUUID()}.jpg"
            val outputFile = File(dir, fileName)

            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.use { input ->
                FileOutputStream(outputFile).use { output ->
                    val bitmap = BitmapFactory.decodeStream(input)
                    bitmap?.compress(Bitmap.CompressFormat.JPEG, 85, output)
                    bitmap?.recycle()
                }
            }

            if (outputFile.exists() && outputFile.length() > 0) outputFile.absolutePath else null
        } catch (e: Exception) {
            null
        }
    }

    fun deleteImage(path: String) {
        try {
            if (path.isNotEmpty()) {
                val file = File(path)
                if (file.exists()) file.delete()
            }
        } catch (_: Exception) {}
    }

    fun imageExists(path: String): Boolean {
        return path.isNotEmpty() && File(path).exists()
    }
}