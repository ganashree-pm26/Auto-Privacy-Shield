package com.example.autoprivacyshield;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class YoloV8Helper {
    private static final String TAG = "YoloV8Helper";
    private Interpreter interpreter;

    public YoloV8Helper(Context context) throws IOException {
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(4);

        try {
            interpreter = new Interpreter(loadModelFile(context, "yolov8_select_tf_ops.tflite"), options);
            Log.d(TAG, "YOLO model loaded successfully");
        } catch (IOException e) {
            Log.e(TAG, "Failed to load YOLO model: " + e.getMessage());
            throw e;
        }
    }

    private MappedByteBuffer loadModelFile(Context context, String modelPath) throws IOException {
        try {
            AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelPath);
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
            inputStream.close();
            fileDescriptor.close();
            return buffer;
        } catch (IOException e) {
            Log.e(TAG, "Error loading model file: " + modelPath, e);
            throw new IOException("Model file not found: " + modelPath + ". Please ensure the YOLO model file is placed in src/main/assets/", e);
        }
    }

    public Interpreter getInterpreter() {
        return interpreter;
    }

    public void close() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
            Log.d(TAG, "YOLO interpreter closed");
        }
    }
}
