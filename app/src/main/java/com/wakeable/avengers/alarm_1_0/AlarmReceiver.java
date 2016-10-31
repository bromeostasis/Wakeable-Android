package com.wakeable.avengers.alarm_1_0;

/**
 * Created by evansnyder on 2/29/16.
 */
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;



public class AlarmReceiver extends WakefulBroadcastReceiver {

    LogService ls = new LogService();

    @Override
    public void onReceive(final Context context, Intent intent) {

        Intent i = new Intent(context, AlarmIntentService.class);
        MainActivity mi = MainActivity.instance();
        if (mi.isInForeground()){
            ls.logString("Receiver", "It's still in the foreground!");
            i.putExtra("foreground", true);
        }
        else{
            ls.logString("Receiver", "Background");
            i.putExtra("foreground", false);
        }
        i.putExtra("from", "Receiver");
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //this will send a notification message
        ls.logString("AlarmReceiver", "trying to send!");
//        context.startActivity(i);

        MainActivity.changeToggle();
        startWakefulService(context, i);

    }
}