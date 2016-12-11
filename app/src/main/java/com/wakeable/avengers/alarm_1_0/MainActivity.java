package com.wakeable.avengers.alarm_1_0;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
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
import android.view.View;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.ToggleButton;
import android.widget.Button;
import java.util.Calendar;

public class MainActivity extends Activity {


    private static PendingIntent pendingIntent;
    private static LogService ls = new LogService();

    public static final int requestCode = 9999;
    private static final String TAG = "MainActivity";
    private static AlarmManager alarmManager;
    private final String PREFS = "preferences";
    private TimePicker alarmTimePicker;
    private static TextView status;
    private static MainActivity inst;
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
    private static SharedPreferences prefs;
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
        inst = this;

        prefs = getApplicationContext().getSharedPreferences(PREFS, Activity.MODE_PRIVATE);

        if (prefs.getString("macAddress", "empty").equals("empty")) {
            ls.logString(TAG, "No address set, sending them to setup page.");
            onResume();
            Intent setup = new Intent(this, SetupActivity.class);
            startActivity(setup);
        }

        editor = prefs.edit();
        editor.remove("connected");
        editor.commit();

        inForeground = true;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        alarmTimePicker = (TimePicker) findViewById(R.id.alarmTimePicker);
        alarmToggle = (ToggleButton) findViewById(R.id.alarmToggle);
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        connectButton = (Button) findViewById(R.id.deviceButton);
        status = (TextView) findViewById(R.id.status);


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
        boolean connected = prefs.getBoolean("connected", false);
        ls.logString(TAG, "connected value: " + connected);
        if (connected){

            inst.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    connectButton.setVisibility(View.INVISIBLE);
                    status.setText(R.string.connected);
                }
            });
        }
        else{
            inst.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    connectButton.setVisibility(View.VISIBLE);
                    status.setText(R.string.connection_required);
                }
            });
        }
    }

    public void onToggleClicked(final View view) {
        if (!prefs.getString("macAddress", "empty").equals("empty")) {
            if (((ToggleButton) view).isChecked()) {
                boolean connected = prefs.getBoolean("connected", false);

                if (connected) {
                    ls.logString("MyActivity", "Alarm On");
                    Calendar selectedTime = getSelectedTime();
                    Intent myIntent = new Intent(getBaseContext(), AlarmReceiver.class);
                    pendingIntent = PendingIntent.getBroadcast(getBaseContext(), 0, myIntent, 0);
//                    PendingIntent pendingIntent = PendingIntent.getBroadcast(getBaseContext(), requestCode, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, selectedTime.getTimeInMillis(), pendingIntent);
                    ls.logString("MyActivity", String.valueOf(selectedTime.getTime()));
                } else {
                    ls.logString("Main", "Could not connect to bluetooth device. Not setting alarm");

                    AlertDialog.Builder builder = new AlertDialog.Builder(inst);
                    builder.setMessage("We can't connect to your WakeAble device right now. Maybe your button is out of range or not plugged in? " +
                            "If you proceed, the alarm may just go off like a regular alarm with no snooze button.")
                        .setPositiveButton("I understand the risk, set my alarm anyway", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Calendar selectedTime = getSelectedTime();
                                Intent myIntent = new Intent(getBaseContext(), AlarmReceiver.class);
                                pendingIntent = PendingIntent.getBroadcast(getBaseContext(), 0, myIntent, 0);
//                                PendingIntent pendingIntent = PendingIntent.getBroadcast(getBaseContext(), requestCode, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                                alarmManager.setExact(AlarmManager.RTC_WAKEUP, selectedTime.getTimeInMillis(), pendingIntent);
                                ls.logString("MyActivity", String.valueOf(selectedTime.getTime()));
                            }
                        })
                        .setNegativeButton("Thanks, I'll get connected and try again", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User cancelled the dialog
                                ((ToggleButton) view).toggle();
                            }
                        });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            } else {
//                Intent myIntent = new Intent(getBaseContext(), AlarmReceiver.class);
//                PendingIntent pendingIntent = PendingIntent.getBroadcast(getBaseContext(), requestCode, myIntent, PendingIntent.FLAG_NO_CREATE);
//                if(pendingIntent != null) {
//                    alarmManager.cancel(pendingIntent);
//                }
                alarmManager.cancel(pendingIntent);
                ls.logString("MyActivity", "Alarm Off");
            }
        } else {
            Intent myIntent = new Intent(getBaseContext(), AlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getBaseContext(), 0, myIntent, 0);
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
            ls.logString("MainActivity", "Let's set this for tomorrow?");
            calendar.set(Calendar.DAY_OF_WEEK, calendar.get(Calendar.DAY_OF_WEEK) + 1);
            ls.logString("MainActivity", "Cool, now we've got: " + String.valueOf(calendar.getTime()));
        }
        return calendar;
    }

    public boolean isInForeground() {
        return inForeground;
    }

    public void onDeviceClicked(final View view) {

        String address = prefs.getString("macAddress", "empty");

        if (address.equals("empty")){
            mHandler = new Handler();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    String address = prefs.getString("macAddress", "empty");
                    if (address.equals("empty")){
                        AlertDialog.Builder builder = new AlertDialog.Builder(inst);
                        builder.setMessage("Couldn't find a WakeAble device. Make sure your device is plugged in and close by!")
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {

                                    }
                                });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        }
        else {
            AlertDialog.Builder builder = new AlertDialog.Builder(inst);
            builder.setMessage("We already have a WakeAble device configured with address " + address + ". Unless you're trying to switch devices, you likely just need to plug your device in, move it closer to your phone, or turn bluetooth on.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                })
                .setNegativeButton("I want to switch my device", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        editor.remove("macAddress");
                        editor.commit();
                        onDeviceClicked(view);
                    }
                });
            AlertDialog dialog = builder.create();
            dialog.show();
        }

    }
    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    if(device != null) {
                        ls.logString(TAG, "Device found: " + device.getName());
                        String deviceName = device.getName();
                        if (deviceName != null && deviceName.toLowerCase().equals("wakeable")) {
                            mBluetoothAdapter.stopLeScan(mLeScanCallback);
                            ls.logString(TAG, "Found a WakeAble! Stopping scan");
                            mBluetoothAddress = device.getAddress();
                            editor.putString("macAddress", mBluetoothAddress);
                            editor.commit();

                            // Use the Builder class for convenient dialog construction
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setMessage("Found device with name " + deviceName + " and address " + device.getAddress() + ". Do you want to connect?")
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
                }
            };



    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();


            String address = prefs.getString("macAddress", "empty");
            if (!address.equals("empty")) {
                boolean connected = prefs.getBoolean("connected", false);
                if (!connected) {
                    ls.logString(TAG, "Back from killed state. Let's try to connect to the device");
                    mBluetoothLeService.connect(address, mBluetoothAdapter);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };
}