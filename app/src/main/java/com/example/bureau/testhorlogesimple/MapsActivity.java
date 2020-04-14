package com.example.bureau.testhorlogesimple;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.support.v4.content.LocalBroadcastManager;
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
        //GoogleApiClient.ConnectionCallbacks,
        //GoogleApiClient.OnConnectionFailedListener,
        OnMapReadyCallback{

    //Preferences
    public static final String MY_PREF="mesPrefs";
    SharedPreferences myVar;
    SharedPreferences.Editor myVarEditor;
    int zoneNumGPS = 4;
    final String[] positionID = {"Famille", "Travail", "Joker", "Maison"};
    String lat="";
    String lng = "";
    Integer radiusSize;
    String title = "";
    Boolean gpsDefine = false;

    //Activity variables
    private static final String TAG = MapsActivity.class.getSimpleName();
    Context appContext;
    public static final int RequestCode_GPS_FINE=2;

    //Map variable
    private TextView textLat, textLong,textSpeed;
    private GoogleMap mMap;
    Marker[] markersArray = new Marker[4];
    Circle[] circlesArray = new Circle[4];

    //Location
    LocationManager locationManager;
    private Location lastLocation;
    private LocationRequest locationRequest;

    //Geofencing API
    private GoogleApiClient googleApiClient;
    private GeofencingClient geofencingClient;
    ArrayList<Geofence> geofenceList= new ArrayList<>(1);
    PendingIntent geofencePendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Preferences
        myVar = getSharedPreferences(MY_PREF, Context.MODE_PRIVATE);
        myVarEditor = myVar.edit();
        if(checkPermission()) {
            if(myVar.getBoolean("GpsEnable",false)) {
                appContext = getApplicationContext();
                //Layout
                setContentView(R.layout.activity_maps);
                textLat = findViewById(R.id.Lat);
                textLong = findViewById(R.id.Long);
                textSpeed = findViewById(R.id.Speed);
                SupportMapFragment mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                mapFrag.getMapAsync(this);
                textLat.setText("Lat: " + GPSTracker.latitude);
                textLong.setText("Lng: " + GPSTracker.longitude);
                textSpeed.setText("Spd: " + GPSTracker.speed);

                LocalBroadcastManager.getInstance(this).registerReceiver(
                        new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                double latitude = intent.getDoubleExtra(GPSTracker.EXTRA_LATITUDE, 00);
                                double longitude = intent.getDoubleExtra(GPSTracker.EXTRA_LONGITUDE, 00);
                                double speed = intent.getDoubleExtra(GPSTracker.EXTRA_SPEED,00);
                                textLat.setText("Lat: " + latitude);
                                textLong.setText("Lng: " + longitude);
                                textSpeed.setText("Spd: " + speed);
                            }
                        }, new IntentFilter(GPSTracker.ACTION_LOCATION_BROADCAST)
                );

                //Location
                locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                geofencingClient = LocationServices.getGeofencingClient(appContext);
                //createGoogleApi();
                // Call GoogleApiClient connection when starting the Activity
                //googleApiClient.connect();
            }else{
                Toast.makeText(getBaseContext(),"GPS non activé",Toast.LENGTH_LONG).show();
                startActivity(new Intent(MapsActivity.this, Main.class));
            }
        }else{
            askPermission();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(48, -2.4), 9));

        //Getting index of last location added
        zoneNumGPS = myVar.getInt("zoneNumGPS", 0);

        //Iterating through all the locations to create circles and markers
        for (int i = 0; i < 4; i++) {
            //Getting the latitude of the i-th location
            lat = myVar.getString("lat" + positionID[i], "0");
            //Getting the longitude of the i-th location
            lng = myVar.getString("lng" + positionID[i], "0");
            //Getting the radius size of the i-th location
            radiusSize = myVar.getInt("radiusSize" + positionID[i], 1000);
            // Getting the title of the i-th location
            title = myVar.getString("title" + positionID[i], "Null");
            //Getting if the i-th location is define
            gpsDefine = myVar.getBoolean("gpsDefine" + positionID[i], false);
            //Creating Markers
            drawMarker(new LatLng(Double.parseDouble(lat), Double.parseDouble(lng)), title, i, gpsDefine);
            //Drawing circle on the map
            drawCircle(new LatLng(Double.parseDouble(lat), Double.parseDouble(lng)), radiusSize, i, gpsDefine);
        }

        //When Map is clicked
        //region MapShortClick
        //Creation or Updating Geofence
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

                        //Radius Size Alert dialog
                        final AlertDialog.Builder seekBarDialog = new AlertDialog.Builder(MapsActivity.this);
                        final LayoutInflater inflater = (LayoutInflater) MapsActivity.this.getSystemService(LAYOUT_INFLATER_SERVICE);
                        final View ViewLayout = inflater.inflate(R.layout.seek_bar_dialog, (ViewGroup) findViewById(R.id.layout_dialog));
                        seekBarDialog.setView(ViewLayout);

                        //Parameters of the seekBar
                        seekBarDialog.setTitle("Sélectionner le rayon de la zone :");
                        final int[] radiusSize = new int[1];
                        final TextView txtView = ViewLayout.findViewById(R.id.txtItem);
                        SeekBar seek = ViewLayout.findViewById(R.id.seekBar);
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

                        // When Radius size is selected
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
                                        myVarEditor.putInt("radiusSize" + positionID[zoneNumGPS], radiusSize[0]);
                                        // Storing index of the last locations
                                        myVarEditor.putInt("locationNum", zoneNumGPS);
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
                //Negative button for geofence creation
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
        //endregion

        //region MapLongClick
        //Geofence deletion
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
                //Abandon deletion
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
        //endregion
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

    //Geofence edition function
    private void setGeofence(LatLng point, int radius,int id){
        //Modify Geofence inside the list
        geofenceList.add(0,new Geofence.Builder()
                .setRequestId(String.valueOf(id))
                .setCircularRegion(point.latitude,point.longitude,radius)
                .setExpirationDuration(NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER|Geofence.GEOFENCE_TRANSITION_EXIT)
                .build());
        //Add the geofence receiver
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
    //Geofence pending intent deletion
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

    /*
    // Create GoogleApiClient instance
    private void createGoogleApi() {
        Log.d(TAG, "createGoogleApi()");
        if ( googleApiClient == null ) {
            googleApiClient = new GoogleApiClient.Builder( appContext )
                    .addConnectionCallbacks( this )
                    .addOnConnectionFailedListener( this )
                    .addApi( LocationServices.API )
                    .build();
        }else{
            Log.d(TAG, "GoogleApi already created");
        }
    }
    // GoogleApiClient.ConnectionCallbacks connected
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "onConnected()");
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
     */

    //Check for permission to access Location
    private boolean checkPermission() {
        Log.d(TAG, "checkPermission()");
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED );
    }
    //Ask ACCESS_FINE_LOCATION
    private void askPermission(){
        // Ask for permission if it wasn't granted yet
        if(!(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)){
            Log.d(TAG, "askPermission()");
            ActivityCompat.requestPermissions(
                    this,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION },RequestCode_GPS_FINE);
        }
    }
    // Verify user's response of the permission requested
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult()");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch ( requestCode ) {
            case RequestCode_GPS_FINE: {
                if ( grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED ){
                    // Permission granted
                    myVarEditor.putBoolean("GpsEnable",true);
                    myVarEditor.apply();
                    recreate();
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        //Show permission explanation dialog...
                        permissionsDenied();
                    }else{
                        //Never ask again selected, or device policy prohibits the app from having that permission.
                        //So, disable that feature, or fall back to another situation...
                        permissionsDeniedForever();
                    }
                }
                break;
            }
        }
    }
    // App cannot work without the permissions
    private void permissionsDenied() {
        Log.w(TAG, "permissionsDenied()");
        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        builder.setTitle("Permissions nécessaires");
        builder.setMessage("Le GPS est nécessaire à l'utilisation de cette fonctionnalité.\n" +
                "Etes-vous sûr de ne pas autoriser l'application à l'utiliser ?\n"+
                "(Cette fonctionnalité ne sera donc pas utilisée)");
        builder.setPositiveButton("Rien à foutre !", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                myVarEditor.putBoolean("GpsEnable",false);
                myVarEditor.apply();
                startActivity(new Intent(MapsActivity.this, Main.class));
            }
        });
        builder.setNegativeButton("Bon, d'accord !", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                askPermission();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    private void permissionsDeniedForever() {
        Log.w(TAG, "permissionsDenied()");
        myVarEditor.putBoolean("GpsEnable",false);
        myVarEditor.apply();
        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        builder.setTitle("Permissions nécessaires");
        builder.setMessage("Le GPS est nécessaire à l'utilisation de cette fonctionnalité.\n" +
                "Veuillez autoriser son utilisation dans les autorisations systèmes de l'application si vous voulez utiliser cette fonctionnalité! ");
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                startActivity(new Intent(MapsActivity.this, Main.class));
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
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
        startService(new Intent(this, GPSTracker.class));
    }
    @Override
    public void onPause() {
        super.onPause();
        stopService(new Intent(this, GPSTracker.class));
    }
}

