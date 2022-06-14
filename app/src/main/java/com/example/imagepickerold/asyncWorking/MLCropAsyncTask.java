package com.example.imagepickerold.asyncWorking;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ProgressBar;

import com.example.imagepickerold.utils.BitmapUtils;
import com.example.imagepickerold.utils.StoreManager;

public class MLCropAsyncTask extends AsyncTask<Void, Void, Void> {
    Activity activity;
    ProgressBar crop_progress_bar;
    Bitmap cropped;
    int left;
    Bitmap mask;
    MLOnCropTaskCompleted onTaskCompleted;
    int top;

    private void show(final boolean z) {
        Activity activity2 = this.activity;
        if (activity2 != null) {
            activity2.runOnUiThread(new Runnable() {


                public void run() {
                    if (z) {
                        MLCropAsyncTask.this.crop_progress_bar.setVisibility(View.VISIBLE);
                    } else {
                        MLCropAsyncTask.this.crop_progress_bar.setVisibility(View.GONE);
                    }
                }
            });
        }
    }

    public MLCropAsyncTask(MLOnCropTaskCompleted mLOnCropTaskCompleted, Activity activity2, ProgressBar progressBar) {
        this.onTaskCompleted = mLOnCropTaskCompleted;
        this.activity = activity2;
        this.crop_progress_bar = progressBar;
    }

    public void onPreExecute() {
        super.onPreExecute();
        show(true);
    }

    public Void doInBackground(Void... voidArr) {
        this.cropped = StoreManager.getCurrentCropedBitmap(this.activity);
        this.left = StoreManager.croppedLeft;
        this.top = StoreManager.croppedTop;
        Bitmap currentCroppedMaskBitmap = StoreManager.getCurrentCroppedMaskBitmap(this.activity);
        this.mask = currentCroppedMaskBitmap;
        if (this.cropped == null && currentCroppedMaskBitmap == null) {
            DeeplabMobile deeplabMobile = new DeeplabMobile();
            deeplabMobile.initialize(this.activity.getApplicationContext());
            Bitmap loadInBackground = loadInBackground(StoreManager.getCurrentOriginalBitmap(this.activity), deeplabMobile);
            this.cropped = loadInBackground;
            StoreManager.setCurrentCropedBitmap(this.activity, loadInBackground);
        }
        return null;
    }

    public void onPostExecute(Void r5) {
        show(false);
        this.onTaskCompleted.onTaskCompleted(this.cropped, this.mask, this.left, this.top);
    }

    public Bitmap loadInBackground(Bitmap bitmap, DeeplabMobile deeplabMobile) {
        if (bitmap == null) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float max = 513.0f / ((float) Math.max(bitmap.getWidth(), bitmap.getHeight()));
        int round = Math.round(((float) width) * max);
        int round2 = Math.round(((float) height) * max);
        Bitmap segment = deeplabMobile.segment(SupportedClass.tfResizeBilinear(bitmap, round, round2));
        this.mask = segment;
        if (segment == null) {
            return null;
        }
        Bitmap createClippedBitmap = BitmapUtils.createClippedBitmap(segment, (segment.getWidth() - round) / 2, (this.mask.getHeight() - round2) / 2, round, round2);
        this.mask = createClippedBitmap;
        Bitmap scaleBitmap = BitmapUtils.scaleBitmap(createClippedBitmap, width, height);
        this.mask = scaleBitmap;
        this.left = (scaleBitmap.getWidth() - width) / 2;
        this.top = (this.mask.getHeight() - height) / 2;
        StoreManager.croppedLeft = this.left;
        int i = this.top;
        StoreManager.croppedTop = i;
        this.top = i;
        StoreManager.setCurrentCroppedMaskBitmap(this.activity, this.mask);
        return SupportedClass.cropBitmapWithMask(bitmap, this.mask, 0, 0);
    }
}