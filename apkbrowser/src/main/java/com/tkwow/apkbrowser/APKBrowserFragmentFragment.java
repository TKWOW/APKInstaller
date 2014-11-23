package com.tkwow.apkbrowser;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.io.File;

/**
 * A fragment representing a list of Items.
 * <p />
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p />
 * interface.
 */
public class APKBrowserFragmentFragment extends Fragment implements ListView.OnItemClickListener,
                                                            FileListAdapter.IDownloadViewClickHandler,
                                                            DownloadProgressPopup.IDownloadProgressHandler {
    private static final String ARG_SERVER = "server";

    private String mServer;
    private String mFilePath;
    /**
     * The fragment's ListView/GridView.
     */
    private ListView mListView;
    private View    mEmptyView;
    private boolean mShouldInstall = false;
    DownloadStateReceiver mDownloadStateReceiver = null;
    DownloadProgressPopup mPopup = new DownloadProgressPopup();
    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private FileListAdapter mAdapter;
    static APKBrowserFragmentFragment sFragment = null;
    public static APKBrowserFragmentFragment newInstance(String server) {
        APKBrowserFragmentFragment fragment = new APKBrowserFragmentFragment();
        sFragment = fragment;
        Bundle args = new Bundle();
        args.putString(ARG_SERVER, server);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mServer = getArguments().getString(ARG_SERVER);
            FileExploreService.startOpen(getActivity(), mServer);

            IntentFilter mStatusIntentFilter = new IntentFilter(Constants.BROADCAST_FILE_OPEN);
            mStatusIntentFilter.addAction(Constants.BROADCAST_FILE_DOWNLOAD);
            mStatusIntentFilter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
            mDownloadStateReceiver = new DownloadStateReceiver();
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                    mDownloadStateReceiver,
                    mStatusIntentFilter);
        }
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mDownloadStateReceiver);
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_apkbrowserfragment, container, false);

        // Set the adapter
        mListView = (ListView) view.findViewById(android.R.id.list);
        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        mEmptyView = view.findViewById(R.id.emptyView);
        return view;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FileEntity entity = (FileEntity)mAdapter.getItem(position);
        if (entity.isDirectory > 0) {
            FileExploreService.startOpen(getActivity(), mServer+entity.filePath+entity.fileName);
        } else {

        }
    }

    public boolean handleBackPressed() {
        if (mPopup!= null && mPopup.isShown()) {
            mPopup.dismiss();
            return true;
        }

        if (mFilePath != null && !mFilePath.equals("")) {
            int lastIndex = mFilePath.lastIndexOf('&');
            String filePath = "";
            if (lastIndex != -1) {
                filePath = mFilePath.substring(0, lastIndex);
            }
            updateData(filePath);
            return true;
        }
        return false;
    }

    private void updateData(String filePath) {
        if (filePath == null) return;
        Cursor c = FileList.getInstance(getActivity()).queryTheCursor(filePath);
        c.moveToFirst();
        if (c.getCount() == 1 && c.getString(c.getColumnIndex(FileDataBase.FILE_NAME)).equals("")) {
            mListView.setVisibility(View.INVISIBLE);
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mListView.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.INVISIBLE);
            mAdapter = new FileListAdapter(getActivity(), R.layout.list_item_view,
                    c, new String[]{},
                    new int[]{}, 0);

            mListView.setAdapter(mAdapter);
            mAdapter.setClickHandler(this);
            mListView.invalidateViews();
        }

        if (filePath.equals("")) {
            getActivity().setTitle("Root");
        } else {
            filePath = filePath.replaceAll("&", "/");
            if (filePath.charAt(filePath.length()-1) == '/') {
                filePath = filePath.substring(0, filePath.length()-1);
            }
            getActivity().setTitle(filePath);
        }
        mFilePath = filePath;

    }

    @Override
    public void onClick(int position) {
        FileEntity entity = (FileEntity)mAdapter.getItem(position);
        entity = FileList.getInstance(this.getActivity()).query(entity.filePath, entity.fileName);
        if (entity.downloadStatus == 1) {
            mPopup.show(this.getActivity(), getView(), entity.downloadID, APKBrowserFragmentFragment.this);
        } else if (entity.downloadStatus == 2){
            DownloadManager downloadManager = (DownloadManager) getActivity().getSystemService
                    (Context.DOWNLOAD_SERVICE);
            Cursor c= downloadManager.query(new DownloadManager.Query().setFilterById(entity.downloadID));

            if(c.moveToFirst()){
                int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
                if(status == DownloadManager.STATUS_SUCCESSFUL){
                    int index = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                    final String localPath = c.getString(index);
                    sFragment.doInstall(localPath);
                }
            }
            c.close();
        } else {
            DownloadService.startDownload(getActivity(), mServer + "download/" + entity.filePath + entity.fileName,
                    entity.filePath);
        }
    }

    private void doInstall(String localPath) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        localPath = localPath.replaceFirst("file://", "");
        intent.setDataAndType(Uri.fromFile(new File(localPath)), "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onDownloadProgressComplete(String localPath) {
        if (mPopup != null && mPopup.isShown()) {
            mPopup.dismiss();
            mShouldInstall = true;
        }
    }

    public static class DownloadStateReceiver extends BroadcastReceiver {
        public DownloadStateReceiver(){super();}
        // Called when the BroadcastReceiver gets an Intent it's registered to receive
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            if (intent.getAction().equals(Constants.BROADCAST_FILE_OPEN)) {
                if (extras.containsKey(Constants.EXTENDED_FILE_OPEN_STATUS)) {
                    boolean status = extras.getBoolean(Constants.EXTENDED_FILE_OPEN_STATUS);
                    if (!status) return;

                    String path = extras.getString(Constants.EXTENDED_FILE_OPEN_FILE_PATH);
                    sFragment.updateData(path);
                }
            } else if (intent.getAction().equals(Constants.BROADCAST_FILE_DOWNLOAD)) {
                if (extras.containsKey(Constants.EXTENDED_FILE_DOWNLOAD_ID)) {
                    long downloadID = extras.getLong(Constants.EXTENDED_FILE_DOWNLOAD_ID);
                    final FileEntity entity = FileList.getInstance(context).queryByDownloadID(downloadID);
                    if (entity != null) {
                        sFragment.getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                sFragment.mPopup.show(sFragment.getActivity(),
                                        sFragment.getView(),
                                        entity.downloadID, sFragment);
                            }
                        });

                    }
                }
            } else if (intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {

                Long dwnId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);

                FileEntity entity = FileList.getInstance(context).queryByDownloadID(dwnId);
                if (entity != null) {
                    entity.downloadStatus = 2;
                    FileList.getInstance(context).update(entity);
                }

                if (!sFragment.mShouldInstall) return;

                DownloadManager downloadManager = (DownloadManager) context.getSystemService
                        (Context.DOWNLOAD_SERVICE);
                Cursor c= downloadManager.query(new DownloadManager.Query().setFilterById(dwnId));

                if(c.moveToFirst()){
                    int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    if(status == DownloadManager.STATUS_SUCCESSFUL){
                        int index = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                        final String localPath = c.getString(index);
                        sFragment.doInstall(localPath);
                    }
                }
                c.close();
            }
        }
    }

}
