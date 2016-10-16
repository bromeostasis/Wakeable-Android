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
import java.util.UUID;

public class AlarmActivity extends AppCompatActivity {
    LogService ls = new LogService();

    private static final String TAG = "AlarmActivity";
    private final String PREFS="preferences";
    private Button btn;
    private BluetoothAdapter mBluetoothAdapter;


    // SPP UUID service
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // MAC-address of Bluetooth module (you must edit this line)

    private static String address;

    @Override
    protected void onPause() {
        super.onPause();

        Log.d(TAG, "...In onPause()...");
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onStart() {
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        super.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ls.logString("AlarmActivity: onCreate");

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btn = (Button) findViewById(R.id.button);

        // Initializes Bluetooth adapter.
//        final BluetoothManager bluetoothManager =
//                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
//        mBluetoothAdapter = bluetoothManager.getAdapter();
//
//        String address = prefs.getString("macAddress", "We Fucked UP");
//        for (BluetoothDevice device : mBluetoothAdapter.getBondedDevices()){
//            Log.d(TAG, device.getName() + " found. address: " + device.getAddress());
//            if (device.getAddress().equals(address)){
//                btn.setVisibility(View.INVISIBLE);
//            }
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFS, Activity.MODE_PRIVATE);
        boolean connected = prefs.getBoolean("connected", true);

        Log.d(TAG, "IN onresume. Visible? " + connected);
        if (connected){
            btn.setVisibility(View.INVISIBLE);
        }
        else{
            btn.setVisibility(View.VISIBLE);
        }
    }

    public void turnOffAlarm(View view){

        Log.d(TAG, "turning it off now?!?!");

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
            Log.d(TAG, "Received something: " + intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                if(intent.getStringExtra(BluetoothLeService.EXTRA_DATA).equals("1")){
                    Log.d(TAG, "Sweet, we got a one! Let's do this thing");
                    turnOffAlarm(btn);
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
