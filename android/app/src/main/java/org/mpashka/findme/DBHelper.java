package org.mpashka.findme;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import org.mpashka.findme.MyWorkManager;
import org.mpashka.findme.R;

import timber.log.Timber;

public class DBHelper extends SQLiteOpenHelper {
    public DBHelper(@Nullable Context context) {
        super(context, context.getString(R.string.app_id) + "_db", null, 4);
        if (!context.isDeviceProtectedStorage()) {
            Timber.e("Error! Not device protected storage is used for DB");
            throw new RuntimeException("Error! Not device protected storage is used for DB");
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Timber.d("onCreate database");
        db.execSQL("CREATE TABLE IF NOT EXISTS accelerometer ("
                + "time integer PRIMARY KEY NOT NULL,"
                + "avg integer NOT NULL,"
                + "max integer NOT NULL,"
                + "battery integer NOT NULL,"
                + "saved integer NOT NULL);");
        db.execSQL("CREATE TABLE IF NOT EXISTS location ("
                + "time integer PRIMARY KEY NOT NULL,"
                + "lat REAL NOT NULL,"
                + "long REAL NOT NULL,"
                + "accuracy REAL NOT NULL,"
                + "battery integer NOT NULL,"
                + "saved integer NOT NULL);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Timber.d("Update db. db version %s -> %s", oldVersion, newVersion);
        if (oldVersion < 3) {
            try {
                db.execSQL("DROP TABLE accelerometer;");
                db.execSQL("DROP TABLE location;");
                onCreate(db);
                db.setVersion(1);
            } catch (SQLException e) {
                Timber.e(e, "Error database upgrade");
            }
/*
            try {
                db.execSQL("ALTER TABLE accelerometer ADD COLUMN saved integer DEFAULT 0 NOT NULL;");
            } catch (SQLException e) {
                Timber.e(e, "Error upgrade accelerometer");
            }
            try {
                db.execSQL("ALTER TABLE location ADD COLUMN saved integer DEFAULT 0 NOT NULL;");
            } catch (SQLException e) {
                Timber.e(e, "Error upgrade location");
            }
*/
        }
    }
}
