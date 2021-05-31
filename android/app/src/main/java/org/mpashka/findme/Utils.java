package org.mpashka.findme;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class Utils {
    private static final String BASE_URL = "https://jsonplaceholder.typicode.com";

    public static int readChargeLevel(Context context) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        return isCharging ? -1 : level;
    }

    public static Retrofit createRetrofitClient(MyPreferences preferences) {
        OkHttpClient.Builder client = new OkHttpClient.Builder();
        if (preferences.getBoolean(R.string.debug_http_id, R.bool.debug_http_default)) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            client.addInterceptor(interceptor);
        }

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
}
