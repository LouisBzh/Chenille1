package com.example.bureau.testhorlogesimple;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;

import static com.google.android.gms.location.Geofence.NEVER_EXPIRE;


public class MapsActivity extends FragmentActivity
        implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        OnMapReadyCallback,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMarkerClickListener,
        LocationListener {

    //Variable preferences
    public static final String MY_PREF="mesPrefs";
    SharedPreferences myVar;
    SharedPreferences.Editor myVarEditor;
    int zoneNumGPS = 4;
    final String[] positionID = {"Famille", "Travail", "Joker", "A la maison"};
    String lat="";
    String lng = "";
    String radiusSize = "";
    String title = "";
    Boolean gpsDefine = false;

    //Variable activity
    private static final String TAG = MapsActivity.class.getSimpleName();
    Context appContext;
    //Map variable
    private TextView textLat, textLong;
    private GoogleMap mMap;
    Marker[] markersArray = new Marker[4];
    Circle[] circlesArray = new Circle[4];

    //Location
    LocationManager locationManager;
    private Location lastLocation;
    private LocationRequest locationRequest;
    private final int UPDATE_INTERVAL =  1000;// Defined in mili seconds.
    private final int FASTEST_INTERVAL = 900;// This number in extremely low, and should be used only for debug

    //Geofencing API
    private GoogleApiClient googleApiClient;
    private GeofencingClient geofencingClient;
    ArrayList<Geofence> geofenceList= new ArrayList<Geofence>(4);
    PendingIntent geofencePendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appContext=getApplicationContext();
        setContentView(R.layout.activity_maps);
        myVar = getSharedPreferences(MY_PREF, Context.MODE_PRIVATE);
        myVarEditor = myVar.edit();
        textLat = (TextView) findViewById(R.id.Lat);
        textLong = (TextView) findViewById(R.id.Long);
        SupportMapFragment mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFrag.getMapAsync(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        createGoogleApi();
        geofencingClient = LocationServices.getGeofencingClient(appContext);
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

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(48, -2.4), 9));

        // Getting index of last location added
        zoneNumGPS = myVar.getInt("zoneNumGPS", 0);

        // Iterating through all the locations
        for (int i = 0; i < 4; i++) {
            // Getting the latitude of the i-th location
            lat = myVar.getString("lat" + positionID[i], "0");
            // Getting the longitude of the i-th location
            lng = myVar.getString("lng" + positionID[i], "0");
            // Getting the radius size of the i-th location
            radiusSize = myVar.getString("radiusSize" + positionID[i], "100");
            // Getting the title of the i-th location
            title = myVar.getString("title" + positionID[i], "Null");
            // Getting if the i-th location is define
            gpsDefine = myVar.getBoolean("gpsDefine" + positionID[i], false);
            // Creating Markers
            drawMarker(new LatLng(Double.parseDouble(lat), Double.parseDouble(lng)), title, i, gpsDefine);
            // Drawing circle on the map
            drawCircle(new LatLng(Double.parseDouble(lat), Double.parseDouble(lng)), Integer.parseInt(radiusSize), i, gpsDefine);
        }

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener(){
            @Override
            public void onMapClick(LatLng point) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                builder.setTitle("Choisissez la position à mettre à jour:");
                final LatLng pointing = point;
                builder.setSingleChoiceItems(positionID, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // Get location selected
                        zoneNumGPS = ((AlertDialog) dialog).getListView().getCheckedItemPosition();

                        final AlertDialog.Builder seekBarDialog = new AlertDialog.Builder(MapsActivity.this);
                        final LayoutInflater inflater = (LayoutInflater) MapsActivity.this.getSystemService(LAYOUT_INFLATER_SERVICE);
                        final View ViewLayout = inflater.inflate(R.layout.seek_bar_dialog, (ViewGroup) findViewById(R.id.layout_dialog));
                        seekBarDialog.setView(ViewLayout);
                        seekBarDialog.setTitle("Sélectionner le rayon de la zone :");

                        final int[] radiusSize = new int[1];

                        final TextView txtView = (TextView) ViewLayout.findViewById(R.id.txtItem);
                        SeekBar seek = (SeekBar) ViewLayout.findViewById(R.id.seekBar);
                        seek.setMax(1000);
                        seek.setProgress(500);
                        radiusSize[0] = 200;
                        seek.setKeyProgressIncrement(125);

                        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                //Do something here with new value
                                int radiusValue = (int) (10 * Math.exp(0.0062147 * progress));
                                txtView.setText(radiusValue + " mètres");
                                radiusSize[0] = radiusValue;
                            }
                            public void onStartTrackingTouch(SeekBar arg0) {
                                // TODO Auto-generated method stub

                            }
                            public void onStopTrackingTouch(SeekBar seekBar) {
                                // TODO Auto-generated method stub

                            }
                        });

                        // Button OK
                        seekBarDialog.setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        // Modify marker position
                                        modifyMarker(pointing, markersArray[zoneNumGPS], positionID[zoneNumGPS]);
                                        // Drawing circle on the map
                                        modifyCircle(pointing, circlesArray[zoneNumGPS], radiusSize[0]);
                                        // Add proximity
                                        setGeofence(pointing,radiusSize[0],zoneNumGPS);
                                        // Storing the latitude for the i-th location
                                        myVarEditor.putString("lat" + positionID[zoneNumGPS], Double.toString(pointing.latitude));
                                        // Storing the longitude for the i-th location
                                        myVarEditor.putString("lng" + positionID[zoneNumGPS], Double.toString(pointing.longitude));
                                        // Storing the radius size for the i-th location
                                        myVarEditor.putString("radiusSize" + positionID[zoneNumGPS], Integer.toString(radiusSize[0]));
                                        // Storing index of the last locations
                                        myVarEditor.putInt("locationNum", zoneNumGPS);
                                        // Storing the zoom level to the shared preferences
                                        myVarEditor.putString("zoom" + positionID[zoneNumGPS], Float.toString(mMap.getCameraPosition().zoom));
                                        // Storing the name to the shared preferences
                                        myVarEditor.putString("title" + positionID[zoneNumGPS], positionID[zoneNumGPS]);
                                        // Confirming GPS definition to the shared preferences
                                        myVarEditor.putBoolean("gpsDefine" + positionID[zoneNumGPS], true);
                                        // Saving the values stored in the shared preferences
                                        myVarEditor.commit();
                                        Toast.makeText(getBaseContext(), "Proximity Alert is added", Toast.LENGTH_SHORT).show();
                                    }

                                });
                        seekBarDialog.create();
                        seekBarDialog.show();

                    }
                });
                builder.setNegativeButton("Annuler", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng point) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                builder.setTitle("Choisissez la position à supprimer:");
                builder.setSingleChoiceItems(positionID, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // Set location to change
                        zoneNumGPS = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        // Removing the marker and circle from the Google Map
                        markersArray[zoneNumGPS].setVisible(false);
                        circlesArray[zoneNumGPS].setVisible(false);
                        // Remove the proximity alert
                        removeGeofence(zoneNumGPS);
                        // Confirming GPS non definition to the shared preferences
                        myVarEditor.putBoolean("gpsDefine" + positionID[zoneNumGPS], false);
                        // Delete position on zone from sharedPreference
                        myVarEditor.putBoolean("in"+positionID[zoneNumGPS]+"GPS", false);
                        // Committing the changes
                        myVarEditor.commit();
                        // Confirming message
                        Toast.makeText(getBaseContext(), "Proximity Alert is removed", Toast.LENGTH_LONG).show();
                    }
                });
                builder.setNegativeButton("Annuler", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    //Circles and Marker edition functions
    private void drawMarker(LatLng point, String title, int i, Boolean gpsDefine) {
        TypedArray ta = getResources().obtainTypedArray(R.array.colors);
        int colorToUse = ta.getResourceId(i, 0);
        float[] hsv = new float[3];
        Color.colorToHSV(ContextCompat.getColor(this, colorToUse), hsv);
        markersArray[i] = mMap.addMarker(new MarkerOptions()
                .position(point)
                .title(title)
                .icon(BitmapDescriptorFactory.defaultMarker(hsv[0])));
        markersArray[i].setVisible(gpsDefine);
    }
    private void modifyMarker(LatLng point, Marker marker, String title) {
        marker.setTitle(title);
        marker.setPosition(point);
        marker.setVisible(true);
    }
    private void drawCircle(LatLng point, int radiusSize, int i, boolean gpsDefine) {
        TypedArray ta = getResources().obtainTypedArray(R.array.colors);
        int colorToUse = ta.getResourceId(i, 0);
        circlesArray[i] = mMap.addCircle(new CircleOptions()
                .center(point)
                .radius(radiusSize)
                .strokeColor(Color.BLACK)
                .fillColor(ContextCompat.getColor(this, colorToUse))
                .strokeWidth(2)
        );
        circlesArray[i].setVisible(gpsDefine);
    }
    private void modifyCircle(LatLng point, Circle circle, int radiusSize) {
        circle.setCenter(point);
        circle.setRadius(radiusSize);
        circle.setVisible(true);
    }

    //Geofences edition function
    private void setGeofence(LatLng point, int radius,int id){
        geofenceList.add(0,new Geofence.Builder()
                .setRequestId(String.valueOf(id))
                .setCircularRegion(point.latitude,point.longitude,radius)
                .setExpirationDuration(NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER|Geofence.GEOFENCE_TRANSITION_EXIT)
                .build());

        geofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent(id))
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(getApplicationContext(), "Sucess", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
    private void removeGeofence(int id){
        geofencingClient.removeGeofences(getGeofencePendingIntent(id))
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(getBaseContext(), "Sucess to delete", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getBaseContext(), "Failed to delete", Toast.LENGTH_LONG).show();
                    }
                });

    }
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER|Geofence.GEOFENCE_TRANSITION_EXIT);
        builder.addGeofences(geofenceList);
        return builder.build();
    }
    private PendingIntent getGeofencePendingIntent(int id) {
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }
        Intent intent = new Intent(appContext, ProximityReceiver.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        geofencePendingIntent = PendingIntent.getBroadcast(appContext, id, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
        return geofencePendingIntent;
    }


    // GoogleApiClient.ConnectionCallbacks connected
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "onConnected()");
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

    //Change in location functions
    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged ["+location+"]");
        lastLocation = location;
        writeActualLocation(location);
    }
    // Get last known location
    private void getLastKnownLocation() {
        Log.d(TAG, "getLastKnownLocation()");
        if ( checkPermission() ) {
            lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            if ( lastLocation != null ) {
                Log.i(TAG, "LasKnown location. " +
                        "Long: " + lastLocation.getLongitude() +
                        " | Lat: " + lastLocation.getLatitude());
                writeLastLocation();
                startLocationUpdates();
            } else {
                Log.w(TAG, "No location retrieved yet");
                startLocationUpdates();
            }
        }
        else askPermission();
    }
    // Start location Updates
    private void startLocationUpdates(){
        Log.i(TAG, "startLocationUpdates()");
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_LOW_POWER)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);

        if ( checkPermission() ) {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        }
    }
    // Write location coordinates on UI
    private void writeActualLocation(Location location) {
        textLat.setText( "Lat: " + location.getLatitude() );
        textLong.setText( "Long: " + location.getLongitude() );
    }
    private void writeLastLocation() {
        writeActualLocation(lastLocation);
    }

    //Permissions
    // Verify user's response of the permission requested
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult()");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch ( requestCode ) {
            case 1: {
                if ( grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED ){
                    // Permission granted
                    getLastKnownLocation();

                } else {
                    // Permission denied
                    permissionsDenied();
                }
                break;
            }
        }
    }
    // Check for permission to access Location
    private boolean checkPermission() {
        Log.d(TAG, "checkPermission()");
        // Ask for permission if it wasn't granted yet
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED );
    }
    // Asks for permission
    private void askPermission() {
        Log.d(TAG, "askPermission()");
        ActivityCompat.requestPermissions(
                this,
                new String[] { Manifest.permission.ACCESS_FINE_LOCATION },1
        );
    }
    // App cannot work without the permissions
    private void permissionsDenied() {
        Log.w(TAG, "permissionsDenied()");
    }

    // Callback called when Map is touched
    @Override
    public void onMapClick(LatLng latLng) {
        Log.d(TAG, "onMapClick("+latLng +")");
    }
    // Callback called when Marker is touched
    @Override
    public boolean onMarkerClick(Marker marker) {
        Log.d(TAG, "onMarkerClickListener: " + marker.getPosition() );
        return false;
    }

    //Get back functions
    @Override
    public void onBackPressed() {
        startActivity(new Intent(MapsActivity.this, Main.class));
    }
    public void getBack(View view) {
        startActivity(new Intent(MapsActivity.this, Main.class));
    }


    @Override
    protected void onStart() {
        super.onStart();
        if(!checkPermission()){
            askPermission();
        }else {
            // Call GoogleApiClient connection when starting the Activity
            googleApiClient.connect();
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
        /*
        // Disconnect GoogleApiClient when stopping Activity
        googleApiClient.disconnect();
         */

    }
    @Override
    public void onResume() {
        super.onResume();
    }
    @Override
    public void onPause() {
        super.onPause();
    }
}

