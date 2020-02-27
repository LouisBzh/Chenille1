package com.example.bureau.testhorlogesimple;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

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

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    //Variable preferences
    public static final String MY_PREF="mesPrefs";
    SharedPreferences myVar;
    SharedPreferences.Editor myVarEditor;

    //Variable activity
    private GoogleMap mMap;
    LocationManager locationManager;
    PendingIntent pendingIntentLocation;

    int zoneNumGPS = 4;
    Marker[] markersArray = new Marker[4];
    Circle[] circlesArray = new Circle[4];
    final String[] positionID = {"Famille", "Travail", "Joker", "A la maison"};
    String lat="";
    String lng = "";
    String radiusSize = "";
    String title = "";
    Boolean gpsDefine = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        myVar = getSharedPreferences(MY_PREF, Context.MODE_PRIVATE);
        SupportMapFragment mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFrag.getMapAsync(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(48, -2.4), 9));

        // Getting index of last location added
        zoneNumGPS = myVar.getInt("zoneNumGPS", 0);
        // Getting stored zoom level if exists else return 9
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

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
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
                                        setProximityAlert(pointing,radiusSize[0],zoneNumGPS);
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
                        removeProximityAlert(zoneNumGPS);
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

    public void getBack(View view) {
        startActivity(new Intent(MapsActivity.this, Main.class));
    }

    public void setProximityAlert(LatLng pointing,int radiusSize,int id){
        // This intent will call the activity ProximityActivity
        Intent proximityIntent = new Intent("com.example.bureau.testhorlogesimple.Proximity_Alert");
        proximityIntent.putExtra("name",positionID[id]);
        proximityIntent.putExtra("id",id);
        // Creating a pending intent which will be invoked by LocationManager when the specified region is
        // entered or exited
        pendingIntentLocation = PendingIntent.getBroadcast(getApplicationContext(), id, proximityIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        // Setting proximity alert
        // The pending intent will be invoked when the device enters or exits the region 20 meters
        // away from the marked point
        // The -1 indicates that, the monitor will not be expired
        try {
            locationManager.addProximityAlert(pointing.latitude, pointing.longitude, radiusSize, -1, pendingIntentLocation);
        } catch (SecurityException e) {
        }
    }
    public void removeProximityAlert(int id){
        Intent proximityIntent = new Intent("com.example.bureau.testhorlogesimple.Proximity_Alert");
        proximityIntent.putExtra("name",positionID[id]);
        proximityIntent.putExtra("id",id);
        pendingIntentLocation = PendingIntent.getBroadcast(getApplicationContext(), id, proximityIntent,PendingIntent.FLAG_CANCEL_CURRENT);
        // Removing the proximity alert
        locationManager.removeProximityAlert(pendingIntentLocation);
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(MapsActivity.this, Main.class));
    }
    @Override
    public void onResume() {
        super.onResume();
    }

    public void onPause() {
        super.onPause();
    }
}

