package com.pplus.go.app.gopplus;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.IOException;
import java.net.URL;
import java.util.Map;


public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private LocalBroadcastManager broadcaster;

    public static final String FCM_PARAM = "picture";
    private static final String CHANNEL_NAME = "FCM";
    private static final String CHANNEL_DESC = "Firebase Cloud Messaging";
    private int numMessages = 0;


    @Override
    public void onCreate() {
        broadcaster = LocalBroadcastManager.getInstance(this);
    }

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        Log.d("FCM", s);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d("onMessageReceived", "onMessageReceived");
        super.onMessageReceived(remoteMessage);
        RemoteMessage.Notification notification = remoteMessage.getNotification();
        Map<String, String> data = remoteMessage.getData();
        sendNotification(notification, data);
    }

    private void sendNotification(RemoteMessage.Notification notification, Map<String, String> data) {
        Bundle bundle = new Bundle();
        bundle.putString(FCM_PARAM, data.get(FCM_PARAM));

        Intent intent = new Intent("MyData");
        intent.putExtra("title", notification.getTitle());
        intent.putExtra("message", notification.getBody());

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, getString(R.string.default_notification_channel_id))
                .setContentTitle("GoPPlus")
                .setContentText(notification.getBody())
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(pendingIntent)
                .setContentInfo(notification.getTitle())
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setLights(Color.RED, 1000, 300)
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setSmallIcon(R.drawable.ic_logo);

        try {
            String picture = data.get(FCM_PARAM);

            if (picture != null && !"".equals(picture)) {

                URL url = new URL(picture);
                Bitmap bigPicture = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                notificationBuilder.setStyle(
                        new NotificationCompat.BigPictureStyle().bigPicture(bigPicture).setSummaryText(notification.getBody())
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(getString(R.string.default_notification_channel_id), CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(CHANNEL_DESC);
            channel.setShowBadge(true);
            channel.canShowBadge();
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500});

            assert notificationManager != null;
            notificationManager.createNotificationChannel(channel);
        }

        assert notificationManager != null;
        notificationManager.notify(0, notificationBuilder.build());

        broadcaster.sendBroadcast(intent);


    }

    public void onNewToken() {
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
    }
}