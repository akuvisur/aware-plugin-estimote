package com.aware.plugin.estimote;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.aware.Aware;

public class Settings extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    //Plugin settings in XML @xml/preferences
    public static final String STATUS_PLUGIN_TEMPLATE = "status_plugin_template";
    public static final String ESTIMOTE_NEARABLE = "estimote_nearable";

    //Plugin settings UI elements
    private static CheckBoxPreference status;
    private static CheckBoxPreference nearable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        status = (CheckBoxPreference) findPreference(STATUS_PLUGIN_TEMPLATE);
        if( Aware.getSetting(this, STATUS_PLUGIN_TEMPLATE).length() == 0 ) {
            Aware.setSetting( this, STATUS_PLUGIN_TEMPLATE, true ); //by default, the setting is true on install
        }
        status.setChecked(Aware.getSetting(getApplicationContext(), STATUS_PLUGIN_TEMPLATE).equals("true"));

        nearable = (CheckBoxPreference) findPreference(ESTIMOTE_NEARABLE);
        if( Aware.getSetting(this, ESTIMOTE_NEARABLE).length() == 0 ) {
            Aware.setSetting( this, ESTIMOTE_NEARABLE, true ); //by default, the setting is true on install
        }
        nearable.setChecked(Aware.getSetting(getApplicationContext(), ESTIMOTE_NEARABLE).equals("true"));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference setting = findPreference(key);

        if( setting.getKey().equals(STATUS_PLUGIN_TEMPLATE) ) {
            Aware.setSetting(this, key, sharedPreferences.getBoolean(key, false));
            status.setChecked(sharedPreferences.getBoolean(key, false));
        }
        if( setting.getKey().equals(ESTIMOTE_NEARABLE) ) {
            Aware.setSetting(this, key, sharedPreferences.getBoolean(key, false));
            nearable.setChecked(sharedPreferences.getBoolean(key, false));
        }

        if (Aware.getSetting(this, STATUS_PLUGIN_TEMPLATE).equals("true")) {
            Aware.startPlugin(getApplicationContext(), "com.aware.plugin.estimote");
        } else {
            Aware.stopPlugin(getApplicationContext(), "com.aware.plugin.estimote");
        }
    }
}
