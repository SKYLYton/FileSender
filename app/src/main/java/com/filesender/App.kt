package com.filesender

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.filesender.socket.client.SocketClientWorker
import com.filesender.socket.server.SocketServerWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * @author Fedotov Yakov
 */
@HiltAndroidApp
class App : Application(), Application.ActivityLifecycleCallbacks {

    @Inject
    lateinit var socketServerWorker: SocketServerWorker

    @Inject
    lateinit var socketClientWorker: SocketClientWorker

    override fun onTerminate() {
        super.onTerminate()
    }

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        /* no-op */
    }

    override fun onActivityStarted(activity: Activity) {
        /* no-op */
    }

    override fun onActivityResumed(activity: Activity) {
        /* no-op */
    }

    override fun onActivityPaused(activity: Activity) {
        /* no-op */
    }

    override fun onActivityStopped(activity: Activity) {
        /* no-op */
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        /* no-op */
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (activity is MainActivity) {
            socketClientWorker.stopClient()
            socketServerWorker.stopServer()
        }
    }

}