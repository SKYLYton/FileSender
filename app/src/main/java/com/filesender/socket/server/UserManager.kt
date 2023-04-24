package com.filesender.socket.server

import android.content.Context
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
                PrintWriter(
                    BufferedWriter(
                        OutputStreamWriter(
                            socket?.getOutputStream(),
                            StandardCharsets.UTF_8
                        )
                    ),
                    true
                )
            }.getOrNull()

            // читаем сообщение от клиента
            bufferInput = runCatching {
                BufferedReader(InputStreamReader(socket?.getInputStream(), StandardCharsets.UTF_8))
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
                isFile = true
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

    var isFile = false

    private fun receiveFile(fileName: String, size: Int) {
        val file = java.io.File(Environment.getExternalStorageDirectory().absolutePath + "/" +fileName)
        val startTime = System.currentTimeMillis()
        Log.e("TAG", fileName)
        Log.e("TAG", size.toString())

        var `in`: InputStream? = null
        var out: OutputStream? = null


        try {
            `in` = socket!!.getInputStream()
        } catch (ex: IOException) {
            Log.e("TAG", "Error " + ex.message ?: "")
        }
        Log.e("TAG", fileName)

        try {
            out = FileOutputStream(file)
        } catch (ex: FileNotFoundException) {
            Log.e("TAG", "Error " + ex.message ?: "")
        }
        Log.e("TAG", fileName)

        val bytes = ByteArray(file.length().toInt())

        Log.e("TAG", "bytes " + bytes[0] ?: "")

        var count: Int
        while (`in`!!.read(bytes).also { count = it } > 0) {
            out!!.write(bytes, 0, count)
        }

        out!!.close()
        `in`!!.close()
        Log.e("TAG", "time: "+(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime)).toString())

    }

    private fun receiveFile2(fileName: String, size: Int) {

        val startTime = System.currentTimeMillis()

        Log.e("TAG", fileName)
        val mybytearray = ByteArray(size)
        Log.e("TAG", size.toString())
        val `is`: InputStream = socket!!.getInputStream()
        val fos = FileOutputStream(Environment.getExternalStorageDirectory().absolutePath + "/" +fileName)
        val bos = BufferedOutputStream(fos)
        val bytesRead = `is`.read(mybytearray, 0, mybytearray.size)
        //Log.e("TAG", bytesRead.toString())
        bos.write(mybytearray, 0, bytesRead)
        bos.close()
        socket!!.close()
        isFile = false

        Log.e("TAG", "time: "+(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime)).toString())
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

    private fun receiveFile2(fileName: String) {

        try {
            val dataInputStream = DataInputStream(socket!!.getInputStream())

            Log.e("TAG", Environment.getExternalStorageDirectory().path)

            var bytes = -0
            val fileOutputStream =
                FileOutputStream(Environment.getExternalStorageDirectory().absolutePath + "/" + fileName)
            Log.e("TAG", "cool1")

            var size: Long = dataInputStream.readLong() // read file size
            val buffer = ByteArray(4 * 1024)
            Log.e("TAG", "cool2")

            while (size > 0
                && dataInputStream.read(
                    buffer, 0, Math.min(buffer.size.toLong(), size).toInt()
                ).also { bytes = it }
                != -1
            ) {
                // Here we write the file using write method
                fileOutputStream.write(buffer, 0, bytes)
                size -= bytes.toLong() // read upto file size
            }
            Log.e("TAG", "cool3")

            // Here we received file
            println("File is Received")
            fileOutputStream.flush()
            fileOutputStream.close()
        } catch (e: java.lang.Exception) {
            e.message
            Log.e("TAG", "err " +e.message)
        }
    }

    fun getInternalStorageDirectoryPath(context: Context): String? {
        val storageDirectoryPath: String?
        storageDirectoryPath =
            Environment.getExternalStorageDirectory().absolutePath
        return storageDirectoryPath
    }

    private fun fileReceived(name: String) {


        if (socket == null) {
            return
        }

        var current = 0
        var fos: FileOutputStream? = null
        var bos: BufferedOutputStream? = null
        try {

            // receive file
            val mybytearray = ByteArray(64 * 1024)
            val inputStream: InputStream = socket!!.getInputStream()
            fos = FileOutputStream(Environment.getExternalStorageDirectory().absolutePath + "/" +name)
            bos = BufferedOutputStream(fos)
            var bytesRead = inputStream.read(mybytearray, 0, mybytearray.size)
            current = bytesRead
            do {
                bytesRead = inputStream.read(mybytearray, current, mybytearray.size - current)
                if (bytesRead >= 0) current += bytesRead
            } while (bytesRead > -1)
            bos.write(mybytearray, 0, current)
            bos.flush()

        } catch (e: Exception) {
            sendMessage(e.message ?: "yt")
            Log.e("TAG", "err " +e.message)
        } finally {
            fos?.close()
            bos?.close()
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