/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.background;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkContinuation;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import android.app.Application;
import android.net.Uri;
import android.text.TextUtils;

import com.example.background.workers.BlurWorker;
import com.example.background.workers.CleanUpWorker;
import com.example.background.workers.SaveImageToFileWorker;

import java.util.List;

import static com.example.background.Constants.IMAGE_MANIPULATION_WORK_NAME;
import static com.example.background.Constants.KEY_IMAGE_URI;
import static com.example.background.Constants.TAG_OUTPUT;

public class BlurViewModel extends AndroidViewModel {

    private Uri mImageUri;
    private WorkManager mWorkManager;
    private Uri mFinalUri;

    private LiveData<List<WorkInfo>> mSavedWorkInfo;

    public BlurViewModel(@NonNull Application application) {

        super(application);
        mWorkManager = WorkManager.getInstance(application);
        mSavedWorkInfo = mWorkManager.getWorkInfosByTagLiveData(TAG_OUTPUT);
    }

    /**
     * Create the WorkRequest to apply the blur and save the resulting image
     * @param blurLevel The amount to blur the image
     */
    void applyBlur(int blurLevel) {

        // Add WorkRequest to Cleanup temporary images
//        WorkContinuation continuation = mWorkManager.beginWith(OneTimeWorkRequest.from(CleanUpWorker.class));

        WorkContinuation continuation = mWorkManager.beginUniqueWork(IMAGE_MANIPULATION_WORK_NAME,
                ExistingWorkPolicy.REPLACE, OneTimeWorkRequest.from(CleanUpWorker.class));

        // Add WorkRequests to blur the image the number of times requested
        for (int i = 0; i < blurLevel; i++) {
            OneTimeWorkRequest.Builder blurBuilder =
                    new OneTimeWorkRequest.Builder(BlurWorker.class);

            // Input the Uri if this is the first blur operation
            // After the first blur operation the input will be the output of previous
            // blur operations.
            if ( i == 0 ) {
                blurBuilder.setInputData(createInputDataForUri());
            }

            continuation = continuation.then(blurBuilder.build());
        }

//        Constraints chargingconstraints = new Constraints.Builder().setRequiresCharging(true).build();
        Constraints storageConstraints = new Constraints.Builder().setRequiresStorageNotLow(true).build();

        // Add WorkRequest to save the image to the filesystem
        OneTimeWorkRequest save = new OneTimeWorkRequest.Builder(SaveImageToFileWorker.class)
//                .setConstraints(chargingconstraints)
                .setConstraints(storageConstraints)
                .addTag(TAG_OUTPUT)
                .build();
        continuation = continuation.then(save);

        // Actually start the work
        continuation.enqueue();

//        WorkContinuation continuation = mWorkManager
//                .beginWith(OneTimeWorkRequest.from(CleanUpWorker.class));
//
//
//        for (int i = 0; i<blurLevel; i++) {
//            OneTimeWorkRequest.Builder blurBuilder =
//                    new OneTimeWorkRequest.Builder(BlurWorker.class);
//
//            if (i == 0) {
//                blurBuilder.setInputData(createInputDataForUri());
//            }
//
//            continuation = continuation.then(blurBuilder.build());
//        }
//
//        OneTimeWorkRequest saveRequest =
//                new OneTimeWorkRequest.Builder(SaveImageToFileWorker.class)
//                .build();
//        continuation = continuation.then(saveRequest);
//
//        continuation.enqueue();

    }

    private Uri uriOrNull(String uriString) {
        if (!TextUtils.isEmpty(uriString)) {
            return Uri.parse(uriString);
        }
        return null;
    }

    /**
     * Setters
     */
    void setImageUri(String uri) {
        mImageUri = uriOrNull(uri);
    }

    /**
     * Getters
     */
    Uri getImageUri() {
        return mImageUri;
    }

    private Data createInputDataForUri() {
        Data.Builder builder = new Data.Builder();
        if (mImageUri != null) {
            builder.putString(KEY_IMAGE_URI,mImageUri.toString());
        }
        return builder.build();
    }
    public LiveData<List<WorkInfo>> getOutputWorkInfo() {
        return mSavedWorkInfo;
    }

    public Uri getFinalUri() {
        return mFinalUri;
    }

    public void setFinalUri(String finalUri) {
        this.mFinalUri = Uri.parse(finalUri);
    }

    public void cancelWork() {
        mWorkManager.cancelUniqueWork(IMAGE_MANIPULATION_WORK_NAME);
    }

}