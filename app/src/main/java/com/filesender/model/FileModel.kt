package com.filesender.model

import com.filesender.socket.model.BaseCommand
import com.filesender.socket.model.File
import com.filesender.socket.model.Online

/**
 * @author Fedotov Yakov
 */
data class FileModel(
    val data: String = "",
    val size: Int = 0
): BaseCommand("File")

val File.toModel: FileModel
    get() = FileModel(
        data,
        size
    )

