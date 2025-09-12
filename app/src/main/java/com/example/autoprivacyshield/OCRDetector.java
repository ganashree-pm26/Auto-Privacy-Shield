package com.example.autoprivacyshield;

import android.graphics.Rect;
import android.text.TextUtils;

import com.google.mlkit.vision.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class OCRDetector {

    // Regex patterns (private, not accessed directly outside)
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("(?:\\+91[-\\s]?)?[6-9]\\d{9}");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern CARD_PATTERN =
            Pattern.compile("\\b\\d{4}([- ]?\\d{4}){3}\\b");
    private static final Pattern AADHAAR_PATTERN =
            Pattern.compile("\\b\\d{4}\\s?\\d{4}\\s?\\d{4}\\b");
    private static final Pattern PAN_PATTERN =
            Pattern.compile("[A-Z]{5}[0-9]{4}[A-Z]");
    private static final Pattern OTP_PATTERN =
            Pattern.compile("\\b\\d{4,8}\\b");

    /**
     * Method 1: Full OCR detection from ML Kit Vision text
     */
    public static List<DetectResult> detectSensitiveInfo(Text visionText) {
        List<DetectResult> results = new ArrayList<>();

        for (Text.TextBlock block : visionText.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                String lineText = line.getText();
                Rect boundingBox = line.getBoundingBox();

                if (!TextUtils.isEmpty(lineText) && boundingBox != null) {
                    String type = "Normal";
                    boolean isSensitive = false;

                    if (EMAIL_PATTERN.matcher(lineText).find()) {
                        type = "Email";
                        isSensitive = true;
                    } else if (CARD_PATTERN.matcher(lineText).find()) {
                        type = "Card Number";
                        isSensitive = true;
                    } else if (AADHAAR_PATTERN.matcher(lineText).find()) {
                        type = "Aadhaar Number";
                        isSensitive = true;
                    } else if (PAN_PATTERN.matcher(lineText).find()) {
                        type = "PAN Number";
                        isSensitive = true;
                    } else if (PHONE_PATTERN.matcher(lineText).find()) {
                        type = "Phone Number";
                        isSensitive = true;
                    } else if (OTP_PATTERN.matcher(lineText).find()
                            || lineText.toLowerCase().contains("otp")) {
                        type = "OTP";
                        isSensitive = true;
                    }

                    results.add(new DetectResult(lineText, isSensitive, type, boundingBox));
                }
            }
        }
        return results;
    }

    /**
     * Method 2: Simple string-based sensitive info detection
     * (For notifications, SMS, clipboard text, etc.)
     */
    public static boolean containsSensitive(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        return EMAIL_PATTERN.matcher(text).find()
                || CARD_PATTERN.matcher(text).find()
                || AADHAAR_PATTERN.matcher(text).find()
                || PAN_PATTERN.matcher(text).find()
                || PHONE_PATTERN.matcher(text).find()
                || OTP_PATTERN.matcher(text).find()
                || text.toLowerCase().contains("otp");
    }

    public static boolean detectSensitiveInfoFromText(String text) {
        return EMAIL_PATTERN.matcher(text).find()
                || CARD_PATTERN.matcher(text).find()
                || AADHAAR_PATTERN.matcher(text).find()
                || PAN_PATTERN.matcher(text).find()
                || PHONE_PATTERN.matcher(text).find()
                || OTP_PATTERN.matcher(text).find()
                || text.toLowerCase().contains("otp");
    }

}
