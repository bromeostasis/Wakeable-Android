package com.avengers.wakeable;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.content.Context;
import android.view.WindowManager;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import java.util.concurrent.ThreadLocalRandom;

public class AlarmActivity extends AppCompatActivity {
    LogService ls = new LogService();

    private static final String TAG = "AlarmActivity";
    private final String PREFS="preferences";
    private static Button btn;
    private static TextView alarmText;
    private static TextView alarmDirections;
    private static String[] quotes;
    private static AlarmActivity inst;

    private static SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    private final static int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeService mBluetoothLeService;

    private int SCAN_PERIOD = 5000;

    @Override
    protected void onPause() {
        super.onPause();

        ls.logString(TAG, "...In onPause()...");
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {

        unbindService(mServiceConnection);
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent where = getIntent();
        String source = where.getStringExtra("from");
        ls.logString(TAG, "Where'd this activity come fram??? " + source);
        where.removeExtra("from");

        inst = this;
        quotes = getResources().getStringArray(R.array.wakeup_texts);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);
        alarmText = (TextView) findViewById(R.id.alarmText);
        alarmDirections = (TextView) findViewById(R.id.alarmDirections);
        btn = (Button) findViewById(R.id.button);

        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }


        ls.logString(TAG, "IN onCreate, about to create the service");
        // Bind service
        Intent bleServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(bleServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

    }

    @Override
    protected void onResume() {
        super.onResume();
        prefs = getApplicationContext().getSharedPreferences(PREFS, Activity.MODE_PRIVATE);
        editor = prefs.edit();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        setFailsafe();

    }

    public static void setFailsafe() {
        // HELLA SHITTY WAY TO DO THIS. I actually want to know more in BLEService.broadcastUpdate TEMP AF

        if (prefs != null) {
            boolean connected = prefs.getBoolean("connected", false);
            Log.d(TAG, "Setting failsafe stuff, what's the status? " + connected);
            if (connected) {
                inst.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btn.setVisibility(View.INVISIBLE);
                        alarmDirections.setVisibility(View.VISIBLE);

                        int randomIndex = ThreadLocalRandom.current().nextInt(0, quotes.length);
                        alarmText.setText(quotes[randomIndex]);
                    }
                });
            } else {
                inst.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btn.setVisibility(View.VISIBLE);
                        alarmDirections.setVisibility(View.INVISIBLE);
                        alarmText.setText(R.string.failsafe_message);
                    }
                });
            }
        }
    }

    public void turnOffAlarm(View view){

        ls.logString(TAG, "turning it off now?!?!");

        Context context = view.getContext();
        Intent ringtoneIntent = new Intent(context, RingtoneService.class);
        context.stopService(ringtoneIntent);
        finish();
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

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            ls.logString(TAG, "Nice, the service is connected");
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();


            String address = prefs.getString("macAddress", "empty");
            ls.logString(TAG, "That address: " + address);
            if (!address.equals("empty")) {
                ls.logString(TAG, "One time, let's try to connect!");
                mBluetoothLeService.connect(address, mBluetoothAdapter);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

}
