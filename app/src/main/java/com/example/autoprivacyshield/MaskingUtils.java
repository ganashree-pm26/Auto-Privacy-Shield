package com.example.autoprivacyshield;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.BlurMaskFilter;

public class MaskingUtils {

    public static Bitmap blurRegions(Bitmap source, Rect[] sensitiveAreas) {
        if (source == null || sensitiveAreas == null) return source;

        Bitmap mutableBitmap = source.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        Paint paint = new Paint();
        paint.setMaskFilter(new BlurMaskFilter(15, BlurMaskFilter.Blur.NORMAL));
        paint.setAlpha(180); // semi-transparent

        for (Rect rect : sensitiveAreas) {
            canvas.drawRect(rect, paint);
        }
        return mutableBitmap;
    }
}
