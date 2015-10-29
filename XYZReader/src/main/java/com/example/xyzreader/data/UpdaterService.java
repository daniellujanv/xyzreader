package com.example.xyzreader.data;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.RemoteException;
import android.text.format.Time;
import android.util.Log;

import com.example.xyzreader.remote.RemoteEndpointUtil;
import com.squareup.okhttp.OkHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

public class UpdaterService extends IntentService {
    private static final String TAG = "UpdaterService";

    public UpdaterService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.w(TAG, "Handling Sync Intent!!.");

        Time time = new Time();

        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null || !ni.isConnected()) {
            Log.w(TAG, "Not online, not refreshing.");
            return;
        }
        // Don't even inspect the intent, we only do one thing, and that's fetch content.
        ArrayList<ContentProviderOperation> cpo = new ArrayList<ContentProviderOperation>();

        Uri dirUri = ItemsContract.Items.buildDirUri();

        // Delete all items
        cpo.add(ContentProviderOperation.newDelete(dirUri).build());

        try {
            JSONArray array = RemoteEndpointUtil.fetchJsonArray();
            if (array == null) {
                throw new JSONException("Invalid parsed item array" );
            }

            for (int i = 0; i < array.length(); i++) {


                ContentValues values = new ContentValues();
                JSONObject object = array.getJSONObject(i);

                String[] uris = saveBitmaps(
                        object.getString("id"), object.getString("thumb"), object.getString("photo"));
//                uris = new String[]{"testing", "testing"};

                values.put(ItemsContract.Items.SERVER_ID, object.getString("id"));
                values.put(ItemsContract.Items.AUTHOR, object.getString("author"));
                values.put(ItemsContract.Items.TITLE, object.getString("title"));
                values.put(ItemsContract.Items.BODY, object.getString("body"));
                values.put(ItemsContract.Items.THUMB_URL, object.getString("thumb"));
                values.put(ItemsContract.Items.THUMB_URI, uris[0]);
                values.put(ItemsContract.Items.PHOTO_URL, object.getString("photo"));
                values.put(ItemsContract.Items.PHOTO_URI, uris[1]);
                values.put(ItemsContract.Items.ASPECT_RATIO, object.getString("aspect_ratio"));
                time.parse3339(object.getString("published_date"));
                values.put(ItemsContract.Items.PUBLISHED_DATE, time.toMillis(false));
                cpo.add(ContentProviderOperation.newInsert(dirUri).withValues(values).build());
            }

            getContentResolver().applyBatch(ItemsContract.CONTENT_AUTHORITY, cpo);

        } catch (JSONException | RemoteException | OperationApplicationException e) {
            Log.e(TAG, "Error updating content.", e);
        }
    }

    private String[] saveBitmaps(String id, String thumb_url, String photo_url){
        String[] result =  new String[2];
        result[0] = "error";
        result[1] = "error";

        try {
            /********** THUMB ******************/
            /** get image**/
            InputStream isThumb = null;
            try {
                OkHttpClient client = new OkHttpClient();
                HttpURLConnection okConn = client.open(new URL(thumb_url));

                isThumb = okConn.getInputStream();
//                int response = okConn.getResponseCode();
//                Log.d("DEBUG TAAAAG", "The response THUMB is: " + response);
                /*** save to file**/
                //convert IS to Bitmap
                Bitmap bitmapThumb = BitmapFactory.decodeStream(isThumb);
                String thumbFileName = "thumb-" + id;
                File thumbFileDir = getApplicationContext().getFilesDir();
                if (thumbFileDir.mkdirs() || thumbFileDir.exists()) {
                    File thumbFile = new File(thumbFileDir, thumbFileName);
                    FileOutputStream fosThumb = new FileOutputStream(thumbFile);
                    bitmapThumb.compress(Bitmap.CompressFormat.PNG, 90, fosThumb);
                    fosThumb.close();
                    isThumb.close();
                    result[0] = thumbFile.getPath();
                } else {
                    Log.e(TAG, "Error creating thumb file ::" + thumbFileName);
                }
            } finally {
                if (isThumb != null) {
                    isThumb.close();
                }
            }
        }catch(IOException e) {
            e.printStackTrace();
        }

        /**********  PHOTO  ******************/
        /** get image**/
        try{
            InputStream isPhoto = null;
            try {
                OkHttpClient client = new OkHttpClient();
                HttpURLConnection okConn = client.open(new URL(photo_url));

                isPhoto = okConn.getInputStream();
//                int response = okConn.getResponseCode();
//                Log.d("DEBUG TAAAAG", "The response THUMB is: " + response);
                /*** save to file**/
                //convert IS to Bitmap
                Bitmap bitmapPhoto = BitmapFactory.decodeStream(isPhoto);
                String photoFileName = "photo-" + id;
                File photoFileDir = getApplicationContext().getFilesDir();
                if (photoFileDir.mkdirs() || photoFileDir.exists()){
                    File photoFile = new File(photoFileDir, photoFileName);
                    FileOutputStream fosPhoto = new FileOutputStream(photoFile);
                    bitmapPhoto.compress(Bitmap.CompressFormat.PNG, 90, fosPhoto);
                    fosPhoto.close();
                    // Makes sure that the InputStream is closed after the app is
                    // finished using it.
                    fosPhoto.close();
                    isPhoto.close();
                    result[1] = photoFile.getPath();
                }else {
                    Log.e(TAG, "Error creating Photo file ::" + photoFileName);
                }
            } finally {
                if (isPhoto != null) {
                    isPhoto.close();
                }
            }
        }catch(IOException e) {
            e.printStackTrace();
        }
        return result;
    }
}
