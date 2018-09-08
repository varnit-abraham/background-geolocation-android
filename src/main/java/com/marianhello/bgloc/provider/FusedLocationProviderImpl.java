package com.marianhello.bgloc.provider;

import android.content.Context;

import com.marianhello.bgloc.Config;

public class FusedLocationProviderImpl extends AbstractLocationProvider {
    public FusedLocationProviderImpl(Context context) {
        super(context);
        PROVIDER_ID = Config.FUSED_LOCATION_PROVIDER;
    }

    @Override
    public void onStart() {

    }

    @Override
    public void onStop() {

    }

    @Override
    public boolean isStarted() {
        return false;
    }
}
