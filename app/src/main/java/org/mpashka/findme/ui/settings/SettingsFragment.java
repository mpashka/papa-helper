package org.mpashka.findme.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.work.WorkManager;

import org.mpashka.findme.MyWorkManager;
import org.mpashka.findme.R;

import timber.log.Timber;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setStorageDeviceProtected();
        preferenceManager.setSharedPreferencesName(MyWorkManager.SETTINGS_NAME);
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
    }

    public void onResume() {
        super.onResume();
        Timber.d("Fragment1 onResume");
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    public void onPause() {
        super.onPause();
        Timber.d("Fragment1 onPause");
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.poll_period_id))) {
            MyWorkManager.getInstance().rescheduleAccelerometerCheck(getContext());
        } else if (key.startsWith("location")) {
            MyWorkManager.getInstance().restartLocationService(getContext());
        }
    }

/*
    public void onStart() {
        super.onStart();
        Timber.d("Fragment1 onStart");
    }

    public void onStop() {
        super.onStop();
        Timber.d("Fragment1 onStop");
    }

    public void onDestroyView() {
        super.onDestroyView();
        Timber.d("Fragment1 onDestroyView");
    }

    public void onDestroy() {
        super.onDestroy();
        Timber.d("Fragment1 onDestroy");
    }

    public void onDetach() {
        super.onDetach();
        Timber.d("Fragment1 onDetach");
    }
*/
}
