package com.filesender.model

import com.filesender.socket.model.BaseCommand
import com.filesender.socket.model.File
import com.filesender.socket.model.Online
import com.filesender.socket.model.ReadyReceiveFile

/**
 * @author Fedotov Yakov
 */
class ReadyReceiveFileModel(): BaseCommand("ReadyReceiveFileModel")

val ReadyReceiveFile.toModel: ReadyReceiveFileModel
    get() = ReadyReceiveFileModel()

