package com.tkwow.apkbrowser;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Tank on 6/11/14.
 */
public class FileList {
    private static FileList sFileList = null;
    private FileDataBase mDBHelper;
    private SQLiteDatabase mDB;
    public static FileList getInstance(Context c) {
        if (sFileList == null) sFileList = new FileList(c);
        return sFileList;
    }

    public void add(ArrayList<FileEntity> files) {
        if (files == null || files.isEmpty()) return;

        mDB.beginTransaction();
        try {
            ArrayList<FileEntity> existFiles = query(files.get(0).filePath);
            if (!existFiles.isEmpty()) {
                for (FileEntity entity : files) {
                    for (FileEntity exist : existFiles) {
                        if (exist.fileName.equals(entity.fileName)) {
                            entity.downloadID = exist.downloadID;
                            entity.downloadStatus = exist.downloadStatus;
                            break;
                        }
                    }
                }
                mDB.delete(FileDataBase.TABLE_NAME, FileDataBase.FILE_PATH+"= ?", new String[]{files.get(0).filePath});
            }

            for (FileEntity file : files) {
                mDB.execSQL("INSERT INTO " + FileDataBase.TABLE_NAME +" VALUES(null, ?, ?, ?, ?, ?)",
                        new Object[]{file.fileName, file.filePath,
                                file.isDirectory, file.downloadStatus, file.downloadID});
            }
            mDB.setTransactionSuccessful();
        } finally {
            mDB.endTransaction();
        }
    }

    public void update(FileEntity entity) {
        ContentValues cv = entity.getContents();
        mDB.update(FileDataBase.TABLE_NAME, cv, FileDataBase.FILE_PATH+"=?"+" and "+FileDataBase.FILE_NAME+"=?", new String[]{
                entity.filePath, entity.fileName
        });
    }

    public FileEntity query(String filePath, String fileName) {
        Cursor c = queryTheCursor(FileDataBase.FILE_PATH+"=?"+" and "+FileDataBase.FILE_NAME+"=?", new String[]{
                filePath, fileName
        });
        return createEntityWithCursor(c);
    }

    public FileEntity queryByDownloadID(long downloadID) {
        Cursor c = queryTheCursor(FileDataBase.DOWNLOAD_ID+"=?", new String[]{
                String.valueOf(downloadID)
        });
        return createEntityWithCursor(c);
    }

    private FileEntity createEntityWithCursor(Cursor c) {
        if (c == null || c.getCount() == 0) return null;

        c.moveToFirst();
        return createWithCursor(c);
    }

    private ArrayList<FileEntity> query(String filePath) {
        ArrayList<FileEntity> files = new ArrayList<FileEntity>();
        Cursor c = queryTheCursor(filePath);
        while (c.moveToNext()) {
            files.add(createWithCursor(c));
        }
        c.close();
        return files;
    }

    private FileEntity createWithCursor(Cursor c) {
        FileEntity file = new FileEntity();
        file.fileName = c.getString(c.getColumnIndex(FileDataBase.FILE_NAME));
        file.filePath = c.getString(c.getColumnIndex(FileDataBase.FILE_PATH));
        file.isDirectory = c.getInt(c.getColumnIndex(FileDataBase.IS_DIRECTORY));
        file.downloadStatus = c.getInt(c.getColumnIndex(FileDataBase.DOWNLOAD_STATUS));
        file.downloadID = c.getLong(c.getColumnIndex(FileDataBase.DOWNLOAD_ID));
        return file;
    }
    /**
     * query all persons, return cursor
     * @return  Cursor
     */
    public Cursor queryTheCursor(String filePath) {
        return queryTheCursor(FileDataBase.FILE_PATH+"=?", new String[]{
                filePath
        });
    }

    public Cursor queryTheCursor(String selection, String[] params) {
        Cursor c = mDB.query(FileDataBase.TABLE_NAME, new String[] {
                FileDataBase.KEY_WORD,
                FileDataBase.FILE_NAME,
                FileDataBase.FILE_PATH,
                FileDataBase.IS_DIRECTORY,
                FileDataBase.DOWNLOAD_STATUS,
                FileDataBase.DOWNLOAD_ID
        }, selection, params, null, null, null);
        return c;
    }

    private FileList(Context c) {
        mDBHelper = new FileDataBase(c);
        mDB = mDBHelper.getWritableDatabase();
    }
}
