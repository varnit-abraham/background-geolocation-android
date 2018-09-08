package com.varnitabraham.bgloc;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import com.marianhello.bgloc.Config;
import com.marianhello.bgloc.HttpPostService;
import com.marianhello.bgloc.data.BackgroundLocation;
import com.marianhello.bgloc.provider.AbstractLocationProvider;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FusedLocationProvider extends AbstractLocationProvider implements
        SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "FusedLocationProvider";
    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private static final long UPDATE_INTERVAL = 10000; // Every 60 seconds.

    /**
     * The fastest rate for active location updates. Updates will never be more frequent
     * than this value, but they may be less frequent.
     */
    private static final long FASTEST_UPDATE_INTERVAL = 5000; // Every 30 seconds

    /**
     * The max time before batched results are delivered by location services. Results may be
     * delivered sooner than this interval.
     */
    private static final long MAX_WAIT_TIME = UPDATE_INTERVAL * 5; // Every 5 minutes.

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    private LocationRequest mLocationRequest;

    /**
     * Provides access to the Fused Location Provider API.
     */
    private FusedLocationProviderClient mFusedLocationClient;

    private ExecutorService mExecutor = null;

    private boolean isStarted = false;
    private Config mConfig = null;

    public FusedLocationProvider(Context context) {
        super(context);
        PROVIDER_ID = Config.FUSED_LOCATION_PROVIDER;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext.getApplicationContext());
        createLocationRequest();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onStart() {
        if (isStarted) {
            return;
        }

        logger.info("Start recording");
        isStarted = true;
        PreferenceManager.getDefaultSharedPreferences(mContext.getApplicationContext())
                .registerOnSharedPreferenceChangeListener(this);
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, getPendingIntent());
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onStop() {
        if (!isStarted) {
            return;
        }
        //mFusedLocationClient.removeLocationUpdates(getPendingIntent());
        PreferenceManager.getDefaultSharedPreferences(mContext.getApplicationContext())
                .unregisterOnSharedPreferenceChangeListener(this);
        isStarted = false;
        //mFusedLocationClient.requestLocationUpdates(mLocationRequest, getPendingIntent());
    }

    @Override
    public boolean isStarted() {
        return isStarted;
    }

    @Override
    public void onConfigure(Config config) {
        if (mConfig == null) {
            mConfig = config;
        }
        if (isStarted) {
            onStop();
            onStart();
        }
        super.onConfigure(config);
    }

    @Override
    public void onDestroy() {
        mExecutor.shutdown();
        super.onDestroy();
    }

    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        // Note: apps running on "O" devices (regardless of targetSdkVersion) may receive updates
        // less frequently than this interval when the app is no longer in the foreground.
        mLocationRequest.setInterval(UPDATE_INTERVAL);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Sets the maximum time when batched location updates are delivered. Updates may be
        // delivered sooner than this interval.
        mLocationRequest.setMaxWaitTime(MAX_WAIT_TIME);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Utils.KEY_LOCATION_UPDATES_RESULT)) {
            logger.debug("FusedLocationProvider, " + Utils.KEY_LOCATION_UPDATES_RESULT);
            String strLocations = Utils.getLocationUpdatesResult(mContext.getApplicationContext());
            String[] locationObj = strLocations.split(",");
            Location receivedLocation = new Location(locationObj[0]);
            receivedLocation.setLatitude(Double.parseDouble(locationObj[1]));
            receivedLocation.setLongitude(Double.parseDouble(locationObj[2]));
            onHandleLocation(receivedLocation);
        }
    }

    private PendingIntent getPendingIntent() {
        // Note: for apps targeting API level 25 ("Nougat") or lower, either
        // PendingIntent.getService() or PendingIntent.getBroadcast() may be used when requesting
        // location updates. For apps targeting API level O, only
        // PendingIntent.getBroadcast() should be used. This is due to the limits placed on services
        // started in the background in "O".

        // TODO(developer): uncomment to use PendingIntent.getService().
//        Intent intent = new Intent(this, LocationUpdatesIntentService.class);
//        intent.setAction(LocationUpdatesIntentService.ACTION_PROCESS_UPDATES);
//        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intent = new Intent(mContext.getApplicationContext(), LocationUpdatesBroadcastReceiver.class);
        intent.setAction(LocationUpdatesBroadcastReceiver.ACTION_PROCESS_UPDATES);
        return PendingIntent.getBroadcast(mContext.getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void onHandleLocation(final Location location) {
        logger.debug("New stationary {}", location.toString());

        if (mExecutor == null) {
            mExecutor = Executors.newSingleThreadExecutor();
        }

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                BackgroundLocation bgLocation = new BackgroundLocation(PROVIDER_ID, location);
                bgLocation.setMockLocationsEnabled(hasMockLocationsEnabled());
                if (postLocation(bgLocation)) {
                    logger.info("Location posted to server successfully");
                }
            }
        });
    }

    private boolean postLocation(BackgroundLocation location) {
        logger.debug("Executing PostLocationTask#postLocation");
        JSONArray jsonLocations = new JSONArray();
        Config config = mConfig;
        if (config!=null && config.hasValidUrl()) {
            try {
                jsonLocations.put(config.getTemplate().locationToJson(location));
            } catch (JSONException e) {
                logger.warn("Location to json failed: {}", location.toString());
                return false;
            }

            String url = config.getUrl();
            logger.debug("Posting json to url: {} headers: {}", url, config.getHttpHeaders());
            int responseCode = 0;

            try {
                if (isNetworkAvailable()) {
                    responseCode = HttpPostService.postJSON(url, jsonLocations, config.getHttpHeaders());
                } else {
                    logger.debug("No network connectivity found.");
                    return false;
                }
            } catch (Exception e) {
                logger.warn("Error while posting locations: {}", e.getMessage());
                return false;
            }

            if (responseCode == 285) {
                // Okay, but we don't need to continue sending these
                logger.debug("Location was sent to the server, and received an \"HTTP 285 Updates Not Required\"");
            }

            // All 2xx statuses are okay
            if (responseCode >= 200 && responseCode < 300) {
                logger.warn("Server error while posting locations responseCode: {}", responseCode);
                return false;
            }
        }
        return true;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm =
                (ConnectivityManager) mContext.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

}

class Utils {
    final static String KEY_LOCATION_UPDATES_REQUESTED = "location-updates-requested";
    final static String KEY_LOCATION_UPDATES_RESULT = "location-update-result";

    /**
     * Returns te text for reporting about a list of  {@link Location} objects.
     *
     * @param locations List of {@link Location}s.
     */
    private static String getLocationResultText(Location locations) {
        StringBuilder sb = new StringBuilder();
        sb.append(locations.getProvider());
        sb.append(",");
        sb.append(locations.getLatitude());
        sb.append(", ");
        sb.append(locations.getLongitude());
        return sb.toString();
    }

    static void setLocationUpdatesResult(Context context, Location location) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(KEY_LOCATION_UPDATES_RESULT, getLocationResultText(location))
                .apply();
    }

    static String getLocationUpdatesResult(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_LOCATION_UPDATES_RESULT, "");
    }

}
