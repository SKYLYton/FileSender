package com.filesender.socket.client.file

import com.filesender.socket.model.ReadyReceiveFile
import com.filesender.socket.model.ResponsePing
import com.filesender.socket.model.toJson

class SocketFileWorkerImpl(
    private val socket: SocketFile
    ) : SocketFileWorker {
    override fun sendReadyReceiveFile(fileName: String, size: Int) {
        socket.sendMessage(ReadyReceiveFile(fileName, size).toJson)
    }

    override fun sendResponsePing() {
        socket.sendMessage(ResponsePing().toJson)
    }

    override fun startClient(address: String, nameFile: String, size: Int) {
        socket.start(address, nameFile, size)
    }

    override fun stopClient() {
        socket.stop()
    }

    override fun isStart(): Boolean = socket.running

    override fun startGettingTime(
        timeListener: (Int) -> Unit,
        savedProcessListener: (progress: Int) -> Unit
    ) {
        socket.fileSaved = timeListener
        socket.savedProcessListener = savedProcessListener
    }
}