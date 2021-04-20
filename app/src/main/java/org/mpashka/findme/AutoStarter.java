package org.mpashka.findme;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.os.UserManagerCompat;

import timber.log.Timber;

public class AutoStarter extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Timber.d("onReceive %s, user unlocked: %s", action, UserManagerCompat.isUserUnlocked(context));
        if (!Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)) {
            return;
        }
        MyWorkManager.getInstance().scheduleCheck(context);
    }
}
