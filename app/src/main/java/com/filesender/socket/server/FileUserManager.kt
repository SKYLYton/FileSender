package com.filesender.socket.server

import android.os.Environment
import android.util.Log
import com.filesender.socket.BaseSocket
import com.filesender.socket.model.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.Socket
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipFile


/**
 * @author Fedotov Yakov
 */
class FileUserManager(
    var socket: Socket?
) : BaseSocket() {
    var fileSaved: ((second: Int) -> Unit)? = null
    var savedProcessListener: ((progress: Int) -> Unit)? = null
    var user = User()
        private set

    var byteArray = ByteArray(0)

    private var running = false

    private val mutex = Mutex()

    private var startTime = System.currentTimeMillis()
    private var timeWork = System.currentTimeMillis()


    private fun runSocket(name: String, size: Int) {
        running = true
        doWork {

            Log.e("TAG", "Юзер сохранен 2")
            runCatching {
                receiveFile(name, size)
            }.onFailure {
                Log.e("TAG", it.stackTraceToString())
            }


            running = false
            //val file = File(Environment.getExternalStorageDirectory().absolutePath + "/" + name)

            //file.unzip()
            //unpackZip(Environment.getExternalStorageDirectory().absolutePath + "/", name)

            val zipFile = Environment.getExternalStorageDirectory()
                .toString() + "/" + name //your zip file location

            val unzipLocation = Environment.getExternalStorageDirectory()
                .toString() + "/" // unzip location

            val df = DecompressFast(zipFile, unzipLocation)
            df.unzip()

            timeWork = System.currentTimeMillis() - startTime
            fileSaved?.invoke(
                TimeUnit.MILLISECONDS.toSeconds(timeWork).toInt()
            )
            Log.e("TAG", "time: " + ((System.currentTimeMillis() - startTime) / 1000).toString())
        }
    }

    data class ZipIO (val entry: ZipEntry, val output: File)

    fun File.unzip(unzipLocationRoot: File? = null) {


        val rootFolder = unzipLocationRoot ?: File(parentFile.absolutePath + File.separator + nameWithoutExtension)
        if (!rootFolder.exists()) {
            rootFolder.mkdirs()
        }

        ZipFile(this).use { zip ->
            zip
                .entries()
                .asSequence()
                .map {
                    val outputFile = File(rootFolder.absolutePath + File.separator + it.name)
                    ZipIO(it, outputFile)
                }
                .map {
                    it.output.parentFile?.run{
                        if (!exists()) mkdirs()
                    }
                    it
                }
                .filter { !it.entry.isDirectory }
                .forEach { (entry, output) ->
                    zip.getInputStream(entry).use { input ->
                        output.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
        }

    }

    @Volatile
    private var progress: Double = 0.0

    private fun updateProgress() {
        doWork {
            var oldProgress = progress
            fileSaved?.invoke(0)
            while (running || progress < 100) {
                delay(200)
                if (oldProgress < progress) {
                    savedProcessListener?.invoke(progress.toInt())
                    oldProgress = progress
                }
            }
            fileSaved?.invoke(
                TimeUnit.MILLISECONDS.toSeconds(timeWork).toInt()
            )
        }
    }


    private fun receiveFile(name: String, size: Int) {

        startTime = System.currentTimeMillis()

        val file = File(Environment.getExternalStorageDirectory().absolutePath + "/" + name)

        val dIn = socket!!.getInputStream()
        val dos = DataInputStream(dIn)

        if (size > 0) {

            val fos = FileOutputStream(file)

            val buffer = ByteArray(32 * 8192)
            updateProgress()
            var index = 0
            var sum = 0

            while (dos.read(buffer).also { index = it } != -1) {
                fos.write(buffer, 0, index)

                sum += index
                progress = (sum.toFloat() / size.toFloat() * 100).toDouble()

            }

            progress = 100.0
            try {
                fos.flush()
                fos.close()
            } catch (e: java.lang.Exception) {
                Log.e("TAG", e.message!!)
            }
        }

        timeWork = System.currentTimeMillis() - startTime
        Log.e("TAG", "time: " + ((System.currentTimeMillis() - startTime) / 1000).toString())

    }


    fun open(name: String, size: Int) {
        user = User(id = 0)
        runSocket(name, size)
    }

    fun close() {
        running = false
        kotlin.runCatching {
            socket?.close()
        }
        socket = null
    }
}