package com.tkwow.apkbrowser;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/**
 * Created by Tank on 8/11/14.
 */
public class FileListAdapter extends SimpleCursorAdapter{
    private IDownloadViewClickHandler mHandler;
    public FileListAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
    }

    public void setClickHandler(IDownloadViewClickHandler handler) {
        mHandler = handler;
    }

    @Override
    public Object getItem(int position) {
        FileEntity file = new FileEntity();
        Cursor c = getCursor();
        c.moveToPosition(position);
        file.fileName = c.getString(c.getColumnIndex(FileDataBase.FILE_NAME));
        file.filePath = c.getString(c.getColumnIndex(FileDataBase.FILE_PATH));
        file.isDirectory = c.getInt(c.getColumnIndex(FileDataBase.IS_DIRECTORY));
        file.downloadStatus = c.getInt(c.getColumnIndex(FileDataBase.DOWNLOAD_STATUS));
        file.downloadID = c.getLong(c.getColumnIndex(FileDataBase.DOWNLOAD_ID));
        return file;
    }

    @Override
    public void bindView(View view, Context context, final Cursor cursor) {
        RelativeLayout rl = null;
        if (view == null) {
            rl = (RelativeLayout)LayoutInflater.from(context).inflate(R.layout.list_item_view, null);
        } else {
            rl = (RelativeLayout)view;
        }

        int isDir = cursor.getInt(cursor.getColumnIndex(FileDataBase.IS_DIRECTORY));
        if (isDir == 1) {
            ((ImageView)(rl.findViewById(R.id.file_type))).setImageResource(R.drawable.folder);
            rl.findViewById(R.id.file_download).setVisibility(View.GONE);
        } else {
            ((ImageView)(rl.findViewById(R.id.file_type))).setImageResource(R.drawable.sbm);
            View download = rl.findViewById(R.id.file_download);
            download.setVisibility(View.VISIBLE);
            final int position = cursor.getPosition();
            download.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mHandler.onClick(position);
                }
            });

        }

        String fileName = cursor.getString(cursor.getColumnIndex(FileDataBase.FILE_NAME));
        ((TextView)(rl.findViewById(R.id.file_name))).setText(fileName);


        if (cursor.getPosition() % 2 == 0) {
            rl.setBackgroundColor(0xffffffff);
        } else {
            rl.setBackgroundColor(0xfff0f0f0);
        }
        super.bindView(rl, context, cursor);
    }

    public interface IDownloadViewClickHandler {
        public void onClick(int position);
    }
}
