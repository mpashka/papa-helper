package org.mpashka.findme;

import android.content.Context;
import android.database.SQLException;
import android.net.ConnectivityManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.exceptions.BleException;

import org.mpashka.findme.MyPreferences;
import org.mpashka.findme.R;
import org.mpashka.findme.Utils;
import org.mpashka.findme.db.AccelerometerDao;
import org.mpashka.findme.db.LocationDao;
import org.mpashka.findme.db.MyDb;
import org.mpashka.findme.db.MyTransmitService;
import org.mpashka.findme.db.io.SaveApi;
import org.mpashka.findme.miband.MiBand;
import org.mpashka.findme.miband.MiBandManager;

import java.io.File;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.plugins.RxJavaPlugins;
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
        Context deviceContext = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                ? applicationContext.createDeviceProtectedStorageContext()
                : applicationContext;
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

    @Provides
    @Singleton
    public RxBleClient rxBleClient(@ApplicationContext Context applicationContext) {
        RxJavaPlugins.setErrorHandler(throwable -> {
            if (throwable instanceof UndeliverableException && throwable.getCause() instanceof BleException) {
                Timber.d(throwable, "Suppressed UndeliverableException");
                return; // ignore BleExceptions as they were surely delivered at least once
            }
            // add other custom handlers if needed
            Timber.e(throwable, "Unexpected Throwable in RxJavaPlugins error handler");
//            throw new RuntimeException("Unexpected Throwable in RxJavaPlugins error handler", throwable);
        });

        return RxBleClient.create(applicationContext);
    }

    @Provides
    @Singleton
    public MiBand miBandManager(RxBleClient bleClient) {
        MiBand miBand = new MiBand();
        String peripheralAddress = "D7:67:6E:54:C7:5C";
        miBand.init(bleClient, peripheralAddress);
        return miBand;
    }
}
