package com.filesender

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.filesender.databinding.ActivityMainBinding
import com.filesender.model.ServerOnlineModel
import com.filesender.socket.LocalNetworkManager
import com.filesender.socket.client.SocketClientWorker
import com.filesender.state.SocketClientState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var socketClientWorker: SocketClientWorker

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
                openTetheringMode()
            }

            val ip = manager.fetchHostIp()
            ipServer.text = ip

            socketClientWorker.startGettingTime({
                time.text = it.toString()
            }) {
                saveProgress.progress = it
            }

            socketClientWorker.startReceivingServerMessages({
                if (it is SocketClientState.File) {
                    Toast.makeText(applicationContext, "Принимаем файл ${it.file.data}", Toast.LENGTH_SHORT).show()
                }
            }, {
                Toast.makeText(applicationContext, "Подключились", Toast.LENGTH_SHORT).show()
            },{
                Toast.makeText(applicationContext, "Отключились", Toast.LENGTH_SHORT).show()
            }) { isStart: Boolean, server: ServerOnlineModel? ->

            }

            startServer.setOnClickListener {
                if (socketClientWorker.isClientStart()) {
                    socketClientWorker.stopClient()
                } else {
                    socketClientWorker.startClient(ip)
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

    private fun openTetheringMode() {
        val tetherSettings = Intent()
        tetherSettings.setClassName(
            "com.android.settings",
            "com.android.settings.TetherSettings"
        )
        tetherSettings.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        applicationContext.startActivity(tetherSettings)
    }
}