package com.wakeable.avengers.alarm_1_0;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.content.Context;
import android.view.WindowManager;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class AlarmActivity extends AppCompatActivity {
    LogService ls = new LogService();

    private static final String TAG = "AlarmActivity";
    private final String PREFS="preferences";
    private Button btn;
    private TextView alarmText;
    private TextView alarmDirections;
    private BluetoothAdapter mBluetoothAdapter;
    private String[] quotes;



    // SPP UUID service
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // MAC-address of Bluetooth module (you must edit this line)

    private static String address;

    @Override
    protected void onPause() {
        super.onPause();

        ls.logString(TAG, "...In onPause()...");
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        quotes = getResources().getStringArray(R.array.wakeup_texts);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        alarmText = (TextView) findViewById(R.id.alarmText);
        alarmDirections = (TextView) findViewById(R.id.alarmDirections);
        btn = (Button) findViewById(R.id.button);

        // Initializes Bluetooth adapter.
//        final BluetoothManager bluetoothManager =
//                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
//        mBluetoothAdapter = bluetoothManager.getAdapter();
//
//        String address = prefs.getString("macAddress", "We Fucked UP");
//        for (BluetoothDevice device : mBluetoothAdapter.getBondedDevices()){
//            ls.logString(TAG, device.getName() + " found. address: " + device.getAddress());
//            if (device.getAddress().equals(address)){
//                btn.setVisibility(View.INVISIBLE);
//            }
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFS, Activity.MODE_PRIVATE);
        boolean connected = prefs.getBoolean("connected", false);

        ls.logString(TAG, "IN onresume. Visible? " + connected);
        if (connected){
            btn.setVisibility(View.INVISIBLE);
            alarmDirections.setVisibility(View.VISIBLE);

            int randomIndex = ThreadLocalRandom.current().nextInt(0, quotes.length);
            alarmText.setText(quotes[randomIndex]);
        }
        else{
            btn.setVisibility(View.VISIBLE);
            alarmDirections.setVisibility(View.INVISIBLE);
            alarmText.setText(R.string.failsafe_message);
        }
    }

    public void turnOffAlarm(View view){

        ls.logString(TAG, "turning it off now?!?!");

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
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ls.logString(TAG, "Received something: " + intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                if(intent.getStringExtra(BluetoothLeService.EXTRA_DATA).contains("1")){
                    ls.logString(TAG, "Sweet, we got a one! Let's do this thing");
                    turnOffAlarm(btn);
                }
                else{
                    ls.logString(TAG, "What's the ferkin data?" + intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                }
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }



}
