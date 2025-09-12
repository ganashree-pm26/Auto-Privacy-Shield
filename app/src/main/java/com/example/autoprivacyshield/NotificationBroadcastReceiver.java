package com.example.autoprivacyshield;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NotificationBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (NotificationService.ACTION_NEW_NOTIFICATION.equals(intent.getAction())) {
            String notificationText = intent.getStringExtra(NotificationService.EXTRA_NOTIFICATION_TEXT);
            Log.d("NotificationReceiver", "Received notification text: " + notificationText);
            // Further processing or UI update can be done here
        }
    }
}
