package com.wakeable.avengers.alarm_1_0;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.content.Context;
import android.os.Vibrator;

public class RingtoneService extends Service {
    private MediaPlayer mPlayer;
    private AudioManager am;
    private int previousVolume;
    private Vibrator vibrator;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {

        // Vibrate
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = {0, 1000, 750};
        vibrator.vibrate(pattern, 0);

        // Ringtone
        Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        am =  (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        previousVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
//        am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
        mPlayer = MediaPlayer.create(this, alarmUri);
        mPlayer.setLooping(true);
        mPlayer.start();

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy()
    {
        mPlayer.stop();
        vibrator.cancel();
        am.setStreamVolume(AudioManager.STREAM_MUSIC, previousVolume, 0);
    }
}