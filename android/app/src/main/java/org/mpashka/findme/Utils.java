package org.mpashka.findme;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Utils {

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

    @NotNull
    public static <T, R> List<R> convertList(List<T> in, MyFunction<T, R> function) {
        List<R> locationIds = new ArrayList<>(in.size());
        for (T location : in) {
            locationIds.add(function.apply(location));
        }
        return locationIds;
    }

    public static <T> boolean anyMatchList(List<T> in, MyPredicate<T> predicate) {
        for (T locationPermission : in) {
            if (predicate.test(locationPermission)) {
                return true;
            }
        }
        return false;
    }

    @FunctionalInterface
    public interface MyFunction<T, R> {
        R apply(T t);
    }

    @FunctionalInterface
    public interface MyPredicate<T> {
        boolean test(T t);
    }
}
