package com.example.autoprivacyshield;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.InputStream;
import java.util.List;

public class OCRActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1;

    private TextView resultTextView;
    private Button uploadButton, copyButton;
    private ImageView imageView;
    private Bitmap selectedBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr);

        resultTextView = findViewById(R.id.resultText);
        uploadButton = findViewById(R.id.uploadButton);
        copyButton = findViewById(R.id.copyTextBtn);
        imageView = findViewById(R.id.imageView);

        uploadButton.setOnClickListener(v -> showImagePickerOptions());

        copyButton.setOnClickListener(v -> {
            String text = resultTextView.getText().toString();
            android.content.ClipboardManager clipboard =
                    (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            android.content.ClipData clip =
                    android.content.ClipData.newPlainText("OCR Text", text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_SHORT).show();
        });
    }

    private void showImagePickerOptions() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryIntent.setType("image/*");
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Intent documentsIntent = new Intent(Intent.ACTION_GET_CONTENT);
        documentsIntent.setType("image/*");

        Intent chooserIntent = Intent.createChooser(galleryIntent, "Select Image");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cameraIntent, documentsIntent});
        startActivityForResult(chooserIntent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            Bitmap bitmap = null;
            Uri imageUri = data.getData();

            if (imageUri == null && data.getExtras() != null) {
                bitmap = (Bitmap) data.getExtras().get("data");
            }

            if (imageUri != null) {
                try {
                    InputStream imageStream = getContentResolver().openInputStream(imageUri);
                    bitmap = BitmapFactory.decodeStream(imageStream);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (bitmap == null) {
                Drawable drawable = ContextCompat.getDrawable(this, R.drawable.test_face);
                bitmap = ((BitmapDrawable) drawable).getBitmap();
            }

            selectedBitmap = bitmap;
            imageView.setImageBitmap(bitmap);
            runTextRecognitionFromBitmap(bitmap);
        }
    }

    private void runTextRecognitionFromBitmap(Bitmap bitmap) {
        try {
            InputImage image = InputImage.fromBitmap(bitmap, 0);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        List<DetectResult> detectResults = OCRDetector.detectSensitiveInfo(visionText);

                        // Draw bounding boxes
                        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                        Canvas canvas = new Canvas(mutableBitmap);
                        Paint paintNormal = new Paint();
                        paintNormal.setColor(Color.GREEN);
                        paintNormal.setStyle(Paint.Style.STROKE);
                        paintNormal.setStrokeWidth(4f);

                        Paint paintSensitive = new Paint();
                        paintSensitive.setColor(Color.BLACK);
                        paintSensitive.setStyle(Paint.Style.FILL);
                        paintSensitive.setStrokeWidth(6f);

                        for (DetectResult result : detectResults) {
                            Rect box = result.getBoundingBox();
                            if (box != null) {
                                if (result.isSensitive()) {
                                    canvas.drawRect(box, paintSensitive);
                                } else {
                                    canvas.drawRect(box, paintNormal);
                                }
                            }
                        }

                        imageView.setImageBitmap(mutableBitmap);

                        // Text output
                        SpannableStringBuilder spannableBuilder = new SpannableStringBuilder();

                        for (DetectResult result : detectResults) {
                            String displayText = result.getText();
                            if (result.isSensitive()) {
                                displayText += "  [Sensitive: " + result.getType() + "]";
                            }
                            displayText += "\n";

                            int start = spannableBuilder.length();
                            spannableBuilder.append(displayText);
                            int end = spannableBuilder.length();

                            if (result.isSensitive()) {
                                spannableBuilder.setSpan(
                                        new ForegroundColorSpan(ContextCompat.getColor(this, android.R.color.holo_red_dark)),
                                        start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                );
                                spannableBuilder.setSpan(
                                        new StyleSpan(android.graphics.Typeface.BOLD),
                                        start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                );
                            }
                        }

                        if (spannableBuilder.length() == 0) {
                            resultTextView.setText("No text detected in the image.");
                        } else {
                            resultTextView.setText(spannableBuilder);
                            copyButton.setVisibility(android.view.View.VISIBLE);
                            uploadButton.setText("Select Another Image");
                        }
                    })
                    .addOnFailureListener(e -> resultTextView.setText("OCR failed: " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            resultTextView.setText("Error processing image: " + e.getMessage());
        }
    }
}
