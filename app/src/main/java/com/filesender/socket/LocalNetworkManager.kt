package com.filesender.socket

import android.content.Context
import android.net.ConnectivityManager
import android.net.DhcpInfo
import android.net.wifi.WifiManager
import android.text.format.Formatter
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections
import javax.inject.Inject


/**
 * @author Fedotov Yakov
 */
class LocalNetworkManager @Inject constructor(
    @ApplicationContext private val context: Context
) : BaseSocket() {
    private lateinit var d: DhcpInfo
    private var wifii: WifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun fetchHostIp(): String {
        d = wifii.dhcpInfo
        return getUSBThetheredIP() ?: "" //intToIp(d.ipAddress)
    }

    fun fetchGatewayIp(): String {
        d = wifii.dhcpInfo
        return intToIp(d.gateway)
    }

    fun getUSBThetheredIP(): String? {
        var bufferedReader: BufferedReader? = null
        var ips: String? = ""
        try {
            bufferedReader = BufferedReader(FileReader("/proc/net/arp"))
            var line: String
            while (bufferedReader.readLine().also { line = it } != null) {
                val splitted = line.split(" +".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                if (splitted != null && splitted.size >= 4) {
                    val ip = splitted[0]
                    val mac = splitted[3]
                    if (mac.matches("..:..:..:..:..:..".toRegex())) {
                        if (mac.matches("00:00:00:00:00:00".toRegex())) {
                            //Log.d("DEBUG", "Wrong:" + mac + ":" + ip);
                        } else {
                            //Log.d("DEBUG", "Correct:" + mac + ":" + ip);
                            ips = ip
                            break
                        }
                    }
                }
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                bufferedReader?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return ips
    }

    fun findAllIpInNetwork(result: (ips: List<String>) -> Unit) {
        doWork {
            val ips = mutableListOf<String>()

            withContext(Dispatchers.IO) {

                try {
                    val wm = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    val connectionInfo = wm.connectionInfo
                    val ipAddress = connectionInfo.ipAddress
                    val ipString = Formatter.formatIpAddress(ipAddress)

                    val prefix = ipString.substring(0, ipString.lastIndexOf(".") + 1)
                    for (i in 0..255) {
                        val testIp = prefix + i.toString()
                        val address = InetAddress.getByName(testIp)
                        val reachable = address.isReachable(100)
                        val hostName = address.canonicalHostName
                        if (reachable) {
                            ips.add(hostName)
                        }
                    }
                } catch (t: Throwable) {
                }
            }

            doWorkInMainThread {
                result(ips)
            }
        }
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