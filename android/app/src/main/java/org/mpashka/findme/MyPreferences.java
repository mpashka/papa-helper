package org.mpashka.findme;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.util.TypedValue;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class MyPreferences {

    public static final String SETTINGS_NAME = MyApplication.NAME + "_preferences";

    private Context deviceContext;
    private SharedPreferences preferences;

    @Inject
    public MyPreferences(@ApplicationContext Context applicationContext) {
        deviceContext = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                ? applicationContext.createDeviceProtectedStorageContext()
                : applicationContext;

        preferences = deviceContext.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE);
//        Timber.i("Database path %s", deviceContext.getDatabasePath("aaa"));
    }

    public SharedPreferences getPreferences() {
        return preferences;
    }

    public int getInt(int resourceNameId, int defaultValueId) {
        String stringVal = preferences.getString(deviceContext.getString(resourceNameId), null);
        return stringVal != null ? Integer.parseInt(stringVal) : deviceContext.getResources().getInteger(defaultValueId);
    }

    public float getFloat(int resourceNameId, int defaultValueId) {
        String stringVal = preferences.getString(deviceContext.getString(resourceNameId), null);
        if (stringVal != null) {
            return Float.parseFloat(stringVal);
        }
        TypedValue outValue = new TypedValue();
        deviceContext.getResources().getValue(defaultValueId, outValue, true);
        if (outValue.type != TypedValue.TYPE_FLOAT) {
            throw new Resources.NotFoundException("Resource default value ID #0x" + Integer.toHexString(defaultValueId)
                    + " type #0x" + Integer.toHexString(outValue.type) + " is not valid float");
        }
        return outValue.getFloat();
    }

    public String getString(int resourceNameId, int defaultValueId) {
        return preferences.getString(deviceContext.getString(resourceNameId), deviceContext.getResources().getString(defaultValueId));
    }

    public boolean getBoolean(int resourceNameId, int defaultValueId) {
        return preferences.getBoolean(deviceContext.getString(resourceNameId), deviceContext.getResources().getBoolean(defaultValueId));
    }
}
