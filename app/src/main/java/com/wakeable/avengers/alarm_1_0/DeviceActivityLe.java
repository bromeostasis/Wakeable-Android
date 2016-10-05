package com.wakeable.avengers.alarm_1_0;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class DeviceActivityLe extends AppCompatActivity {

    private static final String TAG = "DeviceActivityLe";
    private final static int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private String mBluetoothAddress;
    private Button btn;
    private BluetoothLeService mBluetoothLeService;
    private final String PREFS = "preferences";
    private SharedPreferences.Editor editor;

    private static final long SCAN_PERIOD = 10000;

    // Code to manage Service lifecycle.
//    private final ServiceConnection mServiceConnection = new ServiceConnection() {
//
//        @Override
//        public void onServiceConnected(ComponentName componentName, IBinder service) {
//            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName componentName) {
//            mBluetoothLeService = null;
//        }
//    };

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//
//        SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFS, Activity.MODE_PRIVATE);
//        editor = prefs.edit();
//
//
//        setContentView(R.layout.activity_device_activity_le);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//
//        btn = (Button) findViewById(R.id.connect);
//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
//
//        // Initializes Bluetooth adapter.
//        final BluetoothManager bluetoothManager =
//                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
//        mBluetoothAdapter = bluetoothManager.getAdapter();
//
//        // Ensures Bluetooth is available on the device and it is enabled. If not,
//        // displays a dialog requesting user permission to enable Bluetooth.
//        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//        }
//
//        // Bind service
//        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
//        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
//    }

//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//
//        unbindService(mServiceConnection);
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//    }

//    public void scanLeDevices(View view) {
//        // Stops scanning after a pre-defined scan period.
//        mHandler = new Handler();
//        mHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                mScanning = false;
//                mBluetoothAdapter.stopLeScan(mLeScanCallback);
//            }
//        }, SCAN_PERIOD);
//
//        mScanning = true;
//        mBluetoothAdapter.startLeScan(mLeScanCallback);
//
//    }
//
//    public void connect(View view){
//        mBluetoothLeService.connect(mBluetoothAddress, mBluetoothAdapter);
//
////        mBluetoothLeService.setCharacteristicNotification()
//    }
//
//    // Device scan callback.
//    private BluetoothAdapter.LeScanCallback mLeScanCallback =
//            new BluetoothAdapter.LeScanCallback() {
//                @Override
//                public void onLeScan(final BluetoothDevice device, int rssi,
//                                     byte[] scanRecord) {
//                    Log.d(TAG, "Device found: " + device.getName());
//                    if (device.getName().equals("WakeAble")){
//                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
//                        Log.d(TAG, "Found a WakeAble! Stopping scan");
//                        mBluetoothAddress = device.getAddress();
//                        editor.putString("macAddress", mBluetoothAddress);
//                        editor.commit();
//
//                        // Turn on button as soon as BT is available.
//                        btn.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                btn.setVisibility(View.VISIBLE);
//                            }
//                        });
//                    }
//                }
//            };

}
