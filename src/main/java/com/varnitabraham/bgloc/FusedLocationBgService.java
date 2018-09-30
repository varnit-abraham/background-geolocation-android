package com.varnitabraham.bgloc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.spotstamp.spotstampcustomerapp.R;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class FusedLocationBgService extends JobService {
    private static final String TAG = "FusedLocationBgService";
    private FusedLocationProvider fusedLocationProvider;
    private Notification notification;
    public static final String NOTIFICATION_CHANNEL_ID = "4565";

    @Override
    public boolean onStartJob(JobParameters params) {
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate called");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Context context = this.getApplication().getApplicationContext();
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            CharSequence name = context.getString(R.string.app_name);
            String description = "Fused Background Service";
            int importance = NotificationManager.IMPORTANCE_NONE;

            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);

            notificationManager.createNotificationChannel(channel);

            if(notification == null)
                notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID).
                        setSmallIcon(R.mipmap.icon).setChannelId(NOTIFICATION_CHANNEL_ID).setContentTitle("").setContentText("Searching for GPS").build();
        } else {
            if(notification == null)
                notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID).
                        setSmallIcon(R.mipmap.icon).setContentTitle("").setContentText("Searching for GPS").build();
        }


        startForeground(1, notification);
        if (fusedLocationProvider == null) {
            fusedLocationProvider = new FusedLocationProvider(
                    this.getApplication().getApplicationContext()
            );
        } else {
            Log.d(TAG, "FusedProvider exists...");
        }
        fusedLocationProvider.onCreate();
        //startForeground(1, null);
        stopForeground(true);
        stopSelf();
    }
}
