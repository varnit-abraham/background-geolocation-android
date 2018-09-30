package com.varnitabraham.bgloc;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.location.LocationResult;

public class LocationUpdatesBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "FusedLocationBGReceiver";
    private static final int VARNIT_BG_FUSED_LOC_ID = 1000;
    private FusedLocationProvider fusedLocationProvider;

    static final String ACTION_PROCESS_UPDATES =
            "com.varnitabraham.bgloc.LocationUpdatesBroadcastReceiver.action" +
                    ".PROCESS_UPDATES";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive broadcast: " + context.getApplicationContext().getPackageName());
        Log.d(TAG, "Intent received: " + intent.getExtras().toString());
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_PROCESS_UPDATES.equals(action)) {
                LocationResult result = LocationResult.extractResult(intent);
                if (result != null) {
                    Location location = result.getLastLocation();
                    if (location != null) {
                        Utils.setLocationUpdatesResult(context, location);
                        String loc = Utils.getLocationUpdatesResult(context);
                        Log.i(TAG, loc);
                        String[] locationObj = loc.split(",");
                        Location receivedLocation = new Location(locationObj[0]);
                        receivedLocation.setLatitude(Double.parseDouble(locationObj[1]));
                        receivedLocation.setLongitude(Double.parseDouble(locationObj[2]));
                        if (fusedLocationProvider == null) {
                            fusedLocationProvider = new FusedLocationProvider(
                                    context
                            );
                        }
                        fusedLocationProvider.onHandleLocation(receivedLocation);
                    }
                }
            }
        } else {
            Log.d(TAG, "No intent available");
        }
    }
}
