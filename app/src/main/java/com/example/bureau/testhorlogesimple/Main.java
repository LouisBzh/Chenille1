package com.example.bureau.testhorlogesimple;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class Main extends AppCompatActivity {
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

    //Variables Main Activity
    private static final String TAG = Main.class.getSimpleName();
    ImageView aigImg;//Hand
    String aigNewPosit;//New Hand position
    int aigPositInt;//Actual Hand position
    String positGps="0000";//String coding if gps position inside Famille/Travail/Joker/Maison
    final String[] personID ={"Maman","Papa","Marie","Louis","Camille","Perrine","Mathilde","Défaut"};
    int[] aigImgID ={R.mipmap.aig_maman,R.mipmap.aig_papa,R.mipmap.aig_marie,R.mipmap.aig_louis,R.mipmap.aig_camille,R.mipmap.aig_perrine,R.mipmap.aig_mathilde,R.mipmap.aig_defaut};//All Hand mipmap

    //Variables sms functions
    BroadcastReceiver sentBroadcast;
    BroadcastReceiver deliveredBroadcast;
    IntentFilter sentIntentFilter;
    IntentFilter deliveredIntentFilter;
    String SENT = "SMS_SENT";
    String DELIVERED = "SMS_DELIVERED";

    @Override
    protected void onStart() {
        super.onStart();
        //Check Permission
        checkPermission();
        //Register emission/reception sms
        registerReceiver(sentBroadcast, sentIntentFilter);//
        registerReceiver(deliveredBroadcast, deliveredIntentFilter);

    }
    @Override
    protected void onPause() {
        //Delete registration emission/reception sms
        if(deliveredBroadcast!=null) {
            unregisterReceiver(deliveredBroadcast);
            deliveredBroadcast=null;
        }
        if (sentBroadcast!=null) {
            unregisterReceiver(sentBroadcast);
            sentBroadcast=null;
        }
        super.onPause();
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Creation emission/reception sms functions
        sentIntentFilter=new IntentFilter(SENT);
        deliveredIntentFilter=new IntentFilter(DELIVERED);
        sentBroadcast=new sentReceiver();
        deliveredBroadcast=new deliveredReceiver();

        //Layout
        setContentView(R.layout.activity_main);

        //Get preferences inside usable variables
        myVar=getSharedPreferences(MY_PREF,Context.MODE_PRIVATE);
        myVarEditor=myVar.edit();
        aigLastPosit=myVar.getString("aigLastPosit","1");
        inFamilleGps=myVar.getBoolean("inFamilleGPS",false);
        inTravailGps=myVar.getBoolean("inTravailGPS",false);
        inJokerGps=myVar.getBoolean("inJokerGPS",false);
        inMaisonGps=myVar.getBoolean("inMaisonGPS",false);

        //Set Hand image
        aigChoosed=myVar.getInt("aigChoosed",8);
        aigImg = findViewById(R.id.Aiguille);
        aigImg.setImageResource(aigImgID[aigChoosed-1]);

        //Check gps information and hand positioning
        positGps = BooleantoString(inFamilleGps)+BooleantoString(inTravailGps)+BooleantoString(inJokerGps)+BooleantoString(inMaisonGps);
        switch (positGps){
            case "0000": //Not inside any Geofence
                aigNewPosit="4";
                break;
            case "1000": //Inside Family Geofence
                aigNewPosit="1";
                break;
            case "0100": //Inside Travail Geofence
            case "1100": //Inside Family and Travail Geofence
                aigNewPosit="2";
                break;
            case "0010": //Inside Joker Geofence
                aigNewPosit="5";
                break;
            case "0001": //Inside Maison Geofence
                aigNewPosit="7";
                break;
            default:
                aigNewPosit=aigLastPosit; //If none of above possibility = ERROR
                Toast.makeText(getBaseContext(), "Configuration des zones non pris en charge. Veuillez vérifier la disposition des zones GPS.", Toast.LENGTH_LONG).show();
                break;
        }
        setPositAuto(aigNewPosit);
    }
    //Hand positioning by code
    public void setPositAuto(String s){
        try {
            aigPositInt = Integer.parseInt(s);
        } catch(NumberFormatException nfe) {
            Toast.makeText(getBaseContext(), "Valeur position de l'aiguille non sous forme d'entier", Toast.LENGTH_LONG).show();
        }
        aigImg.setRotation(aigPositInt * 40 + 320);
        SendSMS(s);
    }
    //Hand positioning by user choice
    public void setPositManuel(String s) {
        try {
            aigPositInt = Integer.parseInt(s);
        } catch(NumberFormatException nfe) {
            Toast.makeText(getBaseContext(), "Valeur position de l'aiguille non sous forme d'entier", Toast.LENGTH_LONG).show();
        }
        if (positGps.equals("0000")){//If not inside any geofence
            aigImg.setRotation(aigPositInt * 40 + 320);
            SendSMS(s);
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
                    SendSMS(String.valueOf(aigPositInt));
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }


    //All buttons function
    public void Famille (View view){
        setPositManuel("1");
    }
    public void Travail (View view){
        setPositManuel("2");
    }
    public void Voyage (View view){
        setPositManuel("3");
    }
    public void Dehors (View view){
        setPositManuel("4");
    }
    public void Joker (View view){
        setPositManuel("5");
    }
    public void PenseAVous (View view){
        setPositManuel("6");
    }
    public void ALaMaison (View view){
        setPositManuel("7");
    }
    public void PasDeNouvelles (View view){
        setPositManuel("8");
    }
    public void VeuxRentrer (View view){
        setPositManuel("9");
    }

    //Sms functions
    //Function to send an sms
    private void SendSMS(final String x) {
        //Test if last position different than new one
        if (Integer.parseInt(myVar.getString("aigLastPosit", "1")) != Integer.parseInt(x)) {

            PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
                    new Intent(SENT), 0);

            PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
                    new Intent(DELIVERED), 0);

            // Storing the position
            myVarEditor.putString("aigLastPosit", x);
            myVarEditor.apply();
            //---when the SMS has been sent--
            android.telephony.SmsManager sms = android.telephony.SmsManager.getDefault();
            sms.sendTextMessage("+33769424262", null, x, sentPI, deliveredPI);
        }
    }
    //Sms sending receiver and Error handling
    public class sentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    Toast.makeText(getBaseContext(), "SMS sent",
                            Toast.LENGTH_SHORT).show();
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    Toast.makeText(getBaseContext(), "Generic failure",
                            Toast.LENGTH_SHORT).show();
                    break;
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                    Toast.makeText(getBaseContext(), "No service",
                            Toast.LENGTH_SHORT).show();
                    break;
                case SmsManager.RESULT_ERROR_NULL_PDU:
                    Toast.makeText(getBaseContext(), "Null PDU",
                            Toast.LENGTH_SHORT).show();
                    break;
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    Toast.makeText(getBaseContext(), "Radio off",
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }
    //Sms delivered receiver and Error handling
    public class deliveredReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    Toast.makeText(getBaseContext(), "SMS delivered",
                            Toast.LENGTH_SHORT).show();
                    break;
                case Activity.RESULT_CANCELED:
                    Toast.makeText(getBaseContext(), "SMS not delivered",
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    //Function to transform Boolean to String 1 or 0
    public static String BooleantoString(boolean b) {
        return b ? "1" : "0";
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
                Toast.makeText(getBaseContext(), "Créé par Louis Le Nézet", Toast.LENGTH_SHORT).show();
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
                myVarEditor.commit();
                return true;
            case R.id.action_aiguille: //Hand Choice
                AlertDialog.Builder builder = new AlertDialog.Builder(Main.this);
                builder.setTitle("Choisissez l'aiguille à afficher:");
                builder.setSingleChoiceItems(personID, myVar.getInt("aigChoosed",8)-1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
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
                builder.setNegativeButton("Annuler", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    @Override
    protected void onDestroy(){
         super.onDestroy();
    }

    // Check for permission to access Location
    private void checkPermission() {
        Log.d(TAG, "checkPermission()");
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
        builder.setMessage("L'envoi de sms est nécessaire afin de pouvoir communiquer avec l'horloge." +
                "Etes-vous sûr de ne pas autoriser l'application ?"+
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
                checkPermission();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    private void permissionsDeniedForever() {
        Log.w(TAG, "permissionsDenied()");
        AlertDialog.Builder builder = new AlertDialog.Builder(Main.this);
        builder.setTitle("Permissions nécessaires");
        builder.setMessage("L'envoi de sms est nécessaire afin de pouvoir communiquer avec l'horloge." +
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

}
