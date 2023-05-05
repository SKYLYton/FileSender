package com.filesender.model

import com.filesender.socket.model.BaseCommand
import com.filesender.socket.model.File
import com.filesender.socket.model.Online
import com.filesender.socket.model.ReadyReceiveFile

/**
 * @author Fedotov Yakov
 */
class ReadyReceiveFileModel(): BaseCommand("ready_receive_file")

val ReadyReceiveFile.toModel: ReadyReceiveFileModel
    get() = ReadyReceiveFileModel()

