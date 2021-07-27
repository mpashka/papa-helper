package org.mpashka.findme.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.mpashka.findme.R;
import org.mpashka.findme.services.MyState;
import org.mpashka.findme.services.MyTransmitService;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

@AndroidEntryPoint
public class HomeFragment extends Fragment {

    private static final DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Inject
    MyState state;

    @Inject
    MyTransmitService transmitService;

    private Disposable createdDisposable;
    private Disposable transmittedDisposable;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        ((TextView) root.findViewById(R.id.text_home_started_value)).setText(format.format(new Date(state.getCreated())));

        final TextView locationsSavedView = root.findViewById(R.id.text_home_locations_saved);
        final TextView locationsUnsavedView = root.findViewById(R.id.text_home_locations_unsaved);

        createdDisposable = state.getCreatedSubject()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(c -> locationsSavedView.setText(String.valueOf(c)))
                .subscribe();

        transmittedDisposable = state.getPendingSubject()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(c -> locationsUnsavedView.setText(String.valueOf(c)))
                .subscribe();

        root.findViewById(R.id.home_locate).setOnClickListener(v -> {
            Timber.d("Home.locate");
            // todo
        });
        root.findViewById(R.id.home_send).setOnClickListener(v -> {
            Timber.d("Home.send");
            transmitService.transmitLocations()
                    .subscribeOn(Schedulers.io())
                    .subscribe(saveEntity -> Timber.d("Entity saved"),
                            e -> Timber.w(e, "Entity save error"),
                            () -> Timber.d("Complete"));
        });

        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        createdDisposable.dispose();
        transmittedDisposable.dispose();
    }
}
