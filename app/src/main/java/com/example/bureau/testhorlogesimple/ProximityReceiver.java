package com.example.bureau.testhorlogesimple;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;
import java.util.List;
import static android.content.ContentValues.TAG;
import static com.example.bureau.testhorlogesimple.Main.BooleantoString;


public class ProximityReceiver extends BroadcastReceiver {
    //Preferences
    public static final String MY_PREF="mesPrefs";
    final String[] positionID = {"Famille", "Travail", "Joker", "Maison"};
    SharedPreferences myVar;
    SharedPreferences.Editor myVarEditor;
    String aigLastPosit;
    String aigNewPosit;//New Hand position
    //Notifications
    String notificationTitle;
    String notificationContent;
    String tickerMessage;
    //Variables GPS
    Boolean inFamilleGps;
    Boolean inTravailGps;
    Boolean inJokerGps;
    Boolean inMaisonGps;
    String positGps="0000";//String coding if gps position inside Famille/Travail/Joker/Maison
    //Variables sms functions
    SmsSender smsSender;
    BroadcastReceiver sentBroadcast;
    BroadcastReceiver deliveredBroadcast;
    IntentFilter sentIntentFilter;
    IntentFilter deliveredIntentFilter;
    String SENT = "SMS_SENT";
    String DELIVERED = "SMS_DELIVERED";

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        Log.d("Intent1","Intent received");
        //Get the preferences registered
        myVar=context.getSharedPreferences(MY_PREF,context.MODE_PRIVATE);
        myVarEditor = myVar.edit();
        smsSender=new SmsSender(context);
        if(myVar.getBoolean("GpsEnable",false)) {
            //Get the event triggered
            GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
            if (geofencingEvent.hasError()) {
                String errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.getErrorCode());
                Log.e(TAG, errorMessage);
                return;
            }

            //Get the transition type.
            int geofenceTransition = geofencingEvent.getGeofenceTransition();
            Boolean notifDisplay=myVar.getBoolean("notifsEnable",true);


            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                    geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

                // Get the geofences that were triggered.
                // !! A single event can trigger multiple geofences.!!
                List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
                String proximity_name = positionID[Integer.parseInt(triggeringGeofences.get(0).getRequestId())];

                if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                    Toast.makeText(context, "Entering the region", Toast.LENGTH_LONG).show();
                    notificationTitle = "Proximity - Entry";
                    notificationContent = proximity_name;
                    tickerMessage = proximity_name;
                    myVarEditor.putBoolean("in" + proximity_name + "GPS", true);
                    myVarEditor.apply();
                } else {
                    Toast.makeText(context, "Exiting the region", Toast.LENGTH_LONG).show();
                    notificationTitle = "Proximity - Exit";
                    notificationContent = proximity_name;
                    tickerMessage = proximity_name;
                    myVarEditor.putBoolean("in" + proximity_name + "GPS", false);
                    myVarEditor.apply();
                }

                //Show notification if permitted
                if (notifDisplay) {
                    Intent notificationIntent = new Intent(context, Main.class);
                    notificationIntent.putExtra("content", notificationContent);
                    // This is needed to make this intent different from its previous intents
                    notificationIntent.setData(Uri.parse("tel:/" + (int) System.currentTimeMillis()));
                    // Creating different tasks for each notification. See the flag Intent.FLAG_ACTIVITY_NEW_TASK
                    PendingIntent pendingIntentNotif = PendingIntent.getActivity(context.getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    // Getting the System service NotificationManager
                    NotificationManager nManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    //Configuring notification builder to create a notification
                    NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                            .setWhen(System.currentTimeMillis())
                            .setContentText(notificationContent)
                            .setContentTitle(notificationTitle)
                            .setSmallIcon(R.mipmap.chenillelogo)
                            .setAutoCancel(true)
                            .setTicker(tickerMessage)
                            .setContentIntent(pendingIntentNotif);
                    //Creating a notification from the notification builder
                    Notification notification = notificationBuilder.build();
                    /* Sending the notification to system.
                     The first argument ensures that each notification is having a unique id
                     If two notifications share same notification id, then the last notification replaces the first notification
                     */
                    nManager.notify((int) System.currentTimeMillis(), notification);
                }
            }

            //Send SMS
            aigLastPosit=myVar.getString("aigLastPosit","4");
            inFamilleGps=myVar.getBoolean("inFamilleGPS",false);
            inTravailGps=myVar.getBoolean("inTravailGPS",false);
            inJokerGps=myVar.getBoolean("inJokerGPS",false);
            inMaisonGps=myVar.getBoolean("inMaisonGPS",false);

            //Check gps information and hand positioning
            positGps = BooleantoString(inFamilleGps) + BooleantoString(inTravailGps) + BooleantoString(inJokerGps) + BooleantoString(inMaisonGps);
            switch (positGps) {
                case "0000": //Not inside any Geofence
                    aigNewPosit = "4";
                    break;
                case "1000": //Inside Family Geofence
                    aigNewPosit = "1";
                    break;
                case "0100": //Inside Travail Geofence
                case "1100": //Inside Family and Travail Geofence
                    aigNewPosit = "2";
                    break;
                case "0010": //Inside Joker Geofence
                    aigNewPosit = "5";
                    break;
                case "0001": //Inside Maison Geofence
                    aigNewPosit = "7";
                    break;
                default:
                    aigNewPosit = aigLastPosit; //If none of above possibility = ERROR
                    Toast.makeText(context, "Configuration des zones non pris en charge. Veuillez vérifier la disposition des zones GPS.", Toast.LENGTH_LONG).show();
                    break;
            }
            smsSender.SendSMS(aigNewPosit);
        }else{
            Log.e(TAG,"Situation de création ou de déclenchement géofence malgré GPS désactivé !");
        }
    }
}