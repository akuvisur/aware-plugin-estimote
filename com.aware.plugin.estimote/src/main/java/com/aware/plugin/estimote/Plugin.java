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

    // Nearable List:
    // Blue - Shoe - be13971311515b72
    // Blue - Door - dd882f70252e1ee4
    // Green - Bag - 31812b2646a7e655
    // Purple - Dog - ef71c3d5da7eb884
    // Yellow - Chair - 9d3dd0f95e0ea304
    // Pink - Bed - fc9202bbbcb48a8f

    private List<String> mArrayStickers = Arrays.asList("be13971311515b72", "dd882f70252e1ee4", "31812b2646a7e655",
                                                        "ef71c3d5da7eb884", "9d3dd0f95e0ea304", "fc9202bbbcb48a8f");

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
        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        REQUIRED_PERMISSIONS.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        REQUIRED_PERMISSIONS.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        REQUIRED_PERMISSIONS.add(Manifest.permission.BLUETOOTH_ADMIN);

        //To sync data to the server, you'll need to set this variables from your ContentProvider
        DATABASE_TABLES = Provider.DATABASE_TABLES;
        TABLES_FIELDS = Provider.TABLES_FIELDS;
        CONTEXT_URIS = new Uri[]{ Provider.Estimote_Data.CONTENT_URI }; //this syncs dummy Estimote_Data to server

        EstimoteSDK.initialize(this, "care-estimotes-b0n", "6a749930b80298c5dbb16af6c9709da6");
        mBeaconManager = new BeaconManager(getApplicationContext());
        mBeaconManager.setForegroundScanPeriod(100,0);
        mBeaconManager.setBackgroundScanPeriod(100,0);
        mBeaconManager.setNearableListener(new BeaconManager.NearableListener() {
            @Override
            public void onNearablesDiscovered(List<Nearable> list) {
                for (Nearable nearable : list) {

                    if (mArrayStickers.contains(nearable.identifier)) {

                        if(nearable.identifier.equals("be13971311515b72")) { mEstimoteID = "blue_shoe"; }
                        else if(nearable.identifier.equals("dd882f70252e1ee4")) { mEstimoteID = "blue_door"; }
                        else if(nearable.identifier.equals("31812b2646a7e655")) { mEstimoteID = "green_bag"; }
                        else if(nearable.identifier.equals("ef71c3d5da7eb884")) { mEstimoteID = "purple_dog"; }
                        else if(nearable.identifier.equals("9d3dd0f95e0ea304")) { mEstimoteID = "yellow_chair"; }
                        else { mEstimoteID = "pink_bed"; } //fc9202bbbcb48a8f

                        ContentValues estimoteData = new ContentValues();
                        estimoteData.put(Provider.Estimote_Data.TIMESTAMP, System.currentTimeMillis());
                        estimoteData.put(Provider.Estimote_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                        estimoteData.put(Provider.Estimote_Data.ESTIMOTE_ID, nearable.identifier);
                        estimoteData.put(Provider.Estimote_Data.ESTIMOTE_APPEARANCE, mEstimoteID);
                        estimoteData.put(Provider.Estimote_Data.ESTIMOTE_BATTERY, nearable.batteryLevel.toString());
                        estimoteData.put(Provider.Estimote_Data.TEMPERATURE, Double.toString(nearable.temperature));
                        estimoteData.put(Provider.Estimote_Data.X_ACCELERATION, Double.toString(nearable.xAcceleration));
                        estimoteData.put(Provider.Estimote_Data.Y_ACCELERATION, Double.toString(nearable.yAcceleration));
                        estimoteData.put(Provider.Estimote_Data.Z_ACCELERATION, Double.toString(nearable.zAcceleration));
                        estimoteData.put(Provider.Estimote_Data.IS_MOVING, Boolean.toString(nearable.isMoving));
                        getContentResolver().insert(Provider.Estimote_Data.CONTENT_URI, estimoteData);
                    }
                }
            }
        });
    }

    //This function gets called every 5 minutes by AWARE to make sure this plugin is still running.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (PERMISSIONS_OK) {

            DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

            //Initialize our plugin's settings
            Aware.setSetting(this, Settings.STATUS_PLUGIN_TEMPLATE, true);

            // Should be invoked in #onStart.
            mBeaconManager.connect(new BeaconManager.ServiceReadyCallback() {
                @Override public void onServiceReady() {
                    mScanID = mBeaconManager.startNearableDiscovery();
                }
            });

            Applications.isAccessibilityServiceActive(getApplicationContext());
            if (!Aware.isStudy(this)) Aware.joinStudy(getApplicationContext(), "https://api.awareframework.com/index.php/webservice/index/1168/t9dqX3BWpbX9");

            //Initialise AWARE instance in plugin
            Aware.startPlugin(this, "com.aware.plugin.estimote");
            Aware.startAWARE(this);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mBeaconManager.disconnect();
        Aware.setSetting(this, Settings.STATUS_PLUGIN_TEMPLATE, false);

        //Stop AWARE instance in plugin
        Aware.stopAWARE(this);
    }
}
