package com.filesender.socket.server

import android.util.Log
import com.filesender.socket.BaseSocket
import com.filesender.socket.model.ServerOnline
import com.filesender.socket.model.User
import com.filesender.socket.model.toJson
import com.filesender.state.SocketServerState
import dagger.Lazy
import kotlinx.coroutines.delay
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketAddress
import javax.inject.Inject
import javax.inject.Singleton


/**
 * @author Fedotov Yakov
 */
@Singleton
class SocketServer @Inject constructor(private val socketWorker: Lazy<SocketServerWorker>) :
    BaseSocket() {

    private var running = false
        set(value) {
            field = value
            doWorkInMainThread {
                serverStartingListener?.invoke(value, serverSocket?.inetAddress.toString())
            }
        }
    private var serverSocket: ServerSocket? = null
    private var connectedUser: UserManager? = null
    var userConnected: ((user: User) -> Unit)? = null
    var userDisconnected: ((user: User) -> Unit)? = null
    var messageReceived: ((SocketServerState) -> Unit)? = null
    var serverStartingListener: ((isStart: Boolean, ip: String) -> Unit)? = null
    var fileSaved: ((second: Int) -> Unit)? = null
    var savedProcessListener: ((progress: Int) -> Unit)? = null

    private var nameServer = "Server"

    fun isStart() = running

    private fun runServer() {
        doWork {
            runCatching {
                serverSocket = ServerSocket(5656)
                running = true

                while (running) {
                    runCatching {
                        processSocket()
                    }.onFailure {
                        it.message
                    }
                }
                running = false
            }.onFailure {
                it.message
            }
        }
        doWork {
            while (running) {
                if (connectedUser == null) {
                    delay(30000)
                    continue
                }

                connectedUser?.takeIf { !it.isActive() }?.close()

                if (running) {
                    delay(10000)
                }
            }
        }
    }

    private fun processSocket() {
        val client = serverSocket?.accept()
        client?.let {
            saveUser(it)
        }
    }

    private fun processSocketFile(name: String, size: Long) {
        var serverSocket: ServerSocket? = null
        var running = true
        doWork {
            runCatching {
                serverSocket = ServerSocket(5757).apply {
                    receiveBufferSize = 32 * 8192
                }
                Log.e("TAG", "стартовал")
                running = true

                while (running) {
                    runCatching {
                        processSocket()
                    }.onFailure {
                        it.message
                    }
                }
                running = false
            }.onFailure {

                Log.e("TAG", it.message.toString())

            }
        }
        var client: Socket? = null
        while (running) {
            client = serverSocket?.accept()?.apply {
                sendBufferSize = 32 * 8192
                receiveBufferSize = 32 * 8192
            }

            client?.let {
                Log.e("TAG", client.toString())
                FileUserManager(client).apply {
                    open(name, size.toInt())
                    savedProcessListener = {
                        doWorkInMainThread {
                            this@SocketServer.savedProcessListener?.invoke(it)
                        }

                        if (it >= 100) {
                            close()
                            kotlin.runCatching {
                                serverSocket?.close()
                            }
                            running = false
                        }
                    }
                    fileSaved = {
                        doWorkInMainThread {
                            this@SocketServer.fileSaved?.invoke(it)
                        }
                    }
                }
            }
        }

        kotlin.runCatching {
            serverSocket?.close()
        }

    }

    private fun saveUser(socket: Socket) {
        UserManager(socket).apply {
            open(socket.port)
            connectedUser = this
            userConnected = {
                Log.wtf("TAG", "Подключился пользователь")
                socketWorker.get().sendServerOnline()
                doWorkInMainThread {
                    this@SocketServer.userConnected?.invoke(it)
                }
            }
            userDisconnected = {
                doWorkInMainThread {
                    this@SocketServer.userDisconnected?.invoke(it.user)
                }
                removeUser()
            }
            messageReceived = { message ->
                processReceived(message)
                doWorkInMainThread {
                    this@SocketServer.messageReceived?.invoke(message)
                }
            }
        }
    }

    private fun processReceived(message: SocketServerState) {
        when (message) {
            is SocketServerState.OnlineTest -> {
                sendMessage(ServerOnline().toJson)
            }

            is SocketServerState.File -> {
                Log.e("TAG", message.file.data + " " + message.file.size)

                processSocketFile(message.file.data, message.file.size.toLong())

            }

            else -> return
        }
    }

    private fun removeUser() {
        connectedUser = null
    }

    fun sendMessage(message: String) {
        connectedUser?.sendMessage(message)
    }

    fun start(name: String) {
        if (running) {
            return
        }
        if (name.isNotEmpty()) {
            this.nameServer = name
        }
        runServer()
    }

    fun stop() {
        doWork {
            runCatching {
                connectedUser?.close()
            }
            connectedUser = null
            running = false
            // закрытие сервера
            kotlin.runCatching {
                serverSocket?.close()
            }
            serverSocket = null
        }
    }
}