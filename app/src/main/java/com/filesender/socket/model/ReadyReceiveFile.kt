package com.filesender.socket.model

import com.filesender.socket.GsonManager

/**
 * @author Fedotov Yakov
 */
class ReadyReceiveFile(
    val fileName: String,
    val size: Int
): BaseCommand("ready_receive_file")

val ReadyReceiveFile.toJson: String
    get() = GsonManager.gson.toJson(this)