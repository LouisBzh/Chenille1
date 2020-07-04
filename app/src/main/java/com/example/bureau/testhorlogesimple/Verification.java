package com.example.bureau.testhorlogesimple;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;


public class Verification{
    String newPosition = "ToUpdate";

    private static final String TAG = Verification.class.getSimpleName();
    public static final String MY_PREF="mesPrefs";
    SharedPreferences myVar;
    SharedPreferences.Editor myVarEditor;
    final String[] positionIDAll = {"Famille","Travail","Voyage","Dehors","Joker","PenseAVous","Maison","PasDeNouvelles","VeuxRentrer"};

    public void GpsOverlap(Context context){
        myVar = context.getSharedPreferences(MY_PREF, Context.MODE_PRIVATE);
        double REarth=6378137;
        for(int i1=0;i1<9;i1++){
            for(int i2=i1;i2<9;i2++){
                try {
                    double lat1=Deg2Rad(Double.parseDouble(myVar.getString(positionIDAll[i1]+"lat","0")));
                    double lon1=Deg2Rad(Double.parseDouble(myVar.getString(positionIDAll[i1]+"lon","0")));
                    int rad1=myVar.getInt(positionIDAll[i1]+"rad",0);
                    double lat2=Deg2Rad(Double.parseDouble(myVar.getString(positionIDAll[i2]+"lat","0")));
                    double lon2=Deg2Rad(Double.parseDouble(myVar.getString(positionIDAll[i2]+"lon","0")));
                    int rad2=myVar.getInt(positionIDAll[i2]+"rad",0);
                    if(lat1*lon1*lat2*lon2!=0){
                        //Is part of circle x inside circle y
                        double dlon=lon2-lon1;
                        double d = REarth*Math.acos(Math.sin(lat1)*Math.sin(lat2)+
                                Math.cos(lat1)*Math.cos(lat2)*Math.cos(dlon));
                        if (d<rad1+rad2){
                            Log.i(TAG,"Positions "+positionIDAll[i1]+" & "+positionIDAll[i2]+"overlapping");
                        }
                    }
                }catch (NumberFormatException nfe){
                    Log.e(TAG,"Value passed not double");
                }
            }
        }
    }

    public void CalOverlap(Context context){
        myVar = context.getSharedPreferences(MY_PREF, Context.MODE_PRIVATE);
        for(int i1=0;i1<9;i1++){
            for(int i2=i1;i2<9;i2++){
                for(int i3=0;i3<7;i3++) {
                    try {
                        int Hr1E = myVar.getInt(positionIDAll[i1] + "HourEnd", -1);
                        int Min1E = myVar.getInt(positionIDAll[i1] + "MinEnd", -1);
                        int Hr2S = myVar.getInt(positionIDAll[i2] + "HourStart", -1);
                        int Min2S = myVar.getInt(positionIDAll[i2] + "MinStart", -1);
                        //Test if start of 1 activity overlap another for each day
                        double HrMin1E=Hr1E+Min1E/60;
                        double HrMin2S=Hr2S+Min2S/60;
                        if(HrMin2S<HrMin1E){
                            Log.i(TAG,"Positions "+positionIDAll[i1]+" & "+positionIDAll[i2]+"overlapping");
                        }
                    } catch (NumberFormatException nfe) {
                        Log.e(TAG, "Value passed not integer");
                    }
                }
            }
        }
    }

    public void CalCheckIn(Context context){
        myVar = context.getSharedPreferences(MY_PREF, Context.MODE_PRIVATE);
        myVarEditor=myVar.edit();
        for(int i=0;i<9;i++){

        }
    }

