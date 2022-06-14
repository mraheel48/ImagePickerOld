package com.example.imagepickerold.asyncWorking;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.MediaStore;

public class SupportedClass {

    public static Uri addImageToGallery(Context context, String str) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("datetaken", String.valueOf(System.currentTimeMillis()));
        contentValues.put("mime_type", "image/jpeg");
        contentValues.put("_data", str);
        return context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
    }

    public static Bitmap tfResizeBilinear(Bitmap bitmap, int i, int i2) {
        if (bitmap == null) {
            return null;
        }
        Bitmap createBitmap = Bitmap.createBitmap(i, i2, Bitmap.Config.ARGB_8888);
        new Canvas(createBitmap).drawBitmap(bitmap, new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()), new Rect(0, 0, i, i2), (Paint) null);
        return createBitmap;
    }

    public static Bitmap cropBitmapWithMask(Bitmap bitmap, Bitmap bitmap2, int i, int i2) {
        if (!(bitmap == null || bitmap2 == null)) {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            if (width > 0 && height > 0) {
                Bitmap createBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(createBitmap);
                Paint paint = new Paint(1);
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
                canvas.drawBitmap(bitmap, 0.0f, 0.0f, (Paint) null);
                canvas.drawBitmap(bitmap2, (float) i, (float) i2, paint);
                paint.setXfermode(null);
                return createBitmap;
            }
        }
        return null;
    }
}
