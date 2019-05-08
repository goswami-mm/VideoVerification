package com.manmohan.videoverification.ui.captureimage;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Size;
import android.util.TypedValue;
import android.view.TextureView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;
import com.manmohan.videoverification.R;
import com.manmohan.videoverification.aiengine.Classifier;
import com.manmohan.videoverification.aiengine.classifiers.FaceDetectorClassifier;
import com.manmohan.videoverification.aiengine.env.BorderedText;
import com.manmohan.videoverification.aiengine.env.FileUtils;
import com.manmohan.videoverification.aiengine.env.ImageUtils;
import com.manmohan.videoverification.aiengine.env.Logger;
import com.manmohan.videoverification.aiengine.env.tracking.MultiBoxTracker;
import com.manmohan.videoverification.ui.ext.CameraActivity;
import com.manmohan.videoverification.ui.ext.OverlayView;
import com.manmohan.videoverification.utils.StorageUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import static com.manmohan.videoverification.utils.StorageUtils.lengthOfVideo;

public class CaptureImageActivity extends CameraActivity implements ImageReader.OnImageAvailableListener {
    private static final Logger LOGGER = new Logger();

    private static final int FACE_SIZE = 160;
    private static final int CROP_SIZE = 300;

    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);

    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;
    private static final int REQUEST_CROP_IMAGE = 1005;

    private Integer sensorOrientation;

    private FaceDetectorClassifier faceClassifier;

    private long lastProcessingTimeMs;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;

    private boolean computingDetection = false;

    private long timestamp = 0;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private MultiBoxTracker tracker;

    private byte[] luminanceCopy;

    private BorderedText borderedText;

    private Snackbar initSnackbar;
    private OverlayView trackingOverlay;
    private Button takePic;

    private boolean initialized = false;
    private boolean training = false;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout container = findViewById(R.id.container);
        initSnackbar = Snackbar.make(container, "Initializing...", Snackbar.LENGTH_INDEFINITE);
    }

    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        if (!initialized)
            new Thread(this::init).start();

        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);
        takePic = findViewById(R.id.take_pic_bt);
        takePic.setOnClickListener(v -> takePicture());

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation();
        LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(CROP_SIZE, CROP_SIZE, Bitmap.Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        CROP_SIZE, CROP_SIZE,
                        sensorOrientation, false);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        trackingOverlay = findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(
                canvas -> {
                    tracker.draw(canvas);
                    if (isDebug()) {
                        tracker.drawDebug(canvas);
                    }
                });

        addCallback(
                canvas -> {
                    if (!isDebug()) {
                        return;
                    }
                    final Bitmap copy = cropCopyBitmap;
                    if (copy == null) {
                        return;
                    }

                    final int backgroundColor = Color.argb(100, 0, 0, 0);
                    canvas.drawColor(backgroundColor);

                    final Matrix matrix = new Matrix();
                    final float scaleFactor = 2;
                    matrix.postScale(scaleFactor, scaleFactor);
                    matrix.postTranslate(
                            canvas.getWidth() - copy.getWidth() * scaleFactor,
                            canvas.getHeight() - copy.getHeight() * scaleFactor);
                    canvas.drawBitmap(copy, matrix, new Paint());

                    final Vector<String> lines = new Vector<String>();
                    if (faceClassifier != null) {
                        final String statString = faceClassifier.getStatString();
                        final String[] statLines = statString.split("\n");
                        Collections.addAll(lines, statLines);
                    }
                    lines.add("");
                    lines.add("Frame: " + previewWidth + "x" + previewHeight);
                    lines.add("Crop: " + copy.getWidth() + "x" + copy.getHeight());
                    lines.add("View: " + canvas.getWidth() + "x" + canvas.getHeight());
                    lines.add("Rotation: " + sensorOrientation);
                    lines.add("Inference time: " + lastProcessingTimeMs + "ms");

                    borderedText.drawLines(canvas, 10, canvas.getHeight() - 10, lines);
                });
    }

    private void takePicture() {
        LOGGER.e("Take pic", "taking pic");
        imageCropFunction();
    }

    public void imageCropFunction() {
        // Image Crop Code
        try {
            Intent cropIntent = new Intent("com.android.camera.action.CROP");

            cropIntent.setDataAndType(getImageUri(this, ((TextureView)findViewById(R.id.texture)).getBitmap()), "image/*");

            cropIntent.putExtra("crop", "true");
            cropIntent.putExtra("outputX", 180);
            cropIntent.putExtra("outputY", 180);
            cropIntent.putExtra("aspectX", 3);
            cropIntent.putExtra("aspectY", 4);
            cropIntent.putExtra("scaleUpIfNeeded", true);
            cropIntent.putExtra("return-data", true);

            startActivityForResult(cropIntent, REQUEST_CROP_IMAGE);

        } catch (ActivityNotFoundException e) {
            e.fillInStackTrace();
            LOGGER.e("CropActivity", e.getMessage());
        }
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }


    void init() {
        runOnUiThread(()-> initSnackbar.show());
        File dir = new File(FileUtils.ROOT);

        if (!dir.isDirectory()) {
            if (dir.exists()) dir.delete();
            dir.mkdirs();

            AssetManager mgr = getAssets();
            FileUtils.copyAsset(mgr, FileUtils.DATA_FILE);
            FileUtils.copyAsset(mgr, FileUtils.MODEL_FILE);
            FileUtils.copyAsset(mgr, FileUtils.LABEL_FILE);
        }

        try {
            faceClassifier = FaceDetectorClassifier.getInstance(getAssets(), FACE_SIZE, FACE_SIZE);
        } catch (Exception e) {
            LOGGER.e("Exception initializing classifier!", e);
            finish();
        }

        runOnUiThread(()-> initSnackbar.dismiss());
        initialized = true;
    }

    @Override
    protected void processImage() {
        ++timestamp;
        final long currTimestamp = timestamp;
        byte[] originalLuminance = getLuminance();
        tracker.onFrame(
                previewWidth,
                previewHeight,
                getLuminanceStride(),
                sensorOrientation,
                originalLuminance,
                timestamp);
        trackingOverlay.postInvalidate();

        // No mutex needed as this method is not reentrant.
        if (computingDetection || !initialized || training) {
            readyForNextImage();
            return;
        }
        computingDetection = true;
        LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        if (luminanceCopy == null) {
            luminanceCopy = new byte[originalLuminance.length];
        }
        System.arraycopy(originalLuminance, 0, luminanceCopy, 0, originalLuminance.length);
        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }

        runInBackground(
                () -> {
                    LOGGER.i("Running detection on image " + currTimestamp);
                    final long startTime = SystemClock.uptimeMillis();

                    cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                    List<Classifier.Recognition> mappedRecognitions =
                            faceClassifier.recognizeImage(croppedBitmap,cropToFrameTransform);

                    lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
                    tracker.trackResults(mappedRecognitions, luminanceCopy, currTimestamp);
                    trackingOverlay.postInvalidate();
                    requestRender();
                    computingDetection = false;
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK && requestCode == REQUEST_CROP_IMAGE) {
            Uri imageUri = data.getData();
            if(imageUri!=null) {
                returnBackImage(imageUri);
            } else {
                Toast.makeText(this,
                        "Operation cancel", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this,
                    "Operation cancel", Toast.LENGTH_LONG).show();
        }
    }

    private void returnBackImage(Uri imageUri) {
        Intent resultIntent = new Intent();
        resultIntent.setData(imageUri);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.camera_connection_fragment_tracking;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }
}
