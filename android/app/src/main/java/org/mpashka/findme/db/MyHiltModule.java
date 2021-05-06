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
import org.mpashka.findme.db.io.SaveApi;

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

    private static final String BASE_URL = "https://jsonplaceholder.typicode.com";

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
        try {
            MyDb db = Room
                    .databaseBuilder(deviceContext, MyDb.class, applicationContext.getString(R.string.app_id) + "_db")
                    .addMigrations(new Migration(2, 3) {
                        @Override
                        public void migrate(@NonNull SupportSQLiteDatabase db) {
                            Timber.d("Update room db. db version %s", db.getVersion());
                            db.setVersion(1);
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
                        }
                    })
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
    public Retrofit retrofit() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder client = new OkHttpClient.Builder()
                .addInterceptor(interceptor);

        RxJava2CallAdapterFactory rxAdapter =
                RxJava2CallAdapterFactory
                        .createWithScheduler(Schedulers.io());
        // Schedulers.io()
/*
          .addInterceptor(chain -> {
            Request newRequest =
                    chain.request().newBuilder()
                            .addHeader("Accept",
                                    "application/json,text/plain,* / *") <--
                            .addHeader("Content-Type",
                                    "application/json;odata.metadata=minimal")
                            .addHeader("Authorization", mToken)
                            .build();

            return chain.proceed(newRequest);
        });

        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                .create();

.addConverterFactory(GsonConverterFactory.create(gson))
*/

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(rxAdapter)
                .client(client.build())
                .build();
    }

    @Provides
    @Singleton
    public SaveApi saveApi(Retrofit retrofit) {
        return retrofit.create(SaveApi.class);
    }

    @Provides
    @Singleton
    public MyPreferences preferences(@ApplicationContext Context applicationContext) {
        Timber.d("preferences()");
        return new MyPreferences(applicationContext);
    }

}
