package org.mpashka.findme.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import org.mpashka.findme.MyPreferences;
import org.mpashka.findme.services.MyActivityService;
import org.mpashka.findme.services.MyLocationFuseService;
import org.mpashka.findme.services.MyLocationService;
import org.mpashka.findme.services.MyWorkManager;
import org.mpashka.findme.R;
import org.mpashka.findme.services.MyTransmitService;

import javax.inject.Inject;

import timber.log.Timber;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject
    MyPreferences preferences;

    @Inject
    MyTransmitService transmitService;

    @Inject
    MyWorkManager myWorkManager;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setStorageDeviceProtected();
        preferenceManager.setSharedPreferencesName(MyPreferences.SETTINGS_NAME);
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    public void onDestroy() {
        super.onDestroy();
        Timber.i("Fragment1 onDestroy");
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.startsWith("restart_check_id")) {
            myWorkManager.start();
        } else if (key.startsWith("location_gen_")) {
            myWorkManager.reloadService(MyLocationService.class);
        } else if (key.startsWith("location_fuse_")) {
            myWorkManager.reloadService(MyLocationFuseService.class);
        } else if (key.startsWith("activity_provider_")) {
            myWorkManager.reloadService(MyActivityService.class);
        } else if (key.equals(getContext().getString(R.string.send_debug_http_id))) {
            transmitService.createApi();
        }
    }

/*
    public void onStart() {
        super.onStart();
        Timber.i("Fragment1 onStart");
    }

    public void onStop() {
        super.onStop();
        Timber.i("Fragment1 onStop");
    }

    public void onDestroyView() {
        super.onDestroyView();
        Timber.i("Fragment1 onDestroyView");
    }

    public void onDetach() {
        super.onDetach();
        Timber.i("Fragment1 onDetach");
    }
*/
}
