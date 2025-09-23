package com.example.autoprivacyshield;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.IOException;
import java.util.List;

public class FaceDetectionActivity extends AppCompatActivity {
    private static final int PICK_IMAGE = 1;
    private ImageView imageView;
    private TextView resultText, faceCountText;
    private Button uploadButton;
    private Bitmap selectedBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_detection);

        imageView = findViewById(R.id.imageView);
        resultText = findViewById(R.id.resultText);
        faceCountText = findViewById(R.id.faceCount);
        uploadButton = findViewById(R.id.uploadButton);

        uploadButton.setOnClickListener(v -> openGallery());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                selectedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                detectFaces(selectedBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void detectFaces(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();
        FaceDetector detector = FaceDetection.getClient(options);

        detector.process(image)
                .addOnSuccessListener(faces -> processFaceResult(faces, bitmap))
                .addOnFailureListener(e -> Toast.makeText(this, "Face detection failed!", Toast.LENGTH_SHORT).show());
    }

    private void processFaceResult(List<Face> faces, Bitmap originalBitmap) {
        if (faces.isEmpty()) {
            resultText.setText("No faces detected.");
            faceCountText.setText("Faces detected: 0");
            imageView.setImageBitmap(originalBitmap);
            return;
        }

        Bitmap mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(5);

        for (Face face : faces) {
            RectF rect = new RectF(face.getBoundingBox());
            canvas.drawRect(rect, paint);
        }

        resultText.setText("Faces detected successfully!");
        faceCountText.setText("Faces detected: " + faces.size());
        imageView.setImageBitmap(mutableBitmap);
    }
}
