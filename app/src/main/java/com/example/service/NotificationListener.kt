package com.example.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationListener : NotificationListenerService() {
    
    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("NotificationListener", "Notification Listener Connected successfully.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        if (packageName == "com.whatsapp" || packageName == "com.whatsapp.w4b") {
            val extras = sbn.notification.extras
            val title = extras.getCharSequence("android.title")?.toString() ?: "WhatsApp Caller"
            val text = extras.getCharSequence("android.text")?.toString() ?: ""
            Log.d("NotificationListener", "WhatsApp notification intercepted: $title - $text")

            // Identify WhatsApp Call signatures (varies by locale but standard is "Incoming voice call", "Incoming video call", "Incoming call")
            val isCallNotification = text.contains("Incoming voice call", ignoreCase = true) || 
                                     text.contains("Incoming video call", ignoreCase = true) || 
                                     text.contains("Incoming call", ignoreCase = true) ||
                                     text.contains("Call", ignoreCase = true)

            if (isCallNotification) {
                Log.d("NotificationListener", "Detected active WhatsApp call notification from: $title. Routing through MB Connect Bot.")
                
                // In production, we trigger the central answering service with the parsed name
                // CallAnsweringService.get(this).onIncomingCall("", title, "WhatsApp")
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        Log.d("NotificationListener", "Notification removed.")
    }
}
