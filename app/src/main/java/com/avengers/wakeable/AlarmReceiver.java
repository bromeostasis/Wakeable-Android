package com.avengers.wakeable;

/**
 * Created by evansnyder on 2/29/16.
 */
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;


public class AlarmReceiver extends WakefulBroadcastReceiver {

    LogService ls = new LogService();

    @Override
    public void onReceive(final Context context, Intent intent) {

        Intent i = new Intent(context, AlarmIntentService.class);
        MainActivity mi = MainActivity.instance();
        if (mi != null && mi.isInForeground()){
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

        if (mi != null) {
            MainActivity.changeToggle();
        }
        startWakefulService(context, i);

    }
}