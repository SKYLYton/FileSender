package com.filesender.socket.client.file

interface SocketFileWorker {
    fun sendReadyReceiveFile(fileName: String, size: Int)
    fun sendResponsePing()
    fun startClient(address: String, nameFile: String, size: Int)
    fun stopClient()
    fun isStart(): Boolean
    fun startGettingTime(
        timeListener: (Int) -> Unit,
        savedProcessListener: (progress: Int) -> Unit
    )
}