package com.filesender.model

import com.filesender.socket.model.Command
import com.filesender.socket.model.CommandType

/**
 * @author Fedotov Yakov
 */
data class CommandModel(
    val command: String = "",
    val commandType: CommandType
)

val Command.toModel: CommandModel
    get() = CommandModel(
        command,
        CommandType.valueOf(command.uppercase())
    )