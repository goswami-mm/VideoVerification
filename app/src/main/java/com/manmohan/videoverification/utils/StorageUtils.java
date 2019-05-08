package com.manmohan.videoverification.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class StorageUtils {
    private static final String TAG = "StorageUtils";

    public static long lengthOfVideo(Context context,Uri uri){
        long length = 0;
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            //use one of overloaded setDataSource() functions to set your data source
            retriever.setDataSource(context, uri);
            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            length = Long.parseLong(time);
            retriever.release();
        } catch (Exception e) {
        }
        return length;
    }

    public static Bitmap createVideoThumbnail(Context context, Uri uri, long duration) {
        MediaMetadataRetriever mediametadataretriever = new MediaMetadataRetriever();
        try {
            mediametadataretriever.setDataSource(context, uri);
            Bitmap bitmap = mediametadataretriever.getFrameAtTime(duration);
//            if(bitmap!=null) {
//                int j = 250;
//                return ThumbnailUtils.extractThumbnail(bitmap, j, j, 2);
//            }
            return bitmap;
        } catch (Throwable t) {
            Log.e(TAG, "Error : "+ t.getMessage());
            return null;
        } finally {
            try
            {
                mediametadataretriever.release();
            }
            catch(RuntimeException e) { }
        }
    }

    public static Bitmap takeSnapShot(View v) {
        int width = v.getWidth();
        int height = v.getHeight();
        Bitmap b = Bitmap.createBitmap(width , height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        v.draw(c);
        return b;
    }
    
    public static void storeImage(Context context, Bitmap image, String fileName) {
        File pictureFile = getOutputMediaFile(context, fileName);
        if (pictureFile == null) {
            Log.e(TAG,
                    "Error creating media file, check storage permissions: ");// e.getMessage());
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "Error accessing file: " + e.getMessage());
        }
    }

    public static File getOutputMediaFile(Context context, String fileName){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
                + "/Android/data/"
                + context.getApplicationContext().getPackageName()
                + "/Files");

        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                return null;
            }
        }
        // Create a media file name
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + fileName);
        return mediaFile;
    }
}
