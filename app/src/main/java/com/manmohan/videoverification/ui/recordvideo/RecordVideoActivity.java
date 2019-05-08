package com.manmohan.videoverification.ui.recordvideo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.manmohan.videoverification.R;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.manmohan.videoverification.utils.StorageUtils.createVideoThumbnail;
import static com.manmohan.videoverification.utils.StorageUtils.lengthOfVideo;
import static com.manmohan.videoverification.utils.StorageUtils.storeImage;
import static com.manmohan.videoverification.utils.StorageUtils.takeSnapShot;

public class RecordVideoActivity extends AppCompatActivity {

    private static final int VIDEO_CAPTURED = 1001;
    private static final long MIN_VIDEO_LENGTH = 6000;
    private static final String TAG = "RecordVideoActivity";

    private VideoView videoView;

    private Uri videoFileUri;
    private Disposable disposableVideo;
    private ArrayList<String> bitmaps = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_video);

        videoView = findViewById(R.id.video_view);

        findViewById(R.id.record_video_bt)
                .setOnClickListener(v->{
                    openExternalCamera();
                });

        Log.e("mainActivity", "started video activity");

    }

    private void openExternalCamera() {
        Intent captureVideoIntent = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
        startActivityForResult(captureVideoIntent,VIDEO_CAPTURED);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK && requestCode == VIDEO_CAPTURED) {
            videoFileUri = data.getData();
            if(lengthOfVideo(this, videoFileUri)> MIN_VIDEO_LENGTH) {
                playVideo();
            } else {
                Toast.makeText(this,
                        "Please record video min lenght of 5 Secs", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void playVideo() {
        videoView.stopPlayback();
        videoView.setVideoURI(videoFileUri);
        videoView.setOnPreparedListener(mp -> {
            videoView.start();
            startSnaping();
        });
    }

    private void startSnaping() {
        disposableVideo = Observable.interval(500,
                1000, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(t -> {
                    if(bitmaps.size() < 5) {
                        String fileName = "Snap_"+(bitmaps.size()+1) +".png";
                        storeImage(this, createVideoThumbnail(this, videoFileUri, t * bitmaps.size()), fileName);
                        bitmaps.add(fileName);
                    }
                    }, error -> Log.e(TAG, "timer error : " + error));
    }

    @Override
    public void onBackPressed() {
        if(!videoView.isPlaying()) {
            Intent resultIntent = new Intent();
            resultIntent.putStringArrayListExtra("snapshots", bitmaps);
            setResult(bitmaps.size() == 5 ? Activity.RESULT_OK : Activity.RESULT_CANCELED, resultIntent);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(disposableVideo!=null)
            disposableVideo.dispose();
    }
}
