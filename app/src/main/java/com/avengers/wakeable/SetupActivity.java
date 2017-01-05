package com.avengers.wakeable;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class SetupActivity extends AppCompatActivity {

    private final String TAG="SetupActivity";
    private static SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private Handler mHandler;
    private static LogService ls = new LogService();

    private final static int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private String mBluetoothAddress;
    private BluetoothLeService mBluetoothLeService;
    private static Button snoozeButton;

    private static final long SCAN_PERIOD = 5000;
    private final String PREFS = "preferences";
    private final int LOGS_REQUEST_CODE = 122;
    private final int BT_REQUEST_CODE = 39;

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindService(mServiceConnection);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        snoozeButton = (Button) findViewById(R.id.snooze);

        prefs = getApplicationContext().getSharedPreferences(PREFS, Activity.MODE_PRIVATE);
        editor = prefs.edit();

        requestPermissions();


        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        Intent bleServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(bleServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

    }


    public void findWakeable(final View view){

        // Make sure we have location permissions
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            AlertDialog.Builder builder = new AlertDialog.Builder(SetupActivity.this);
            builder.setMessage(R.string.bt_permission_request)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            ActivityCompat.requestPermissions(SetupActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, BT_REQUEST_CODE);
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();

        }
        else {
            mHandler = new Handler();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    String address = prefs.getString("macAddress", "empty");
                    if (address.equals("empty")) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(SetupActivity.this);
                        builder.setMessage(R.string.setup_fail)
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

                            // Storing address so we don't show the dialog saying scan failed
                            mBluetoothAddress = device.getAddress();
                            editor.putString("macAddress", mBluetoothAddress);
                            editor.commit();

                            AlertDialog.Builder builder = new AlertDialog.Builder(SetupActivity.this);
                            builder.setMessage("We found a Wakeable with serial code " + device.getAddress() + ". Do you want to connect and go to your alarm?")
                                    .setPositiveButton("Let's do it!", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            mBluetoothLeService.connect(mBluetoothAddress, mBluetoothAdapter);


                                            Intent home = new Intent(SetupActivity.this, MainActivity.class);
                                            startActivity(home);
                                        }
                                    })
                                    .setNegativeButton("Not now", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            // User cancelled the dialog

                                            editor.putString("macAddress", "placeholder");
                                            editor.commit();
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
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case BT_REQUEST_CODE: {
                    findWakeable(snoozeButton);
                }
                case LOGS_REQUEST_CODE: {
                    ls.logString(TAG, "Sweet, they want to log!");
                }
                default: {
                    ls.logString(TAG, "Weird request code returned.");
                }
            }
        }
    }

    private void requestPermissions() {


        int permission = ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);


        if (permission != PackageManager.PERMISSION_GRANTED) {


            AlertDialog.Builder builder = new AlertDialog.Builder(SetupActivity.this);
            builder.setMessage(R.string.log_permission_request)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            String[] PERMISSIONS_STORAGE = {
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                            };
                            // We don't have permission so prompt the user
                            ActivityCompat.requestPermissions(
                                    SetupActivity.this,
                                    PERMISSIONS_STORAGE,
                                    LOGS_REQUEST_CODE
                            );
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }
}
