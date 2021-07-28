package org.mpashka.findme.ui.permissions;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.google.android.material.snackbar.Snackbar;

import org.mpashka.findme.services.MyWorkManager;
import org.mpashka.findme.R;
import org.mpashka.findme.Utils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

@AndroidEntryPoint
public class PermissionsFragment extends Fragment {

    @Inject
    MyWorkManager myWorkManager;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_permissions, container, false);
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        Fragment settings = new PermissionsSettingsFragment(root, myWorkManager);
        transaction.add(R.id.permissions_fragment_container_view, settings);
        transaction.commit();
        return root;
    }

    public static class PermissionsSettingsFragment extends PreferenceFragmentCompat {
        private ActivityResultLauncher<String> requestPermissionLauncher;
        private List<UiPermissionInfo> uiPermissions = new ArrayList<>();
        private List<UiPredicateInfo> uiPredicates = new ArrayList<>();
        private View rootView;
        private MyWorkManager myWorkManager;

        public PermissionsSettingsFragment(View rootView, MyWorkManager myWorkManager) {
            this.rootView = rootView;
            this.myWorkManager = myWorkManager;
        }

        @SuppressLint("NewApi")
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.permissions_preferences, rootKey);
            addPermissionSwitch(R.string.permission_location_fine_id, Manifest.permission.ACCESS_FINE_LOCATION);
            addPermissionSwitch(R.string.permission_location_coarse_id, Manifest.permission.ACCESS_COARSE_LOCATION);
            addPermissionSwitch(R.string.permission_location_background_id, Manifest.permission.ACCESS_BACKGROUND_LOCATION);
            addPermissionSwitch(R.string.permission_activity_recognition_id, Manifest.permission.ACCESS_BACKGROUND_LOCATION);

            Context context = getContext();
            if (context != null) {
                ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                addPredicateSwitch(R.string.predicate_background_restricted_id, Build.VERSION_CODES.P, s -> activityManager.isBackgroundRestricted());
                addPredicateSwitch(R.string.predicate_lock_task_mode_id, Build.VERSION_CODES.BASE, s -> activityManager.isInLockTaskMode());
                addPredicateSwitch(R.string.predicate_low_memory_kill_report_supported_id, Build.VERSION_CODES.BASE, s -> ActivityManager.isLowMemoryKillReportSupported());
                addPredicateSwitch(R.string.predicate_low_ram_device_id, Build.VERSION_CODES.BASE, s -> activityManager.isLowRamDevice());
            }
        }

        @Override
        public void onStart() {
            super.onStart();
            requestPermissionLauncher =
                    registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                        Timber.i("onRequestPermissionsResult");
                        Snackbar.make(rootView,
                                isGranted
                                        ? "Permission successfully granted"
                                        : "Permission not granted"
                                , Snackbar.LENGTH_LONG)
                                .setAction(R.string.snack_ok, v -> Timber.i("Snack ok"))
                                .show();
                        update();
                        if (isGranted) {
                            myWorkManager.startServices();
                        }
                    });
        }

        @Override
        public void onStop() {
            super.onStop();
            requestPermissionLauncher.unregister();
        }

        private void update() {
            for (UiPermissionInfo uiPermission : uiPermissions) {
                uiPermission.update();
            }
            for (UiPredicateInfo uiPredicate : uiPredicates) {
                uiPredicate.update();
            }
        }

        private void addPermissionSwitch(int uiId, String permission) {
            String stringId = getString(uiId);
            SwitchPreference preference = findPreference(stringId);
            if (preference != null) {
                UiPermissionInfo uiPermissionInfo = new UiPermissionInfo(preference, permission);
                uiPermissionInfo.update();
                uiPermissions.add(uiPermissionInfo);
            } else {
                Timber.e("Can't find preference %s/%s", uiId, stringId);
            }
        }

        private void addPredicateSwitch(int uiId, int minVersion, Utils.MyPredicate<String> predicate) {
            String predicateId = getString(uiId);
            SwitchPreference preference = findPreference(predicateId);
            if (preference != null) {
                if (Build.VERSION.SDK_INT < minVersion) {
                    preference.setVisible(false);
                    return;
                }
                UiPredicateInfo uiPredicateInfo = new UiPredicateInfo(preference, predicateId, predicate);
                uiPredicates.add(uiPredicateInfo);
                uiPredicateInfo.update();
            } else {
                Timber.e("Can't find preference %s/%s", uiId, predicateId);
            }
        }

        private class UiPermissionInfo {
            private SwitchPreference ui;
            private String permission;

            public UiPermissionInfo(SwitchPreference ui, String permission) {
                this.ui = ui;
                this.permission = permission;
                ui.setOnPreferenceClickListener(preference -> {
                    Timber.i("onPreferenceClick. Checked %s. Granted %s. Should show %s"
                            , ui.isChecked(), isGranted(), isShowRationale());
                    if (ui.isChecked() && !isGranted()) {
                        if (isShowRationale()) {
                            Snackbar.make(rootView, String.format("Permission '%s' required", permission), Snackbar.LENGTH_LONG)
                                    .setAction(R.string.snack_ok, v -> {
                                        Timber.i("Permission rationale shown");
                                        requestPermissionLauncher.launch(permission);
                                    })
                                    .show();
                        } else {
                            requestPermissionLauncher.launch(permission);
                        }
                    }
                    return true;
                });
            }

            public void update() {
                ui.setChecked(isGranted());
            }

            private boolean isGranted() {
                Context context = getContext();
                return context != null && ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
            }

            public boolean isShowRationale() {
                return shouldShowRequestPermissionRationale(permission);
            }
        }

        private class UiPredicateInfo {
            private SwitchPreference ui;
            private String id;
            private Utils.MyPredicate<String> predicate;

            public UiPredicateInfo(SwitchPreference ui, String id, Utils.MyPredicate<String> predicate) {
                this.ui = ui;
                this.id = id;
                this.predicate = predicate;
            }

            public void update() {
                ui.setChecked(predicate.test(id));
            }
        }
    }
}
