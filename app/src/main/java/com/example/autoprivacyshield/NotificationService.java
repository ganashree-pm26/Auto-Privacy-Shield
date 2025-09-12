package com.example.autoprivacyshield;

import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.List;

public class NotificationService extends NotificationListenerService {

    public static final String ACTION_NEW_NOTIFICATION = "com.example.autoprivacyshield.NEW_NOTIFICATION";
    public static final String EXTRA_NOTIFICATION_TEXT = "extra_notification_text";
    public static final String EXTRA_NOTIFICATION_SENDER = "extra_notification_sender";
    public static final String EXTRA_IS_SENSITIVE = "extra_is_sensitive";

    private static final String TAG = "NotificationService";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        if (notification == null) return;

        Bundle extras = notification.extras;
        if (extras == null) return;

        String sender = extras.getString(Notification.EXTRA_TITLE); // sender name (e.g., WhatsApp contact)
        String message = "";

        // ✅ Extract single message
        CharSequence text = extras.getCharSequence(Notification.EXTRA_TEXT);
        if (!TextUtils.isEmpty(text)) {
            message = text.toString();
        }

        // ✅ Extract multiple messages (grouped notifications)
        CharSequence[] lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if (lines != null && lines.length > 0) {
            List<String> msgs = new ArrayList<>();
            for (CharSequence line : lines) {
                msgs.add(line.toString());
            }
            message = TextUtils.join("\n", msgs);
        }

        // ✅ Handle expanded big text (sometimes OTPs are here)
        CharSequence bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        if (!TextUtils.isEmpty(bigText)) {
            message = bigText.toString();
        }

        if (TextUtils.isEmpty(message)) return;

        // Check sensitivity (OCRDetector handles OTP, phone, card, etc.)
        boolean isSensitive = OCRDetector.detectSensitiveInfoFromText(message);

        Log.d(TAG, "📩 New notification from " + sender + ": " + message + " | Sensitive=" + isSensitive);

        // Send broadcast to MainActivity
        Intent intent = new Intent(ACTION_NEW_NOTIFICATION);
        intent.putExtra(EXTRA_NOTIFICATION_TEXT, message);
        intent.putExtra(EXTRA_NOTIFICATION_SENDER, sender);
        intent.putExtra(EXTRA_IS_SENSITIVE, isSensitive);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
