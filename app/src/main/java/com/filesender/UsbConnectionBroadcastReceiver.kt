package com.filesender

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager


class UsbConnectionBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val extraFilterToGetBatteryInfo = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val extraIntentToGetBatteryInfo =
            context.applicationContext.registerReceiver(null, extraFilterToGetBatteryInfo)

        val chargePlug = extraIntentToGetBatteryInfo!!.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB

        onUSBConnected?.invoke(usbCharge)
    }

    companion object {
        fun isConnected(context: Context): Boolean {
            val intent =
                context.registerReceiver(
                    null,
                    IntentFilter("android.hardware.usb.action.USB_STATE")
                )
            return intent?.extras?.getBoolean("connected") ?: false
        }

        var onUSBConnected: ((Boolean) -> Unit)? = null
    }
}