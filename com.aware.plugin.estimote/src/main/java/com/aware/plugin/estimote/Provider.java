package com.aware.plugin.estimote;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
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

    public static final int DATABASE_VERSION = 6; //increase this if you make changes to the database structure, i.e., rename columns, etc.
    public static final String DATABASE_NAME = "plugin_estimote.db"; //the database filename, use plugin_xxx for plugins.

    //Add here your database table names, as many as you need
    public static final String DB_TBL_ESTIMOTE = "estimote_data";

    //For each table, add two indexes: DIR and ITEM. The index needs to always increment. Next one is 3, and so on.
    private static final int ESTIMOTE_ONE_DIR = 1;
    private static final int ESTIMOTE_ONE_ITEM = 2;

    //Put tables names in this array so AWARE knows what you have on the database
    public static final String[] DATABASE_TABLES = {
            DB_TBL_ESTIMOTE
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
    public static final class Estimote_Data implements AWAREColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + DB_TBL_ESTIMOTE);
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.com.aware.plugin.estimote.provider.estimote_data"; //modify me
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.com.aware.plugin.estimote.provider.estimote_data"; //modify me

        //Note: integers and strings don't need a type prefix_
        public static final String ESTIMOTE_ID = "estimote_id";
        public static final String ESTIMOTE_BATTERY = "estimote_battery";
        public static final String TEMPERATURE = "temperature";
        public static final String X_ACCELERATION = "x_acceleration";
        public static final String Y_ACCELERATION = "y_acceleration";
        public static final String Z_ACCELERATION = "z_acceleration";
        public static final String IS_MOVING = "is_moving";
    }

    //Define each database table fields
    private static final String DB_TBL_ESTIMOTE_FIELDS =
        Estimote_Data._ID + " integer primary key autoincrement," +
        Estimote_Data.TIMESTAMP + " real default 0," +
        Estimote_Data.DEVICE_ID + " text default ''," +
        Estimote_Data.ESTIMOTE_ID + " text default ''," +
        Estimote_Data.ESTIMOTE_BATTERY + " text default ''," +
        Estimote_Data.TEMPERATURE + " text default ''," +
        Estimote_Data.X_ACCELERATION + " text default ''," +
        Estimote_Data.Y_ACCELERATION + " text default ''," +
        Estimote_Data.Z_ACCELERATION + " text default ''," +
        Estimote_Data.IS_MOVING + " text default ''";

    /**
     * Share the fields with AWARE so we can replicate the table schema on the server
     */
    public static final String[] TABLES_FIELDS = {
            DB_TBL_ESTIMOTE_FIELDS
    };

    //Helper variables for ContentProvider - DO NOT CHANGE
    private UriMatcher sUriMatcher;
    private DatabaseHelper dbHelper;
    private static SQLiteDatabase database;
    private void initialiseDatabase() {
        Log.d("AWW", "AWW 1");
        if (dbHelper == null)
            Log.d("AWW", "AWW 2");
        dbHelper = new DatabaseHelper(getContext(), DATABASE_NAME, null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS);
        if (database == null)
            Log.d("AWW", "AWW 3");
        database = dbHelper.getWritableDatabase();
    }
    //--

    //For each table, create a hashmap needed for database queries
    private HashMap<String, String> estimoteHash;

    @Override
    public boolean onCreate() {
        //This is a hack to allow providers to be reusable in any application/plugin by making the authority dynamic using the package name of the parent app
        AUTHORITY = getContext().getPackageName() + ".provider.estimote"; //make sure xxx matches the first string in this class

        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        //For each table, add indexes DIR and ITEM
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0], ESTIMOTE_ONE_DIR);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0] + "/#", ESTIMOTE_ONE_ITEM);

        //Create each table hashmap so Android knows how to insert data to the database. Put ALL table fields.
        estimoteHash = new HashMap<>();
        estimoteHash.put(Estimote_Data._ID, Estimote_Data._ID);
        estimoteHash.put(Estimote_Data.TIMESTAMP, Estimote_Data.TIMESTAMP);
        estimoteHash.put(Estimote_Data.DEVICE_ID, Estimote_Data.DEVICE_ID);
        estimoteHash.put(Estimote_Data.ESTIMOTE_ID, Estimote_Data.ESTIMOTE_ID);
        estimoteHash.put(Estimote_Data.ESTIMOTE_BATTERY, Estimote_Data.ESTIMOTE_BATTERY);
        estimoteHash.put(Estimote_Data.TEMPERATURE, Estimote_Data.TEMPERATURE);
        estimoteHash.put(Estimote_Data.X_ACCELERATION, Estimote_Data.X_ACCELERATION);
        estimoteHash.put(Estimote_Data.Y_ACCELERATION, Estimote_Data.Y_ACCELERATION);
        estimoteHash.put(Estimote_Data.Z_ACCELERATION, Estimote_Data.Z_ACCELERATION);
        estimoteHash.put(Estimote_Data.IS_MOVING, Estimote_Data.IS_MOVING);

        return true;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        initialiseDatabase();

        database.beginTransaction();

        int count;
        switch (sUriMatcher.match(uri)) {

            //Add each table DIR case, increasing the index accordingly
            case ESTIMOTE_ONE_DIR:
                count = database.delete(DATABASE_TABLES[0], selection, selectionArgs);
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
            case ESTIMOTE_ONE_DIR:
                long _id = database.insert(DATABASE_TABLES[0], Estimote_Data.DEVICE_ID, values);
                database.setTransactionSuccessful();
                database.endTransaction();
                if (_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(Estimote_Data.CONTENT_URI, _id);
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
            case ESTIMOTE_ONE_DIR:
                qb.setTables(DATABASE_TABLES[0]);
                qb.setProjectionMap(estimoteHash); //the hashmap of the table
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
            case ESTIMOTE_ONE_DIR:
                return Estimote_Data.CONTENT_TYPE;
            case ESTIMOTE_ONE_ITEM:
                return Estimote_Data.CONTENT_ITEM_TYPE;

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
            case ESTIMOTE_ONE_DIR:
                count = database.update(DATABASE_TABLES[0], values, selection, selectionArgs);
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
