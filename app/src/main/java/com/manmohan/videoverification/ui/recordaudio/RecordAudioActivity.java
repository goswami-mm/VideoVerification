package com.manmohan.videoverification.ui.recordaudio;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.manmohan.videoverification.R;

import java.io.IOException;
import java.util.Random;

public class RecordAudioActivity extends AppCompatActivity {

    TextView userIdTv;
    private MediaRecorder mediaRecorder;

    Button buttonStart;
    Button buttonStop;
    Button buttonPlayLastRecordAudio;
    Button buttonStopPlayingRecording;
    String AudioSavePathInDevice = null;
    Random random;
    String RandomAudioFileName = "audioFile";

    MediaPlayer mediaPlayer ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_audio);
        userIdTv = findViewById(R.id.user_id_tv);
        userIdTv.setText(generateRandomId());

        buttonStart = findViewById(R.id.button);
        buttonStop =  findViewById(R.id.button2);
        buttonPlayLastRecordAudio = findViewById(R.id.button3);
        buttonStopPlayingRecording = findViewById(R.id.button4);

        buttonStop.setEnabled(false);
        buttonPlayLastRecordAudio.setEnabled(false);
        buttonStopPlayingRecording.setEnabled(false);

        random = new Random();

        buttonStart.setOnClickListener(view -> {
                AudioSavePathInDevice =
                        Environment.getExternalStorageDirectory().getAbsolutePath() + "/" +
                                createRandomAudioFileName(5) + "AudioRecording.3gp";

                mediaRecorderReady();

                try {
                    mediaRecorder.prepare();
                    mediaRecorder.start();
                } catch (IllegalStateException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                buttonStart.setEnabled(false);
                buttonStop.setEnabled(true);

                Toast.makeText(RecordAudioActivity.this, "Recording started",
                        Toast.LENGTH_LONG).show();

        });

        buttonStop.setOnClickListener(view -> {
            mediaRecorder.stop();
            buttonStop.setEnabled(false);
            buttonPlayLastRecordAudio.setEnabled(true);
            buttonStart.setEnabled(true);
            buttonStopPlayingRecording.setEnabled(false);

            Toast.makeText(this, "Recording Completed",
                    Toast.LENGTH_LONG).show();
        });

        buttonPlayLastRecordAudio.setOnClickListener(view -> {
            buttonStop.setEnabled(false);
            buttonStart.setEnabled(false);
            buttonStopPlayingRecording.setEnabled(true);

            mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setDataSource(AudioSavePathInDevice);
                mediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mediaPlayer.start();
            Toast.makeText(this, "Recording Playing",
                    Toast.LENGTH_LONG).show();
        });

        buttonStopPlayingRecording.setOnClickListener(view -> {
            buttonStop.setEnabled(false);
            buttonStart.setEnabled(true);
            buttonStopPlayingRecording.setEnabled(false);
            buttonPlayLastRecordAudio.setEnabled(true);

            if(mediaPlayer != null){
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaRecorderReady();
            }
        });

    }

    public void mediaRecorderReady(){
        mediaRecorder=new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        mediaRecorder.setOutputFile(AudioSavePathInDevice);
    }

    public String createRandomAudioFileName(int string){
        StringBuilder stringBuilder = new StringBuilder( string );
        int i = 0 ;
        while(i < string ) {
            stringBuilder.append(RandomAudioFileName.
                    charAt(random.nextInt(RandomAudioFileName.length())));

            i++ ;
        }
        return stringBuilder.toString();
    }

    private String generateRandomId() {
        Random random = new Random();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(random.nextInt(10));
        stringBuilder.append(random.nextInt(10));
        stringBuilder.append(random.nextInt(10));
        stringBuilder.append(random.nextInt(10));
        stringBuilder.append(random.nextInt(10));
        return stringBuilder.toString();
    }

    @Override
    public void onBackPressed() {
        if(!mediaPlayer.isPlaying()) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("audioPath", AudioSavePathInDevice);
            setResult(AudioSavePathInDevice != null? Activity.RESULT_OK : Activity.RESULT_CANCELED, resultIntent);
            finish();
        } else {
            Toast.makeText(this, "Please stop playing", Toast.LENGTH_LONG).show();
        }

    }
}
