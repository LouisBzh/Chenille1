package com.example.bureau.testhorlogesimple;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.Toast;

public class TimePicker {
    private static final String TAG = TimePicker.class.getSimpleName();
    final String[] positionIDAll = {"Famille","Travail","Voyage","Dehors","Joker","PenseAVous","Maison","PasDeNouvelles","VeuxRentrer"};
    public static final String MY_PREF="mesPrefs";
    SharedPreferences myVar;
    SharedPreferences.Editor myVarEditor;

    protected void showDialog(final Context context, final int ID){
        myVar=context.getSharedPreferences(MY_PREF,Context.MODE_PRIVATE);
        myVarEditor=myVar.edit();

        AlertDialog.Builder builderTime = new AlertDialog.Builder(context);
        LayoutInflater inflater= LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.duration_picker,null);
        builderTime.setView(dialogView);
        builderTime.setTitle("Choisir la durée à rester sur la position "+positionIDAll[ID]+":");

        //Time choice
        final NumberPicker nPDuration = dialogView.findViewById(R.id.NumberPickerDurationAlarm);
        final String[] durationPicker = {"1","2","3","4","5","10","15","20","25","30","40","50","60"};
        final Spinner spinnerDuration = dialogView.findViewById(R.id.durationUnitSpinner);
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
        builderTime.setPositiveButton("Valider", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                //Get Time update GPS
                Integer durationNumber = Integer.valueOf(durationPicker[nPDuration.getValue() - 1]);
                String durationUnit = spinnerDuration.getSelectedItem().toString();
                Integer durationAlarm;
                switch (durationUnit) {
                    case "Heures":
                        durationAlarm = durationNumber * 60 * 60 * 1000;
                        break;
                    default:
                        durationAlarm = durationNumber * 60 * 1000;
                        break;
                }

                if(myVar.getBoolean("PenseAVousCalendarDefine",true)||
                        myVar.getBoolean("VeuxRentrerCalendarDefine",true)){
                    Toast.makeText(context,"L'ancien évènement ponctuel programmé sera remplacé par celui-ci",Toast.LENGTH_LONG).show();
                    myVarEditor.putBoolean("PenseAVousCalendarIn",false);
                    myVarEditor.putBoolean("PenseAVousCalendarDefine",false);
                    myVarEditor.putBoolean("VeuxRentrerCalendarIn",false);
                    myVarEditor.putBoolean("VeuxRentrerCalendarDefine",false);
                    new AlarmReceiver().cancelAlarm(context, 5);
                    new AlarmReceiver().cancelAlarm(context, 8);
                }
                new AlarmReceiver().setElapsedAlarm(context,durationAlarm,ID);
                myVarEditor.putBoolean(positionIDAll[ID]+"CalendarIn",true);
                myVarEditor.putBoolean(positionIDAll[ID]+"CalendarDefine",true);
                myVarEditor.apply();
                Toast.makeText(context,"Position :"+positionIDAll[ID]+"\n"+
                        "Duration : "+durationNumber+" "+durationUnit,Toast.LENGTH_LONG).show();
                new Verification().UpDate(context,"TimePicker: punctual event added");
            }
        });
        builderTime.setNeutralButton("Annuler", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        if(myVar.getBoolean(positionIDAll[ID]+"CalendarIn",false)) {
            builderTime.setNegativeButton("Supprimer l'évènement", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    new AlarmReceiver().cancelAlarm(context, ID);
                    dialog.dismiss();
                    new Verification().UpDate(context,"TimePicker: ppunctual event deleted");
                }
            });
        }
        builderTime.show();
    }
}
