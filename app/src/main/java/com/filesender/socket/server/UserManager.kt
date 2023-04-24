package com.filesender.socket.server

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import com.filesender.model.toModel
import com.filesender.socket.BaseSocket
import com.filesender.socket.model.BaseCommand
import com.filesender.socket.model.Command
import com.filesender.socket.model.CommandType
import com.filesender.socket.model.File
import com.filesender.socket.model.Offline
import com.filesender.socket.model.Online
import com.filesender.socket.model.User
import com.filesender.state.SocketServerState
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.*
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit


/**
 * @author Fedotov Yakov
 */
class UserManager(
    var socket: Socket?
) : BaseSocket() {
    var userConnected: ((connectedUser: User) -> Unit)? = null
    var userDisconnected: ((userManager: UserManager) -> Unit)? = null
    var messageReceived: ((message: SocketServerState) -> Unit)? = null

    var user = User()
        private set
    private var bufferSender: PrintWriter? = null
    private var bufferInput: BufferedReader? = null

    private var running = false

    private val mutex = Mutex()

    private var startTime = System.currentTimeMillis()


    private fun runSocket() {
        running = true
        doWork {
            // отправляем сообщение клиенту
            bufferSender = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    PrintWriter(
                        BufferedWriter(
                            OutputStreamWriter(
                                socket?.getOutputStream(),
                                StandardCharsets.UTF_8
                            )
                        ),
                        true
                    )
                } else {
                    PrintWriter(
                        BufferedWriter(
                            OutputStreamWriter(
                                socket?.getOutputStream()
                            )
                        ),
                        true
                    )
                }
            }.getOrNull()

            // читаем сообщение от клиента
            bufferInput = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    BufferedReader(InputStreamReader(socket?.getInputStream(), StandardCharsets.UTF_8))
                } else {
                    BufferedReader(InputStreamReader(socket?.getInputStream()))
                }
            }.getOrNull()

            bufferInput?.let {
                Log.e("TAG", "Юзер сохранен")
                while (running) {
                    runCatching {
                        processSocket(it)
                    }.onFailure {
                        Log.e("TAG", "Error123" + it.message ?: "")
                    }
                }
            }

            running = false
        }
    }

    private suspend fun processSocket(input: BufferedReader) {
        val message: String?
        mutex.withLock {
            message = kotlin.runCatching { input.readLine() }.getOrNull()
        }

        fetchCommand(message)?.let {
            if (!processReceived(it)) {
                messageReceived?.invoke(it)
            }
        }
    }

    private fun processReceived(message: SocketServerState): Boolean {
        when (message) {
            is SocketServerState.Offline -> {
                close()
            }

            is SocketServerState.Online -> {
                user = User(message.online.name, user.id)
                userConnected?.invoke(user)
            }

            else -> return false
        }
        return true
    }

    private suspend fun fetchCommand(message: String?): SocketServerState? {
        if (message.isNullOrEmpty()) {
            return null
        }
        //Log.e("TAG", "Сообщение: $message")
        var command: BaseCommand = gson.fromJson(message, Command::class.java)

        val socketState: SocketServerState

        when ((command as? Command)?.toModel?.commandType) {
            CommandType.ONLINE -> {
                command = gson.fromJson(message, Online::class.java)
                socketState = SocketServerState.Online(command.toModel)
            }

            CommandType.OFFLINE -> {
                command = gson.fromJson(message, Offline::class.java)
                socketState = SocketServerState.Offline(command.toModel)
            }

            CommandType.FILE -> {
                command = gson.fromJson(message, File::class.java)
                socketState = SocketServerState.File(command.toModel)
                mutex.withLock {
                    //receiveFile(socketState.file.data, socketState.file.size)
                }
            }

            else -> {
                processReceivedWithoutUi(command)
                return null
            }
        }

        return socketState
    }



    private fun processReceivedWithoutUi(command: BaseCommand) {
        Log.e("TAG", "${user.id} юзер пинганул")
        when ((command as? Command)?.toModel?.commandType) {
            CommandType.RESPONSE_PING -> {
                Log.e("TAG", "${user.id} юзер пинганул")
            }

            else -> return
        }
    }

    fun open(id: Int) {
        user = User(id = id)
        runSocket()
    }

    fun close() {
        running = false
        kotlin.runCatching {
            bufferSender?.let {
                it.flush()
                it.close()
            }
        }
        bufferSender = null
        kotlin.runCatching {
            socket?.close()
        }
        socket = null
        userDisconnected?.invoke(this)
    }

    fun isActive() = System.currentTimeMillis() - startTime <= 60000

    fun isConnected() =
        socket?.isConnected == true && System.currentTimeMillis() - startTime <= 90000 && running

    fun sendMessage(message: String?) {
        bufferSender?.let {
            if (!it.checkError()) {
                it.println(message)
                it.flush()
            }
        }
    }
}