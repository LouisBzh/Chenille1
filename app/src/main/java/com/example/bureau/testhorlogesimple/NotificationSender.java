package com.example.bureau.testhorlogesimple;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

public class NotificationSender {
    //Preferences
    public static final String MY_PREF="mesPrefs";
    SharedPreferences myVar;
    SharedPreferences.Editor myVarEditor;

    public void Build(Context context, String title, String content, String ticker ){
        myVar=context.getSharedPreferences(MY_PREF,context.MODE_PRIVATE);
        myVarEditor=myVar.edit();

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
                .setContentTitle(title)
                .setSmallIcon(R.mipmap.chenillelogo)
                .setAutoCancel(true)
                .setTicker(content+" "+ticker)
                .setContentIntent(pendingIntentNotif);
        /* Sending the notification to system.
         The first argument ensures that each notification is having a unique id
         If two notifications share same notification id, then the last notification replaces the first notification
        */
        if(myVar.getString("AutoManualSet","Manual").equals("Manual")){
            Intent setAuto=new Intent(context,NotifButton.class);
            setAuto.putExtra("ActionID","setAuto");
            PendingIntent setAutoPI = PendingIntent.getActivity(context.getApplicationContext(),0,setAuto,PendingIntent.FLAG_UPDATE_CURRENT);
            notificationBuilder.setContentText("Attention application en mode Manuel!");
            notificationBuilder.addAction(R.drawable.circle_button,"Mettre en automatique",setAutoPI);
            android.app.Notification notification = notificationBuilder.build();
            nManager.notify((int) System.currentTimeMillis(), notification);
        }else {
            //Creating a notification from the notification builder
            if (myVar.getBoolean("notifsEnable", true)) {
                android.app.Notification notification = notificationBuilder.build();
                nManager.notify((int) System.currentTimeMillis(), notification);
            }
        }
    }

    public static class NotifButton extends BroadcastReceiver{
        SharedPreferences myVar;
        SharedPreferences.Editor myVarEditor;
        public static final String MY_PREF="mesPrefs";
        @Override
        public void onReceive(Context context, Intent intent) {
            intent.getStringExtra("ActionID");
            if(intent.getStringExtra("ActionID").equals("setAuto")){
                myVar=context.getSharedPreferences(MY_PREF,context.MODE_PRIVATE);
                myVarEditor=myVar.edit();
                myVarEditor.putString("AutoManualSet","Auto");
                myVarEditor.putBoolean("SurEcritureAutorise",false);
                myVarEditor.apply();
                new Verification().UpDate(context,"NotificationSender: setting in automatic");
                context.startActivity(new Intent(context, MapsActivity.class));
            }
        }
    }
}
