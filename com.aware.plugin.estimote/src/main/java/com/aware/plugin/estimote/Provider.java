package com.aware.plugin.estimote;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;
import android.util.Log;

import com.aware.Aware;
import com.aware.utils.DatabaseHelper;

import java.util.HashMap;

public class Provider extends ContentProvider {

    public static String AUTHORITY = "com.aware.plugin.estimote.provider.estimote"; //change to package.provider.your_plugin_name

    public static final int DATABASE_VERSION = 18; //increase this if you make changes to the database structure, i.e., rename columns, etc.
    public static final String DATABASE_NAME = "plugin_estimote.db"; //the database filename, use plugin_xxx for plugins.

    //Add here your database table names, as many as you need
    public static final String DB_TBL_NEARABLE = "nearable_data";
    public static final String DB_TBL_TELEMETRY = "telemetry_data";

    //For each table, add two indexes: DIR and ITEM. The index needs to always increment. Next one is 3, and so on.
    private static final int NEARABLE_ONE_DIR = 1;
    private static final int NEARABLE_ONE_ITEM = 2;
    private static final int TELEMETRY_TWO_DIR = 3;
    private static final int TELEMETRY_TWO_ITEM = 4;

    //Put tables names in this array so AWARE knows what you have on the database
    public static final String[] DATABASE_TABLES = {
            DB_TBL_NEARABLE,
            DB_TBL_TELEMETRY
    };

    //These are columns that we need to sync data, don't change this!
    public interface AWAREColumns extends BaseColumns {
        String _ID = "_id";
        String TIMESTAMP = "timestamp";
        String DEVICE_ID = "device_id";
    }

