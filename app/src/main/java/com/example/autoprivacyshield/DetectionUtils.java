package com.example.autoprivacyshield;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class DetectionUtils {
    private static final String TAG = "DetectionUtils";

    private FaceDetector faceDetector;
    private TextRecognizer textRecognizer;
    private YoloV8Helper yoloHelper;

    public DetectionUtils(Context context) {
        FaceDetectorOptions faceOptions = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .build();

        faceDetector = FaceDetection.getClient(faceOptions);
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        try {
            yoloHelper = new YoloV8Helper(context);
            Log.d(TAG, "✅ YOLO model loaded successfully!");
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to load YOLO model (this is optional)", e);
            yoloHelper = null;
        }

        Log.d(TAG, "DetectionUtils initialized with ML Kit Face + OCR detection");
    }

    public void detectSensitiveRegions(Bitmap bitmap, DetectionCallback callback) {
        List<DetectResult> results = new ArrayList<>();
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        faceDetector.process(image)
                .addOnSuccessListener(faces -> {
                    Log.d(TAG, "Face detection completed - found " + faces.size() + " faces");

                    for (Face face : faces) {
                        DetectResult faceResult = new DetectResult(
                                "Face detected", // text
                                true,            // sensitive
                                "FACE",          // type
                                face.getBoundingBox() // boundingBox
                        );
                        results.add(faceResult);
                    }

                    textRecognizer.process(image)
                            .addOnSuccessListener(visionText -> {
                                Log.d(TAG, "OCR detection completed");

                                List<Text.TextBlock> blocks = visionText.getTextBlocks();
                                for (Text.TextBlock block : blocks) {
                                    String blockText = block.getText();
                                    if (blockText != null && !blockText.trim().isEmpty()) {
                                        boolean isSensitive = isSensitiveText(blockText);
                                        String sensitiveType = getSensitiveType(blockText);

                                        DetectResult textResult = new DetectResult(
                                                blockText.trim(),      // text
                                                isSensitive,           // sensitive
                                                sensitiveType,         // type
                                                block.getBoundingBox() // boundingBox
                                        );
                                        results.add(textResult);
                                    }
                                }

                                Log.d(TAG, "Detection complete - total results: " + results.size());
                                callback.onDetectionComplete(results);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "OCR failed", e);
                                callback.onDetectionComplete(results);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Face detection failed", e);

                    // Try OCR only if face detection fails
                    textRecognizer.process(image)
                            .addOnSuccessListener(visionText -> {
                                List<Text.TextBlock> blocks = visionText.getTextBlocks();
                                for (Text.TextBlock block : blocks) {
                                    String blockText = block.getText();
                                    if (blockText != null && !blockText.trim().isEmpty()) {
                                        boolean isSensitive = isSensitiveText(blockText);
                                        String sensitiveType = getSensitiveType(blockText);

                                        DetectResult textResult = new DetectResult(
                                                blockText.trim(),      // text
                                                isSensitive,           // sensitive
                                                sensitiveType,         // type
                                                block.getBoundingBox() // boundingBox
                                        );
                                        results.add(textResult);
                                    }
                                }
                                callback.onDetectionComplete(results);
                            })
                            .addOnFailureListener(ocrError -> {
                                Log.e(TAG, "Both face and OCR detection failed", ocrError);
                                callback.onDetectionComplete(new ArrayList<>());
                            });
                });
    }

    // Sensitive data detection logic (Aadhaar, PAN, phone number, DOB, OTP, password, PIN)
    private boolean isSensitiveText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        String cleanText = text.replaceAll("\\s+", " ").trim();

        return Pattern.matches(".*\\b\\d{12}\\b.*", cleanText) ||  // Aadhaar (12 digits)
                Pattern.matches(".*\\b[A-Z]{5}[0-9]{4}[A-Z]\\b.*", cleanText) ||  // PAN
                Pattern.matches(".*\\b\\d{10}\\b.*", cleanText) ||  // Phone number (10 digits)
                Pattern.matches(".*\\b\\d{2}/\\d{2}/\\d{4}\\b.*", cleanText) ||  // Date of birth
                Pattern.matches(".*\\b\\d{4}-\\d{2}-\\d{2}\\b.*", cleanText) ||  // ISO date
                cleanText.toLowerCase().contains("otp") ||
                cleanText.toLowerCase().contains("password") ||
                cleanText.toLowerCase().contains("pin") ||
                Pattern.matches(".*\\b\\d{4}\\b.*", cleanText); // 4-digit codes (PIN/OTP)
    }

    private String getSensitiveType(String text) {
        if (text == null) return "TEXT";

        String cleanText = text.replaceAll("\\s+", " ").trim();

        if (Pattern.matches(".*\\b\\d{12}\\b.*", cleanText)) return "AADHAAR";
        if (Pattern.matches(".*\\b[A-Z]{5}[0-9]{4}[A-Z]\\b.*", cleanText)) return "PAN";
        if (Pattern.matches(".*\\b\\d{10}\\b.*", cleanText)) return "PHONE";
        if (Pattern.matches(".*\\b\\d{2}/\\d{2}/\\d{4}\\b.*", cleanText) ||
                Pattern.matches(".*\\b\\d{4}-\\d{2}-\\d{2}\\b.*", cleanText)) return "DOB";
        if (cleanText.toLowerCase().contains("otp")) return "OTP";
        if (cleanText.toLowerCase().contains("password")) return "PASSWORD";
        if (cleanText.toLowerCase().contains("pin")) return "PIN";
        if (Pattern.matches(".*\\b\\d{4}\\b.*", cleanText)) return "CODE";

        return "TEXT";
    }

    public interface DetectionCallback {
        void onDetectionComplete(List<DetectResult> results);
    }

    public void cleanup() {
        if (yoloHelper != null) {
            yoloHelper.close();
        }
        Log.d(TAG, "DetectionUtils cleaned up");
    }
}
