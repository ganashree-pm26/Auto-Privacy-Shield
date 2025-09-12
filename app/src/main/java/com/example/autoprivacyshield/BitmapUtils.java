package com.example.autoprivacyshield;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextPaint;

public class BitmapUtils {

    /**
     * Converts a plain text string into a bitmap for OCR processing.
     * @param text The text to render into a bitmap
     * @return Bitmap containing the text
     */
    public static Bitmap textToBitmap(String text) {
        if (text == null || text.trim().isEmpty()) {
            text = "";
        }

        TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLACK);
        paint.setTextSize(48f);

        float padding = 20f;
        float textWidth = paint.measureText(text);
        float textHeight = paint.getFontMetrics().bottom - paint.getFontMetrics().top;

        // Create bitmap big enough for the text
        Bitmap bitmap = Bitmap.createBitmap(
                (int) (textWidth + padding * 2),
                (int) (textHeight + padding * 2),
                Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        canvas.drawText(text, padding, padding - paint.getFontMetrics().top, paint);

        return bitmap;
    }
}
