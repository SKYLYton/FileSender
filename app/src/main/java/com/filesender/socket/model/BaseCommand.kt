package com.filesender.socket.model

import com.filesender.socket.GsonManager
import com.google.gson.annotations.SerializedName

/**
 * @author Fedotov Yakov
 */
abstract class BaseCommand (
    @SerializedName("command")
    val command: String = ""
)

inline fun <reified T> String.fromJson(): T = GsonManager.gson.fromJson(this, T::class.java)