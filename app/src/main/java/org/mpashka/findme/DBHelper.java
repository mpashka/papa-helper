package org.mpashka.findme;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import org.mpashka.findme.MyWorkManager;
import org.mpashka.findme.R;

import timber.log.Timber;

public class DBHelper extends SQLiteOpenHelper {
    public DBHelper(@Nullable Context context) {
        super(context, context.getString(R.string.app_id) + "_db", null, 1);
        if (!context.isDeviceProtectedStorage()) {
            Timber.e("Error! Not device protected storage is used for DB");
            throw new RuntimeException("Error! Not device protected storage is used for DB");
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Timber.d("onCreate database");
        db.execSQL("create table accelerometer ("
                + "time integer primary key,"
                + "avg integer,"
                + "max integer,"
                + "battery integer);");
        db.execSQL("create table location ("
                + "time integer primary key,"
                + "lat REAL,"
                + "long REAL,"
                + "accuracy REAL,"
                + "battery integer);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
