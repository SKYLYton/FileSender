package com.filesender.socket.client

import android.util.Log
import com.filesender.model.ServerOnlineModel
import com.filesender.socket.BaseSocket
import com.filesender.socket.model.BaseCommand
import com.filesender.socket.model.Command
import com.filesender.socket.model.CommandType
import com.filesender.socket.model.Online
import com.filesender.model.toModel
import com.filesender.socket.client.file.SocketFile
import com.filesender.socket.client.file.SocketFileWorker
import com.filesender.socket.model.File
import com.filesender.socket.model.Offline
import com.filesender.socket.model.fromJson
import com.filesender.state.SocketClientState
import dagger.Lazy
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.*
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton


/**
 * @author Fedotov Yakov
 */
@Singleton
class SocketClient @Inject constructor(
    private val socketWorker: Lazy<SocketClientWorker>
) : BaseSocket() {
    var fileSaved: ((second: Int) -> Unit)? = null
    var savedProcessListener: ((progress: Int) -> Unit)? = null

    private var server: ServerOnlineModel? = null

    private var address: String = ""

    var messageReceived: ((SocketClientState) -> Unit)? = null
    var onConnected: ((server: ServerOnlineModel) -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null
    var clientStartingListener: ((isStart: Boolean, server: ServerOnlineModel?) -> Unit)? = null
        set(value) {
            field = value
            field?.invoke(mRun, server)
        }

    private var mServerMessage: String? = null
    private var mRun = false // флаг, определяющий, запущен ли сервер
        set(value) {
            field = value
            if (!value) {
                server = null
            }
            clientStartingListener?.invoke(value, server)
        }

    private var startTime = System.currentTimeMillis()
    private val mutex = Mutex()


    private var mBufferOut: PrintWriter? = null
    private var mBufferIn: BufferedReader? = null
    private var socket: Socket? = null

    fun isStart() = mRun

    fun start(address: String) {
        if (mRun) {
            return
        }
        this.address = address
        runClient()
    }

    private fun runClient() {
        mRun = true
        doWork {
            runCatching {
                socket = Socket(address, 5656)

                // отправляем сообщение клиенту
                mBufferOut = runCatching {
                    PrintWriter(BufferedWriter(OutputStreamWriter(socket?.getOutputStream())), true)
                }.getOrNull()

                // читаем сообщение от клиента
                mBufferIn = runCatching {
                    BufferedReader(InputStreamReader(socket?.getInputStream()))
                }.getOrNull()

                mBufferIn?.let {
                    socketWorker.get().sendOnline("")
                    Log.wtf("TAG", "Юзер подключился")
                    while (mRun) {
                        runCatching {
                            processSocket(it)
                        }.onFailure {
                            Log.wtf("TAG", it.message)
                        }
                    }
                }
            }.onFailure {
                Log.wtf("TAG", "Юзер не смог подключиться")
            }.also {
                runCatching {
                    socket.takeIf { it != null && it.isConnected }?.close()
                }
                Log.wtf("TAG", "Юзер свалил")
            }
            mRun = false
            onDisconnected?.invoke()
        }

        doWork {
            while (mRun) {
                delay(10000)
                if (!mRun) {
                    break
                }
                mutex.withLock {
                    if (System.currentTimeMillis() - startTime >= 30000) {
                        Log.wtf("TAG", "Юзер пинганул")
                        socketWorker.get().sendResponsePing()
                    }
                }
            }
        }
    }

    private suspend fun processSocket(input: BufferedReader) {
        val message = kotlin.runCatching { input.readLine() }.onFailure {
            Log.wtf("TAG", it.message)
        }.getOrNull()

        if (message.isNullOrEmpty()) {
            Log.wtf("TAG", "отключился")
            stop()
        }

        hasCommand(message)?.let {
            doWorkInMainThread {
                messageReceived?.invoke(it)
            }
        }
    }

    private suspend fun hasCommand(message: String?): SocketClientState? {
        if (message.isNullOrEmpty()) {
            return null
        }
        Log.wtf("TAG", "Сообщение: $message")
        var command: BaseCommand = gson.fromJson(message, Command::class.java)
        val socketState: SocketClientState

        when ((command as? Command)?.toModel?.commandType) {
            CommandType.ONLINE -> {
                command = message.fromJson<Online>()
                socketState = SocketClientState.Online(command.toModel)
            }

            CommandType.OFFLINE -> {
                command = message.fromJson<Offline>()
                server = null
                socketState = SocketClientState.Offline(command.toModel)
            }

            CommandType.FILE -> {
                command = message.fromJson<File>()
                socketState = SocketClientState.File(command.toModel)
                processFile(socketState)
            }

            else -> {
                processReceivedWithoutUi(command)
                return null
            }
        }

        return socketState
    }

    private suspend fun processFile(file: SocketClientState.File) {
        SocketFile().apply {
            start(address, file.file.data, file.file.size)
            fileSaved = {
                doWorkInMainThread {
                    sharedMutex.withLock {
                        this@SocketClient.fileSaved?.invoke(it)
                    }
                }
            }
            savedProcessListener = {
                doWorkInMainThread {
                    sharedMutex.withLock {
                        this@SocketClient.savedProcessListener?.invoke(it)
                    }
                }

            }
        }

    }

    private fun processReceivedWithoutUi(command: BaseCommand) {
        when ((command as? Command)?.toModel?.commandType) {
            CommandType.REQUEST_PING -> socketWorker.get().sendResponsePing()
            else -> return
        }
    }

    fun stop() {
        mRun = false
        doWork {
            //socketFileWorker.stopClient()
            socketWorker.get().sendOffline()
            mBufferOut?.let {
                it.flush()
                it.close()
            }
            runCatching {
                socket.takeIf { it != null && it.isConnected }?.close()
            }
            messageReceived = null
            mBufferOut = null
            mBufferIn = null
            mServerMessage = null
            onDisconnected?.invoke()
        }
    }

    fun sendMessage(message: String) {
        mBufferOut.takeIf { it != null && !it.checkError() }?.let {
            runCatching {
                it.println(message)
                it.flush()
            }
        }
        doWork {
            mutex.withLock {
                startTime = System.currentTimeMillis()
            }
        }
    }

}