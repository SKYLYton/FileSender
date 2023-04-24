package com.filesender.model

import com.filesender.socket.model.BaseCommand
import com.filesender.socket.model.ServerOnline

/**
 * @author Fedotov Yakov
 */
class ServerOnlineModel(
) : BaseCommand("server_online")

val ServerOnline.toModel: ServerOnlineModel
    get() = ServerOnlineModel()