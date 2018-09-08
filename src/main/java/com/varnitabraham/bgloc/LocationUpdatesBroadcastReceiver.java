package com.varnitabraham.bgloc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.LocationResult;

public class LocationUpdatesBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "FusedLocationBGReceiver";
    private static final int VARNIT_BG_FUSED_LOC_ID = 1000;

    static final String ACTION_PROCESS_UPDATES =
            "com.varnitabraham.bgloc.LocationUpdatesBroadcastReceiver.action" +
                    ".PROCESS_UPDATES";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive broadcast: " + context.getApplicationContext().getPackageName());
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
                        Intent postLocInBg = new Intent(context, FusedLocationBgService.class);
                        postLocInBg.putExtra("location", loc);
                        context.startService(postLocInBg);
                    }
                }
            }
        }
    }
}
