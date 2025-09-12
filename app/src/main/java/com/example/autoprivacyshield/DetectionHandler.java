package com.example.autoprivacyshield;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DetectionHandler {
    private static final String TAG = "DetectionHandler";
    private static DetectionUtils detectionUtils;

    // ===== Regex patterns =====
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?:\\+91[-\\s]?)?[6-9]\\d{9}");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern CARD_PATTERN = Pattern.compile("\\b\\d{4}([- ]?\\d{4}){3}\\b");
    private static final Pattern AADHAAR_PATTERN = Pattern.compile("\\b\\d{4}\\s?\\d{4}\\s?\\d{4}\\b");
    private static final Pattern PAN_PATTERN = Pattern.compile("[A-Z]{5}[0-9]{4}[A-Z]");
    private static final Pattern OTP_PATTERN = Pattern.compile("\\b\\d{4,8}\\b");

    public static void initialize(Context context) {
        if (detectionUtils == null) {
            detectionUtils = new DetectionUtils(context);
            Log.d(TAG, "DetectionHandler initialized");
        }
    }

    // ===== Process Images (Team B) =====
    public static void processBitmap(Bitmap bitmap, ProcessingCallback callback) {
        if (detectionUtils == null) {
            Log.e(TAG, "DetectionHandler not initialized!");
            callback.onProcessingComplete(bitmap, new Rect[0]);
            return;
        }

        detectionUtils.detectSensitiveRegions(bitmap, results -> {
            List<Rect> sensitiveBoxes = new ArrayList<>();

            for (DetectResult result : results) {
                if (result.isSensitive()) {
                    sensitiveBoxes.add(result.getBoundingBox());
                    Log.d(TAG, "Found sensitive " + result.getType() + ": " + result.getText());
                }
            }

            Rect[] sensitiveAreas = sensitiveBoxes.toArray(new Rect[0]);
            Bitmap maskedBitmap = MaskingUtils.blurRegions(bitmap, sensitiveAreas);
            callback.onProcessingComplete(maskedBitmap, sensitiveAreas);
        });
    }

    // ===== Process Notification Text =====
    public static boolean checkForSensitiveContent(String text) {
        if (text == null || text.isEmpty()) return false;

        String lower = text.toLowerCase();

        return EMAIL_PATTERN.matcher(text).find() ||
                CARD_PATTERN.matcher(text).find() ||
                AADHAAR_PATTERN.matcher(text).find() ||
                PAN_PATTERN.matcher(text).find() ||
                PHONE_PATTERN.matcher(text).find() ||
                OTP_PATTERN.matcher(text).find() ||
                lower.contains("otp") ||
                lower.contains("password") ||
                lower.contains("pin");
    }

    // ===== NEW: Find exact sensitive matches with ranges =====
    public static List<MatchRegion> findSensitiveRegions(String text) {
        List<MatchRegion> matches = new ArrayList<>();
        if (text == null || text.isEmpty()) return matches;

        addMatches(text, EMAIL_PATTERN, matches);
        addMatches(text, CARD_PATTERN, matches);
        addMatches(text, AADHAAR_PATTERN, matches);
        addMatches(text, PAN_PATTERN, matches);
        addMatches(text, PHONE_PATTERN, matches);
        addMatches(text, OTP_PATTERN, matches);

        // Keyword-based
        String lower = text.toLowerCase();
        if (lower.contains("otp")) {
            int index = lower.indexOf("otp");
            matches.add(new MatchRegion(index, index + 3, "otp"));
        }
        if (lower.contains("password")) {
            int index = lower.indexOf("password");
            matches.add(new MatchRegion(index, index + 8, "password"));
        }
        if (lower.contains("pin")) {
            int index = lower.indexOf("pin");
            matches.add(new MatchRegion(index, index + 3, "pin"));
        }

        return matches;
    }

    private static void addMatches(String text, Pattern pattern, List<MatchRegion> matches) {
        Matcher m = pattern.matcher(text);
        while (m.find()) {
            matches.add(new MatchRegion(m.start(), m.end(), m.group()));
        }
    }

    // ===== Callback Interface =====
    public interface ProcessingCallback {
        void onProcessingComplete(Bitmap processedBitmap, Rect[] sensitiveAreas);
    }
}
