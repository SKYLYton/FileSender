package com.filesender.socket.server

import android.util.Log
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


class DecompressFast(
    private val zipFile: String,
    private val location: String
) {
    fun unzip() {
        try {
            val fin: FileInputStream = FileInputStream(zipFile)
            val zin = ZipInputStream(fin)
            var ze: ZipEntry? = null
            while (zin.nextEntry.also { ze = it } != null) {
                Log.v("Decompress", "Unzipping " + ze!!.name)
                if (ze!!.isDirectory) {
                    _dirChecker(ze!!.name)
                } else {
                    val fout: FileOutputStream = FileOutputStream(location + ze!!.name)
                    val bufout = BufferedOutputStream(fout)
                    val buffer = ByteArray(1024 * 8192)
                    var read = 0
                    while (zin.read(buffer).also { read = it } != -1) {
                        bufout.write(buffer, 0, read)
                    }
                    bufout.close()
                    zin.closeEntry()
                    fout.close()
                }
            }
            zin.close()
            Log.d("Unzip", "Unzipping complete. path :  $location")
        } catch (e: Exception) {
            Log.e("Decompress", "unzip", e)
            Log.d("Unzip", "Unzipping failed")
        }
    }

    private fun _dirChecker(dir: String) {
        val f = File(location + dir)
        if (!f.isDirectory()) {
            f.mkdirs()
        }
    }
}