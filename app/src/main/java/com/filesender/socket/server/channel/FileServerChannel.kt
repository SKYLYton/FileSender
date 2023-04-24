package com.filesender.socket.server.channel

import android.os.Environment
import android.util.Log
import com.filesender.socket.BaseSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit


class FileServerChannel : BaseSocket() {
    private var timeWork = System.currentTimeMillis()
    private var startTime = System.currentTimeMillis()

    private var server: ServerSocketChannel? = null
    private var clientSerial = 0

    var fileSaved: ((second: Int) -> Unit)? = null
    var savedProcessListener: ((progress: Int) -> Unit)? = null

    private var running = false

    fun start(name: String, size: Long) {
        doWork {
            try {
                withContext(Dispatchers.IO) {
                    server = ServerSocketChannel.open()

                    server?.let {
                        running = true
                        startServerNonBlocking(it, name, size)

                    }
                }
            } catch (e: Exception) {
                Log.e("TAG", e.stackTraceToString())
                e.printStackTrace()
            }
        }
    }

    fun stop() {
        shutdownServer()
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
                    fileSaved?.invoke(
                        TimeUnit.MILLISECONDS.toSeconds(timeWork).toInt()
                    )
                }
            }
            Log.e("TAG", TimeUnit.MILLISECONDS.toSeconds(timeWork).toString())

        }
    }

    private fun startServerNonBlocking(server: ServerSocketChannel, name: String, size: Long) {
        try {
            server.socket().bind(InetSocketAddress(5757)) // bind,
            server.configureBlocking(false) // non-blocking mode,
            val selector = Selector.open()
            server.register(selector, SelectionKey.OP_ACCEPT)
            while (running) {

                val client = server.accept()
                client?.let {
                    Log.e("TAG", it.toString())
                    saveFile(client, name, size)
                    it.close()
                    server.close()
                }


            }
        } catch (e: Exception) {
            Log.e("TAG", e.stackTraceToString())

        }
    }

    private fun saveFile(client: SocketChannel, name: String, size: Long) {
        val file = File(Environment.getExternalStorageDirectory().absolutePath + "/" + name)
        /*        if (!file.exists()) {
                    file.createNewFile()
                }
                val fileChannel: FileChannel =
                    FileOutputStream(Environment.getExternalStorageDirectory().absolutePath + "/" + name).channel

                val rbc = ReadableConsumerByteChannel(client, size, {
                    savedProcessListener?.invoke(it)
                }) {
                    fileSaved?.invoke(it)
                }

                fileChannel.transferFrom(rbc, 0, size)*/


        val bb = ByteBuffer.allocate(64 * 8192)
        //var bytesRead: Int = client.read(bb)
        val bout = FileOutputStream(file)
        val sbc = bout.channel

        startTime = System.currentTimeMillis()
        updateProgress()

        var sum = 0
        while (client.read(bb).also { sum += it } > 0) {
            bb.flip();
            sbc.write(bb);
            bb.clear();
            //sum += 8192
            progress = (sum.toDouble() / size * 100)
            timeWork = System.currentTimeMillis() - startTime

        }

        Log.e("TAG", TimeUnit.MILLISECONDS.toSeconds(timeWork).toString())
        running = false

        /*        while (bytesRead != -1) {
                    bb.flip()
                    sbc.write(bb)
                    bb.compact()
                    bytesRead = client.read(bb)
                }*/
        //fileChannel.close()
    }

    // close server,
    private fun shutdownServer() {
        try {
            running = false
            server?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

}