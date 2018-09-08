package com.varnitabraham.bgloc;

import android.app.IntentService;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

public class FusedLocationBgService extends IntentService {
    private static final String TAG = "FusedLocationBgService";
    private FusedLocationProvider fusedLocationProvider;
    public FusedLocationBgService() {
        super("FusedLocationBgService");
    }
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        try {
            Bundle locBundle = intent.getExtras();
            String strLocations = locBundle.getString("location");
            String[] locationObj = strLocations.split(",");
            Location receivedLocation = new Location(locationObj[0]);
            receivedLocation.setLatitude(Double.parseDouble(locationObj[1]));
            receivedLocation.setLongitude(Double.parseDouble(locationObj[2]));
            if (fusedLocationProvider == null) {
                fusedLocationProvider = new FusedLocationProvider(
                        this.getApplication().getApplicationContext()
                );
            }
            fusedLocationProvider.onHandleLocation(receivedLocation);
            Log.d(TAG, "Intent data received: " + locBundle.get("location"));
        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage());
        }
    }
}
