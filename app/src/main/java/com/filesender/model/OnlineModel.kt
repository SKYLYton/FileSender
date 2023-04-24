package com.filesender.model

import com.filesender.socket.model.BaseCommand
import com.filesender.socket.model.Online

/**
 * @author Fedotov Yakov
 */
data class OnlineModel(
    val name: String = ""
): BaseCommand("Online")

val Online.toModel: OnlineModel
    get() = OnlineModel(
        name
    )
