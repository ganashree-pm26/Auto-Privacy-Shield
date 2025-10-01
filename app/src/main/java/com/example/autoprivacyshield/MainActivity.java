package com.example.autoprivacyshield;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
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
import androidx.appcompat.app.AlertDialog;
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

    // State tracking
    private boolean isPrivacyActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestNotificationAccess();

        initTeamAComponents();
        initTeamBComponents();

        DetectionHandler.initialize(this);

        Log.d(TAG, "AutoPrivacyShield initialized");
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

        startBtn.setOnClickListener(v -> {
            if (isPrivacyActive) {
                stopPrivacyProtection();
            } else {
                startPrivacyProtection();
            }
        });
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
            Log.d(TAG, "YOLOv8 model loaded successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to load YOLOv8 model", e);
        }
    }

    /** ------------------- PRIVACY PROTECTION ------------------- */

    /**
     * Start the privacy protection system
     * Checks permissions and starts screen capture with overlay
     */
    private void startPrivacyProtection() {
        // Step 1: Check overlay permission (Android 6.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                showOverlayPermissionDialog();
                return;
            }
        }

        // Step 2: Request screen capture permission
        requestScreenCapture();
    }

    /**
     * Show dialog explaining why overlay permission is needed
     */
    private void showOverlayPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Overlay Permission Required")
                .setMessage("Auto Privacy Shield needs overlay permission to display the privacy-protected screen on top of your apps.\n\n" +
                        "This is essential for the privacy protection to work during screen sharing.")
                .setPositiveButton("Grant Permission", (dialog, which) -> requestOverlayPermission())
                .setNegativeButton("Cancel", (dialog, which) -> {
                    Toast.makeText(this, "Privacy protection cannot work without overlay permission",
                            Toast.LENGTH_LONG).show();
                })
                .setCancelable(false)
                .show();
    }

    /**
     * Request overlay permission
     */
    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));

            Toast.makeText(this,
                    "Please enable 'Display over other apps' permission",
                    Toast.LENGTH_LONG).show();

            overlayPermissionLauncher.launch(intent);
        }
    }

    /**
     * Handle overlay permission result
     */
    private final ActivityResultLauncher<Intent> overlayPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Settings.canDrawOverlays(this)) {
                        Toast.makeText(this, "Overlay permission granted!", Toast.LENGTH_SHORT).show();
                        // Now request screen capture
                        handler.postDelayed(this::requestScreenCapture, 500);
                    } else {
                        Toast.makeText(this,
                                "Overlay permission is required for privacy protection to work",
                                Toast.LENGTH_LONG).show();
                    }
                }
            });

    /** ------------------- SCREEN CAPTURE ------------------- */

    /**
     * Request screen capture permission from system
     */
    private void requestScreenCapture() {
        android.media.projection.MediaProjectionManager projectionManager =
                (android.media.projection.MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        if (projectionManager != null) {
            Intent intent = projectionManager.createScreenCaptureIntent();
            screenCaptureResultLauncher.launch(intent);
        }
    }

    /**
     * Handle screen capture permission result
     */
    private final ActivityResultLauncher<Intent> screenCaptureResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Start the screen capture service with overlay
                    Intent serviceIntent = new Intent(this, ScreenCaptureService.class);
                    serviceIntent.putExtra("resultCode", result.getResultCode());
                    serviceIntent.putExtra("data", result.getData());
                    startForegroundService(serviceIntent);

                    // Update UI
                    isPrivacyActive = true;
                    startBtn.setText("Stop Privacy Protection");
                    startBtn.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));

                    Toast.makeText(this,
                            "Privacy Protection Active!\n\nSensitive content will be automatically hidden.",
                            Toast.LENGTH_LONG).show();

                    Log.d(TAG, "Privacy protection service started successfully");
                } else {
                    Toast.makeText(this,
                            "Screen capture permission denied. Privacy protection cannot start.",
                            Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "Screen capture permission denied by user");
                }
            });

    /**
     * Stop privacy protection service
     */
    private void stopPrivacyProtection() {
        Intent serviceIntent = new Intent(this, ScreenCaptureService.class);
        stopService(serviceIntent);

        isPrivacyActive = false;
        startBtn.setText("Start Privacy Protection");
        startBtn.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));

        Toast.makeText(this, "Privacy protection stopped", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Privacy protection service stopped");
    }

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
                    Log.d(TAG, "Highlighted sensitive: " + match.matchedText);
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

    @Override
    protected void onResume() {
        super.onResume();
        // Update button state in case service was stopped externally
        // You can add a mechanism to check if service is running
    }
}