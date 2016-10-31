package com.wakeable.avengers.alarm_1_0;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

public class DeviceActivity extends AppCompatActivity {

    private static LogService ls = new LogService();

    private static final String TAG = "DeviceActivity";
    private static DeviceActivity inst;
    private final String PREFS = "preferences";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private String newDevicePrefix = "Available Device: ";
    BluetoothSocket btSocket;

    private BluetoothAdapter btAdapter = null;
    ArrayAdapter<String> mArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        inst = this;

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        this.registerReceiver(mReceiver, filter);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);


        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();
        mArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1 );
        ListView listView = (ListView) findViewById(R.id.deviceListView);
        listView.setAdapter(mArrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                final String name = parent.getItemAtPosition(position).toString();

                String[] nameArr = name.split(" ");
                final String address = name.split("- ")[1];
                if (nameArr[0].equals("Available")){
                    // TODO: Better way to do this??
                    BluetoothDevice device = btAdapter.getRemoteDevice(address);
//                    mArrayAdapter.remove(name);

                    if(pairDevice(device)) {
                        mArrayAdapter.remove(name);
                        mArrayAdapter.add(name.substring(newDevicePrefix.length()));
                    }
//                    if(connectToDevice(device)){
//                        mArrayAdapter.remove(name);
//                        mArrayAdapter.add(name.substring(newDevicePrefix.length()));
//                    }
                }
                else {

                    AlertDialog.Builder builder = new AlertDialog.Builder(inst);
                    builder.setMessage("You are about to set " + name.split(" ")[0] + " as your wakeable device, do you want to continue?");
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFS, Activity.MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("macAddress", address);
                            editor.commit();
                        }
                    });
                    builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                        }
                    });

                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            }
        });

        listDevices();
    }

    @Override
    protected void onResume() {

        super.onResume();
    }

    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if (btAdapter == null) {
            errorExit("Fatal Error", "Bluetooth not support");
        } else {
            if (btAdapter.isEnabled()) {
                ls.logString(TAG, "...Bluetooth ON...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    private void listDevices() {

        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter.add(device.getName() + "- " + device.getAddress());
            }
        }
    }


    private void errorExit(String title, String message) {
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }


    private boolean pairDevice(BluetoothDevice device) {
        boolean result;
        try {
            ls.logString(TAG, "Start Pairing...");


            Method m = device.getClass()
                    .getMethod("createBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
            result = true;

            ls.logString(TAG, "Pairing finished.");
        } catch (Exception e) {
            result=false;
            Log.e(TAG, e.getMessage());
        }
        return result;
    }

    public void discover(View view) {

        ls.logString(TAG, "Discovering...");
        if (btAdapter.isDiscovering()) {
            btAdapter.cancelDiscovery();
        }
        btAdapter.startDiscovery();

    }

    @Override
    protected void onPause() {
        try {
            this.unregisterReceiver(mReceiver);
        }
        catch(IllegalArgumentException e){
            ls.logString(TAG, e.getMessage());
        }

//        if(btSocket.isConnected()){
//            try {
//                btSocket.close();
//            } catch (IOException e2) {
//                errorExit("Fatal Error", "In onPause() and unable to close socket during connection failure" + e2.getMessage() + ".");
//            }
//        }

        btAdapter.cancelDiscovery();

        super.onPause();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            //Finding devices
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter.add(newDevicePrefix + device.getName() + " - " + device.getAddress());
            }

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);

                if (state == BluetoothAdapter.STATE_ON){
                    // We turned bluetooth on, let's refresh the list!
                    listDevices();
                }
            }
        }
    };

}


//    private boolean connectToDevice(BluetoothDevice device){
//
//        // Two things are needed to make a connection:
//        //   A MAC address, which we got above.
//        //   A Service ID or UUID.  In this case we are using the
//        //     UUID for SPP.
//        btSocket = null;
//        try {
//            btSocket = createBluetoothSocket(device);
//        } catch (IOException e) {
//            errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
//        }
//
//        // Discovery is resource intensive.  Make sure it isn't going on
//        // when you attempt to connect and pass your message.
//        btAdapter.cancelDiscovery();
//
//        // Establish the connection.  This will block until it connects.
//        ls.logString(TAG, "...Connecting...");
//        boolean result;
//        try {
//            btSocket.connect();
//            ls.logString(TAG, "....Connection ok...");
//            result = true;
//        } catch (IOException e) {
//            try {
//                btSocket.close();
//                result = false;
//            } catch (IOException e2) {
//                errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
//                result = false;
//            }
//        }
////        try {
////            btSocket.close();
////        } catch (IOException e2) {
////            errorExit("Fatal Error", "In connectToDevice() and unable to close socket during connection failure" + e2.getMessage() + ".");
////        }
//        return result;
//    }



//    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
//        if (Build.VERSION.SDK_INT >= 10) {
//            try {
//                final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[]{UUID.class});
//                return (BluetoothSocket) m.invoke(device, MY_UUID);
//            } catch (Exception e) {
//                Log.e(TAG, "Could not create Insecure RFComm Connection", e);
//            }
//        }
//        return device.createRfcommSocketToServiceRecord(MY_UUID);
//    }