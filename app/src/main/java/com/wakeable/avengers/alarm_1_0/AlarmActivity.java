package com.wakeable.avengers.alarm_1_0;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.content.Context;
import android.view.WindowManager;
import android.view.Window;

public class AlarmActivity extends AppCompatActivity {

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    public void turnOffAlarm(View view){
        Context context = view.getContext();
        Intent ringtoneIntent = new Intent(context, RingtoneService.class);
        context.stopService(ringtoneIntent);
        if(getIntent().getBooleanExtra("foreground", false)){
            Intent i = new Intent(context, MainActivity.class);
            startActivity(i);
            finish();
        }
        else{
            finishAffinity();
        }
    }



}
