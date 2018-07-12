package com.avengers.wakeable;

import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.List;
import java.util.UUID;

public class BluetoothLeService extends Service {

    private static LogService ls = new LogService();

    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";
    private final String PREFS = "preferences";

    public final static UUID UUID_HM10_CHARACTERISTIC = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    public final static UUID UUID_HM10_SERVICE = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    public BluetoothLeService() {
    }


    public boolean connect(final String address, BluetoothAdapter btAdapter) {
        mBluetoothAdapter = btAdapter;
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            ls.logString(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFS, Activity.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                ls.logString(TAG, "Connection existed already, resetting just in case??!?");
                editor.putBoolean("connected", true);
                editor.commit();
                MainActivity.toggleConnectionButton();

                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        ls.logString(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }


    // Various callback methods defined by the BLE API.
    private final BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                    int newState) {
                    String intentAction;
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        intentAction = ACTION_GATT_CONNECTED;
                        mConnectionState = STATE_CONNECTED;
                        broadcastUpdate(intentAction);
                        Log.i(TAG, "Connected to GATT server.");
                        Log.i(TAG, "Attempting to start service discovery:" +
                                mBluetoothGatt.discoverServices());

                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        intentAction = ACTION_GATT_DISCONNECTED;
                        mConnectionState = STATE_DISCONNECTED;
                        Log.i(TAG, "Disconnected from GATT server.");
                        broadcastUpdate(intentAction);
                    }
                }

                @Override
                // New services discovered
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    List<BluetoothGattService> services = mBluetoothGatt.getServices();
                    for (BluetoothGattService service : services){
                        ls.logString(TAG, "Available service: " + service.getUuid().toString());
                        if (service.getUuid().equals(UUID_HM10_SERVICE)){
                            ls.logString(TAG, "Discovered HM10 service! Listing characteristics");
                            for (BluetoothGattCharacteristic characteristic :  service.getCharacteristics()){
                                ls.logString(TAG, "Available characteristic: " + characteristic.getUuid().toString());
                                if (characteristic.getUuid().equals(UUID_HM10_CHARACTERISTIC)){
                                    ls.logString(TAG, "Found the HM10 characteristic! Want to try reading and notifying!");
                                    setCharacteristicNotification(characteristic, true);
                                }
                            }
                        }
                    }
                }

                @Override
                // Result of a characteristic read operation
                public void onCharacteristicRead(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic,
                                                 int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                    }
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt,
                                                    BluetoothGattCharacteristic characteristic) {
                    broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                }
            };

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }


    private void broadcastUpdate(final String action) {
        ls.logString(TAG, "Broadcasting update: " + action);

        SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFS, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
            ls.logString(TAG, "Connected to BLE device");
            editor.putBoolean("connected", true);
            editor.commit();
            AlarmActivity.setFailsafe();
            MainActivity.toggleConnectionButton();

        }
        else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
            ls.logString(TAG, "Disconnected from BLE device");
            editor.putBoolean("connected", false);
            editor.commit();
            AlarmActivity.setFailsafe();
            MainActivity.toggleConnectionButton();
        }

        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }
    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        ls.logString(TAG, "In broadcast update. characteristic id: " + characteristic.getUuid().toString());

        Log.v(TAG, "broadcastUpdate()");

        final byte[] data = characteristic.getValue();

        Log.v(TAG, "data.length: " + data.length);

        if (data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for(byte byteChar : data) {

                Log.v(TAG, String.format("%02X ", byteChar));
            }
            intent.putExtra(EXTRA_DATA, new String(data));
            ls.logString(TAG, intent.getStringExtra(EXTRA_DATA));
        }
        sendBroadcast(intent);
    }


//    Binding stuff

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();
}
