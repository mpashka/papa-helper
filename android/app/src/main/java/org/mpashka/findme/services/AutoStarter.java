package org.mpashka.findme.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.os.UserManagerCompat;

import org.mpashka.findme.ui.MainActivity;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

@AndroidEntryPoint
public class AutoStarter extends BroadcastReceiver {

    @Inject
    MyWorkManager myWorkManager;

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Timber.i("onReceive %s, user unlocked: %s", action, UserManagerCompat.isUserUnlocked(context));
        if (Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)) {
            myWorkManager.startIfNeeded();
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            // Show permissions dialog if needed after boot
            if (!myWorkManager.checkPermissions() && UserManagerCompat.isUserUnlocked(context)) {
                Intent permissionsIntent = new Intent(context, MainActivity.class);
                context.startActivity(permissionsIntent);
            }
        }
    }
}
