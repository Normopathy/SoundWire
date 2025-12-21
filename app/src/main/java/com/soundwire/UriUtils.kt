package com.soundwire

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

object UriUtils {

    fun queryDisplayName(context: Context, uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return it.getString(idx)
            }
        }
        return null
    }

    /** Копирует Uri в временный файл (cacheDir) и возвращает File. */
    fun copyToCacheFile(context: Context, uri: Uri, prefix: String = "upload_"): File {
        val name = queryDisplayName(context, uri) ?: (prefix + System.currentTimeMillis())
        val outFile = File(context.cacheDir, name)

        context.contentResolver.openInputStream(uri).use { input ->
            if (input == null) throw IllegalStateException("Не удалось открыть файл")
            FileOutputStream(outFile).use { output ->
                input.copyTo(output)
            }
        }

        return outFile
    }
}
