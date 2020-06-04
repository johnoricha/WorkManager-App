package com.example.background.workers;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.background.R;

import java.io.FileNotFoundException;

import static com.example.background.Constants.KEY_IMAGE_URI;

public class BlurWorker extends Worker {

    private static final String TAG = "BlurWorker";
    public BlurWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        Context applicationContext = getApplicationContext();

        WorkerUtils.makeStatusNotification("Blurring image", applicationContext);
        WorkerUtils.sleep();
        String resourceUri = getInputData().getString(KEY_IMAGE_URI);

        try {
            // REPLACE THIS CODE:
            // Bitmap picture = BitmapFactory.decodeResource(
            //        applicationContext.getResources(),
            //        R.drawable.test);
            // WITH
            if (TextUtils.isEmpty(resourceUri)) {
                Log.e(TAG, "invalid input uri");
                throw new IllegalStateException("invalid input Uri");
            }

            ContentResolver resolver = applicationContext.getContentResolver();

            //create a bitmap
            Bitmap bitmap = BitmapFactory.decodeStream(resolver.openInputStream(Uri.parse(resourceUri)));

            // Blur the bitmap
            Bitmap output = WorkerUtils.blurBitmap(bitmap, applicationContext);

            // Write bitmap to a temp file
            Uri outputUri = WorkerUtils.writeBitmapToFile(applicationContext, output);

            Data outputData = new Data.Builder().putString(KEY_IMAGE_URI, outputUri.toString()).build();

            // If there were no errors, return SUCCESS
            return Result.success(outputData);
        } catch (Throwable throwable) {
            // Technically WorkManager will return Result.failure()
            // but it's best to be explicit about it.
            // Thus if there were errors, we're return FAILURE
            Log.e(TAG, "error applying blur", throwable);
            return Result.failure();
        }
    }
}
