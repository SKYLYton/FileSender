package com.filesender.socket.model

import com.filesender.socket.GsonManager

/**
 * @author Fedotov Yakov
 */
class ResponsePing: BaseCommand("response_ping")

val ResponsePing.toJson: String
    get() = GsonManager.gson.toJson(ResponsePing())