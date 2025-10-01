package com.example.autoprivacyshield;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class ScreenCaptureService extends Service {
    private static final String CHANNEL_ID = "ScreenCaptureChannel";
    private static final String TAG = "ScreenCaptureService";
    private static final int PROCESS_EVERY_N_FRAMES = 3; // Process every 3rd frame for performance

    // Actions for notification buttons
    public static final String ACTION_STOP_PRIVACY = "com.example.autoprivacyshield.STOP_PRIVACY";
    public static final String ACTION_TOGGLE_PRIVACY = "com.example.autoprivacyshield.TOGGLE_PRIVACY";

    private MediaProjection mediaProjection;
    private ImageReader imageReader;
    private Handler handler;
    private int screenDensity;
    private int screenWidth;
    private int screenHeight;

    // Overlay components
    private WindowManager windowManager;
    private View overlayView;
    private ImageView overlayImageView;
    private boolean isOverlayShowing = false;
    private boolean isPrivacyEnabled = true;

    // Frame processing
    private int frameCount = 0;
    private Bitmap lastProcessedBitmap = null;
    private final Object bitmapLock = new Object();

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        // Initialize components
        handler = new Handler(Looper.getMainLooper());
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // Initialize detection handler
        DetectionHandler.initialize(this);
        Log.d(TAG, "Detection handler initialized");

        // Get screen dimensions
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);
        screenDensity = metrics.densityDpi;
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;

        Log.d(TAG, "Screen: " + screenWidth + "x" + screenHeight + " @" + screenDensity + "dpi");

        // Create and show overlay
        createOverlay();

        // Start foreground with notification
        startForeground(1, createNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Handle notification actions
        if (intent != null) {
            String action = intent.getAction();

            if (ACTION_STOP_PRIVACY.equals(action)) {
                stopPrivacyProtection();
                return START_NOT_STICKY;
            } else if (ACTION_TOGGLE_PRIVACY.equals(action)) {
                togglePrivacyMode();
                return START_STICKY;
            }
        }

        // Start screen capture
        int resultCode = intent.getIntExtra("resultCode", -1);
        Intent data = intent.getParcelableExtra("data");

        MediaProjectionManager projectionManager =
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        if (projectionManager != null && data != null) {
            mediaProjection = projectionManager.getMediaProjection(resultCode, data);
            initVirtualDisplay();
        } else {
            Log.e(TAG, "Failed to get MediaProjection");
            stopSelf();
        }

        return START_STICKY;
    }

    /**
     * Create the overlay window that will display privacy-protected screen
     */
    private void createOverlay() {
        // Check if we have overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !Settings.canDrawOverlays(this)) {
            Log.e(TAG, "No overlay permission!");
            return;
        }

        // Inflate overlay layout
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        overlayView = inflater.inflate(R.layout.overlay_privacy_screen, null);
        overlayImageView = overlayView.findViewById(R.id.overlayImageView);

        // Configure window parameters
        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 0;

        // Add overlay to window manager
        try {
            windowManager.addView(overlayView, params);
            isOverlayShowing = true;
            overlayView.setVisibility(View.VISIBLE);
            Log.d(TAG, "Overlay created and added to window");
        } catch (Exception e) {
            Log.e(TAG, "Failed to add overlay view", e);
            isOverlayShowing = false;
        }
    }

    /**
     * Initialize virtual display for screen capture
     */
    private void initVirtualDisplay() {
        if (mediaProjection == null) {
            Log.e(TAG, "MediaProjection is null");
            return;
        }

        mediaProjection.registerCallback(new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
                Log.d(TAG, "MediaProjection stopped");
                cleanupResources();
                stopSelf();
            }
        }, handler);

        // Create ImageReader for capturing frames
        imageReader = ImageReader.newInstance(
                screenWidth,
                screenHeight,
                PixelFormat.RGBA_8888,
                2
        );

        // Create virtual display
        mediaProjection.createVirtualDisplay(
                "AutoPrivacyShield-Capture",
                screenWidth,
                screenHeight,
                screenDensity,
                0, // No flags needed
                imageReader.getSurface(),
                null,
                handler
        );

        // Set up frame capture callback
        imageReader.setOnImageAvailableListener(this::onFrameAvailable, handler);

        Log.d(TAG, "Virtual display initialized");
    }

    /**
     * Called when a new frame is available from screen capture
     */
    private void onFrameAvailable(ImageReader reader) {
        Image image = null;
        try {
            image = reader.acquireLatestImage();
            if (image == null) return;

            frameCount++;

            // Process every Nth frame for performance
            if (frameCount % PROCESS_EVERY_N_FRAMES != 0) {
                // Use last processed frame
                if (lastProcessedBitmap != null && isPrivacyEnabled) {
                    updateOverlay(lastProcessedBitmap);
                }
                return;
            }

            // Convert image to bitmap
            Bitmap frameBitmap = ImageUtils.imageToBitmap(image);

            if (frameBitmap == null) {
                Log.w(TAG, "Failed to convert image to bitmap");
                return;
            }

            if (!isPrivacyEnabled) {
                // Privacy disabled, show raw frame
                updateOverlay(frameBitmap);
                return;
            }

            // Process frame for sensitive content
            processFrameForPrivacy(frameBitmap);

        } catch (Exception e) {
            Log.e(TAG, "Error processing frame", e);
        } finally {
            if (image != null) {
                image.close();
            }
        }
    }

    /**
     * Process frame to detect and mask sensitive content
     */
    private void processFrameForPrivacy(Bitmap originalFrame) {
        // Create a copy for processing (don't modify original)
        Bitmap frameCopy = originalFrame.copy(originalFrame.getConfig(), true);

        // Run detection and masking
        DetectionHandler.processBitmap(frameCopy, new DetectionHandler.ProcessingCallback() {
            @Override
            public void onProcessingComplete(Bitmap processedBitmap, Rect[] sensitiveAreas) {
                // Store processed bitmap for reuse
                synchronized (bitmapLock) {
                    if (lastProcessedBitmap != null && !lastProcessedBitmap.isRecycled()) {
                        lastProcessedBitmap.recycle();
                    }
                    lastProcessedBitmap = processedBitmap;
                }

                // Update overlay with processed frame
                updateOverlay(processedBitmap);

                if (sensitiveAreas.length > 0) {
                    Log.d(TAG, "Masked " + sensitiveAreas.length + " sensitive regions");
                }

                // Clean up original frame
                if (!originalFrame.isRecycled()) {
                    originalFrame.recycle();
                }
            }
        });
    }

    /**
     * Update the overlay with new frame
     */
    private void updateOverlay(Bitmap bitmap) {
        if (!isOverlayShowing || overlayImageView == null) {
            return;
        }

        handler.post(() -> {
            try {
                overlayImageView.setImageBitmap(bitmap);
            } catch (Exception e) {
                Log.e(TAG, "Error updating overlay", e);
            }
        });
    }

    /**
     * Toggle privacy protection on/off
     */
    private void togglePrivacyMode() {
        isPrivacyEnabled = !isPrivacyEnabled;
        Log.d(TAG, "Privacy mode: " + (isPrivacyEnabled ? "ENABLED" : "DISABLED"));

        // Update notification
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(1, createNotification());
        }
    }

    /**
     * Stop privacy protection and service
     */
    private void stopPrivacyProtection() {
        Log.d(TAG, "Stopping privacy protection");
        stopSelf();
    }

    /**
     * Create notification with action buttons
     */
    private Notification createNotification() {
        Intent stopIntent = new Intent(this, ScreenCaptureService.class);
        stopIntent.setAction(ACTION_STOP_PRIVACY);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent toggleIntent = new Intent(this, ScreenCaptureService.class);
        toggleIntent.setAction(ACTION_TOGGLE_PRIVACY);
        PendingIntent togglePendingIntent = PendingIntent.getService(
                this, 1, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String toggleText = isPrivacyEnabled ? "Disable Privacy" : "Enable Privacy";
        String contentText = isPrivacyEnabled ?
                "Privacy protection active - sensitive content is masked" :
                "Privacy protection paused";

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Auto Privacy Shield")
                .setContentText(contentText)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .addAction(R.mipmap.ic_launcher, toggleText, togglePendingIntent)
                .addAction(R.mipmap.ic_launcher, "Stop", stopPendingIntent)
                .build();
    }

    /**
     * Create notification channel for Android O+
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Privacy Protection Service",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Controls screen privacy protection");
            channel.setShowBadge(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Clean up all resources
     */
    private void cleanupResources() {
        // Stop media projection
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }

        // Close image reader
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }

        // Remove overlay
        if (isOverlayShowing && overlayView != null) {
            try {
                windowManager.removeView(overlayView);
                isOverlayShowing = false;
            } catch (Exception e) {
                Log.e(TAG, "Error removing overlay", e);
            }
        }

        // Recycle bitmaps
        synchronized (bitmapLock) {
            if (lastProcessedBitmap != null && !lastProcessedBitmap.isRecycled()) {
                lastProcessedBitmap.recycle();
                lastProcessedBitmap = null;
            }
        }

        Log.d(TAG, "Resources cleaned up");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
        cleanupResources();
    }
}