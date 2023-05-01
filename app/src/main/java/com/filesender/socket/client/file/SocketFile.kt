package com.filesender.socket.client.file

import android.os.Environment
import android.util.Log
import com.filesender.socket.BaseSocket
import com.filesender.socket.server.DecompressFast
import dagger.Lazy
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.DataInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketFile @Inject constructor(
    private val socketFileWorker: Lazy<SocketFileWorker>
) : BaseSocket() {

    var fileSaved: ((second: Int) -> Unit)? = null
    var savedProcessListener: ((progress: Int) -> Unit)? = null

    private var address: String = ""

    var running = false // флаг, определяющий, запущен ли сервер
        private set

    private var mBufferOut: PrintWriter? = null
    private var mBufferIn: BufferedReader? = null
    private var socket: Socket? = null

    private val mutex = Mutex()

    private var startTime = System.currentTimeMillis()
    private var timeWork = System.currentTimeMillis()

    @Volatile
    private var progress: Double = 0.0

    fun start(address: String, nameFile: String, size: Int) {
        doWork {
            sharedMutex.withLock {
                if (!running) {
                    this@SocketFile.address = address
                    runClient(nameFile, size)
                }
            }
        }
        startPingProcess()
    }

    private fun runClient(nameFile: String, size: Int) {
        running = true
        runCatching {
            socket = Socket(address, 5656)

            // отправляем сообщение клиенту
            mBufferOut = runCatching {
                PrintWriter(BufferedWriter(OutputStreamWriter(socket?.getOutputStream())), true)
            }.getOrNull()

            // читаем сообщение от клиента
            mBufferIn = runCatching {
                BufferedReader(InputStreamReader(socket?.getInputStream()))
            }.getOrNull()

            mBufferIn?.let {
                socketFileWorker.get().sendReadyReceiveFile()
                Log.wtf("TAG", "Юзер подключился")
                var isDownloaded = false
                while (running) {
                    runCatching {
                        isDownloaded = receiveFile(nameFile, size)
                    }.onFailure {
                        Log.wtf("TAG", it.message)
                    }
                    if (isDownloaded) {
                        break
                    }
                }
                unzip(nameFile)
            }
        }.onFailure {
            Log.wtf("TAG", "Юзер не смог подключиться")
        }.also {
            runCatching {
                socket.takeIf { it != null && it.isConnected }?.close()
            }
            Log.wtf("TAG", "Юзер свалил")
        }
        running = false
    }

    private fun startPingProcess() {
        doWork {
            while (running) {
                delay(10000)
                if (!running) {
                    break
                }
                mutex.withLock {
                    if (System.currentTimeMillis() - startTime >= 30000) {
                        Log.wtf("TAG", "Юзер пинганул")
                        socketFileWorker.get().sendResponsePing()
                    }
                }
            }
        }
    }

    private fun unzip(nameFile: String) {
        val zipFile = Environment.getExternalStorageDirectory()
            .toString() + "/" + nameFile //your zip file location

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

    private fun receiveFile(name: String, size: Int): Boolean {

        startTime = System.currentTimeMillis()

        val file = File(Environment.getExternalStorageDirectory().absolutePath + "/" + name)

        val dIn = socket!!.getInputStream()
        val dos = DataInputStream(dIn)

        if (size > 0) {

            val fos = FileOutputStream(file)

            val buffer = ByteArray(32 * 8192)
            updateProgress()
            var index = 0

            val firstData = dos.read(buffer)
            if (firstData <= -1) {
                return false
            }
            fos.write(buffer, 0, firstData)
            var sum = firstData
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

        return true
    }

    fun sendMessage(message: String) {
        mBufferOut.takeIf { it != null && !it.checkError() }?.let {
            runCatching {
                it.println(message)
                it.flush()
            }
        }
        doWork {
            mutex.withLock {
                startTime = System.currentTimeMillis()
            }
        }
    }

    fun stop() {
        running = false
        doWork {
            mBufferOut?.let {
                it.flush()
                it.close()
            }
            runCatching {
                socket.takeIf { it != null && it.isConnected }?.close()
            }
            savedProcessListener = null
            mBufferOut = null
            mBufferIn = null
            fileSaved = null
        }
    }
}