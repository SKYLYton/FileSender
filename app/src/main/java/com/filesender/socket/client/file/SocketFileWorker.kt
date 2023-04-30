package com.filesender.socket.client.file

interface SocketFileWorker {
    fun sendReadyReceiveFile()
    fun sendResponsePing()
    fun startClient(address: String, nameFile: String, size: Int)
    fun stopClient()
    fun isStart(): Boolean
    fun startGettingTime(
        timeListener: (Int) -> Unit,
        savedProcessListener: (progress: Int) -> Unit
    )
}