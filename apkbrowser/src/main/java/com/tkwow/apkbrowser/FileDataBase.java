package com.tkwow.apkbrowser;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Tank on 6/11/14.
 */
public class FileDataBase extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "file_data.db";
    public static final String KEY_WORD = "_id";
    public static final String FILE_NAME = "file_name";
    public static final String FILE_PATH = "file_path";
    public static final String IS_DIRECTORY = "is_directory";
    public static final String DOWNLOAD_STATUS = "download_status";
    public static final String DOWNLOAD_ID  = "download_id";
    private static final int DATABASE_VERSION = 1;
    public static final String TABLE_NAME = "file_list";
    private static final String DICTIONARY_TABLE_CREATE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    KEY_WORD + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    FILE_NAME + " TEXT, " +
                    FILE_PATH + " TEXT, " +
                    IS_DIRECTORY + " INTEGER, " +
                    DOWNLOAD_STATUS + " INTEGER, "+
                    DOWNLOAD_ID + " INTEGER);";

    FileDataBase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DICTIONARY_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
