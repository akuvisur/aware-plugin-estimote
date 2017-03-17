package com.aware.plugin.estimote;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.utils.Aware_Plugin;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.EstimoteSDK;
import com.estimote.sdk.Nearable;
import com.estimote.sdk.telemetry.EstimoteTelemetry;

import java.util.Arrays;
import java.util.List;

import static java.lang.Math.round;

public class Plugin extends Aware_Plugin {

    private String mScanID;
    private BeaconManager mBeaconManager;
    private List<String> mArrayStickers = Arrays.asList("4493eef642ecd8bd", "ef71c3d5da7eb884");

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
        mBeaconManager.setForegroundScanPeriod(800, 0);
        mBeaconManager.setBackgroundScanPeriod(800,0);
       // mBeaconManager.
        mBeaconManager.setNearableListener(new BeaconManager.NearableListener() {
            @Override
            public void onNearablesDiscovered(List<Nearable> list) {
                for (Nearable nearable : list) {

                    if (mArrayStickers.contains(nearable.identifier)) {
                    Log.d("ABC 11", "Packet Found !!!");
                        ContentValues estimoteData = new ContentValues();
                        estimoteData.put(Provider.Estimote_Data.TIMESTAMP, System.currentTimeMillis());
                        estimoteData.put(Provider.Estimote_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                        estimoteData.put(Provider.Estimote_Data.ESTIMOTE_ID, nearable.identifier);
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


        mBeaconManager.setTelemetryListener(new BeaconManager.TelemetryListener() {
            @Override
            public void onTelemetriesFound(List<EstimoteTelemetry> telemetries) {
                for (EstimoteTelemetry tlm : telemetries) {
                    Log.d("ABC 22", "Beacon !!!");

                    //Log.d("TELEMETRY", "beaconID: " + tlm.deviceId +
                      //    ", temperature: " + tlm.temperature + " °C");
                                                     //  934d0df6aca78dcd75b02d8ee9a0d814
                    if(tlm.deviceId.toString().equals("[934d0df6aca78dcd75b02d8ee9a0d814]")) {
                        Log.d("TELEMETRY",
                                "Beacon ID : " + tlm.deviceId +
                                ", Temperature : " + tlm.temperature + " °C" +
                                ", Accelerometer : " + tlm.accelerometer +
                                ", Lux : " + tlm.ambientLight +
                                ", Motion state: " +  tlm.motionState);
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
                    mScanID = mBeaconManager.startTelemetryDiscovery();
                    //mScanID = mBeaconManager.startNearableDiscovery();
                }
            });

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
