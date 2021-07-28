package org.mpashka.findme.ui.debug;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.messaging.FirebaseMessaging;

import org.mpashka.findme.services.MyWorkManager;
import org.mpashka.findme.R;
import org.mpashka.findme.miband.MiBandManager;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

@AndroidEntryPoint
public class DebugFragment extends Fragment {

    @Inject
    MyWorkManager workManager;

    @Inject
    MiBandManager miBandManager;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_debug, container, false);
        root.findViewById(R.id.btnDebug_heart).setOnClickListener(v -> miBandManager
                .readMiBandInfo()
//                    .observeOn(AndroidSchedulers.mainThread())
                .subscribe(i -> Timber.i("Mi band info %s", i)));

        root.findViewById(R.id.btnDebug_fcmToken).setOnClickListener(v -> getFirebaseToken());

        root.findViewById(R.id.btnDebug_start).setOnClickListener(v -> workManager.startIfNeeded());
        return root;
    }

    private void getFirebaseToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Timber.w(task.getException(), "Fetching FCM registration token failed ");
                        return;
                    }

                    // Get new FCM registration token
                    String token = task.getResult();

                    // Log and toast
                    Timber.i("FCM registration token: %s", token);
                    Toast.makeText(getContext(), token, Toast.LENGTH_SHORT).show();
                });
    }
}
