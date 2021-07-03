package org.mpashka.findme.ui.debug;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

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
        return root;
    }
}
