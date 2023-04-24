package com.filesender.socket.model

import com.filesender.socket.GsonManager

/**
 * @author Fedotov Yakov
 */
class ServerOnline : BaseCommand("Server_online")

val ServerOnline.toJson: String
    get() = GsonManager.gson.toJson(ServerOnline())