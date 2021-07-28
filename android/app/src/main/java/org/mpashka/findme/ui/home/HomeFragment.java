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
import org.mpashka.findme.services.MyWorkManager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

@AndroidEntryPoint
public class HomeFragment extends Fragment {

    private static final DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    @Inject
    MyState state;

    @Inject
    MyTransmitService transmitService;

    @Inject
    MyWorkManager workManager;

    private CompositeDisposable disposable;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        ((TextView) root.findViewById(R.id.text_home_started_value)).setText(format.format(new Date(state.getStarted())));

        final TextView locationsSavedView = root.findViewById(R.id.text_home_locations_count);
        final TextView locationsUnsavedView = root.findViewById(R.id.text_home_pending_count);
        final TextView createdTimeView = root.findViewById(R.id.text_home_created_date);
        final TextView transmittedTimeView = root.findViewById(R.id.text_home_transmitted_date);

        disposable = new CompositeDisposable();
        disposable.add(state.getCreatedSubject()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(c -> locationsSavedView.setText(String.valueOf(c)))
                .subscribe());

        disposable.add(state.getPendingSubject()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(c -> locationsUnsavedView.setText(String.valueOf(c)))
                .subscribe());

        disposable.add(state.getCreateTimeSubject()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(c -> createdTimeView.setText(timeFormat.format(new Date(c))))
                .subscribe());

        disposable.add(state.getTransmitTimeSubject()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(c -> transmittedTimeView.setText(timeFormat.format(new Date(c))))
                .subscribe());

        root.findViewById(R.id.home_locate).setOnClickListener(v -> {
            Timber.d("Home.locate");
            workManager.fetchCurrentLocation();
        });
        root.findViewById(R.id.home_send).setOnClickListener(v -> {
            Timber.d("Home.send");
            transmitService.transmitPending()
                    .subscribeOn(Schedulers.io())
                    .subscribe(saveEntity -> Timber.d("Entity saved"),
                            e -> Timber.w(e, "Entity save error"));
        });

        return root;
    }

    @Override
    public void onDestroy() {
        disposable.dispose();
        super.onDestroy();
    }
}
