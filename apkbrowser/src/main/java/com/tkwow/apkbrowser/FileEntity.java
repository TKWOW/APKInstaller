package com.tkwow.apkbrowser;

import android.content.ContentValues;

/**
 * Created by Tank on 6/11/14.
 */
public class FileEntity {
    public String fileName;
    public String filePath;
    public Integer  isDirectory;
    public Integer  downloadStatus;
    public Long     downloadID;
    @Override
    public String toString() {
        return fileName;
    }

    public ContentValues getContents() {
        ContentValues cv = new ContentValues();
        cv.put(FileDataBase.FILE_NAME, fileName);
        cv.put(FileDataBase.FILE_PATH, filePath);
        cv.put(FileDataBase.IS_DIRECTORY, isDirectory);
        cv.put(FileDataBase.DOWNLOAD_STATUS, downloadStatus);
        cv.put(FileDataBase.DOWNLOAD_ID, downloadID);
        return cv;
    }
}
