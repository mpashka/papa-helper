package org.mpashka.findme.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.mpashka.findme.MyPreferences;
import org.mpashka.findme.R;
import org.mpashka.findme.db.AccelerometerDao;
import org.mpashka.findme.db.LocationDao;
import org.mpashka.findme.db.MyTransmitService;

import java.sql.Time;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

@AndroidEntryPoint
public class HomeFragment extends Fragment {

    @Inject
    LocationDao locationDao;

    @Inject
    AccelerometerDao accelerometerDao;

    @Inject
    MyTransmitService transmitService;

    private HomeViewModel homeViewModel;
    private ScheduledExecutorService timer;
    private Runnable timerTask;
    private ScheduledFuture<?> timerTaskFuture;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        final TextView locationsSavedView = root.findViewById(R.id.text_home_locations_saved);
        homeViewModel.getLocationsSaved().observe(getViewLifecycleOwner(), s -> locationsSavedView.setText(String.valueOf(s)));
        final TextView locationsUnsavedView = root.findViewById(R.id.text_home_locations_unsaved);
        homeViewModel.getLocationsUnsaved().observe(getViewLifecycleOwner(), s -> locationsUnsavedView.setText(String.valueOf(s)));
        final TextView accelerationsSavedView = root.findViewById(R.id.text_home_accelerations_saved);
        homeViewModel.getAccelerometersSaved().observe(getViewLifecycleOwner(), s -> accelerationsSavedView.setText(String.valueOf(s)));
        final TextView accelerationsUnsavedView = root.findViewById(R.id.text_home_accelerations_unsaved);
        homeViewModel.getAccelerometersUnsaved().observe(getViewLifecycleOwner(), s -> accelerationsUnsavedView.setText(String.valueOf(s)));
        timer = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "MyTimer"));
        timerTask = () -> {
            Timber.d("Timer task");
            try {
                int locationsSaved = locationDao.getSavedCount();
                int locationsUnsaved = locationDao.getUnsavedCount();
                int accelerometersSaved = accelerometerDao.getSavedCount();
                int accelerometersUnsaved = accelerometerDao.getUnsavedCount();
                getActivity().runOnUiThread(() -> {
                    homeViewModel.getLocationsSaved().setValue(locationsSaved);
                    homeViewModel.getLocationsUnsaved().setValue(locationsUnsaved);
                    homeViewModel.getAccelerometersSaved().setValue(accelerometersSaved);
                    homeViewModel.getAccelerometersUnsaved().setValue(accelerometersUnsaved);
                });
            } catch (Exception e) {
                Timber.e(e, "Error query db");
            }
        };

//        Todo use main view thread instead of timer
//        root.postDelayed()

        root.findViewById(R.id.home_send).setOnClickListener(v -> {
            Timber.d("Press send");
            transmitService.transmitLocations()
                    .subscribeOn(Schedulers.io())
                    .doOnComplete(() -> timerTask.run())
                    .subscribe(saveEntity -> Timber.d("Entity saved"),
                            e -> Timber.w(e, "Entity save error"),
                            () -> Timber.d("Complete"));
        });

        return root;
    }

    @Override
    public synchronized void onResume() {
        Timber.d("onResume() %s", getLifecycle().getCurrentState());
        super.onResume();
        long uiCheckSec = new MyPreferences(getContext()).getInt(R.string.ui_check_id, R.integer.ui_check_default);

        Timber.d("Check %s", uiCheckSec);
        timerTaskFuture = timer.scheduleAtFixedRate(timerTask, 0, uiCheckSec, TimeUnit.SECONDS);
    }

    @Override
    public void onPause() {
        Timber.d("onPause() %s", getLifecycle().getCurrentState());
        super.onPause();
        if (timerTaskFuture != null) {
            timerTaskFuture.cancel(false);
            timerTaskFuture = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        timer.shutdown();
    }
}