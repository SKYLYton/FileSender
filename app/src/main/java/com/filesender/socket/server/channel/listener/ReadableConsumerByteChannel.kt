package com.filesender.socket.server.channel.listener

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel
import java.util.concurrent.TimeUnit
import java.util.function.IntConsumer


class ReadableConsumerByteChannel(
    private val rbc: ReadableByteChannel,
    private val size: Long,
    val progressListener: (progress: Int) -> Unit,
    val timeListener: (seconds: Int) -> Unit
): WritableByteChannel {
    private var startTime = System.currentTimeMillis()

    private var totalByteRead = 0

    @Throws(IOException::class)
    override fun write(dst: ByteBuffer?): Int {
        val nRead = rbc.read(dst)
        notifyBytesRead(nRead)
        return nRead
    }

    protected fun notifyBytesRead(nRead: Int) {
        if (nRead <= 0) {
            return
        }
        totalByteRead += nRead
        timeListener(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime).toInt())
        progressListener((totalByteRead.toFloat() / size * 100).toInt())
    }

    override fun isOpen(): Boolean {
        startTime = System.currentTimeMillis()
        return rbc.isOpen
    }

    @Throws(IOException::class)
    override fun close() {
        rbc.close()
    }
}