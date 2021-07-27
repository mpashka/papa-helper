package org.mpashka.findme;

import android.content.Context;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Build;

import androidx.room.Room;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.exceptions.BleException;

import org.mpashka.findme.db.LocationDao;
import org.mpashka.findme.db.MyDb;
import org.mpashka.findme.miband.MiBandManager;
import org.mpashka.findme.services.MyAccelerometerService;
import org.mpashka.findme.services.MyActivityService;
import org.mpashka.findme.services.MyLocationFuseService;
import org.mpashka.findme.services.MyLocationService;
import org.mpashka.findme.services.MyState;
import org.mpashka.findme.services.MyTransmitService;
import org.mpashka.findme.miband.MiBand;
import org.mpashka.findme.services.MyWorkManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.plugins.RxJavaPlugins;
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
    public MyState state() {
        return new MyState();
    }

    @Provides
    @Singleton
    public MyTransmitService myTransmitService(LocationDao locationDao,
                                               MyPreferences preferences,
                                               ConnectivityManager connectivityManager,
                                               MyState state)
    {
        return new MyTransmitService(locationDao, preferences, connectivityManager, state);
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

    @Provides
    @Singleton
    public MyAccelerometerService accelerometerService(@ApplicationContext Context context, MyPreferences preferences) {
        return new MyAccelerometerService(context, preferences);
    }

    @Provides
    @Singleton
    public MyActivityService activityService(@ApplicationContext Context context) {
        return new MyActivityService(context);
    }

    @Provides
    @Singleton
    public MyLocationFuseService locationFuseService(@ApplicationContext Context context, MyPreferences preferences) {
        return new MyLocationFuseService(context, preferences);
    }

    @Provides
    @Singleton
    public MyLocationService locationService(@ApplicationContext Context context, MyPreferences preferences) {
        return new MyLocationService(context, preferences);
    }

    @Provides
    @Singleton
    public MyTransmitService transmitService(LocationDao locationDao,
                                             MyPreferences preferences,
                                             ConnectivityManager connectivityManager,
                                             MyState state) {
        return new MyTransmitService(locationDao, preferences, connectivityManager, state);
    }

    @Provides
    @Singleton
    public MyWorkManager workManager(@ApplicationContext Context context, MyState state, MyPreferences preferences,
                                     MyLocationService locationService, MyLocationFuseService locationFuseService,
                                     MyAccelerometerService accelerometerService, MyActivityService activityService,
                                     MiBandManager miBandManager, MyTransmitService transmitService, LocationDao locationDao) {
        return new MyWorkManager(context, state, preferences, locationService, locationFuseService,
                accelerometerService, activityService, miBandManager, transmitService, locationDao);
    }
}
