package com.manmohan.videoverification.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.manmohan.videoverification.R;
import com.manmohan.videoverification.ui.captureimage.CaptureImageActivity;
import com.manmohan.videoverification.ui.recordvideo.RecordVideoActivity;

import java.util.ArrayList;

import static com.manmohan.videoverification.utils.StorageUtils.storeImage;
import static com.manmohan.videoverification.utils.StorageUtils.takeSnapShot;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST = 1;
    private static final int RECORD_VIDEO_CODE = 1002;
    private static final int CROP_IMAGE_CODE = 1007;


    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    private static final String PERMISSION_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private static final String PERMISSION_AUDIO = Manifest.permission.RECORD_AUDIO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.record_video_bt).setOnClickListener(v ->
                openRecordVideoScreen());
        findViewById(R.id.take_pic_bt).setOnClickListener(v ->
                openCaptureImageScreen());

        if (!hasPermission()) {
            requestPermission();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            final int requestCode, final String[] permissions, final int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED
                    && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
            } else {
                requestPermission();
            }
        }
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(PERMISSION_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(PERMISSION_AUDIO) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA) ||
                    shouldShowRequestPermissionRationale(PERMISSION_AUDIO) ||
                    shouldShowRequestPermissionRationale(PERMISSION_STORAGE)) {
                Toast.makeText(this,
                        "Permission are required for this demo", Toast.LENGTH_LONG).show();
            }
            requestPermissions(new String[] {PERMISSION_CAMERA, PERMISSION_STORAGE, PERMISSION_AUDIO}, PERMISSIONS_REQUEST);
        }
    }

    private void openRecordVideoScreen() {
        startActivityForResult(new Intent(this, RecordVideoActivity.class), RECORD_VIDEO_CODE);
    }


    private void openCaptureImageScreen() {
        startActivityForResult(new Intent(this, CaptureImageActivity.class), CROP_IMAGE_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK && requestCode == RECORD_VIDEO_CODE) {
            ArrayList<String> bitmapURIs = data.getStringArrayListExtra("snapshots");
            if(bitmapURIs!=null) {
                Log.e("MainActivity", "get Uris of size : " + bitmapURIs.size());
            } else {
                Toast.makeText(this, "Error occured", Toast.LENGTH_LONG).show();
            }
        }else if (resultCode == RESULT_OK && requestCode == CROP_IMAGE_CODE) {
            Uri imageUri = data.getData();
            if(imageUri!=null) {
                Log.e("MainActivity", "File at : " + imageUri.toString());
            } else {
                Toast.makeText(this, "Error occured", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "operation cancel", Toast.LENGTH_LONG).show();
        }
    }
}
