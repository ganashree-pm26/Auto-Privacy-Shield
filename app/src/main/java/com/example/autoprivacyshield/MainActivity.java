package com.example.autoprivacyshield;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.tensorflow.lite.Interpreter;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    // UI
    private ImageView imageView;
    private TextView notificationTextView;
    private Button startBtn;
    private Handler handler;
    private NotificationBroadcastReceiver notificationReceiver;

    // Team B Buttons
    private Button btnFaceOcr, btnOcrOnly, btnFaceOnly;
    private Interpreter yoloInterpreter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestNotificationAccess();

        initTeamAComponents();
        initTeamBComponents();

        DetectionHandler.initialize(this);

        Log.d(TAG, "✅ AutoPrivacyShield initialized");
    }

    /** ------------------- TEAM A ------------------- */
    private void initTeamAComponents() {
        imageView = findViewById(R.id.imageView);
        notificationTextView = findViewById(R.id.notificationTextView);
        startBtn = findViewById(R.id.startBtn);
        handler = new Handler();

        notificationReceiver = new NotificationBroadcastReceiver();
        IntentFilter filter = new IntentFilter(NotificationService.ACTION_NEW_NOTIFICATION);
        LocalBroadcastManager.getInstance(this).registerReceiver(notificationReceiver, filter);

        startBtn.setOnClickListener(v -> requestScreenCapture());
    }

    /** ------------------- TEAM B ------------------- */
    private void initTeamBComponents() {
        btnFaceOcr = findViewById(R.id.btnFaceOcr);
        btnOcrOnly = findViewById(R.id.btnOcrOnly);
        btnFaceOnly = findViewById(R.id.btnFaceOnly);

        btnFaceOcr.setOnClickListener(v -> startActivity(new Intent(this, FaceOcrActivity.class)));
        btnOcrOnly.setOnClickListener(v -> startActivity(new Intent(this, OCRActivity.class)));
        btnFaceOnly.setOnClickListener(v -> startActivity(new Intent(this, FaceDetectionActivity.class)));

        try {
            YoloV8Helper helper = new YoloV8Helper(this);
            yoloInterpreter = helper.getInterpreter();
            Log.d(TAG, "✅ YOLOv8 model loaded successfully");
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to load YOLOv8 model", e);
        }
    }

    /** ------------------- SCREEN CAPTURE ------------------- */
    private void requestScreenCapture() {
        android.media.projection.MediaProjectionManager projectionManager =
                (android.media.projection.MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        if (projectionManager != null) {
            Intent intent = projectionManager.createScreenCaptureIntent();
            screenCaptureResultLauncher.launch(intent);
        }
    }

    private final ActivityResultLauncher<Intent> screenCaptureResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent serviceIntent = new Intent(this, ScreenCaptureService.class);
                    serviceIntent.putExtra("resultCode", result.getResultCode());
                    serviceIntent.putExtra("data", result.getData());
                    startForegroundService(serviceIntent);

                    Toast.makeText(this, "Privacy protection is now active!", Toast.LENGTH_LONG).show();
                    startBtn.setText("Privacy Protection Active");
                    startBtn.setEnabled(false);
                } else {
                    Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    /** ------------------- NOTIFICATION HANDLER ------------------- */
    private class NotificationBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (NotificationService.ACTION_NEW_NOTIFICATION.equals(intent.getAction())) {
                String notificationText = intent.getStringExtra(NotificationService.EXTRA_NOTIFICATION_TEXT);
                String sender = intent.getStringExtra(NotificationService.EXTRA_NOTIFICATION_SENDER);

                if (notificationText == null) notificationText = "";

                // Highlight sensitive parts instead of hiding everything
                SpannableString spannable = new SpannableString("From " + sender + ": " + notificationText);

                List<MatchRegion> matches = DetectionHandler.findSensitiveRegions(notificationText);
                for (MatchRegion match : matches) {
                    int start = ("From " + sender + ": ").length() + match.start;
                    int end = ("From " + sender + ": ").length() + match.end;
                    spannable.setSpan(
                            new BackgroundColorSpan(Color.YELLOW),
                            start, end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                    Log.d(TAG, "🔒 Highlighted sensitive: " + match.matchedText);
                }

                notificationTextView.setText(spannable);
            }
        }
    }

    /** ------------------- NOTIFICATION ACCESS ------------------- */
    private void requestNotificationAccess() {
        if (!isNotificationServiceEnabled()) {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "Please enable AutoPrivacyShield notification access", Toast.LENGTH_LONG).show();
        }
    }

    private boolean isNotificationServiceEnabled() {
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(),
                "enabled_notification_listeners");
        return flat != null && flat.contains(pkgName);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationReceiver);
        handler.removeCallbacksAndMessages(null);
    }
}
