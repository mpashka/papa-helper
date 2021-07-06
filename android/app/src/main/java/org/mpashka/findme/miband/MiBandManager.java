package org.mpashka.findme.miband;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.Maybe;
import io.reactivex.Single;
import timber.log.Timber;

public class MiBandManager {

    private static final int HEART_RATE_COUNT = 3;
    public static final int HEART_RATE_TIMEOUT = 60;

    private MiBand miBand;
    private int prevSteps;

    @Inject
    public MiBandManager(MiBand miBand) {
        this.miBand = miBand;
    }

    public Single<MiBandInfo> readMiBandInfo() {
        MiBandInfo miBandInfo = new MiBandInfo();

        return miBand.connect()
                .flatMap(i -> miBand.auth())
                .flatMapSingle(i -> miBand.readBatteryInfo())
                .doOnNext(miBandInfo::setBattery)
                .flatMapSingle(i -> miBand.readSteps())
                .doOnNext(miBandInfo::setSteps)
                .flatMapSingle(i -> readHeartRateIfNeeded(miBandInfo))
                .doOnNext(s -> prevSteps = miBandInfo.getSteps())
                .doOnTerminate(() -> miBand.disconnect())
                .firstOrError();
    }

    private Single<MiBandInfo> readHeartRateIfNeeded(MiBandInfo miBandInfo) {
//        if (prevSteps > 0 && miBandInfo.getSteps() != prevSteps) {
//            return Single.just(miBandInfo);
//        }
        Timber.d("Request heart rate since prev steps %s are zero or same %s", prevSteps, miBandInfo.getSteps());
        return miBand.heartRateSubscribe()
                .flatMapMaybe(rate -> miBandInfo.addHeartRate(miBandInfo.heartRateCount)
                        ? Maybe.just(miBandInfo.getHeartRate())
                        : Maybe.empty())
                .timeout(HEART_RATE_TIMEOUT, TimeUnit.SECONDS)
                .onErrorReturn(e -> {
                    Timber.d(e, "Heart rate timeout occurred. Return current value %s", miBandInfo.getHeartRate());
                    return miBandInfo.getHeartRate();
                })
                .map(i -> miBandInfo)
                .firstOrError();
    }

    public static class MiBandInfo {
        private int battery = -1;
        private int steps = -1;
        private int heartRateSum;
        private int heartRateCount;

        public int getBattery() {
            return battery;
        }

        public void setBattery(int battery) {
            this.battery = battery;
        }

        public int getSteps() {
            return steps;
        }

        public void setSteps(int steps) {
            this.steps = steps;
        }

        public boolean addHeartRate(int heartRate) {
            heartRateCount++;
            heartRateSum += heartRate;
            return heartRateCount >= HEART_RATE_COUNT;
        }

        public int getHeartRate() {
            return heartRateCount > 0 ? heartRateSum / heartRateCount : -1;
        }

        @Override
        public String toString() {
            return "MiBandInfo{" +
                    "battery=" + battery +
                    ", steps=" + steps +
                    ", heartRateSum=" + heartRateSum +
                    ", heartRateCount=" + heartRateCount +
                    '}';
        }
    }
/*
    private Context context;
    private RxBleClient rxBleClient;

    public void init(Context context) {
        this.context = context;
        rxBleClient = MyBleApp.getRxBleClient(context);
    }

    public MiBand getMiBand(String address) {
        MiBand miBand = new MiBand();
        miBand.init(context, rxBleClient, address);
        return miBand;
    }
*/
}
