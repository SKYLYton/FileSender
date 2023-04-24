package com.filesender.socket.model

data class File(val data: String, val size: Int): BaseCommand("File")