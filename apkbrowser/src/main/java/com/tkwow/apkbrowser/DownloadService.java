package com.tkwow.apkbrowser;

import android.app.DownloadManager;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class DownloadService extends IntentService {
    private static final String ACTION_DOWNLOAD = "com.tkwow.apkbrowser.action.download";
    private static final String EXTRA_FILE_PATH_EXTERNAL = "com.tkwow.apkbrowser.extra.file_path_external";
    private static final String EXTRA_FILE_PATH_INTERNAL = "com.tkwow.apkbrowser.extra.file_path_internal";

    private long mDownloadID = 0;

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startDownload(Context context, String externalPath, String internalPath) {
        Intent intent = new Intent(context, DownloadService.class);
        intent.setAction(ACTION_DOWNLOAD);
        intent.putExtra(EXTRA_FILE_PATH_EXTERNAL, externalPath);
        intent.putExtra(EXTRA_FILE_PATH_INTERNAL, internalPath);
        context.startService(intent);
    }


    public DownloadService() {
        super("DownloadService");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_DOWNLOAD.equals(action)) {
                String externalPath = intent.getStringExtra(EXTRA_FILE_PATH_EXTERNAL);
                int lastSeparator = externalPath.lastIndexOf('&')+1;
                String fileName = externalPath.substring(lastSeparator, externalPath.length());
                mDownloadID = handleActionDownload(externalPath);
                String internalPath = intent.getStringExtra(EXTRA_FILE_PATH_INTERNAL);
                FileEntity entity = FileList.getInstance(getApplicationContext()).query(internalPath, fileName);
                if (entity != null) {
                    entity.downloadStatus = 1;
                    entity.downloadID = mDownloadID;
                    FileList.getInstance(getApplicationContext()).update(entity);
                    sendCallback(mDownloadID);
                }
            }
        }
    }

    /**
     * Handle action Open in the provided background thread with the provided
     * parameters.
     */
    private long handleActionDownload(String param1) {
        try {
            return downloadUrl(param1);
        } catch (IOException e) {
            return 0;
        }
    }

    private void sendCallback(long downloadID) {
        Intent localIntent = new Intent(Constants.BROADCAST_FILE_DOWNLOAD);
        localIntent.putExtra(Constants.EXTENDED_FILE_DOWNLOAD_ID, downloadID);

        // Broadcasts the Intent to receivers in this app.
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    public static boolean isSDCardAvailable() {
        return android.os.Environment.getExternalStorageState().equals
                (android.os.Environment.MEDIA_MOUNTED);
    }
    private long downloadUrl(String signedURL) throws IOException {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(signedURL));

        int index = signedURL.lastIndexOf("&");
        String fileName = signedURL.substring(index+1, signedURL.length());
        // Set the notification's title to be the file name.
        request.setTitle("Download "+fileName);

        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        String mimeString = mimeTypeMap.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(fileName));
        request.setMimeType(mimeString);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        request.setVisibleInDownloadsUi(true);
        // Check if SD card available. If so save download file in SD card. By default
        // downloads are saved in the shared download cache and may be deleted by the system at
        // any time to reclaim space.
        // We save the file
        if (isSDCardAvailable()) {
            if (fileName != null) {

                // Get the file extension
                String ext = "";
                if (fileName.contains(".")) {
                    ext = fileName.substring(fileName.lastIndexOf("."));
                }

                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            }
        }

        DownloadManager downloadManager = (DownloadManager) getApplication().
                getSystemService(Context.DOWNLOAD_SERVICE);
        return downloadManager.enqueue(request);
    }

    public static Cursor queryDownloadById(Context context, long downloadId) {
        DownloadManager downloadManager = (DownloadManager) context.getSystemService
                (Context.DOWNLOAD_SERVICE);

        DownloadManager.Query baseQuery = new DownloadManager.Query();
        baseQuery.setFilterById(downloadId);
        return downloadManager.query(baseQuery);
    }

    /**
     * Returns download status (int) as defined in DownloadManager.
     *
     * @param cursor holding wanted download.
     * @return int represents download status as defined in DownloadManager
     */
    public static int getDownloadStatus(Cursor cursor) {
        return cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
    }

    /**
     * Returns download failure reason String (if failure occurred). The reason is read as int
     * from DownloadManager and being translated to String to easier use.
     *
     * @param cursor holding wanted download.
     * @return String describing the failure reason if failure occurred.
     */
    public static String getDownloadFailReason(Cursor cursor) {
        int reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));
        return getDownloadFailReason(reason);
    }

    public static String getDownloadFailReason(int reason) {
        String reasonDescription;
        switch (reason) {
            case DownloadManager.ERROR_FILE_ERROR:
                reasonDescription = "ERROR_FILE_ERROR";
                break;
            case DownloadManager.ERROR_HTTP_DATA_ERROR:
                reasonDescription = "ERROR_HTTP_DATA_ERROR";
                break;
            case DownloadManager.ERROR_TOO_MANY_REDIRECTS:
                reasonDescription = "ERROR_TOO_MANY_REDIRECTS ";
                break;
            case DownloadManager.ERROR_INSUFFICIENT_SPACE:
                reasonDescription = "ERROR_INSUFFICIENT_SPACE";
                break;
            case DownloadManager.ERROR_DEVICE_NOT_FOUND:
                reasonDescription = "ERROR_DEVICE_NOT_FOUND ";
                break;
            case DownloadManager.ERROR_CANNOT_RESUME:
                reasonDescription = "ERROR_CANNOT_RESUME";
                break;
            case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
                reasonDescription = "ERROR_FILE_ALREADY_EXISTS";
                break;
            case DownloadManager.PAUSED_WAITING_TO_RETRY:
                reasonDescription = "PAUSED_WAITING_TO_RETRY";
                break;
            case DownloadManager.PAUSED_WAITING_FOR_NETWORK:
                reasonDescription = "PAUSED_WAITING_FOR_NETWORK";
                break;
            case DownloadManager.PAUSED_QUEUED_FOR_WIFI:
                reasonDescription = "PAUSED_QUEUED_FOR_WIFI";
                break;
            case DownloadManager.PAUSED_UNKNOWN:
                reasonDescription = "PAUSED_UNKNOWN";
                break;
            default:
                reasonDescription = "NO_REASON";
        }

        return reasonDescription;
    }
}
