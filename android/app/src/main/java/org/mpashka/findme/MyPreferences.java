package org.mpashka.findme;

import android.content.Context;
import android.content.SharedPreferences;


public class MyPreferences {

    public static final String SETTINGS_NAME = MyWorkManager.NAME + "_preferences";

    private Context deviceContext;
    private SharedPreferences preferences;

    public MyPreferences(Context context) {
        deviceContext = context.createDeviceProtectedStorageContext();
        preferences = deviceContext.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE);
//        Timber.d("Database path %s", deviceContext.getDatabasePath("aaa"));
    }

    public int getInt(int resourceNameId, int defaultValueId) {
        String stringVal = preferences.getString(deviceContext.getString(resourceNameId), null);
        return stringVal != null ? Integer.parseInt(stringVal) : deviceContext.getResources().getInteger(defaultValueId);
    }

    public float getFloat(int resourceNameId, int defaultValueId) {
        String stringVal = preferences.getString(deviceContext.getString(resourceNameId), null);
        return stringVal != null ? Float.parseFloat(stringVal) : deviceContext.getResources().getFloat(defaultValueId);
    }

    public String getString(int resourceNameId, int defaultValueId) {
        return preferences.getString(deviceContext.getString(resourceNameId), deviceContext.getResources().getString(defaultValueId));
    }

    public boolean getBoolean(int resourceNameId, int defaultValueId) {
        return preferences.getBoolean(deviceContext.getString(resourceNameId), deviceContext.getResources().getBoolean(defaultValueId));
    }
}
