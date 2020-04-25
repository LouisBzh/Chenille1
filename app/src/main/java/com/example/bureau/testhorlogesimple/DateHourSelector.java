package com.example.bureau.testhorlogesimple;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.widget.ListView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Date;

public class DateHourSelector {
    private static final String TAG = DateHourSelector.class.getSimpleName();
    final String[] positionIDAll = {"Famille","Travail","Voyage","Dehors","Joker","Pense à Vous","Maison","Pas de Nouvelles","Veux rentrer"};

    protected void showDialog(final Context context,final int ID){
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(context);
        builderSingle.setTitle("Choisir les jours pour la position "+positionIDAll[ID]+":");
        String[] daysOfWeek={"Lundi","Mardi","Mercredi","Jeudi","Vendredi","Samedi","Dimanche"};
        final boolean[] checkedDays={false,false,false,false,false,false,false};

        builderSingle.setMultiChoiceItems(daysOfWeek, checkedDays, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                checkedDays[which]=isChecked;
            }
        });
        builderSingle.setNegativeButton("Supprimer les alarmes", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new AlarmReceiver().cancelAlarm(context,ID);
                dialog.dismiss();
            }
        });
        builderSingle.setPositiveButton("Confirmer", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String checkedDaysString="";
                for(int i=0;i<checkedDays.length;i++){
                    checkedDaysString=checkedDaysString+BooleantoString(checkedDays[i]);
                }
                final String checkedDaysStringF=checkedDaysString;
                dialog.dismiss();

                // Get Current Time
                final Calendar c = Calendar.getInstance();
                final int mHour = c.get(Calendar.HOUR_OF_DAY);
                final int mMinute = c.get(Calendar.MINUTE);

                // Launch Time Picker Dialog
                TimePickerDialog timePickerStart = new TimePickerDialog(context,
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, final int startHour, final int startMinute) {
                                TimePickerDialog timePickerEnd = new TimePickerDialog(context,
                                        new TimePickerDialog.OnTimeSetListener() {
                                            @Override
                                            public void onTimeSet(TimePicker view, int endHour, int endMinute) {
                                                Toast.makeText(context,"Position :"+positionIDAll[ID]+"\n"+
                                                        "Days selected : "+checkedDaysStringF+"\n"+
                                                        "Start :"+startHour+":"+startMinute+"\n"+
                                                        "End :"+endHour+":"+endMinute,Toast.LENGTH_LONG).show();
                                                for(int i=0;i<checkedDays.length;i++){
                                                    if(checkedDays[i]){
                                                        int dayNum;
                                                        if(i==6) {
                                                            dayNum = 1;
                                                        }else{
                                                            dayNum = i + 2;
                                                        }
                                                        Calendar start= Calendar.getInstance();
                                                        Date date=new Date();
                                                        start.setTime(date);
                                                        start.set(Calendar.DAY_OF_WEEK,dayNum);
                                                        start.set(Calendar.HOUR_OF_DAY,startHour);
                                                        start.set(Calendar.MINUTE,startMinute);
                                                        start.set(Calendar.SECOND,0);
                                                        new AlarmReceiver().setRepeatedAlarm(context,start,true,ID+1);
                                                        Calendar end= Calendar.getInstance();
                                                        end.setTime(date);
                                                        end.set(Calendar.DAY_OF_WEEK,dayNum);
                                                        end.set(Calendar.HOUR_OF_DAY,endHour);
                                                        end.set(Calendar.MINUTE,endMinute);
                                                        end.set(Calendar.SECOND,0);
                                                        new AlarmReceiver().setRepeatedAlarm(context,end,false,ID+1);
                                                    }
                                                }
                                            }
                                        }, mHour, mMinute, true);
                                timePickerEnd.show();
                            }
                        }, mHour, mMinute, true);
                timePickerStart.show();
            }
        });

        builderSingle.show();
    }
    //Function to transform Boolean to String 1 or 0
    public static String BooleantoString(boolean b) {
        return b ? "1" : "0";
    }
}
