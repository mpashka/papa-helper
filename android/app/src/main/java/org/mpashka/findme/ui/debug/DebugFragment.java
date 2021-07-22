package org.mpashka.findme.ui.debug;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import org.mpashka.findme.MainActivity;
import org.mpashka.findme.MyWorkManager;
import org.mpashka.findme.R;
import org.mpashka.findme.miband.MiBandManager;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

@AndroidEntryPoint
public class DebugFragment extends Fragment {

    @Inject
    MiBandManager miBandManager;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_debug, container, false);
        root.findViewById(R.id.btnDebug_heart).setOnClickListener(v -> miBandManager
                .readMiBandInfo()
//                    .observeOn(AndroidSchedulers.mainThread())
                .subscribe(i -> Timber.d("Mi band info %s", i)));

        root.findViewById(R.id.btnDebug_fcmToken).setOnClickListener(v -> getFirebaseToken());

        root.findViewById(R.id.btnDebug_start).setOnClickListener(v -> MyWorkManager.getInstance().startServices(getContext()));
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
                    Timber.d("FCM registration token: %s", token);
                    Toast.makeText(getContext(), token, Toast.LENGTH_SHORT).show();
                });
    }
}
