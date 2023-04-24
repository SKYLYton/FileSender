package com.filesender.model

import com.filesender.socket.model.BaseCommand
import com.filesender.socket.model.Offline

/**
 * @author Fedotov Yakov
 */
class OfflineModel(): BaseCommand("Offline")

val Offline.toModel: OfflineModel
    get() = OfflineModel()
