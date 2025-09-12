package com.example.autoprivacyshield;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Bitmap.Config;
import android.media.Image;

import java.nio.ByteBuffer;

public class ImageUtils {

    public static Bitmap imageToBitmap(Image image) {
        if (image.getFormat() == PixelFormat.RGBA_8888 || image.getFormat() == ImageFormat.YUV_420_888) {
            Image.Plane[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * image.getWidth();

            Bitmap bitmap = Bitmap.createBitmap(
                    image.getWidth() + rowPadding / pixelStride,
                    image.getHeight(),
                    Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);

            // Crop the bitmap to original size
            Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, image.getWidth(), image.getHeight());
            bitmap.recycle();
            return croppedBitmap;
        }
        return null;
    }
}