    /**
     * Create one of these per database table
     * In this example, we are adding example columns
     */
    public static final class Nearable_Data implements AWAREColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + DB_TBL_NEARABLE);
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.com.aware.plugin.estimote.provider.nearable_data"; //modify me
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.com.aware.plugin.estimote.provider.nearable_data"; //modify me

        //Note: integers and strings don't need a type prefix_
        public static final String ESTIMOTE_ID = "estimote_id";
        public static final String ESTIMOTE_APPEARANCE = "estimote_appearance";
        public static final String ESTIMOTE_BATTERY = "estimote_battery";
        public static final String TEMPERATURE = "temperature";
        public static final String X_ACCELERATION = "x_acceleration";
        public static final String Y_ACCELERATION = "y_acceleration";
        public static final String Z_ACCELERATION = "z_acceleration";
        public static final String IS_MOVING = "is_moving";
        public static final String RSSI = "rssi";
        public static final String COMPUTED_PROXIMITY = "computed_proximity";
    }

    public static final class Telemetry_Data implements AWAREColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + DB_TBL_TELEMETRY);
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.com.aware.plugin.estimote.provider.telemetry_data"; //modify me
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.com.aware.plugin.estimote.provider.telemetry_data"; //modify me

        //Note: integers and strings don't need a type prefix_
        public static final String ESTIMOTE_ID = "estimote_id";
        public static final String ESTIMOTE_APPEARANCE = "estimote_appearance";
        public static final String ESTIMOTE_BATTERY = "estimote_battery";
        public static final String TEMPERATURE = "temperature";
        public static final String AMBIENT_LIGHT = "ambient_light";
        public static final String MAGNETOMETER = "magnetometer";
        public static final String PRESSURE = "pressure";
        public static final String IS_MOVING = "is_moving";
    }


    //Define each database table fields
    private static final String DB_TBL_NEARABLE_FIELDS =
        Nearable_Data._ID + " integer primary key autoincrement," +
        Nearable_Data.TIMESTAMP + " real default 0," +
        Nearable_Data.DEVICE_ID + " text default ''," +
        Nearable_Data.ESTIMOTE_APPEARANCE + " text default ''," +
        Nearable_Data.ESTIMOTE_ID + " text default ''," +
        Nearable_Data.ESTIMOTE_BATTERY + " text default ''," +
        Nearable_Data.TEMPERATURE + " text default ''," +
        Nearable_Data.X_ACCELERATION + " text default ''," +
        Nearable_Data.Y_ACCELERATION + " text default ''," +
        Nearable_Data.Z_ACCELERATION + " text default ''," +
        Nearable_Data.IS_MOVING + " text default ''," +
        Nearable_Data.COMPUTED_PROXIMITY + " text default ''," +
        Nearable_Data.RSSI + " text default ''";

    //Define each database table fields
    private static final String DB_TBL_TELEMETRY_FIELDS =
        Telemetry_Data._ID + " integer primary key autoincrement," +
        Telemetry_Data.TIMESTAMP + " real default 0," +
        Telemetry_Data.DEVICE_ID + " text default ''," +
        Telemetry_Data.ESTIMOTE_APPEARANCE + " text default ''," +
        Telemetry_Data.ESTIMOTE_ID + " text default ''," +
        Telemetry_Data.ESTIMOTE_BATTERY + " text default ''," +
        Telemetry_Data.TEMPERATURE + " text default ''," +
        Telemetry_Data.AMBIENT_LIGHT + " text default ''," +
        Telemetry_Data.MAGNETOMETER + " text default ''," +
        Telemetry_Data.PRESSURE + " text default ''," +
        Telemetry_Data.IS_MOVING + " text default ''";

    /**
     * Share the fields with AWARE so we can replicate the table schema on the server
     */
    public static final String[] TABLES_FIELDS = {
            DB_TBL_NEARABLE_FIELDS,
            DB_TBL_TELEMETRY_FIELDS
    };

    //Helper variables for ContentProvider - DO NOT CHANGE
    private UriMatcher sUriMatcher;
    private DatabaseHelper dbHelper;
    private static SQLiteDatabase database;

    private void initialiseDatabase() {
        if (dbHelper == null)
        dbHelper = new DatabaseHelper(getContext(), DATABASE_NAME, null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS);
        if (database == null)
        database = dbHelper.getWritableDatabase();
    }
    //--

    public static String getAuthority(Context context) {
        AUTHORITY = context.getPackageName() + ".provider.estimote";
        return AUTHORITY;
    }

    //For each table, create a hashmap needed for database queries
    private HashMap<String, String> nearableHash;
    private HashMap<String, String> telemetryHash;

    @Override
    public boolean onCreate() {
        //This is a hack to allow providers to be reusable in any application/plugin by making the authority dynamic using the package name of the parent app
        AUTHORITY = getContext().getPackageName() + ".provider.estimote"; //make sure xxx matches the first string in this class

        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        //For each table, add indexes DIR and ITEM
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0], NEARABLE_ONE_DIR);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0] + "/#", NEARABLE_ONE_ITEM);

        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[1], TELEMETRY_TWO_DIR);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[1] + "/#", TELEMETRY_TWO_ITEM);

        //Create each table hashmap so Android knows how to insert data to the database. Put ALL table fields.
        nearableHash = new HashMap<>();
        nearableHash.put(Nearable_Data._ID, Nearable_Data._ID);
        nearableHash.put(Nearable_Data.TIMESTAMP, Nearable_Data.TIMESTAMP);
        nearableHash.put(Nearable_Data.DEVICE_ID, Nearable_Data.DEVICE_ID);
        nearableHash.put(Nearable_Data.ESTIMOTE_ID, Nearable_Data.ESTIMOTE_ID);
        nearableHash.put(Nearable_Data.ESTIMOTE_APPEARANCE, Nearable_Data.ESTIMOTE_APPEARANCE);
        nearableHash.put(Nearable_Data.ESTIMOTE_BATTERY, Nearable_Data.ESTIMOTE_BATTERY);
        nearableHash.put(Nearable_Data.TEMPERATURE, Nearable_Data.TEMPERATURE);
        nearableHash.put(Nearable_Data.X_ACCELERATION, Nearable_Data.X_ACCELERATION);
        nearableHash.put(Nearable_Data.Y_ACCELERATION, Nearable_Data.Y_ACCELERATION);
        nearableHash.put(Nearable_Data.Z_ACCELERATION, Nearable_Data.Z_ACCELERATION);
        nearableHash.put(Nearable_Data.IS_MOVING, Nearable_Data.IS_MOVING);
        nearableHash.put(Nearable_Data.RSSI, Nearable_Data.RSSI);
        nearableHash.put(Nearable_Data.COMPUTED_PROXIMITY, Nearable_Data.COMPUTED_PROXIMITY);

        telemetryHash = new HashMap<>();
        telemetryHash.put(Telemetry_Data._ID, Telemetry_Data._ID);
        telemetryHash.put(Telemetry_Data.TIMESTAMP, Telemetry_Data.TIMESTAMP);
        telemetryHash.put(Telemetry_Data.DEVICE_ID, Telemetry_Data.DEVICE_ID);
        telemetryHash.put(Telemetry_Data.ESTIMOTE_ID, Telemetry_Data.ESTIMOTE_ID);
        telemetryHash.put(Telemetry_Data.ESTIMOTE_APPEARANCE, Telemetry_Data.ESTIMOTE_APPEARANCE);
        telemetryHash.put(Telemetry_Data.ESTIMOTE_BATTERY, Telemetry_Data.ESTIMOTE_BATTERY);
        telemetryHash.put(Telemetry_Data.TEMPERATURE, Telemetry_Data.TEMPERATURE);
        telemetryHash.put(Telemetry_Data.AMBIENT_LIGHT, Telemetry_Data.AMBIENT_LIGHT);
        telemetryHash.put(Telemetry_Data.MAGNETOMETER, Telemetry_Data.MAGNETOMETER);
        telemetryHash.put(Telemetry_Data.PRESSURE, Telemetry_Data.PRESSURE);
        telemetryHash.put(Telemetry_Data.IS_MOVING, Telemetry_Data.IS_MOVING);

        return true;
}

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        initialiseDatabase();

        database.beginTransaction();

        int count;
        switch (sUriMatcher.match(uri)) {

            //Add each table DIR case, increasing the index accordingly
            case NEARABLE_ONE_DIR:
                count = database.delete(DATABASE_TABLES[0], selection, selectionArgs);
                break;
            case TELEMETRY_TWO_DIR:
                count = database.delete(DATABASE_TABLES[1], selection, selectionArgs);
                break;

            default:
                database.endTransaction();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        database.setTransactionSuccessful();
        database.endTransaction();

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {

        initialiseDatabase();

        ContentValues values = (initialValues != null) ? new ContentValues(initialValues) : new ContentValues();

        database.beginTransaction();

        switch (sUriMatcher.match(uri)) {

            //Add each table DIR case
            case NEARABLE_ONE_DIR:
                long _id = database.insert(DATABASE_TABLES[0], Nearable_Data.DEVICE_ID, values);
                database.setTransactionSuccessful();
                database.endTransaction();
                if (_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(Nearable_Data.CONTENT_URI, _id);
                    getContext().getContentResolver().notifyChange(dataUri, null);
                    return dataUri;
                }
                database.endTransaction();
                throw new SQLException("Failed to insert row into " + uri);
            case TELEMETRY_TWO_DIR:
                _id = database.insert(DATABASE_TABLES[1], Telemetry_Data.DEVICE_ID, values);
                database.setTransactionSuccessful();
                database.endTransaction();
                if (_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(Telemetry_Data.CONTENT_URI, _id);
                    getContext().getContentResolver().notifyChange(dataUri, null);
                    return dataUri;
                }
                database.endTransaction();
                throw new SQLException("Failed to insert row into " + uri);

            default:
                database.endTransaction();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        initialiseDatabase();

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (sUriMatcher.match(uri)) {

            //Add all tables' DIR entries, with the right table index
            case NEARABLE_ONE_DIR:
                qb.setTables(DATABASE_TABLES[0]);
                qb.setProjectionMap(nearableHash); //the hashmap of the table
                break;
            case TELEMETRY_TWO_DIR:
                qb.setTables(DATABASE_TABLES[1]);
                qb.setProjectionMap(telemetryHash); //the hashmap of the table
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        //Don't change me
        try {
            Cursor c = qb.query(database, projection, selection, selectionArgs,
                    null, null, sortOrder);
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        } catch (IllegalStateException e) {
            if (Aware.DEBUG)
                Log.e(Aware.TAG, e.getMessage());
            return null;
        }
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {

            //Add each table indexes DIR and ITEM
            case NEARABLE_ONE_DIR:
                return Nearable_Data.CONTENT_TYPE;
            case NEARABLE_ONE_ITEM:
                return Nearable_Data.CONTENT_ITEM_TYPE;
            case TELEMETRY_TWO_DIR:
                return Telemetry_Data.CONTENT_TYPE;
            case TELEMETRY_TWO_ITEM:
                return Telemetry_Data.CONTENT_ITEM_TYPE;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        initialiseDatabase();

        database.beginTransaction();

        int count;
        switch (sUriMatcher.match(uri)) {

            //Add each table DIR case
            case NEARABLE_ONE_DIR:
                count = database.update(DATABASE_TABLES[0], values, selection, selectionArgs);
                break;
            case TELEMETRY_TWO_DIR:
                count = database.update(DATABASE_TABLES[1], values, selection, selectionArgs);
                break;
            default:
                database.endTransaction();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        database.setTransactionSuccessful();
        database.endTransaction();

        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }
}
