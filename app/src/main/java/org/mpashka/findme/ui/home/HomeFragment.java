package org.mpashka.findme.ui.home;

import android.content.Context;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.mpashka.findme.DBHelper;
import org.mpashka.findme.MyPreferences;
import org.mpashka.findme.MyWorkManager;
import org.mpashka.findme.R;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;
    private ScheduledExecutorService timer;
    private Runnable timerTask;
    private ScheduledFuture<?> timerTaskFuture;
    private DBHelper dbHelper;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        final TextView locationsView = root.findViewById(R.id.text_home_locations);
        homeViewModel.getLocations().observe(getViewLifecycleOwner(), s -> locationsView.setText(String.valueOf(s)));
        final TextView accelerationsView = root.findViewById(R.id.text_home_accelerations);
        homeViewModel.getAccelerometers().observe(getViewLifecycleOwner(), s -> accelerationsView.setText(String.valueOf(s)));
        Context deviceContext = getContext().createDeviceProtectedStorageContext();
        dbHelper = new DBHelper(deviceContext);
        timer = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "MyTimer"));
        timerTask = () -> {
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                int locations = (int) DatabaseUtils.queryNumEntries(db, "location");
                int accelerometers = (int) DatabaseUtils.queryNumEntries(db, "accelerometer");
                db.close();
                getActivity().runOnUiThread(() -> {
                    homeViewModel.getLocations().setValue(locations);
                    homeViewModel.getAccelerometers().setValue(accelerometers);
                });
            } catch (Exception e) {
                Timber.e(e, "Error query db");
            }
        };
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
        dbHelper.close();
    }
}