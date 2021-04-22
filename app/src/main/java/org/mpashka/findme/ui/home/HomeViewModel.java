package org.mpashka.findme.ui.home;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomeViewModel extends ViewModel {

    private MutableLiveData<Integer> locations = new MutableLiveData<>(0);
    private MutableLiveData<Integer> accelerometers = new MutableLiveData<>(0);

    public MutableLiveData<Integer> getLocations() {
        return locations;
    }

    public MutableLiveData<Integer> getAccelerometers() {
        return accelerometers;
    }
}