package com.varnitabraham.bgloc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.LocationResult;

import java.util.List;

public class LocationUpdatesBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "FusedBroadcastReceiver";

    static final String ACTION_PROCESS_UPDATES =
            "com.varnitabraham.bgloc.LocationUpdatesBroadcastReceiver.action" +
                    ".PROCESS_UPDATES";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive broadcast");
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_PROCESS_UPDATES.equals(action)) {
                LocationResult result = LocationResult.extractResult(intent);
                if (result != null) {
                    Location location = result.getLastLocation();
                    if (location != null) {
                        Utils.setLocationUpdatesResult(context, location);
                        Log.i(TAG, Utils.getLocationUpdatesResult(context));
                    }
                }
            }
        }
    }
}
