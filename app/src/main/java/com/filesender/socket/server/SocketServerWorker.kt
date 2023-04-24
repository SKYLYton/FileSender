package com.filesender.socket.server

import com.filesender.socket.model.User
import com.filesender.state.SocketServerState

/**
 * @author Fedotov Yakov
 */
interface SocketServerWorker {
    fun startReceivingServerMessages(
        messageReceived: ((SocketServerState) -> Unit),
        userConnected: (User) -> Unit,
        userDisconnected: ((User) -> Unit),
        serverStartingListener: ((isStart: Boolean, ip: String) -> Unit)
    )

    fun startGettingTime(
        timeListener: (Int) -> Unit,
        savedProcessListener: ((progress: Int) -> Unit)
    )

    fun stopReceivingMessages()
    fun isServerStart(): Boolean
    fun startServer(name: String)
    fun stopServer()

    fun sendServerOnline()

    fun sendPing()
}