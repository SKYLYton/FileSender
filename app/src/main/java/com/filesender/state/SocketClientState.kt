package com.filesender.state

import com.filesender.model.FileModel
import com.filesender.model.OfflineModel
import com.filesender.model.OnlineModel
import com.filesender.model.ServerOnlineModel
/**
 * @author Fedotov Yakov
 */
sealed class SocketClientState {
    data class Error(val throwable: Throwable) : SocketClientState()
    data class Online(val online: OnlineModel) : SocketClientState()
    data class Offline(val offline: OfflineModel) : SocketClientState()
    data class File(val file: FileModel) : SocketClientState()
    object ReadyReceiveFile : SocketClientState()
    object OnlineTest : SocketClientState()
    data class ClientStarted(val isStart: Boolean): SocketClientState()
    data class ClientConnected(val online: ServerOnlineModel? = null): SocketClientState()

}