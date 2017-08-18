package com.unipi.nicola.indoorlocator.fingerprinting;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;

/**
 * Created by Nicola on 15/04/2017.
 *
 * Class needed in order to manage the db creation, upgrade / downgrade, and a bunch of useful DB
 * constants.
 */

public class WifiFingerprintDBHelper extends SQLiteOpenHelper {
    // If the database schema is changed, the database version must be incremented.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "WifiFingerprints.db";

    public static final String TABLE_FINGERPRINTS = "Fingerprint";
    public static final String TABLE_ACCESS_POINTS = "AccessPoint";

    public static final String COLUMN_FPID = "id";
    public static final String COLUMN_HWADDR = "hwAddress";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_ALTITUDE = "altitude";
    public static final String COLUMN_FPLABEL = "label";
    public static final String COLUMN_SIGNALSTRENGTH = "level";

    //SQL code to create fingerprint table
    private static final String SQL_CREATE_FP_TABLE =
            "CREATE TABLE if not exists " + TABLE_FINGERPRINTS +
            " (" + COLUMN_FPID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            COLUMN_FPLABEL + " TEXT, " +
            COLUMN_LATITUDE + " REAL, " +
            COLUMN_LONGITUDE + " REAL, " +
            COLUMN_ALTITUDE + " REAL)";

    //SQL code to create the access points table
    private static final String SQL_CREATE_AP_TABLE =
            "CREATE TABLE if not exists " + TABLE_ACCESS_POINTS +
            " (" + COLUMN_FPID + " INTEGER NOT NULL, " +
            COLUMN_HWADDR + " TEXT NOT NULL, " +
            COLUMN_SIGNALSTRENGTH + " INTEGER, " +
            "PRIMARY KEY (" + COLUMN_FPID + ", " + COLUMN_HWADDR + "), " +
            "FOREIGN KEY (" + COLUMN_FPID + ") REFERENCES " + TABLE_FINGERPRINTS + "(" + COLUMN_FPID + ") ON DELETE CASCADE)";


    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + TABLE_ACCESS_POINTS + "; " +
            "DROP TABLE IF EXISTS " + TABLE_FINGERPRINTS ;

    public WifiFingerprintDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        File dbFile = context.getDatabasePath(DATABASE_NAME);
        boolean t = dbFile.exists();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_FP_TABLE);
        db.execSQL(SQL_CREATE_AP_TABLE);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Simply discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Same as upgrade
        onUpgrade(db, oldVersion, newVersion);
    }
}
