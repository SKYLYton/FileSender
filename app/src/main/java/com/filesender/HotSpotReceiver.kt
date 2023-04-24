package com.filesender

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import com.vkpapps.apmanager.APManager


class HotSpotReceiver : BroadcastReceiver() {
    private var WIFI_AP_STATE_DISABLING = 10 //выключается

    private var WIFI_AP_STATE_DISABLED = 11 //выключен

    private var WIFI_AP_STATE_ENABLING = 12 //включается

    private var WIFI_AP_STATE_ENABLED = 13 //включен

    private var WIFI_AP_STATE_FAILED = 14 //сломалсо

    override fun onReceive(context: Context, intent: Intent) {

        val action = intent.action;
        if (action.equals("android.net.wifi.WIFI_AP_STATE_CHANGED")) {
            val state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0)
            if (WifiManager.WIFI_STATE_ENABLED == state % 10) {
                hotSpotOff(context)
            }
        }
    }

    private fun hotSpotOff(context: Context) {
        APManager.getApManager(context).disableWifiAp()
    }

}