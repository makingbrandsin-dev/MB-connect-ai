package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log

class CallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            // Note: EXTRA_INCOMING_NUMBER requires READ_CALL_LOG or READ_PHONE_STATE permissions
            val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
            Log.d("CallReceiver", "SIM Phone State Change: $state. Incoming number: $incomingNumber")

            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                val callerNumber = incomingNumber ?: "Private Number"
                Log.d("CallReceiver", "SIM Call ringing from $callerNumber. Triggering auto-routing.")
                
                // In a fully deployed context, we'd invoke the CallAnsweringService here:
                // CallAnsweringService.get(context).onIncomingCall(callerNumber, "", "SIM")
            }
        }
    }
}
