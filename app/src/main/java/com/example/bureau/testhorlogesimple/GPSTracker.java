package com.example.bureau.testhorlogesimple;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;

import static com.example.bureau.testhorlogesimple.MapsActivity.RequestCode_GPS_FINE;

public class GPSTracker extends Service
        implements LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    private final Context mContext;
    private Context appContext;
    private static final String TAG = GPSTracker.class.getSimpleName();
    //Preferences
    public static final String MY_PREF="mesPrefs";
    SharedPreferences myVar;
    SharedPreferences.Editor myVarEditor;

    // flag for GPS status
    boolean isGPSEnabled = false;
    // flag for network status
    boolean isNetworkEnabled = false;
    // flag for GPS status
    boolean canGetLocation = false;

    public static double
            latitude, // latitude
            longitude, // longitude
            speed; //speed

    public static final String
            ACTION_LOCATION_BROADCAST = GPSTracker.class.getName() + "LocationBroadcast",
            EXTRA_LATITUDE = "extra_latitude",
            EXTRA_LONGITUDE = "extra_longitude",
            EXTRA_SPEED = "extra_speed";

    //Location
    LocationManager locationManager;
    private Location lastLocation;
    private LocationRequest locationRequest;
    private int UPDATE_INTERVAL;// Defined in mili seconds.
    private int FASTEST_INTERVAL;// This number in extremely low, and should be used only for debug
    private int UPDATE_DISTANCE; // The minimum distance to change Updates in meters
    private int speedMin;

    //Geofencing API
    private GoogleApiClient googleApiClient;


    public GPSTracker(Context context) {
        this.mContext = context;
        getLocation();
    }

    public void getLocation() {
        locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);
        // getting GPS status
        isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // getting network status
        isNetworkEnabled = locationManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (!isGPSEnabled && !isNetworkEnabled) {
            // no network provider is enabled
        } else {
            this.canGetLocation = true;
            if (checkPermission()) {
                myVar = mContext.getSharedPreferences(MY_PREF, MODE_PRIVATE);
                UPDATE_INTERVAL=myVar.getInt("timeUpDateGPS",1000);
                UPDATE_DISTANCE=myVar.getInt("distanceUpDateGPS",100);
                FASTEST_INTERVAL= (int) (UPDATE_INTERVAL/1.5);
                speedMin=myVar.getInt("speedMin",30);
                myVarEditor = myVar.edit();
                if (myVar.getBoolean("GpsEnable", false)) {
                    appContext = mContext.getApplicationContext();
                    //Location
                    locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);
                    createGoogleApi();
                    // Call GoogleApiClient connection when starting the Activity
                    googleApiClient.connect();
                } else {
                    Toast.makeText(getBaseContext(), "Fonction GPS non activée dans l'application", Toast.LENGTH_LONG).show();
                }
            } else {
                askPermission();
            }
        }
    }

    /**
     * Function to check GPS/wifi enabled
     * @return boolean
     **/

    public boolean canGetLocation() {
        return this.canGetLocation;
    }

    /**
     * Function to show settings alert dialog
     * On pressing Settings button will lauch Settings Options
     * */

    public void showSettingsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);

        // Setting Dialog Title
        alertDialog.setTitle("Paramètres GPS");

        // Setting Dialog Message
        alertDialog.setMessage("GPS non activé. Voulez-vous accéder au paramètres système?");

        // On pressing Settings button
        alertDialog.setPositiveButton("Paramètres", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                mContext.startActivity(intent);
            }
        });

        // on pressing cancel button
        alertDialog.setNegativeButton("Annuler", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Showing Alert Message
        alertDialog.show();
    }

    // Create GoogleApiClient instance
    private void createGoogleApi() {
        Log.d(TAG, "createGoogleApi()");
        if ( googleApiClient == null ) {
            googleApiClient = new GoogleApiClient.Builder( appContext )
                    .addConnectionCallbacks( this )
                    .addOnConnectionFailedListener( this )
                    .addApi( LocationServices.API )
                    .build();
        }
    }
    // GoogleApiClient.ConnectionCallbacks connected
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "onConnected()");
        removeLocationUpdates();//Update location request by removing them
        getLastKnownLocation();
    }
    // GoogleApiClient.ConnectionCallbacks suspended
    @Override
    public void onConnectionSuspended(int i) {
        Log.w(TAG, "onConnectionSuspended()");
    }
    // GoogleApiClient.OnConnectionFailedListener fail
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.w(TAG, "onConnectionFailed()");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged ["+location+"]");
        lastLocation = location;
        sendBroadcastMessage(location);
        if(location.getSpeed()>speedMin){
            SmsSender smsSender=new SmsSender(mContext);
            myVarEditor.putBoolean("Voyage",true);
            smsSender.SendSMS("3");
        }else{
            myVarEditor.putBoolean("Voyage",false);
        }
        myVarEditor.apply();
    }

    // Get last known location
    private Location getLastKnownLocation() {
        Log.d(TAG, "getLastKnownLocation()");
        if ( checkPermission() ) {
            lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            if ( lastLocation != null ) {
                Log.i(TAG, "LasKnown location. ["+lastLocation+"]");
                sendBroadcastMessage(lastLocation);
                startLocationUpdates();
                return lastLocation;
            } else {
                Log.w(TAG, "No location retrieved yet");
                startLocationUpdates();
                return null;
            }
        }else{
            return null;
        }
    }
    // Start location Updates
    private void startLocationUpdates(){
        Log.i(TAG, "startLocationUpdates()");
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_LOW_POWER)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL)
                .setSmallestDisplacement(UPDATE_DISTANCE);
        if ( checkPermission() ) {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest,this);
        }
    }
    //Remove location Updates
    private void removeLocationUpdates(){
        Log.i(TAG, "removeLocationUpdates()");
        if ( checkPermission() ) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient,this);
        }
    }

    private void sendBroadcastMessage(Location location) {
        if (location != null) {
            Intent intent = new Intent(ACTION_LOCATION_BROADCAST);
            intent.putExtra(EXTRA_LATITUDE, location.getLatitude());
            intent.putExtra(EXTRA_LONGITUDE, location.getLongitude());
            intent.putExtra(EXTRA_SPEED, location.getSpeed());
            this.latitude=location.getLatitude();
            this.longitude=location.getLongitude();
            this.speed=location.getSpeed();
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    /**Permissions
    *Check for permission to access Location
     **/
    private boolean checkPermission() {
        Log.d(TAG, "checkPermission()");
        if(ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            Toast.makeText(mContext,"Permission Fine Location denied",Toast.LENGTH_LONG).show();
        }
        return (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED );
    }
    //Ask ACCESS_FINE_LOCATION
    private void askPermission(){
        // Ask for permission if it wasn't granted yet
        if(!(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)){
            Log.d(TAG, "askPermission()");
            ActivityCompat.requestPermissions(
                    (Activity)mContext,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION },RequestCode_GPS_FINE);
        }
    }
}