    public String UpDate(Context context,String context2){
        Log.i(TAG,"Update asked from "+context2);
        myVar = context.getSharedPreferences(MY_PREF, Context.MODE_PRIVATE);
        myVarEditor=myVar.edit();
        //Get Manual positioning
        String AutoManual = myVar.getString("AutoManualSet","Auto");
        final String manualPosit = myVar.getString("ManualPosit","4");
        //Get SharedPreference for Calendar and Gps
        final String[] IDGPSCal = {"Famille", "Travail", "Joker", "Maison"};
        String aigLastPosit = myVar.getString("aigLastPosit", "4");
        String GpsCalPriority = myVar.getString("GpsCalPriority", "Cal");
        //Stock all Gps and Calendar infos inside boolean arrays
        Boolean[] GpsIn = new Boolean[4];
        Boolean[] GpsDefine = new Boolean[4];
        Boolean[] CalIn = new Boolean[4];
        Boolean[] CalDefine = new Boolean[4];
        for (int i = 0; i < 4; i++) {
            GpsIn[i] = myVar.getBoolean(IDGPSCal[i] + "GpsIn", true);
            GpsDefine[i] = myVar.getBoolean(IDGPSCal[i] + "GpsDefine", false);
            CalIn[i] = myVar.getBoolean(IDGPSCal[i] + "CalendarIn", true);
            CalDefine[i] = myVar.getBoolean(IDGPSCal[i] + "CalendarDefine", false);
        }
        //Get all punctual event (Voyage, PenseAVous, VeuxRentrer)
        String PunctualEvent = BooleantoString(myVar.getBoolean("Voyage", false)) +
                BooleantoString(myVar.getBoolean("PenseAVousCalendarIn", false)) +
                BooleantoString(myVar.getBoolean("VeuxRentrerCalendarIn", false));
        //Transform arrays in string
        String GpsCalInS = "";
        String CalInS = "";
        String GpsInS = "";
        String CalDefineS = "";
        String GpsDefineS = "";
        for (int i = 0; i < 4; i++) {
            CalInS = CalInS + BooleantoString(CalIn[i] & CalDefine[i]);
            GpsInS = GpsInS + BooleantoString(GpsIn[i] & GpsDefine[i]);
            CalDefineS = CalDefineS + BooleantoString(CalDefine[i]);
            GpsDefineS = GpsDefineS + BooleantoString(GpsDefine[i]);

        }

        if(AutoManual.equals("Manual")){
            if (GpsInS.equals("0000")&CalInS.equals("0000")){//If not inside any geofence
                newPosition = manualPosit;
            }else {
                if(myVar.getBoolean("SurEcritureAutorise",false)){
                    newPosition=manualPosit;
                }else{
                    if(!GpsInS.equals("0000")&CalInS.equals("0000")) {
                        newPosition = "ErreurSurEcritureGPS";
                    }else if(GpsInS.equals("0000")&!CalInS.equals("0000")){
                        newPosition = "ErreurSurEcritureCal";
                    }else if(!GpsInS.equals("0000")&!CalInS.equals("0000")){
                        newPosition = "ErreurSurEcritureGPSCal";
                    }
                }

            }
        }else if (AutoManual.equals("Auto")) {
            //Select between calendar and gps informations
            if (!CalInS.equals("0000")) {//Inside an event
                if (GpsInS != "0000") {//Inside Gps
                    //Test Calendar VS Gps priority
                    if (GpsCalPriority == "Cal") {
                        GpsCalInS = CalInS;
                    } else if (GpsCalPriority == "Gps") {
                        GpsCalInS = GpsInS;
                    } else {
                        Log.e(TAG, "GpsCalPriority not correctly define");
                    }
                } else {//Inside an event and outside Gps
                    GpsCalInS = CalInS;
                }
            } else if (!GpsInS.equals("0000")) {//Outside any event and inside Gps
                GpsCalInS = GpsInS;
            } else {//Outside any event and outside Gps
                GpsCalInS = "0000";
            }
            //If no punctual event use CalGps events
            if (PunctualEvent.equals("000")) {
                //No punctual event and no programed event
                if (GpsDefineS.equals("0000") & CalDefineS.equals("0000")) {
                    newPosition = "8";//Pas de nouvelles
                //No punctual event but programed event possible
                } else {
                    switch (GpsCalInS) {
                        case "0000": //Not inside any Geofence
                            newPosition = "4";
                            break;
                        case "1000": //Inside Family Geofence
                            newPosition = "1";
                            break;
                        case "0100": //Inside Travail Geofence
                        case "1100": //Inside Family and Travail
                        case "0101": //Inside Maison and Joker
                            newPosition = "2";
                            break;
                        case "0010": //Inside Joker Geofence
                        case "1010": //Inside Family and Joker
                        case "0011": //Inside Maison and Joker
                            newPosition = "5";
                            break;
                        case "0001": //Inside Maison Geofence
                            newPosition = "7";
                            break;
                        default:
                            newPosition = aigLastPosit; //If none of above possibility = ERROR
                            Toast.makeText(context, "Configuration des zones non pris en charge. Veuillez vérifier la disposition des zones GPS ou Calendrier.", Toast.LENGTH_LONG).show();
                            GpsOverlap(context);
                            CalOverlap(context);
                            break;
                    }
                }
            //Punctual event in process
            } else {
                switch (PunctualEvent) {
                    case "100":
                        newPosition = "3";
                        break;
                    case "110":
                    case "010":
                        newPosition = "6";
                        break;
                    case "101":
                    case "001":
                        newPosition = "9";
                        break;
                    default:
                        newPosition = aigLastPosit; //If none of above possibility = ERROR
                        Toast.makeText(context, "Erreur dans les évènements ponctuels !", Toast.LENGTH_LONG).show();

                }
            }
        }else{
            Log.e(TAG,"AutoManualSet not defined correctly");
        }
        if(!newPosition.equals("ErreurSurEcriture")) {
            SmsSender smsSender = new SmsSender(context);
            smsSender.SendSMS(newPosition);
        }
        return newPosition;
    }

    //Function to transform Degrees in Radians
    private double Deg2Rad(double deg){
        double rad=deg*Math.PI/180;
        return rad;
    }
    //Function to transform Boolean to String 1 or 0
    public static String BooleantoString(boolean b) {
        return b ? "1" : "0";
    }
}
