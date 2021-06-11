package org.mpashka.findme.db;

import android.content.Context;
import android.database.SQLException;
import android.net.ConnectivityManager;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import org.mpashka.findme.MyPreferences;
import org.mpashka.findme.R;
import org.mpashka.findme.Utils;
import org.mpashka.findme.db.io.SaveApi;

import java.io.File;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

@InstallIn(SingletonComponent.class)
@Module
public class MyHiltModule {

    @Provides
    @Singleton
    public MyPreferences preferences(@ApplicationContext Context applicationContext) {
        Timber.d("preferences()");
        return new MyPreferences(applicationContext);
    }

    @Provides
    @Singleton
    public ConnectivityManager connectivityManager(@ApplicationContext Context applicationContext) {
        return (ConnectivityManager)applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Provides
    @Singleton
    public MyDb myDb(@ApplicationContext Context applicationContext) {
        Timber.d("myDb()");
        Context deviceContext = applicationContext.createDeviceProtectedStorageContext();
        String dbName = applicationContext.getString(R.string.app_id) + "_db";
/*
        File filePath = deviceContext.getDatabasePath(dbName);
        filePath.delete();
*/
        try {
            MyDb db = Room
                    .databaseBuilder(deviceContext, MyDb.class, dbName)
                    .build();
//        db.close();

            return db;
        } catch (Exception e) {
            Timber.e(e, "Error room create");
            return null;
        }
    }

    @Provides
    @Singleton
    public LocationDao locationDao(MyDb db) {
        return db.locationDao();
    }

    @Provides
    @Singleton
    public AccelerometerDao accelerometerDao(MyDb db) {
        return db.accelerometerDao();
    }

    @Provides
    @Singleton
    public MyTransmitService myTransmitService(LocationDao locationDao, AccelerometerDao accelerometerDao,
                                               MyPreferences preferences,
                                               ConnectivityManager connectivityManager)
    {
        return new MyTransmitService(locationDao, accelerometerDao, preferences, connectivityManager);
    }

}
