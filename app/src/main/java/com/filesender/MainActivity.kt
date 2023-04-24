package com.filesender

import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.filesender.databinding.ActivityMainBinding
import com.filesender.socket.LocalNetworkManager
import com.filesender.socket.server.SocketServerWorker
import dagger.hilt.android.AndroidEntryPoint
import java.lang.reflect.Field
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var socketServerWorker: SocketServerWorker

    @Inject
    lateinit var manager: LocalNetworkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            val isUsbConnected = UsbConnectionBroadcastReceiver.isConnected(applicationContext)
            //startServer.isEnabled = isUsbConnected
            //usbModem.isEnabled = isUsbConnected
            usbModem.setOnClickListener { view ->
                switchOnTethering()
            }

            ipServer.text = manager.fetchHostIp()

            socketServerWorker.startReceivingServerMessages( {
                Toast.makeText(applicationContext, it.toString(), Toast.LENGTH_SHORT).show()
            }, {
                Toast.makeText(applicationContext, it.toString(), Toast.LENGTH_SHORT).show()

            }, {
                Toast.makeText(applicationContext, it.toString(), Toast.LENGTH_SHORT).show()
            }) { isStarted, ip ->
                Toast.makeText(applicationContext, isStarted.toString(), Toast.LENGTH_SHORT).show()
                //ipServer.text = ip
            }

            socketServerWorker.startGettingTime({
                time.text = it.toString()
            }) {
                saveProgress.progress = it
            }

            startServer.setOnClickListener {
                if (socketServerWorker.isServerStart()) {
                    socketServerWorker.stopServer()
                } else {
                    socketServerWorker.startServer("ку")
                }
            }
        }

        UsbConnectionBroadcastReceiver.onUSBConnected = {isConnected ->
            //binding.startServer.isEnabled = isConnected
            //binding.usbModem.isEnabled = isConnected
            if (!isConnected) {
                //socketServerWorker.stopServer()
            }
        }
    }

    fun mobileDataEnable(enabled: Boolean) {
        try {
            val conman = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            conman
            val conmanClass = Class.forName(conman.javaClass.name)
            val iConnectivityManagerField: Field = conmanClass.getDeclaredField("mService")
            iConnectivityManagerField.isAccessible = true
            val iConnectivityManager: Any = iConnectivityManagerField.get(conman)
            val iConnectivityManagerClass = Class.forName(iConnectivityManager.javaClass.name)
            val setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod(
                "setMobileDataEnabled",
                java.lang.Boolean.TYPE
            )
            setMobileDataEnabledMethod.isAccessible = true
            setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun switchOnTethering() {
        val tetherSettings = Intent()
        tetherSettings.setClassName(
            "com.android.settings",
            "com.android.settings.TetherSettings"
        )
        tetherSettings.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        applicationContext.startActivity(tetherSettings)
    }
}