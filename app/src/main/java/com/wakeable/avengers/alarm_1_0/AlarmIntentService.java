package com.wakeable.avengers.alarm_1_0;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class AlarmIntentService extends IntentService {

    LogService ls = new LogService();

    public AlarmIntentService() {
        super("AlarmIntentService");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        Intent startIntent = new Intent(this, RingtoneService.class);
        this.startService(startIntent);

        Intent i = new Intent(this, AlarmActivity.class);
        i.putExtra("foreground", intent.getBooleanExtra("foreground", true));
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);

        AlarmReceiver.completeWakefulIntent(intent);
    }


}
