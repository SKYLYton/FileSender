package com.filesender.socket

import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlin.coroutines.CoroutineContext

/**
 * @author Fedotov Yakov
 */
abstract class BaseSocket {


    private val coroutineContext =
        SupervisorJob() + Dispatchers.IO /*+ CoroutineExceptionHandler { coroutineContext, throwable ->
            throwable
        }*/
    protected val job = Job()
    private val socketScope = CoroutineScope(coroutineContext + job)

    protected val gson = Gson()

    protected fun <P> doWorkInMainThread(doOnAsyncBlock: suspend CoroutineScope.() -> P) {
        doCoroutineWork(doOnAsyncBlock, socketScope, Dispatchers.Main)
    }

    protected fun <P> doWork(doOnAsyncBlock: suspend CoroutineScope.() -> P) =
        doCoroutineWork(doOnAsyncBlock, socketScope, Dispatchers.IO)

    private inline fun <P> doCoroutineWork(
        crossinline doOnAsyncBlock: suspend CoroutineScope.() -> P,
        coroutineScope: CoroutineScope,
        context: CoroutineContext
    ) = coroutineScope.launch {
            withContext(context) {
                doOnAsyncBlock.invoke(this)
            }
        }

    companion object {
        val sharedMutex = Mutex()
    }
}