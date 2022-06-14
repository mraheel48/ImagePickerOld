package com.example.imagepickerold.asyncWorking;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.core.view.ViewCompat;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.File;

public class DeeplabMobile implements DeeplabInterface {

    private static final String INPUT_NAME = "ImageTensor";
    public static final int INPUT_SIZE = 513;
    private static final String MODEL_FILE = "file:///android_asset/aa.pb";
    private static final String OUTPUT_NAME = "SemanticPredictions";
    private volatile TensorFlowInferenceInterface sTFInterface = null;


    @Override
    public int getInputSize() {
        return 513;
    }

    @Override
    public void initialize(Context context) {
        new File(MODEL_FILE);
        sTFInterface = new TensorFlowInferenceInterface(context.getAssets(), MODEL_FILE);
    }

    @Override
    public boolean isInitialized() {
        return sTFInterface != null;
    }

    @Override
    public Bitmap segment(Bitmap bitmap) {
        Bitmap bitmap2 = null;
        if (!(this.sTFInterface == null || bitmap == null)) {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            if (width <= 513 && height <= 513) {
                int i = width * height;
                int[] iArr = new int[i];
                byte[] bArr = new byte[(i * 3)];
                int[] iArr2 = new int[i];
                bitmap.getPixels(iArr, 0, width, 0, 0, width, height);
                for (int i2 = 0; i2 < i; i2++) {
                    int i3 = iArr[i2];
                    int i4 = i2 * 3;
                    bArr[i4] = (byte) ((i3 >> 16) & 255);
                    bArr[i4 + 1] = (byte) ((i3 >> 8) & 255);
                    bArr[i4 + 2] = (byte) (i3 & 255);
                }
                System.currentTimeMillis();
                sTFInterface.feed(INPUT_NAME, bArr, 1, height, width, 3);
                sTFInterface.run(new String[]{OUTPUT_NAME}, true);
                sTFInterface.fetch(OUTPUT_NAME, iArr2);
                System.currentTimeMillis();
                bitmap2 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                for (int i5 = 0; i5 < height; i5++) {
                    for (int i6 = 0; i6 < width; i6++) {
                        bitmap2.setPixel(i6, i5, iArr2[(i5 * width) + i6] == 0 ? 0 : ViewCompat.MEASURED_STATE_MASK);
                    }
                }
            }
        }
        return bitmap2;
    }

}
