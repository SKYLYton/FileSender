package com.filesender.socket.model


/**
 * @author Fedotov Yakov
 */
class Command : BaseCommand()

enum class CommandType {
    ONLINE, OFFLINE, FILE, RESPONSE_PING, REQUEST_PING
}