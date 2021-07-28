package org.mpashka.findme.services;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorker;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.mpashka.findme.MyPreferences;
import org.mpashka.findme.R;
import org.mpashka.findme.db.LocationDao;
import org.mpashka.findme.miband.MiBandManager;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import dagger.hilt.android.AndroidEntryPoint;
import dagger.hilt.android.qualifiers.ApplicationContext;
import timber.log.Timber;

@Singleton
public class MyWorkManager {

    @ApplicationContext
    Context context;

    @Inject
    MyState state;

    @Inject
    MyPreferences preferences;

    @Inject
    MyLocationService locationService;

    @Inject
    MyLocationFuseService locationFuseService;

    @Inject
    MyAccelerometerService accelerometerService;

    @Inject
    MyActivityService activityService;

    @Inject
    MyTransmitService transmitService;

    @Inject
    LocationDao locationDao;

    private boolean running;

    private ServiceInfo[] services;
    private ServiceInfo fuseServiceInfo;

    @Inject
    public MyWorkManager(@ApplicationContext Context context, MyState state, MyPreferences preferences,
                         MyLocationService locationService, MyLocationFuseService locationFuseService,
                         MyAccelerometerService accelerometerService, MyActivityService activityService,
                         MyTransmitService transmitService, LocationDao locationDao) {
        this.context = context;
        this.state = state;
        this.preferences = preferences;
        this.locationService = locationService;
        this.locationFuseService = locationFuseService;
        this.accelerometerService = accelerometerService;
        this.activityService = activityService;
        this.transmitService = transmitService;
        this.locationDao = locationDao;

        fuseServiceInfo = new ServiceInfo(locationFuseService, R.string.location_fuse_provider_enabled_id, R.bool.location_fuse_provider_enabled_default);
        this.services = new ServiceInfo[]{
                new ServiceInfo(locationService, R.string.location_gen_provider_enabled_id, R.bool.location_gen_provider_enabled_default),
                fuseServiceInfo,
                new ServiceInfo(activityService, R.string.activity_provider_enabled_id, R.bool.activity_provider_enabled_default),
        };
    }

    @HiltWorker
    public static class PeriodicWorker extends Worker {
        private MyState state;
        private MyPreferences preferences;
        private MyWorkManager workManager;

        @AssistedInject
        public PeriodicWorker(@Assisted @NonNull Context appContext,
                              @Assisted @NonNull WorkerParameters workerParams,
                              MyState state, MyPreferences preferences, MyWorkManager workManager) {
            super(appContext, workerParams);
            this.state = state;
            this.preferences = preferences;
            this.workManager = workManager;
        }

        @NonNull
        @Override
        public Result doWork() {
            Timber.d("RestartWorker::doWork()");

            int maxTimeMinutes = preferences.getInt(R.string.restart_location_fuse_max_time_id, R.integer.restart_location_fuse_max_time_default);
            if (state.getLastCreateTime() < System.currentTimeMillis() - maxTimeMinutes * 60 * 1000) {
                workManager.fetchCurrentLocation();
            }
            return Result.success();
        }
    }

    /**
     * Starts work manager if necessary
     */
    public void startIfNeeded() {
        if (!running) {
            start();
            startServices();
        }
    }

    /**
     * Starts work manager (or update check interval)
     */
    public void start() {
        Timber.d("startWorkerManager");
        try {
            WorkManager workManager = WorkManager.getInstance(context);
            int restartCheckMinutes = preferences.getInt(R.string.restart_check_id, R.integer.restart_check_default);
            workManager.enqueueUniquePeriodicWork(context.getString(R.string.app_id) + "_check", ExistingPeriodicWorkPolicy.REPLACE,
                    new PeriodicWorkRequest.Builder(PeriodicWorker.class, restartCheckMinutes, TimeUnit.MINUTES).build());
            running = true;
        } catch (Exception e) {
            Timber.e(e, "Error startWorkerManager()");
        }
    }

    public boolean checkPermissions() {
        for (ServiceInfo service : services) {
            if (!checkPermissions(service)) {
                return false;
            }
        }
        return true;
    }

    public boolean checkPermissions(ServiceInfo service) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        String[] permissions = service.getService().getPermissions();
        if (permissions == null || permissions.length == 0) {
            return true;
        }
        for (String permission : permissions) {
            if (context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    public void startServices() {
        Timber.d("startServices");
        for (ServiceInfo service : services) {
            reloadService(service);
        }
    }

    public void reloadService(Class<? extends MyListenableServiceInterface> serviceClass) {
        ServiceInfo service = findService(serviceClass);
        if (service != null) {
            reloadService(service);
        }
    }

    public void reloadService(@NonNull ServiceInfo service) {
        if (service.isEnabled()) {
            if (checkPermissions(service)) {
                service.start();
            }
        } else {
            service.stop();
        }
    }

    public void stop() {
        Timber.d("stopServices");
        for (ServiceInfo service : services) {
            service.stop();
        }
    }

    public void fetchCurrentLocation() {
        if (fuseServiceInfo.isEnabled() && checkPermissions(fuseServiceInfo)) {
            locationFuseService.fetchCurrentLocation();
        }
    }

    public boolean isEnabled(Class<? extends MyListenableServiceInterface> serviceClass) {
        ServiceInfo service = findService(serviceClass);
        return service != null && service.isEnabled();
    }

    private ServiceInfo findService(Class<? extends MyListenableServiceInterface> serviceClass) {
        for (ServiceInfo service : services) {
            if (service.getService().getClass() == serviceClass) {
                return service;
            }
        }
        return null;
    }

    private final class ServiceInfo {
        private MyListenableServiceInterface service;
        private int preferenceEnabledId;
        private int preferenceEnabledDefault;
        private boolean running;

        public ServiceInfo(MyListenableServiceInterface service, int preferenceEnabledId, int preferenceEnabledDefault) {
            this.service = service;
            this.preferenceEnabledId = preferenceEnabledId;
            this.preferenceEnabledDefault = preferenceEnabledDefault;
        }

        public MyListenableServiceInterface getService() {
            return service;
        }

        public boolean isEnabled() {
            return preferences.getBoolean(preferenceEnabledId, preferenceEnabledDefault);
        }

        public void start() {
            if (!running) {
                try {
                    Timber.i("Starting service %s", service.getClass().getSimpleName());
                    service.startListen();
                } catch (Exception e) {
                    Timber.w(e, "Error starting service");
                }
                running = true;
            }
        }

        public void stop() {
            if (running) {
                try {
                    Timber.i("Stop service %s", service.getClass().getSimpleName());
                    service.stopListen();
                } catch (Exception e) {
                    Timber.w(e, "Error stop service");
                }
                running = false;
            }
        }
    }
}
