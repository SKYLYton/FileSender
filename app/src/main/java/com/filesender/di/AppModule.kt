package com.filesender.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder

import com.filesender.socket.client.SocketClient
import com.filesender.socket.client.SocketClientWorker
import com.filesender.socket.client.SocketClientWorkerImpl
import com.filesender.socket.client.file.SocketFile
import com.filesender.socket.client.file.SocketFileWorker
import com.filesender.socket.client.file.SocketFileWorkerImpl
import com.filesender.socket.server.SocketServer
import com.filesender.socket.server.SocketServerWorker
import com.filesender.socket.server.SocketServerWorkerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * @author Fedotov Yakov
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun providesGson(): Gson = GsonBuilder()
        .setLenient()
        .create()

    @Provides
    @Singleton
    fun providesSocketClientWorker(socket: SocketClient, gson: Gson): SocketClientWorker =
        SocketClientWorkerImpl(socket, gson)

    @Provides
    @Singleton
    fun providesSocketServerWorker(socket: SocketServer, gson: Gson): SocketServerWorker =
        SocketServerWorkerImpl(socket, gson)


}