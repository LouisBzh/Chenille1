package com.example.bureau.testhorlogesimple;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;

import static com.google.android.gms.location.Geofence.NEVER_EXPIRE;

public class Main extends AppCompatActivity
    implements View.OnClickListener,
    View.OnLongClickListener{
    //Variables preferences
    public static final String MY_PREF="mesPrefs";
    public static final int RequestCode_SEND_SMS=1;
    SharedPreferences myVar;
    SharedPreferences.Editor myVarEditor;
    int aigChoosed;
    String aigLastPosit;

    //Variables GPS
    Boolean inFamilleGps;
    Boolean inTravailGps;
    Boolean inJokerGps;
    Boolean inMaisonGps;
    private GeofencingClient geofencingClient;
    PendingIntent geofencePendingIntent;
    ArrayList<Geofence> geofenceList= new ArrayList<>(1);
    final String[] positionID = {"Famille", "Travail", "Joker", "Maison"};
    GPSTracker gpsTracker;
    SmsSender smsSender;

    //Variables Main Activity
    private static final String TAG = Main.class.getSimpleName();
    Context appContext;
    ImageView aigImg;//Hand
    String aigNewPosit;//New Hand position
    int aigPositInt;//Actual Hand position
    String positGps="0000";//String coding if gps position inside Famille/Travail/Joker/Maison
    final String[] personID ={"Maman","Papa","Marie","Louis","Camille","Perrine","Mathilde","Défaut"};
    int[] aigImgID ={R.mipmap.aig_maman,R.mipmap.aig_papa,R.mipmap.aig_marie,R.mipmap.aig_louis,
            R.mipmap.aig_camille,R.mipmap.aig_perrine,R.mipmap.aig_mathilde,R.mipmap.aig_defaut};//All Hand mipmap ID
    int[] positionImgID={R.id.BtnFamille,R.id.BtnAuTravail,R.id.BtnVoyage,R.id.BtnDehors,R.id.BtnJoker,
            R.id.BtnPenseAVous,R.id.BtnALaMaison,R.id.BtnPasDeNouvelles,R.id.BtnVeuxRentrer};//All position button ID

    @Override
    protected void onStart() {
        super.onStart();
    }
    @Override
    protected void onRestart() {
        super.onRestart();
        recreate();
    }
    @Override
    protected void onStop() {
        super.onStop();
        smsSender.unRegistered();
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Layout
        setContentView(R.layout.activity_main);

        //Set onClick and onLongClick all the buttons
        for(int button=0;button<=8;button++){
            ImageView imagePosition = findViewById(positionImgID[button]);
            imagePosition.setOnClickListener(this); // calling onClick() method
            imagePosition.setOnLongClickListener(this); // calling onLongClick() method
        }

        //Get preferences inside usable variables
        myVar = getSharedPreferences(MY_PREF, Context.MODE_PRIVATE);
        myVarEditor = myVar.edit();
        aigChoosed = myVar.getInt("aigChoosed", 8);
        aigImg = findViewById(R.id.Aiguille);
        aigImg.setImageResource(aigImgID[aigChoosed - 1]);
        aigLastPosit = myVar.getString("aigLastPosit", "4");
        inFamilleGps = myVar.getBoolean("inFamilleGPS", false);
        inTravailGps = myVar.getBoolean("inTravailGPS", false);
        inJokerGps = myVar.getBoolean("inJokerGPS", false);
        inMaisonGps = myVar.getBoolean("inMaisonGPS", false);
        smsSender=new SmsSender(Main.this);
        gpsTracker=new GPSTracker(Main.this);
        if(gpsTracker.canGetLocation()){
            double latitude = gpsTracker.latitude;
            double longitude = gpsTracker.longitude;
            double speed = gpsTracker.speed;
            if(latitude!=0) {
                Toast.makeText(getApplicationContext(), "Votre position est - \nLat: "
                        + latitude + "\nLong: " + longitude + "\nVit: " + speed, Toast.LENGTH_LONG).show();
            }
        }else{
            /** can't get location
            *GPS or Network is not enabled
            *Ask user to enable GPS/network in settings
             **/
            gpsTracker.showSettingsAlert();
        }
        

        //Check Permission
        if(checkPermission()) {
            appContext = getApplicationContext();
            geofencingClient = LocationServices.getGeofencingClient(appContext);
            //Check gps information and hand positioning
            positGps = BooleantoString(inFamilleGps) + BooleantoString(inTravailGps) + BooleantoString(inJokerGps) + BooleantoString(inMaisonGps);
            if(!myVar.getBoolean("Voyage",false)){
                if (positGps.equals("0000")) {
                    Log.i(TAG,"Not inside any Geofence, neither in movement");
                    setPositManuel(aigLastPosit);
                }else{
                    Log.i(TAG,"Inside a Geofence but not in movement");
                    Toast.makeText(this,"Position GPS defined",Toast.LENGTH_SHORT).show();
                }
            }else{
                Log.i(TAG,"In movement");
                setPositManuel("3");
            }

        }else{
            askPermission();
        }
    }

    @Override
    public void onClick(View v) {
        int btnClickedID=v.getId();
        int btnClickedNum=-1;
        switch (btnClickedID){
            case R.id.BtnFamille:
            case R.id.BtnAuTravail:
            case R.id.BtnVoyage:
            case R.id.BtnDehors:
            case R.id.BtnJoker:
            case R.id.BtnALaMaison:
                btnClickedNum = indexOfInt(positionImgID,btnClickedID)+1;
                break;
            case R.id.BtnPenseAVous:
            case R.id.BtnPasDeNouvelles:
            case R.id.BtnVeuxRentrer:
                btnClickedNum = indexOfInt(positionImgID,btnClickedID)+1;
            default:
                Log.e(TAG, "onClick: Erreur");
                break;
        }
        setPositManuel(String.valueOf(btnClickedNum));
    }
    @Override
    public boolean onLongClick(View v) {
        int btnClickedID=v.getId();
        int btnClickedNum=-1;
        switch (btnClickedID){
            case R.id.BtnFamille:
            case R.id.BtnAuTravail:
            case R.id.BtnDehors:
            case R.id.BtnJoker:
            case R.id.BtnALaMaison:
                btnClickedNum = indexOfInt(positionImgID,btnClickedID);
                new DateHourSelector().showDialog(Main.this,btnClickedNum);
                break;
            case R.id.BtnPenseAVous:
            case R.id.BtnPasDeNouvelles:
            case R.id.BtnVeuxRentrer:
                btnClickedNum = indexOfInt(positionImgID,btnClickedID);
            default:
                Log.e(TAG, "onClick: NonParamétrable");
                break;
        }
        return false;
    }

    //Function to transform Boolean to String 1 or 0
    public static String BooleantoString(boolean b) {
        return b ? "1" : "0";
    }
    //Function to get index of a key inside a int array
    public static int indexOfInt(int[] array, int key){
        int returnedValue=-1;
        for (int i=0;i<array.length;i++){
            if(array[i]==key){
                returnedValue=i;
                break;
            }
        }
        return returnedValue;
    }

    //Hand positioning by user choice
    public void setPositManuel(String s) {
        //Set Hand image
        try {
            aigPositInt = Integer.parseInt(s);
        } catch(NumberFormatException nfe) {
            Log.e(TAG,"Value passed not an Integer");
            Toast.makeText(getBaseContext(), "Valeur position de l'aiguille non sous forme d'entier", Toast.LENGTH_LONG).show();
        }
        if (positGps.equals("0000")||!myVar.getBoolean("GpsEnable",false)){//If not inside any geofence
            aigImg.setRotation(aigPositInt * 40 + 320);
            smsSender.SendSMS(s);
        }else {
            AlertDialog.Builder builder = new AlertDialog.Builder(Main.this);
            builder.setTitle("Attention vous êtes définis dans une zone GPS !");
            builder.setMessage("Etes vous sûr de vouloir écraser votre position actuelle ? \n"+
                    "(Votre position se remettra à jour au prochain franchissement)");
            builder.setNegativeButton("Non", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.setPositiveButton("Oui", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    aigImg.setRotation(aigPositInt * 40 + 320);
                    smsSender.SendSMS(String.valueOf(aigPositInt));
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }


    //Menu Functions
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        //Set the notifications menu item checked corresponding to registered preferences
        menu.findItem(R.id.action_bar_notifs).setChecked(myVar.getBoolean("notifsEnable",true));
        //Set the notifications menu item checked corresponding to registered preferences
        menu.findItem(R.id.action_bar_GPS).setChecked(myVar.getBoolean("GpsEnable",false));
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_gps:
                startActivity(new Intent(Main.this, MapsActivity.class));
                return true;
            case R.id.action_infos: //Pop-up d'informations
                Toast.makeText(getBaseContext(), "Créé par Louis Le Nézet \n"+
                        "Version 1.2.0", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.action_bar_notifs: //Gps notifications permission
                if(item.isChecked()){
                    Toast.makeText(getBaseContext(), "Notifications désactivées", Toast.LENGTH_SHORT).show();
                    item.setChecked(false);
                    myVarEditor.putBoolean("notifsEnable",false);
                }else{
                    Toast.makeText(getBaseContext(), "Notifications activées", Toast.LENGTH_SHORT).show();
                    item.setChecked(true);
                    myVarEditor.putBoolean("notifsEnable",true);
                }
                myVarEditor.apply();
                return true;
            case R.id.action_bar_GPS: //Gps fonction permission
                if(item.isChecked()){
                    Toast.makeText(getBaseContext(), "Fonction GPS désactivée", Toast.LENGTH_SHORT).show();
                    item.setChecked(false);
                    myVarEditor.putBoolean("GpsEnable",false);
                    removeAllGeofence();
                }else{
                    Toast.makeText(getBaseContext(), "Fonction GPS activée", Toast.LENGTH_SHORT).show();
                    item.setChecked(true);
                    myVarEditor.putBoolean("GpsEnable",true);
                    setAllGeofence();
                }
                myVarEditor.apply();
                return true;
            case R.id.action_aiguille: //Hand Choice
                AlertDialog.Builder builderAig = new AlertDialog.Builder(Main.this);
                builderAig.setTitle("Choisissez l'aiguille à afficher:");
                builderAig.setSingleChoiceItems(personID, myVar.getInt("aigChoosed",8)-1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
                builderAig.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // Get item selected
                        aigChoosed = ((AlertDialog) dialog).getListView().getCheckedItemPosition()+1;
                        // Opening the editor object to replace data from sharedPreferences
                        SharedPreferences.Editor myVarEditor = myVar.edit();
                        // Change hand preference from sharedPreference
                        myVarEditor.putInt("aigChoosed",aigChoosed);
                        // Committing the changes
                        myVarEditor.apply();
                        // Restart activity
                        recreate();
                    }
                });
                builderAig.setNegativeButton("Annuler", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                AlertDialog dialogAig = builderAig.create();
                dialogAig.show();
                return true;
            case R.id.params_Gps: //GPS parameters selection
                GPSsettings();
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    //GPS settings
    public void GPSsettings(){
        AlertDialog.Builder builderGps = new AlertDialog.Builder(Main.this);
        // Get the layout inflater
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.gps_settings,null);
        builderGps.setView(dialogView);

        //Time interval choice
        final NumberPicker nPDuration = dialogView.findViewById(R.id.NumberPickerDuration);
        final String[] durationPicker = {"1","2","3","4","5","10","15","20","25","30","40","50","60"};
        final Spinner spinnerDuration = dialogView.findViewById(R.id.durationSpinner);
        nPDuration.setMinValue(1);
        nPDuration.setMaxValue(durationPicker.length);
        nPDuration.setValue(1);
        nPDuration.setDisplayedValues(durationPicker);
        nPDuration.setWrapSelectorWheel(true);
        nPDuration.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                Log.d(TAG, "onValueChange: ");
            }
        });

        //Time interval choice
        final NumberPicker nPDistance = dialogView.findViewById(R.id.NumberPickerDistance);
        final String[] distancePicker = {"1","2","3","4","5","6","7","8","9","10"};
        final Spinner spinnerDistance = dialogView.findViewById(R.id.distanceSpinner);
        nPDistance.setMinValue(1);
        nPDistance.setMaxValue(distancePicker.length);
        nPDistance.setValue(1);
        nPDistance.setDisplayedValues(distancePicker);
        nPDistance.setWrapSelectorWheel(true);
        nPDistance.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                Log.d(TAG, "onValueChange: ");
            }
        });

        //Voyage speed choice
        final NumberPicker nPSpeed = dialogView.findViewById(R.id.NumberPickerSpeed);
        final String[] speedPicker = {"5","10","15","20","25","30","40","50","60","70","80","90","100"};
        nPSpeed.setMinValue(1);
        nPSpeed.setMaxValue(speedPicker.length);
        nPSpeed.setValue(6);
        nPSpeed.setDisplayedValues(speedPicker);
        nPSpeed.setWrapSelectorWheel(true);
        nPSpeed.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                Log.d(TAG, "onValueChange: ");
            }
        });
        builderGps.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                //Get Time update GPS
                Integer durationNumber= Integer.valueOf(durationPicker[nPDuration.getValue()-1]);
                String durationUnit =spinnerDuration.getSelectedItem().toString();
                Integer timeUpDateGPS;
                switch (durationUnit){
                    case "Minutes":
                        timeUpDateGPS=durationNumber*60*1000;
                        break;
                    case "Heures":
                        timeUpDateGPS=durationNumber*60*60*1000;
                        break;
                    default:
                        timeUpDateGPS=durationNumber*1000;
                        break;
                }
                //Get Distance update GPS
                Integer distanceNumber = Integer.valueOf(distancePicker[nPDistance.getValue()-1]);
                String distanceUnit = spinnerDistance.getSelectedItem().toString();
                Integer distanceUpDateGPS;
                switch (distanceUnit){
                    case "Kilomètres":
                        distanceUpDateGPS=distanceNumber*1000;
                        break;
                    case "Hectomètres":
                        distanceUpDateGPS=distanceNumber*100;
                        break;
                    case "Décamètres":
                        distanceUpDateGPS=distanceNumber*10;
                        break;
                    default:
                        distanceUpDateGPS=distanceNumber;
                        break;
                }
                // Get Minimal speed selected
                Integer speedNumber=Integer.valueOf(speedPicker[nPSpeed.getValue()-1]);
                // Opening the editor object to replace data from sharedPreferences
                SharedPreferences.Editor myVarEditor = myVar.edit();
                // Change GPS settings in sharedPreference
                myVarEditor.putInt("timeUpDateGPS",timeUpDateGPS);
                myVarEditor.putInt("distanceUpDateGPS",distanceUpDateGPS);
                myVarEditor.putInt("speedMin",speedNumber);
                Toast.makeText(getBaseContext(),"Update interval in millisecondes : "+timeUpDateGPS,Toast.LENGTH_SHORT).show();// Committing the changes
                Toast.makeText(getBaseContext(),"Update distance in meters : "+distanceUpDateGPS,Toast.LENGTH_SHORT).show();// Committing the changes
                Toast.makeText(getBaseContext(),"Minimal speed for Voyage : "+speedNumber+" km/h",Toast.LENGTH_SHORT).show();// Committing the changes
                myVarEditor.apply();
                recreate();
            }
        });
        builderGps.setNegativeButton("Annuler", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialogGps = builderGps.create();
        dialogGps.show();
    }

    @Override
    protected void onDestroy(){
         super.onDestroy();
    }

    // Check for permission to access Location
    private boolean checkPermission() {
        Log.d(TAG, "checkPermission()");
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED );
    }
    //Ask for permission to send sms
    public void askPermission(){
        if (!(ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED )){

            Log.d(TAG, "askPermission()");
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.SEND_SMS}, RequestCode_SEND_SMS);
        }
    }
    // Verify user's response of the permission requested
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult()");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch ( requestCode ) {
            case RequestCode_SEND_SMS: {
                if ( grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED ){
                    // Permission granted continue activity
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(Main.this, Manifest.permission.SEND_SMS)) {
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
        AlertDialog.Builder builder = new AlertDialog.Builder(Main.this);
        builder.setTitle("Permissions nécessaires");
        builder.setMessage("L'envoi de sms est nécessaire afin de pouvoir communiquer avec l'horloge.\n" +
                "Etes-vous sûr de ne pas autoriser l'application ?\n"+
                "(L'application se fermera !)");
        builder.setPositiveButton("Rien à foutre !", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });
        builder.setNegativeButton("Oups, désolé !", new DialogInterface.OnClickListener() {
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
        AlertDialog.Builder builder = new AlertDialog.Builder(Main.this);
        builder.setTitle("Permissions nécessaires");
        builder.setMessage("L'envoi de sms est nécessaire afin de pouvoir communiquer avec l'horloge.\n" +
                "Veuillez autoriser son utilisation dans les autorisations systèmes de l'application si vous voulez utiliser cette application! ");
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    public void onBackPressed() {
        finish();
    }

    //Activation/Deletion GPS fonction
    private void removeAllGeofence(){
        for(int idGeofence=0;idGeofence<=3;idGeofence++){
            final String idGeofenceString = positionID[idGeofence];
            if(myVar.getBoolean("gpsDefine"+idGeofenceString,false)) {
                geofencingClient.removeGeofences(getGeofencePendingIntent(idGeofence))
                        .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(getBaseContext(), idGeofenceString + " succeed to delete", Toast.LENGTH_LONG).show();
                            }
                        })
                        .addOnFailureListener(this, new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(getBaseContext(), idGeofenceString + " failed to delete\n" + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
            }
        }
    }
    //Geofence edition function
    private void setAllGeofence(){
        for(int idGeofence=0;idGeofence<=3;idGeofence++) {
            final String idGeofenceString=positionID[idGeofence];
            if(myVar.getBoolean("gpsDefine"+idGeofenceString,false)) {
                LatLng point = new LatLng(Double.parseDouble(myVar.getString("lat" + idGeofenceString, "NA")),
                        Double.parseDouble(myVar.getString("lng" + idGeofenceString, "NA")));
                int radius = myVar.getInt("radiusSize" + idGeofenceString, 0);
                //Modify Geofence inside the list
                geofenceList.add(0, new Geofence.Builder()
                        .setRequestId(String.valueOf(idGeofence))
                        .setCircularRegion(point.latitude, point.longitude,radius)
                        .setExpirationDuration(NEVER_EXPIRE)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                        .build());
                //Add the geofence receiver
                geofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent(idGeofence))
                        .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(getApplicationContext(), idGeofenceString + "Sucess", Toast.LENGTH_LONG).show();
                            }
                        })
                        .addOnFailureListener(this, new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(getApplicationContext(), idGeofenceString + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
            }
        }
    }
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER|Geofence.GEOFENCE_TRANSITION_EXIT);
        builder.addGeofences(geofenceList);
        return builder.build();
    }
    private PendingIntent getGeofencePendingIntent(int id) {
        if (geofencePendingIntent != null) {
            Log.v("Geofence","Geofence pending intent already existant");
            return geofencePendingIntent;
        }
        Intent intent = new Intent(appContext, ProximityReceiver.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        geofencePendingIntent = PendingIntent.getBroadcast(appContext, id, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
        return geofencePendingIntent;
    }
}
