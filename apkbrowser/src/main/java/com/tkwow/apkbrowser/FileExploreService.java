package com.tkwow.apkbrowser;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class FileExploreService extends IntentService {
    private static final String ACTION_OPEN = "com.tkwow.apkbrowser.action.open";
    private static final String EXTRA_FILE_PATH = "com.tkwow.apkbrowser.extra.file_path";

    /**
     * Starts this service to perform action Open with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startOpen(Context context, String param1) {
        Intent intent = new Intent(context, FileExploreService.class);
        intent.setAction(ACTION_OPEN);
        intent.putExtra(EXTRA_FILE_PATH, param1);
        context.startService(intent);
    }

    public FileExploreService() {
        super("FileExploreService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_OPEN.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_FILE_PATH);
                String ret = handleActionOpen(param1);
                Log.v("Find Me", ret);
                String path = updateDatabase(ret);
                sendCallback(path);
            }
        }
    }

    /**
     * Handle action Open in the provided background thread with the provided
     * parameters.
     */
    private String handleActionOpen(String param1) {
        try {
            return downloadUrl(param1);
        } catch (IOException e) {
            return "Unable to retrieve web page. URL may be invalid.";
        }
    }

    private String downloadUrl(String myurl) throws IOException {
        InputStream is = null;
        // Only display the first 500 characters of the retrieved
        // web page content.

        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            //Log.d(DEBUG_TAG, "The response is: " + response);
            is = conn.getInputStream();

            // Convert the InputStream into a string
            String contentAsString = readIt(is);
            return contentAsString;

            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    // Reads an InputStream and converts it to a String.
    public String readIt(InputStream stream) throws IOException, UnsupportedEncodingException {
        // Read the string as a buffer
        BufferedReader r = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        StringBuilder jsonReceived = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            jsonReceived.append(line);
        }

        return jsonReceived.toString();
    }

    private void sendCallback(String path) {
        Intent localIntent = new Intent(Constants.BROADCAST_FILE_OPEN);
        localIntent.putExtra(Constants.EXTENDED_FILE_OPEN_STATUS, path != null);
        localIntent.putExtra(Constants.EXTENDED_FILE_OPEN_FILE_PATH, path);
        // Broadcasts the Intent to receivers in this app.
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }
    private String updateDatabase(String data) {
        ArrayList<FileEntity> al = new ArrayList<FileEntity>();

        try {
            JSONObject jo = new JSONObject(data);
            Object root = jo.get("fileInfo");
            if (JSONArray.class.isInstance(root)) {
                JSONArray jsa = jo.getJSONArray("fileInfo");
                for (int i = 0; i < jsa.length(); i++) {
                    JSONObject jFile = (JSONObject) jsa.get(i);
                    FileEntity entity = new FileEntity();
                    entity.fileName = jFile.getString("fileName");
                    entity.filePath = jFile.getString("filePath");
                    entity.isDirectory = jFile.getInt("isDirectory");
                    entity.downloadID = 0L;
                    entity.downloadStatus = 0;
                    al.add(entity);
                }
            } else {
                JSONObject jFile = (JSONObject)root;
                FileEntity entity = new FileEntity();
                entity.fileName = jFile.getString("fileName");
                entity.filePath = jFile.getString("filePath");
                entity.isDirectory = jFile.getInt("isDirectory");
                entity.downloadID = 0L;
                entity.downloadStatus = 0;
                al.add(entity);
            }
        } catch(JSONException e) {
            Log.d("Parse Json", e.toString());
        }
        FileList.getInstance(getApplicationContext()).add(al);
        if (al.isEmpty()) {
            return null;
        }
        return al.get(0).filePath;
    }
}
