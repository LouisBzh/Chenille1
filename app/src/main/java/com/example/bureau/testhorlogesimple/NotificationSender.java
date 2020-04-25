package com.example.bureau.testhorlogesimple;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

public class NotificationSender {
    //Preferences
    public static final String MY_PREF="mesPrefs";
    SharedPreferences myVar;

    public void Build(Context context, String title, String content, String ticker ){
        myVar=context.getSharedPreferences(MY_PREF,context.MODE_PRIVATE);
        Boolean notifDisplay=myVar.getBoolean("notifsEnable",true);

        if (notifDisplay) {
            Intent notificationIntent = new Intent(context, Main.class);
            notificationIntent.putExtra("content", content);
            // This is needed to make this intent different from its previous intents
            notificationIntent.setData(Uri.parse("tel:/" + (int) System.currentTimeMillis()));
            // Creating different tasks for each notification. See the flag Intent.FLAG_ACTIVITY_NEW_TASK
            PendingIntent pendingIntentNotif = PendingIntent.getActivity(context.getApplicationContext(),
                    0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            // Getting the System service NotificationManager
            NotificationManager nManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            //Configuring notification builder to create a notification
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                    .setWhen(System.currentTimeMillis())
                    .setContentText(content)
                    .setContentTitle(title)
                    .setSmallIcon(R.mipmap.chenillelogo)
                    .setAutoCancel(true)
                    .setTicker(ticker)
                    .setContentIntent(pendingIntentNotif);
            //Creating a notification from the notification builder
            android.app.Notification notification = notificationBuilder.build();
                    /* Sending the notification to system.
                     The first argument ensures that each notification is having a unique id
                     If two notifications share same notification id, then the last notification replaces the first notification
                     */
            nManager.notify((int) System.currentTimeMillis(), notification);
        }
    }
}
