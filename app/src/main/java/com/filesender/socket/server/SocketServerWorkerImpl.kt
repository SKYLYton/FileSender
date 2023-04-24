package com.filesender.socket.server

import com.filesender.socket.model.RequestPing
import com.filesender.socket.model.ServerOnline
import com.filesender.socket.model.User
import com.filesender.state.SocketServerState
import com.google.gson.Gson

/**
 * @author Fedotov Yakov
 */
class SocketServerWorkerImpl(
    private val socket: SocketServer,
    private val gson: Gson
) : SocketServerWorker {
    override fun startReceivingServerMessages(
        messageReceived: (SocketServerState) -> Unit,
        userConnected: (User) -> Unit,
        userDisconnected: (User) -> Unit,
        serverStartingListener: ((isStart: Boolean, ip:String) -> Unit)
    ) {
        socket.messageReceived = messageReceived
        socket.userConnected = userConnected
        socket.userDisconnected = userDisconnected
        socket.serverStartingListener = serverStartingListener
    }

    override fun startGettingTime(
        timeListener: (Int) -> Unit,
        savedProcessListener: (progress: Int) -> Unit
    ) {
        socket.fileSaved = timeListener
        socket.savedProcessListener = savedProcessListener
    }

    override fun stopReceivingMessages() {
        socket.messageReceived = null
        socket.userConnected = null
        socket.userDisconnected = null
    }

    override fun isServerStart(): Boolean = socket.isStart()

    override fun startServer(name: String) {
        socket.start(name)
    }

    override fun stopServer() {
        stopReceivingMessages()
        socket.stop()
    }

    override fun sendServerOnline() {
        val message = gson.toJson(ServerOnline())
        socket.sendMessage(message)
    }

    override fun sendPing() {
        val message = gson.toJson(RequestPing())
        socket.sendMessage(message)
    }

}