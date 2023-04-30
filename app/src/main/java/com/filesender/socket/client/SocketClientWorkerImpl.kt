package com.filesender.socket.client

import com.filesender.model.ServerOnlineModel
import com.filesender.socket.client.SocketClient
import com.filesender.socket.client.SocketClientWorker
import com.filesender.socket.model.Offline
import com.google.gson.Gson
import com.filesender.socket.model.Online
import com.filesender.socket.model.ResponsePing
import com.filesender.state.SocketClientState

/**
 * @author Fedotov Yakov
 */
class SocketClientWorkerImpl constructor(
    private val socket: SocketClient,
    private val gson: Gson
) : SocketClientWorker {

    override fun startReceivingServerMessages(
        messageReceived: ((SocketClientState) -> Unit),
        onConnected: ((ServerOnlineModel) -> Unit),
        onDisconnected: (() -> Unit),
        clientStartingListener: ((isStart: Boolean, server: ServerOnlineModel?) -> Unit)
    ) {
        socket.messageReceived = messageReceived
        socket.onConnected = onConnected
        socket.onDisconnected = onDisconnected
        socket.clientStartingListener = clientStartingListener
    }

    override fun stopReceivingMessages() {
        socket.messageReceived = null
        socket.onConnected = null
        socket.onDisconnected = null
    }

    override fun isClientStart(): Boolean = socket.isStart()

    override fun startClient(address: String) {
        socket.start(address)
    }

    override fun stopClient() {
        stopReceivingMessages()
        socket.stop()
    }

    override fun sendOnline(name: String) {
        val message = gson.toJson(Online(name))
        socket.sendMessage(message)
    }

    override fun sendOffline() {
        val message = gson.toJson(Offline())
        socket.sendMessage(message)
    }

    override fun sendResponsePing() {
        val message = gson.toJson(ResponsePing())
        socket.sendMessage(message)
    }

    override fun startGettingTime(
        timeListener: (Int) -> Unit,
        savedProcessListener: (progress: Int) -> Unit
    ) {
        socket.fileSaved = timeListener
        socket.savedProcessListener = savedProcessListener
    }
}