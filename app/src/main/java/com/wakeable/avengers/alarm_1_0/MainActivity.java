package com.wakeable.avengers.alarm_1_0;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.ToggleButton;
import android.widget.Button;
import java.util.Calendar;

public class MainActivity extends Activity {

    AlarmManager alarmManager;
    private PendingIntent pendingIntent;
    private TimePicker alarmTimePicker;
    private Button bluetoothButton;
    private static MainActivity inst;
    private TextView alarmTextView;
    private static ToggleButton alarmToggle;
    private boolean inForeground = false;

    public static MainActivity instance() {
        return inst;
    }

    @Override
    public void onStart() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.commit();

        inForeground = true;
        super.onStart();
        inst = this;
    }

    @Override
    protected void onPause() {
        inForeground = false;
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        inForeground = true;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bluetoothButton = (Button) findViewById(R.id.bluetoothButton);
        alarmTimePicker = (TimePicker) findViewById(R.id.alarmTimePicker);
        alarmTextView = (TextView) findViewById(R.id.alarmText);
        alarmToggle = (ToggleButton) findViewById(R.id.alarmToggle);
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
    }

    public static void changeToggle(){
        alarmToggle.toggle();
    }

    public void onToggleClicked(View view) {
        if (((ToggleButton) view).isChecked()) {
            Log.d("MyActivity", "Alarm On");
            Calendar today = Calendar.getInstance();
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, alarmTimePicker.getCurrentHour());
            calendar.set(Calendar.MINUTE, alarmTimePicker.getCurrentMinute());
            calendar.set(Calendar.SECOND, 0);
            if (calendar.compareTo(today) < 0 ){
                Log.d("MainActivity", "Let's set this for tomorrow?");
                calendar.set(Calendar.DAY_OF_WEEK, calendar.get(Calendar.DAY_OF_WEEK) + 1);
                Log.d("MainActivity", "Cool, now we've got: " + String.valueOf(calendar.getTime()));
            }
            Intent myIntent = new Intent(MainActivity.this, AlarmReceiver.class);
            pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, myIntent, 0);
            alarmManager.setExact(AlarmManager.RTC, calendar.getTimeInMillis(), pendingIntent);
            Log.d("MyActivity", String.valueOf(calendar.getTime()));
        } else {
            alarmManager.cancel(pendingIntent);
            setAlarmText("");
            Log.d("MyActivity", "Alarm Off");
        }
    }

    public void onBluetoothClicked(View view){
        Intent btIntent = new Intent(getApplicationContext(), BluetoothActivity.class);
        startActivity(btIntent);
    }

    public void setAlarmText(String alarmText) {
        alarmTextView.setText(alarmText);
    }
    public boolean isInForeground(){return inForeground;}
}