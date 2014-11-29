package com.tkwow.apkbrowser;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class DownloadProgressPopup {
    private PopupWindow mPopup = null;
    private ProgressBar mPB = null;
    private TextView mProgress;
    private Context mContext;
    private Timer mTimer;
    private Cursor mCursor;
    private IDownloadProgressHandler mHandler;
    private boolean mIsShown;
    public void setHandler(IDownloadProgressHandler handler) {mHandler = handler;}
    public void show(Context context, View anchor, final long downloadID, IDownloadProgressHandler handler) {
        if (mPopup != null) mPopup = null;
        mIsShown = true;
        mContext = context;
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View contentView = inflater.inflate(R.layout.download_progress, null);

        mPB = (ProgressBar) contentView.findViewById(R.id.download_progressBar);
        mProgress = (TextView)contentView.findViewById(R.id.download_progress);
        mPopup = new PopupWindow(contentView, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT);
        mPopup.setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    dismiss();
                    return true;
                }
                return false;
            }
        });
        mPopup.setOutsideTouchable(true);
        mPopup.setFocusable(false);
        mPopup.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.background_blank));
        mPopup.showAtLocation(anchor, Gravity.CENTER, 0, 0);

        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                updateProgress(downloadID);
            }
        }, 0, 300);

        mHandler = handler;
    }

    public void dismiss() {
        if (mPopup != null) {
            mTimer.cancel();
            mTimer = null;
            mPopup.dismiss();
            mPopup = null;
            mIsShown = false;
            if (mHandler != null) {
                mHandler.onProgressDismiss();
            }
        }
    }

    public boolean isShown() {return mIsShown;}
    private Cursor queryDownloadById(Context context, long downloadId) {
        DownloadManager downloadManager = (DownloadManager) context.getSystemService
                (Context.DOWNLOAD_SERVICE);

        DownloadManager.Query baseQuery = new DownloadManager.Query();
        baseQuery.setFilterById(downloadId);
        return downloadManager.query(baseQuery);
    }
    private void updateProgress(long downloadID) {
        mCursor = queryDownloadById(mContext,downloadID);
        mCursor.moveToFirst();
        int bytes_downloaded = mCursor.getInt(mCursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
        int bytes_total = mCursor.getInt(mCursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

        mPB.setProgress(Math.round(bytes_downloaded * 100.0f / bytes_total));
        final String progress = String.format("%d%%", mPB.getProgress());

        ((Activity)mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgress.setText(progress);
            }
        });

        if (bytes_downloaded == bytes_total) {

            ((Activity)mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mHandler.onDownloadProgressComplete("");
                }
            });
        }

        mCursor.close();
    }

    public interface IDownloadProgressHandler {
        public void onDownloadProgressComplete(String localPath);
        public void onProgressDismiss();
    }
}
