package com.example.bureau.testhorlogesimple;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class Main extends AppCompatActivity {
    //Variables preferences
    public static final String MY_PREF="mesPrefs";
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
    ImageView aigImg;//Aiguille
    String aigNewPosit;//Nouvelle position aiguille
    int aigPositInt;//Position actuelle aiguille
    String positGps="0000";//String codant si à l'intérieur de Famille/Travail/Joker/Maison
    final String[] personID ={"Maman","Papa","Marie","Louis","Camille","Perrine","Mathilde","Défaut"};
    int[] aigImgID ={R.mipmap.aig_maman,R.mipmap.aig_papa,R.mipmap.aig_marie,R.mipmap.aig_louis,R.mipmap.aig_camille,R.mipmap.aig_perrine,R.mipmap.aig_mathilde,R.mipmap.aig_defaut};//Ensemble images aiguilles personnes

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
        //Enregistrement emission/reception sms
        registerReceiver(sentBroadcast, sentIntentFilter);//
        registerReceiver(deliveredBroadcast, deliveredIntentFilter);
    }
    @Override
    protected void onPause() {
        //Supression enregistrement emission/reception
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
        //Creation emission/reception sms
        sentIntentFilter=new IntentFilter(SENT);
        deliveredIntentFilter=new IntentFilter(DELIVERED);
        sentBroadcast=new sentReceiver();
        deliveredBroadcast=new deliveredReceiver();

        setContentView(R.layout.activity_main);

        //Get preferences inside usable variables
        myVar=getSharedPreferences(MY_PREF,Context.MODE_PRIVATE);
        myVarEditor = myVar.edit();
        aigLastPosit=myVar.getString("aigLastPosit","1");
        inFamilleGps=myVar.getBoolean("inFamilleGPS",false);
        inTravailGps=myVar.getBoolean("inTravailGPS",false);
        inJokerGps=myVar.getBoolean("inJokerGPS",false);
        inMaisonGps=myVar.getBoolean("inMaisonGPS",false);

        //Set aiguille image
        aigChoosed=myVar.getInt("aigChoosed",8);
        aigImg = findViewById(R.id.Aiguille);
        aigImg.setImageResource(aigImgID[aigChoosed-1]);

        //Verification gps information and hand positionning
        positGps = BooleantoString(inFamilleGps)+BooleantoString(inTravailGps)+BooleantoString(inJokerGps)+BooleantoString(inMaisonGps);
        switch (positGps){
            case "0000":
                aigNewPosit="4";
                break;
            case "1000":
                aigNewPosit="1";
                break;
            case "0100":
            case "1100":
                aigNewPosit="2";
                break;
            case "0010":
                aigNewPosit="5";
                break;
            case "0001":
                aigNewPosit="7";
                break;
            default:
                aigNewPosit=aigLastPosit;
                Toast.makeText(getBaseContext(), "Configuration des zones non pris en charge. Veuillez vérifier la disposition des zones GPS.", Toast.LENGTH_LONG).show();
                break;
        }

        setPosit(aigNewPosit);
    }

    public void setPosit(String s) { //Fonction to set hand rotation
        try {
            aigPositInt = Integer.parseInt(s);
        } catch(NumberFormatException nfe) {
            Toast.makeText(getBaseContext(), "Valeur position de l'aiguille non sous forme d'entier", Toast.LENGTH_LONG).show();
        }
        aigImg.setRotation(aigPositInt*40+320);
        SendSMS(s);
    }


    //Ensemble des fonctions lors d'un appui sur un des boutons
    public void Famille (View view){
        setPosit("1");
    }
    public void Travail (View view){
        setPosit("2");
    }
    public void Voyage (View view){
        setPosit("3");
    }
    public void Dehors (View view){
        setPosit("4");
    }
    public void Joker (View view){
        setPosit("5");
    }
    public void PenseAVous (View view){
        setPosit("6");
    }
    public void ALaMaison (View view){
        setPosit("7");
    }
    public void PasDeNouvelles (View view){
        setPosit("8");

    }
    public void VeuxRentrer (View view){
        setPosit("9");
    }

    private void SendSMS(final String x) { //Fonction d'envoi des sms
        if (Integer.parseInt(myVar.getString("aigLastPosit", "1")) != Integer.parseInt(x)) { //Test if last position different than new one

            PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
                    new Intent(SENT), 0);

            PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
                    new Intent(DELIVERED), 0);

            // Storing the position
            myVarEditor.putString("aigLastPosit", x);
            myVarEditor.commit();
            //---when the SMS has been sent--
            android.telephony.SmsManager sms = android.telephony.SmsManager.getDefault();
            sms.sendTextMessage("+36769424262", null, x, sentPI, deliveredPI);
        }
    }//Fonction d'envoi des sms

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
    } //Fonction emission sms

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
    } //Fonction reception sms

    public static String BooleantoString(boolean b) {
        return b ? "1" : "0";
    }


    //Fonctions de menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        //Set the notifications menu item checked corresponding to preferences
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
            case R.id.action_bar_notifs: //Autorisation ou non des notifications GPS
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
            case R.id.action_aiguille: //Choix de l'aiguille a afficher
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
                        myVarEditor.commit();
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

}
