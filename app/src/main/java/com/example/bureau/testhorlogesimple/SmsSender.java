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
import android.widget.Toast;

import static com.example.bureau.testhorlogesimple.Main.MY_PREF;

public class SmsSender extends Service {
    //Service
    Context mContext;
    //Preference
    SharedPreferences myVar;
    SharedPreferences.Editor myVarEditor;
    //Variables sms functions
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
        sentBroadcast = new sentReceiver();
        deliveredBroadcast = new deliveredReceiver();
        //Register emission/reception sms
        mContext.registerReceiver(sentBroadcast, sentIntentFilter);
        mContext.registerReceiver(deliveredBroadcast, deliveredIntentFilter);
    }

    protected void unRegistered() {
        //Delete registration emission/reception sms
        if(deliveredBroadcast!=null) {
            mContext.unregisterReceiver(deliveredBroadcast);
            deliveredBroadcast=null;
        }
        if (sentBroadcast!=null) {
            mContext.unregisterReceiver(sentBroadcast);
            sentBroadcast=null;
        }
    }

    //Sms functions
    //Function to send an sms
    public void SendSMS(String x) {
        //Test if last position different than new one
        if (Integer.parseInt(myVar.getString("aigLastPosit", "1")) != Integer.parseInt(x)) {

            PendingIntent sentPI = PendingIntent.getBroadcast(mContext, 0,
                    new Intent(SENT), 0);

            PendingIntent deliveredPI = PendingIntent.getBroadcast(mContext, 0,
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
        Context receiverContext=mContext.getApplicationContext();
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    Toast.makeText(receiverContext, "SMS sent",
                            Toast.LENGTH_SHORT).show();
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    Toast.makeText(receiverContext, "Generic failure",
                            Toast.LENGTH_SHORT).show();
                    break;
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                    Toast.makeText(receiverContext, "No service",
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
        Context receiverContext= mContext.getApplicationContext();
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    Toast.makeText(receiverContext, "SMS delivered",
                            Toast.LENGTH_SHORT).show();
                    break;
                case Activity.RESULT_CANCELED:
                    Toast.makeText(receiverContext, "SMS not delivered",
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
