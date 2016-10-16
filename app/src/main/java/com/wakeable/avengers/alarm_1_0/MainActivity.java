package com.wakeable.avengers.alarm_1_0;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.ToggleButton;
import android.widget.Button;
import java.util.Calendar;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    AlarmManager alarmManager;
    private final String PREFS = "preferences";
    private PendingIntent pendingIntent;
    private TimePicker alarmTimePicker;
    private static MainActivity inst;
    private TextView alarmTextView;
    private static ToggleButton alarmToggle;
    private static Button connectButton;
    private boolean inForeground = false;

    private Handler mHandler;

    // From device activity
    private final static int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private String mBluetoothAddress;
    private BluetoothLeService mBluetoothLeService;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    private static final long SCAN_PERIOD = 10000;


    public static MainActivity instance() {
        return inst;
    }

    @Override
    public void onStart() {

        inForeground = true;
        super.onStart();
        inst = this;
    }

    @Override
    protected void onDestroy() {

        unbindService(mServiceConnection);
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        inForeground = false;
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getApplicationContext().getSharedPreferences(PREFS, Activity.MODE_PRIVATE);

        editor = prefs.edit();
        editor.remove("connected");
        editor.commit();

        inForeground = true;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        alarmTimePicker = (TimePicker) findViewById(R.id.alarmTimePicker);
        alarmTextView = (TextView) findViewById(R.id.alarmText);
        alarmToggle = (ToggleButton) findViewById(R.id.alarmToggle);
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        connectButton = (Button) findViewById(R.id.deviceButton);


        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // Bind service
        Intent bleServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(bleServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

    }

    public static void changeToggle() {
        alarmToggle.toggle();
    }

    public static void toggleConnectionButton(){
        if (connectButton.isEnabled()){

            inst.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    connectButton.setEnabled(false);
                    connectButton.setText(R.string.connected);
                }
            });
        }
        else{
            inst.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    connectButton.setEnabled(true);
                    connectButton.setText(R.string.connection_required);
                }
            });
        }
    }

    public void onToggleClicked(final View view) {
        if (!prefs.getString("macAddress", "empty").equals("empty")) {
            if (((ToggleButton) view).isChecked()) {
                boolean connected = prefs.getBoolean("connected", false);

                if (connected) {
                    Log.d("MyActivity", "Alarm On");
                    Calendar selectedTime = getSelectedTime();
                    Intent myIntent = new Intent(getBaseContext(), AlarmReceiver.class);
                    pendingIntent = PendingIntent.getBroadcast(getBaseContext(), 0, myIntent, 0);
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, selectedTime.getTimeInMillis(), pendingIntent);
                    Log.d("MyActivity", String.valueOf(selectedTime.getTime()));
                } else {
                    Log.d("Main", "Could not connect to bluetooth device. Not setting alarm");

                    AlertDialog.Builder builder = new AlertDialog.Builder(inst);
                    builder.setMessage("We can't connect to your WakeAble device right now. Maybe your button is out of range or not plugged in? " +
                            "If you proceed, the alarm may just go off like a regular alarm with no snooze button.");
                    builder.setPositiveButton("I understand the risk, set my alarm anyway", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Calendar selectedTime = getSelectedTime();
                            Intent myIntent = new Intent(getBaseContext(), AlarmReceiver.class);
                            pendingIntent = PendingIntent.getBroadcast(getBaseContext(), 0, myIntent, 0);
                            alarmManager.setExact(AlarmManager.RTC_WAKEUP, selectedTime.getTimeInMillis(), pendingIntent);
                            Log.d("MyActivity", String.valueOf(selectedTime.getTime()));
                        }
                    });
                    builder.setNegativeButton("Thanks, I'll get connected and try again", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                            ((ToggleButton) view).toggle();
                        }
                    });

                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            } else {
                alarmManager.cancel(pendingIntent);
                Log.d("MyActivity", "Alarm Off");
            }
        } else {
            ((ToggleButton) view).toggle();
            AlertDialog.Builder builder = new AlertDialog.Builder(inst);

            builder.setMessage("You have not configured a device yet. Please connect to WakeAble before trying to set an alarm!!")
            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int id) {}});

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }


    private Calendar getSelectedTime(){
        Calendar today = Calendar.getInstance();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, alarmTimePicker.getCurrentHour());
        calendar.set(Calendar.MINUTE, alarmTimePicker.getCurrentMinute());
        calendar.set(Calendar.SECOND, 0);
        if (calendar.compareTo(today) < 0) {
            Log.d("MainActivity", "Let's set this for tomorrow?");
            calendar.set(Calendar.DAY_OF_WEEK, calendar.get(Calendar.DAY_OF_WEEK) + 1);
            Log.d("MainActivity", "Cool, now we've got: " + String.valueOf(calendar.getTime()));
        }
        return calendar;
    }

    public boolean isInForeground() {
        return inForeground;
    }


    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();


            String address = prefs.getString("macAddress", "empty");
            if (!address.equals("empty")) {
                boolean connected = prefs.getBoolean("connected", false);
                if (!connected) {
                    Log.d(TAG, "Back from killed state. Let's try to connect to the device");
                    mBluetoothLeService.connect(address, mBluetoothAdapter);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    public void onDeviceClicked(View view) {
        mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mScanning = false;
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }, SCAN_PERIOD);

        mScanning = true;
        mBluetoothAdapter.startLeScan(mLeScanCallback);
    }
    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    Log.d(TAG, "Device found: " + device.getName());
                    if (device.getName().equals("WakeAble")) {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                        Log.d(TAG, "Found a WakeAble! Stopping scan");
                        mBluetoothAddress = device.getAddress();
                        editor.putString("macAddress", mBluetoothAddress);
                        editor.commit();

                        // Use the Builder class for convenient dialog construction
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setMessage("Found device with name " + device.getName() + " and address " + device.getAddress() + ". Do you want to connect?")
                                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        mBluetoothLeService.connect(mBluetoothAddress, mBluetoothAdapter);
                                    }
                                })
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        // User cancelled the dialog
                                    }
                                });
                        builder.create().show();

                    }
                }
            };
}