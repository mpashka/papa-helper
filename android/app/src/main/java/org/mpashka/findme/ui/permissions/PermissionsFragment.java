package org.mpashka.findme.ui.permissions;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import org.mpashka.findme.MyWorkManager;
import org.mpashka.findme.R;

import java.util.Arrays;
import java.util.List;

import timber.log.Timber;

public class PermissionsFragment extends Fragment {

    private static final List<String> LOCATION_PERMISSIONS = Arrays.asList(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
//            Manifest.permission.ACCESS_BACKGROUND_LOCATION
    );

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_permissions, container, false);
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        Fragment settings = new PremissionsSettingsFragment();
        transaction.add(R.id.permissions_fragment_container_view, settings);
        transaction.commit();

/*

todo add

        isBackgroundRestricted
                isInLockTaskMode
        isLowMemoryKillReportSupported
        https://developer.android.com/reference/android/app/ActivityManager#isBackgroundRestricted()
*/

        return root;
    }

    public static class PremissionsSettingsFragment extends PreferenceFragmentCompat {
        private SwitchPreference location;
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.permissions_preferences, rootKey);
            location = findPreference(getString(R.string.location_provider_id));
            location.setChecked(isLocationGranted());
            location.setOnPreferenceClickListener(preference -> {
                Timber.d("onPreferenceClick thread-%s. Checked %s. Granted %s. Should show %s", Thread.currentThread().getName()
                        , location.isChecked(), isLocationGranted(), shouldShowLocationPermission());
                if (true /*location.isChecked()
                        && !isLocationGranted()
                        && shouldShowLocationPermission()*/)
                {
                    requestPermissions(LOCATION_PERMISSIONS.toArray(new String[0]), 1);
                }
                location.setChecked(isLocationGranted());
                return true;
            });
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            Timber.d("onRequestPermissionsResult");
            location.setChecked(isLocationGranted());
            MyWorkManager.getInstance().restartLocationService(getContext());
        }

        private boolean shouldShowLocationPermission() {
            return LOCATION_PERMISSIONS.stream().anyMatch(this::shouldShowRequestPermissionRationale);
        }

        private boolean isLocationGranted() {
            return LOCATION_PERMISSIONS.stream().anyMatch(p -> ContextCompat.checkSelfPermission(getContext(), p) == PackageManager.PERMISSION_GRANTED);
        }
    }
}
