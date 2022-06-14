package com.example.imagepickerold.utils;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import com.example.imagepickerold.R;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class StoreManager {

    private static final String BITMAP_CROPED_FILE_NAME = "temp_croped_bitmap.png";
    private static final String BITMAP_CROPED_MASK_FILE_NAME = "temp_croped_mask_bitmap.png";
    private static final String BITMAP_FILE_NAME = "temp_bitmap.png";
    private static final String BITMAP_ORIGINAL_FILE_NAME = "temp_original_bitmap.png";
    public static int croppedLeft = 0;
    public static int croppedTop = 0;
    public static boolean isNull = false;

    public static Bitmap getCurrentCroppedMaskBitmap(Activity activity) {
        if (isNull) {
            return null;
        }
        return getBitmapByFileName(activity, BITMAP_CROPED_MASK_FILE_NAME);
    }

    public static Bitmap getCurrentBitmap(Activity activity) {
        return getBitmapByFileName(activity, BITMAP_FILE_NAME);
    }

    public static Bitmap getCurrentCropedBitmap(Activity activity) {
        if (isNull) {
            return null;
        }
        return getBitmapByFileName(activity, BITMAP_CROPED_FILE_NAME);
    }

    public static Bitmap getCurrentOriginalBitmap(Activity activity) {
        return getBitmapByFileName(activity, BITMAP_ORIGINAL_FILE_NAME);
    }

    private static Bitmap getBitmapByFileName(Activity activity, String str) {
        File dir = new ContextWrapper(activity).getDir(activity.getResources().getString(R.string.directory), 0);
        Log.i("PathOfFile", dir.getAbsolutePath() + "/" + str);
        return BitmapFactory.decodeFile(dir.getAbsolutePath() + "/" + str);
    }

    public static Bitmap getCurrentEffecdedBitmap(Activity activity) {
        return getCurrentBitmap(activity);
    }

    public static void saveFile(Context context, Bitmap bitmap, String str) {
        if (bitmap != null) {
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(new File(new ContextWrapper(context).getDir(context.getResources().getString(R.string.directory), 0), str));
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                fileOutputStream.flush();
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void deleteFile(Context context, String str) {
        File file = new File(context.getFilesDir().getAbsolutePath() + "/" + str);
        if (file.exists()) {
            file.delete();
        }
    }

    public static void setCurrentBitmap(Activity activity, Bitmap bitmap) {
        if (bitmap == null) {
            deleteFile(activity, BITMAP_FILE_NAME);
        }
        saveFile(activity, bitmap, BITMAP_FILE_NAME);
    }

    public static void setCurrentCropedBitmap(Activity activity, Bitmap bitmap) {
        if (bitmap == null) {
            deleteFile(activity, BITMAP_CROPED_FILE_NAME);
            isNull = true;
        } else {
            isNull = false;
        }
        saveFile(activity, bitmap, BITMAP_CROPED_FILE_NAME);
    }

    public static void setCurrentCroppedMaskBitmap(Activity activity, Bitmap bitmap) {
        if (bitmap == null) {
            deleteFile(activity, BITMAP_CROPED_MASK_FILE_NAME);
        }
        saveFile(activity, bitmap, BITMAP_CROPED_MASK_FILE_NAME);
    }

    public static void setCurrentOriginalBitmap(Activity activity, Bitmap bitmap) {
        if (bitmap == null) {
            deleteFile(activity, BITMAP_ORIGINAL_FILE_NAME);
        }
        saveFile(activity, bitmap, BITMAP_ORIGINAL_FILE_NAME);
    }
}
