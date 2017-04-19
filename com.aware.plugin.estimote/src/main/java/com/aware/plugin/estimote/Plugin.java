package com.aware.plugin.estimote;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.aware.Applications;
import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.utils.Aware_Plugin;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.EstimoteSDK;
import com.estimote.sdk.Nearable;
import com.estimote.sdk.telemetry.EstimoteTelemetry;

import java.util.Arrays;
import java.util.List;

public class Plugin extends Aware_Plugin {

    private String mScanID;
    private String mEstimoteID;
    private BeaconManager mBeaconManager;

    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
    };

    // Nearable List:
    // Blue - Generic - c178a0d5583f2d23
    // Purple - Shoe -  ff7a63a36382217e
    // Green - Door -   090477fd0acda00f
    // Blue - Bicycle - ceeee16f3bb5b7d6
    // Yellow - Bag -   7dd4ce4765cfa161

    private List<String> mArrayStickers = Arrays.asList("c178a0d5583f2d23", "ff7a63a36382217e", "090477fd0acda00f",
                                                        "ceeee16f3bb5b7d6", "7dd4ce4765cfa161");

    @Override
    public void onCreate() {
        super.onCreate();

        TAG = "AWARE::"+getResources().getString(R.string.app_name);

        /**
         * Plugins share their current status, i.e., context using this method.
         * This method is called automatically when triggering
         * {@link Aware#ACTION_AWARE_CURRENT_CONTEXT}
         **/
        CONTEXT_PRODUCER = new ContextProducer() {
            @Override
            public void onContext() {
                //Broadcast your context here
            }
        };

        //Add permissions you need (Android M+).
        //By default, AWARE asks access to the #Manifest.permission.WRITE_EXTERNAL_STORAGE
        REQUIRED_PERMISSIONS.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_WIFI_STATE);
        REQUIRED_PERMISSIONS.add(Manifest.permission.BLUETOOTH);
        REQUIRED_PERMISSIONS.add(Manifest.permission.BLUETOOTH_ADMIN);
        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_FINE_LOCATION);

        mBeaconManager = new BeaconManager(getApplicationContext());
        mBeaconManager.setForegroundScanPeriod(500,0);
        mBeaconManager.setBackgroundScanPeriod(500,0);
        mBeaconManager.setNearableListener(new BeaconManager.NearableListener() {
            @Override
            public void onNearablesDiscovered(List<Nearable> list) {
                for (Nearable nearable : list) {
                    if (mArrayStickers.contains(nearable.identifier)) {
                        if(nearable.identifier.equals("c178a0d5583f2d23")) { mEstimoteID = "blue_generic"; }
                        else if(nearable.identifier.equals("ff7a63a36382217e")) { mEstimoteID = "purple_shoe"; }
                        else if(nearable.identifier.equals("090477fd0acda00f")) { mEstimoteID = "green_door"; }
                        else if(nearable.identifier.equals("ceeee16f3bb5b7d6")) { mEstimoteID = "blue_bicycle"; }
                        else { mEstimoteID = "yellow_bag"; }

                        ContentValues nearableData = new ContentValues();
                        nearableData.put(Provider.Nearable_Data.TIMESTAMP, System.currentTimeMillis());
                        nearableData.put(Provider.Nearable_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                        nearableData.put(Provider.Nearable_Data.ESTIMOTE_ID, nearable.identifier);
                        nearableData.put(Provider.Nearable_Data.ESTIMOTE_APPEARANCE, mEstimoteID);
                        nearableData.put(Provider.Nearable_Data.ESTIMOTE_BATTERY, nearable.batteryLevel.toString());
                        nearableData.put(Provider.Nearable_Data.TEMPERATURE, Double.toString(nearable.temperature));
                        nearableData.put(Provider.Nearable_Data.X_ACCELERATION, Double.toString(nearable.xAcceleration));
                        nearableData.put(Provider.Nearable_Data.Y_ACCELERATION, Double.toString(nearable.yAcceleration));
                        nearableData.put(Provider.Nearable_Data.Z_ACCELERATION, Double.toString(nearable.zAcceleration));
                        nearableData.put(Provider.Nearable_Data.IS_MOVING, Boolean.toString(nearable.isMoving));
                        getContentResolver().insert(Provider.Nearable_Data.CONTENT_URI, nearableData);
                    }
                }
            }
        });
        mBeaconManager.setTelemetryListener(new BeaconManager.TelemetryListener() {
            @Override
            public void onTelemetriesFound(List<EstimoteTelemetry> list) {
                for (EstimoteTelemetry tlm : list) {
                    ContentValues telemetryData = new ContentValues();
                    telemetryData.put(Provider.Telemetry_Data.TIMESTAMP, System.currentTimeMillis());
                    telemetryData.put(Provider.Telemetry_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                    telemetryData.put(Provider.Telemetry_Data.ESTIMOTE_ID, String.valueOf(tlm.deviceId));
                    telemetryData.put(Provider.Telemetry_Data.ESTIMOTE_APPEARANCE, "Bacon!");
                    telemetryData.put(Provider.Telemetry_Data.ESTIMOTE_BATTERY, tlm.batteryPercentage.toString());
                    telemetryData.put(Provider.Telemetry_Data.TEMPERATURE, Double.toString(tlm.temperature));
                    telemetryData.put(Provider.Telemetry_Data.AMBIENT_LIGHT, tlm.ambientLight);
                    telemetryData.put(Provider.Telemetry_Data.MAGNETOMETER, String.valueOf(tlm.magnetometer));
                    telemetryData.put(Provider.Telemetry_Data.PRESSURE, tlm.pressure);
                    telemetryData.put(Provider.Telemetry_Data.IS_MOVING, Boolean.toString(tlm.motionState));
                    getContentResolver().insert(Provider.Telemetry_Data.CONTENT_URI, telemetryData);
                }
            }
        });

        EstimoteSDK.initialize(this, "care-estimotes-b0n", "6a749930b80298c5dbb16af6c9709da6");

        //To sync data to the server, you'll need to set this variables from your ContentProvider
        DATABASE_TABLES = Provider.DATABASE_TABLES;
        TABLES_FIELDS = Provider.TABLES_FIELDS;
        CONTEXT_URIS = new Uri[]{ Provider.Nearable_Data.CONTENT_URI }; //this syncs dummy Nearable_Data to server

    }

    //This function gets called every 5 minutes by AWARE to make sure this plugin is still running.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //super.onStartCommand(intent, flags, startId);

        Aware.setSetting(this, Settings.STATUS_PLUGIN_ESTIMOTE, true);
        DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

        // Should be invoked in #onStart.
        mBeaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                mScanID = mBeaconManager.startNearableDiscovery();
                mScanID = mBeaconManager.startTelemetryDiscovery();
            }
        });

        Applications.isAccessibilityServiceActive(getApplicationContext());
        if (!Aware.isStudy(this)) Aware.joinStudy(getApplicationContext(), "https://api.awareframework.com/index.php/webservice/index/1172/J3msPlz1wsCb");

        //Initialise AWARE instance in plugin
        Aware.startAWARE(this);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBeaconManager.stopNearableDiscovery(mScanID);
        mBeaconManager.stopTelemetryDiscovery(mScanID);

        mBeaconManager.disconnect();
        Aware.setSetting(this, Settings.STATUS_PLUGIN_ESTIMOTE, false);

        //Stop AWARE instance in plugin
        Aware.stopAWARE(this);
    }
}