package org.mpashka.findme;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Build;

import androidx.room.Room;

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
    public MiBandManager miBandManager(@ApplicationContext Context applicationContext) {
        RxBleClient rxBleClient = RxBleClient.create(applicationContext);
        RxBleClient.State state = rxBleClient.getState();
        Timber.i("BLE state: %s", state);
        if (state == RxBleClient.State.BLUETOOTH_NOT_AVAILABLE) {
            Timber.i("Bluetooth not present. MiBandManager won't be accessible");
            return new MiBandManager(null);
        }

        RxJavaPlugins.setErrorHandler(throwable -> {
            if (throwable instanceof UndeliverableException && throwable.getCause() instanceof BleException) {
                Timber.d(throwable, "Suppressed UndeliverableException");
                return; // ignore BleExceptions as they were surely delivered at least once
            }
            // add other custom handlers if needed
            Timber.e(throwable, "Unexpected Throwable in RxJavaPlugins error handler");
//            throw new RuntimeException("Unexpected Throwable in RxJavaPlugins error handler", throwable);
        });


        MiBand miBand = new MiBand();
        String peripheralAddress = "D7:67:6E:54:C7:5C";
        miBand.init(rxBleClient, peripheralAddress);

        return new MiBandManager(miBand);
    }
}
