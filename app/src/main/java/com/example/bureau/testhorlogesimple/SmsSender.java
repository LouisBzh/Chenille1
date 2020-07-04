package com.example.bureau.testhorlogesimple;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import static com.example.bureau.testhorlogesimple.Main.MY_PREF;

public class SmsSender extends Service {
    private static final String TAG = SmsSender.class.getSimpleName();
    //Service
    Context mContext;
    //Preference
    SharedPreferences myVar;
    SharedPreferences.Editor myVarEditor;
    //Variables sms functions
    boolean register;
    BroadcastReceiver sentBroadcast;
    BroadcastReceiver deliveredBroadcast;
    IntentFilter sentIntentFilter;
    IntentFilter deliveredIntentFilter;
    String SENT = "SMS_SENT";
    String DELIVERED = "SMS_DELIVERED";
    public static final int RequestCode_SEND_SMS=1;

    public SmsSender(Context context){
        mContext=context;
        onCreate();
    }

    @Override
    public void onCreate(){
        //Preference
        //Get preferences inside usable variables
        myVar = mContext.getSharedPreferences(MY_PREF, MODE_PRIVATE);
        myVarEditor = myVar.edit();
        //Creation emission/reception sms functions
        sentIntentFilter = new IntentFilter(SENT);
        deliveredIntentFilter = new IntentFilter(DELIVERED);
        //Register emission/reception sms
        sentBroadcast = new sentReceiver();
        deliveredBroadcast = new deliveredReceiver();

    }
    protected void Registered(){
        mContext.registerReceiver(sentBroadcast, sentIntentFilter);
        mContext.registerReceiver(deliveredBroadcast, deliveredIntentFilter);
        this.register=true;
    }

    protected void unRegistered() {
        try {
            mContext.unregisterReceiver(deliveredBroadcast);
            deliveredBroadcast=null;
            mContext.unregisterReceiver(sentBroadcast);
            sentBroadcast=null;
            this.register=false;
        }catch (IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    //Sms functions
    //Function to send an sms
    public void SendSMS(String x) {
        try {
            //Test if last position different than new one
            if (Integer.parseInt(myVar.getString("aigLastPosit", "1")) != Integer.parseInt(x)) {
                PendingIntent sentPI = PendingIntent.getBroadcast(mContext, RequestCode_SEND_SMS,
                        new Intent(SENT), PendingIntent.FLAG_CANCEL_CURRENT);

                PendingIntent deliveredPI = PendingIntent.getBroadcast(mContext, RequestCode_SEND_SMS,
                        new Intent(DELIVERED), PendingIntent.FLAG_CANCEL_CURRENT);

                // Storing the position
                myVarEditor.putString("aigLastPosit", x);
                myVarEditor.apply();
                //---when the SMS has been sent--
                android.telephony.SmsManager sms = android.telephony.SmsManager.getDefault();
                sms.sendTextMessage("+33769424262", null, x, sentPI, deliveredPI);
                Intent starterIntent = new Intent(mContext,Main.class);
                mContext.startActivity(starterIntent);
            }
        } catch(NumberFormatException nfe) {
            Log.e(TAG,"Value passed not an Integer");
        }
    }
    //Sms sending receiver and Error handling
    public class sentReceiver extends BroadcastReceiver {
        Context receiverContext=mContext;
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    Toast.makeText(receiverContext, "SMS envoyé",
                            Toast.LENGTH_SHORT).show();
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    Toast.makeText(receiverContext, "Generic failure",
                            Toast.LENGTH_SHORT).show();
                    break;
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                    Toast.makeText(receiverContext, "Pas de service disponible",
                            Toast.LENGTH_SHORT).show();
                    break;
                case SmsManager.RESULT_ERROR_NULL_PDU:
                    Toast.makeText(receiverContext, "Null PDU",
                            Toast.LENGTH_SHORT).show();
                    break;
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    Toast.makeText(receiverContext, "Radio off",
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }
    //Sms delivered receiver and Error handling
    public class deliveredReceiver extends BroadcastReceiver{
        Context receiverContext= mContext;
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    Toast.makeText(receiverContext, "SMS reçu",
                            Toast.LENGTH_SHORT).show();
                    break;
                case Activity.RESULT_CANCELED:
                    Toast.makeText(receiverContext, "SMS non reçu",
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
