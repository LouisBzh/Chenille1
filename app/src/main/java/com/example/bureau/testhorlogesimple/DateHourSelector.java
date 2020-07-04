package com.example.bureau.testhorlogesimple;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.Calendar;
import java.util.Date;

public class DateHourSelector {
    private static final String TAG = DateHourSelector.class.getSimpleName();
    final String[] positionIDAll = {"Famille","Travail","Voyage","Dehors","Joker","PenseAVous","Maison","PasDeNouvelles","VeuxRentrer"};
    int[] daysBtnID={R.id.lundiBtn,R.id.mardiBtn,R.id.mercrediBtn,R.id.jeudiBtn,R.id.vendrediBtn,
            R.id.samediBtn,R.id.dimancheBtn};//All days button ID
    public static final String MY_PREF="mesPrefs";
    SharedPreferences myVar;
    SharedPreferences.Editor myVarEditor;
    boolean[] checkedDays=new boolean[7];
    ToggleButton[] daysOfWeekBtn=new ToggleButton[7];


    protected void showDialog(final Context context, final int ID){
        myVar=context.getSharedPreferences(MY_PREF,Context.MODE_PRIVATE);
        myVarEditor=myVar.edit();

        AlertDialog.Builder builderTime = new AlertDialog.Builder(context);
        LayoutInflater inflater= LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.date_hour_selector,null);
        builderTime.setView(dialogView);
        builderTime.setTitle("Choisir les jours pour la position "+positionIDAll[ID]+":");

        String prevDayOfWeek=myVar.getString(positionIDAll[ID]+"DaysOfWeek","0000000");

        //Days selection
        for (int i=0;i<7;i++){
            checkedDays[i]=CharToBoolean(prevDayOfWeek.charAt(i));
            daysOfWeekBtn[i]=dialogView.findViewById(daysBtnID[i]);
            daysOfWeekBtn[i].setChecked(checkedDays[i]);//Set already existant progammation
            daysOfWeekBtn[i].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        int btnChecked=0;
                        for(int i1=0;i1<7;i1++){
                            if(daysOfWeekBtn[i1].equals(buttonView)){
                                btnChecked=i1;
                            }
                        }
                        checkedDays[btnChecked]=isChecked;
                }
            });
        }

        //Hour Selection
        // Get if existant previous value
        int HourStart = myVar.getInt(positionIDAll[ID]+"HourStart",8);
        int MinStart = myVar.getInt(positionIDAll[ID]+"MinStart",0);
        int HourEnd = myVar.getInt(positionIDAll[ID]+"HourEnd",16);
        int MinEnd = myVar.getInt(positionIDAll[ID]+"MinEnd",0);

        TimePicker timePickerStart = dialogView.findViewById(R.id.timeStart);
        TimePicker timePickerEnd = dialogView.findViewById(R.id.timeEnd);
        //Set existant time
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            timePickerStart.setHour(HourStart);
            timePickerStart.setMinute(MinStart);
            timePickerEnd.setHour(HourEnd);
            timePickerEnd.setMinute(MinEnd);
        }
        timePickerStart.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hour, int min) {
                myVarEditor.putInt(positionIDAll[ID]+"HourStart",hour);
                myVarEditor.putInt(positionIDAll[ID]+"MinStart",min);
                myVarEditor.apply();
            }
        });
        timePickerEnd.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hour, int min) {
                myVarEditor.putInt(positionIDAll[ID]+"HourEnd",hour);
                myVarEditor.putInt(positionIDAll[ID]+"MinEnd",min);
                myVarEditor.apply();
            }
        });

        builderTime.setNegativeButton("Supprimer les alarmes", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new AlarmReceiver().cancelAlarm(context,ID);
                dialog.dismiss();
                new Verification().UpDate(context,"DateHourSelector: Calendar delete alarms");
            }
        });
        builderTime.setPositiveButton("Confirmer", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {

                String checkedDaysString="";
                for(int i=0;i<7;i++){
                    checkedDaysString=checkedDaysString+BooleantoString(checkedDays[i]);
                }
                int startHour = myVar.getInt(positionIDAll[ID]+"HourStart",12);
                int startMin = myVar.getInt(positionIDAll[ID]+"MinStart",0);
                int endHour = myVar.getInt(positionIDAll[ID]+"HourEnd",12);
                int endMin = myVar.getInt(positionIDAll[ID]+"MinEnd",0);

                Toast.makeText(context,"Position :"+positionIDAll[ID]+"\n"+
                        "Days selected : "+checkedDaysString+"\n"+
                        "Start :"+startHour+":"+startMin+"\n"+
                        "End :"+endHour+":"+endMin,Toast.LENGTH_LONG).show();

                double HrMinStart=startHour+startMin/60;
                double HrMinEnd=endHour+endMin/60;
                if(HrMinEnd>HrMinStart) {
                    for (int i = 0; i < checkedDays.length; i++) {
                        if (checkedDays[i]) {
                            int dayNum;
                            if (i == 6) {
                                dayNum = 1;
                            } else {
                                dayNum = i + 2;
                            }
                            Calendar start = Calendar.getInstance();
                            Date date = new Date();
                            start.setTime(date);
                            start.set(Calendar.DAY_OF_WEEK, dayNum);
                            start.set(Calendar.HOUR_OF_DAY, startHour);
                            start.set(Calendar.MINUTE, startMin);
                            start.set(Calendar.SECOND, 0);
                            new AlarmReceiver().setRepeatedAlarm(context, start, true, ID + 1);
                            Calendar end = Calendar.getInstance();
                            end.setTime(date);
                            end.set(Calendar.DAY_OF_WEEK, dayNum);
                            end.set(Calendar.HOUR_OF_DAY, endHour);
                            end.set(Calendar.MINUTE, endMin);
                            end.set(Calendar.SECOND, 0);
                            new AlarmReceiver().setRepeatedAlarm(context, end, false, ID + 1);
                            if(start.getTimeInMillis()>end.getTimeInMillis()){
                                start.add(Calendar.DAY_OF_YEAR, -7);
                            }
                            if(start.getTimeInMillis()<System.currentTimeMillis()&end.getTimeInMillis()>System.currentTimeMillis()){
                                myVarEditor.putBoolean(positionIDAll[ID]+"CalendarIn",true);
                            }
                            myVarEditor.putString(positionIDAll[ID] + "DaysOfWeek", checkedDaysString);
                        }
                    }
                    myVarEditor.putBoolean(positionIDAll[ID]+"CalendarDefine",true);
                    myVarEditor.apply();
                }else{
                    Toast.makeText(context, "Heure de fin définie après heure de début", Toast.LENGTH_LONG).show();
                }
                new Verification().UpDate(context,"DateHourSelector: Setting new alarm");
            }
        });

        builderTime.show();
    }
    //Function to transform Boolean to String 1 or 0
    public static String BooleantoString(boolean b) {
        return b ? "1" : "0";
    }
    //Function to transform Char to Boolean 1 or 0
    public static Boolean CharToBoolean(char b) {
        if (b=='1'){
            return true;
        }else {
            return false;
        }
    }
}
