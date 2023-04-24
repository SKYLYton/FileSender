package com.filesender.socket

import android.content.Context
import android.net.DhcpInfo
import android.net.wifi.WifiManager
import android.provider.Settings
import android.util.Log
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.*
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.Socket
import java.security.AccessController.getContext
import java.util.*
import javax.inject.Inject


/**
 * @author Fedotov Yakov
 */
class LocalNetworkManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private lateinit var d: DhcpInfo
    private var wifii: WifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun fetchHostIp(): String {
        d = wifii.dhcpInfo
        return intToIp(d.ipAddress)
    }

    fun fetchGatewayIp(): String {
        d = wifii.dhcpInfo
        return intToIp(d.gateway)
    }

    fun fetchAllIps(): List<String> {
        val list = mutableListOf<String>()
        list.add("127.0.0.1")
        //list.add("192.168.0.10")
        //list.add("192.168.0.13")
        //list.add("192.168.10.12")
        //list.add(fetchGatewayIp().takeIf { it != "0.0.0.0" } ?: "127.0.0.1")
        try {
            val interfaces: List<NetworkInterface> =
                Collections.list(NetworkInterface.getNetworkInterfaces())

            for (i in interfaces) {
                (Collections.list(i.inetAddresses) as? List<InetAddress>)?.apply {
                    val addrs = filter {
                        !it.isLoopbackAddress &&
                                it is Inet4Address && !it.hostAddress.isNullOrEmpty()
                    }.map { it.hostAddress ?: "" }

                    list.addAll(addrs.distinct())
                }
            }
        } catch (ex: Exception) {
        } // for now eat exceptions
        return list
    }

    private fun intToIp(i: Int): String {
        return (i and 0xFF).toString() + "." +
                (i shr 8 and 0xFF) + "." +
                (i shr 16 and 0xFF) + "." +
                (i shr 24 and 0xFF)
    }

}