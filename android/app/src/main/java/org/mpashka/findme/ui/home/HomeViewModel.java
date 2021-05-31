package org.mpashka.findme.ui.home;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomeViewModel extends ViewModel {

    private MutableLiveData<Integer> locationsUnsaved = new MutableLiveData<>(0);
    private MutableLiveData<Integer> locationsSaved = new MutableLiveData<>(0);
    private MutableLiveData<Integer> accelerometersUnsaved = new MutableLiveData<>(0);
    private MutableLiveData<Integer> accelerometersSaved = new MutableLiveData<>(0);

    public MutableLiveData<Integer> getLocationsUnsaved() {
        return locationsUnsaved;
    }

    public MutableLiveData<Integer> getLocationsSaved() {
        return locationsSaved;
    }

    public MutableLiveData<Integer> getAccelerometersSaved() {
        return accelerometersSaved;
    }

    public MutableLiveData<Integer> getAccelerometersUnsaved() {
        return accelerometersUnsaved;
    }
}