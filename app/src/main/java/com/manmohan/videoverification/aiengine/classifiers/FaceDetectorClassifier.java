/* Copyright 2015 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package com.manmohan.videoverification.aiengine.classifiers;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;

import androidx.core.util.Pair;


import com.manmohan.videoverification.aiengine.Classifier;
import com.manmohan.videoverification.aiengine.wrapper.MTCNN;

import java.util.LinkedList;
import java.util.List;

/**
 * Generic interface for interacting with different recognition engines.
 */
public class FaceDetectorClassifier implements Classifier {

    private static final int FACE_SIZE = 160;
    public static final int EMBEDDING_SIZE = 512;
    private static FaceDetectorClassifier classifier;

    private MTCNN mtcnn;

    private FaceDetectorClassifier() {}

    public static FaceDetectorClassifier getInstance(AssetManager assetManager) throws Exception {
        return getInstance(assetManager, FACE_SIZE, FACE_SIZE);
    }

    public static FaceDetectorClassifier getInstance(AssetManager assetManager,
                                                     int inputHeight,
                                                     int inputWidth) throws Exception {
        if (classifier != null) return classifier;

        classifier = new FaceDetectorClassifier();

        classifier.mtcnn = MTCNN.create(assetManager);

        return classifier;
    }

    public List<Recognition> recognizeImage(Bitmap bitmap, Matrix matrix) {
        synchronized (this) {
            Pair faces[] = mtcnn.detect(bitmap);

            final List<Recognition> mappedRecognitions = new LinkedList<>();

            for (Pair face : faces) {
                RectF rectF = (RectF) face.first;

                Rect rect = new Rect();
                rectF.round(rect);

                matrix.mapRect(rectF);

                Recognition result =
                        new Recognition("", "", 1F, rectF);
                mappedRecognitions.add(result);
            }
            return mappedRecognitions;
        }

    }

    public void enableStatLogging(final boolean debug){
    }

    public String getStatString() {
        return mtcnn.getStatString();
    }

    public void close() {
        mtcnn.close();
    }
}
