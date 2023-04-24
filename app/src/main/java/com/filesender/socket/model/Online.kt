package com.filesender.socket.model

import com.google.gson.annotations.SerializedName

/**
 * @author Fedotov Yakov
 */
data class Online(
    @SerializedName("name")
    val name: String = ""
): BaseCommand("Online")