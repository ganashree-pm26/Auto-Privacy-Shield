package com.example.autoprivacyshield;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
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
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class ScreenCaptureService extends Service {
    private static final String CHANNEL_ID = "ScreenCaptureChannel";
    private static final String TAG = "ScreenCaptureService";

    private MediaProjection mediaProjection;
    private ImageReader imageReader;
    private Handler handler;
    private int screenDensity;
    private int screenWidth;
    private int screenHeight;
    private Bitmap currentFrameBitmap;
    private Bitmap processedFrameBitmap;  // Processed frame with masking applied

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("AutoPrivacyShield")
                .setContentText("Screen capture with privacy protection is running")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .build();

        startForeground(1, notification);

        handler = new Handler(Looper.getMainLooper());

        // Initialize detection handler (Team B)
        DetectionHandler.initialize(this);
        Log.d(TAG, "Team B detection initialized");

        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);
        screenDensity = metrics.densityDpi;
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;

        Log.d(TAG, "Screen dimensions: " + screenWidth + "x" + screenHeight + " density: " + screenDensity);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int resultCode = intent.getIntExtra("resultCode", -1);
        Intent data = intent.getParcelableExtra("data");

        MediaProjectionManager projectionManager =
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        if (projectionManager != null && data != null) {
            mediaProjection = projectionManager.getMediaProjection(resultCode, data);
            initVirtualDisplay();
        } else {
            Log.e(TAG, "Failed to get MediaProjection - resultCode: " + resultCode + " data: " + data);
            stopSelf();
        }

        return START_STICKY;
    }

    private void initVirtualDisplay() {
        if (mediaProjection == null) {
            Log.e(TAG, "MediaProjection is null, cannot initialize VirtualDisplay");
            return;
        }

        mediaProjection.registerCallback(new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
                Log.d(TAG, "MediaProjection stopped");
                if (imageReader != null) {
                    imageReader.close();
                    imageReader = null;
                }
                mediaProjection = null;
                stopSelf();
            }
        }, handler);

        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);

        mediaProjection.createVirtualDisplay(
                "AutoPrivacyShield-ScreenCapture",
                screenWidth,
                screenHeight,
                screenDensity,
                0,
                imageReader.getSurface(),
                null,
                handler);

        imageReader.setOnImageAvailableListener(reader -> {
            Image image = null;
            try {
                image = reader.acquireLatestImage();
                if (image != null) {
                    Bitmap bitmap = ImageUtils.imageToBitmap(image);
                    image.close();

                    if (bitmap != null) {
                        currentFrameBitmap = bitmap;
                        Log.d(TAG, "Captured frame size: " + bitmap.getWidth() + "x" + bitmap.getHeight());

                        // Process with Team B detection + masking
                        processFrame(bitmap);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error capturing frame", e);
            } finally {
                if (image != null) image.close();
            }
        }, handler);

        Log.d(TAG, "VirtualDisplay initialized successfully");
    }

    private void processFrame(Bitmap originalFrame) {
        Bitmap frameCopy = originalFrame.copy(originalFrame.getConfig(), false);

        DetectionHandler.processBitmap(frameCopy, new DetectionHandler.ProcessingCallback() {
            @Override
            public void onProcessingComplete(Bitmap processedBitmap, Rect[] sensitiveAreas) {
                processedFrameBitmap = processedBitmap;

                if (sensitiveAreas.length > 0) {
                    Log.d(TAG, "Frame processed with " + sensitiveAreas.length + " sensitive areas masked");
                } else {
                    Log.d(TAG, "Frame processed - no sensitive content detected");
                }
            }
        });
    }

    public Bitmap getCurrentFrame() {
        return processedFrameBitmap != null ? processedFrameBitmap : currentFrameBitmap;
    }

    public Bitmap getRawFrame() {
        return currentFrameBitmap;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not bound
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "ScreenCaptureService destroyed");

        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }

        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "AutoPrivacyShield Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("Screen capture with privacy protection");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}
