package com.wakeable.avengers.alarm_1_0;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Set;

public class DeviceActivity extends AppCompatActivity {

    private static final String TAG = "DeviceActivity";
    private static DeviceActivity inst;
    private final String PREFS = "preferences";

    public static DeviceActivity instance() {
        return inst;
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            Log.d(TAG, "We received anything?");

            //Finding devices
            if (BluetoothDevice.ACTION_FOUND.equals(action))
            {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter.add("Available Device: " + device.getName() + "\n" + device.getAddress());
                Log.d(TAG, "WE GOT ONE?!?!?" + device.getName());

            }
        }
    };
    private BluetoothAdapter btAdapter = null;
    ArrayAdapter<String> mArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        inst = this;
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

                AlertDialog.Builder builder = new AlertDialog.Builder(inst);

                builder.setMessage("You are about to set " + name.split(" ")[0] + " as your wakeable device, do you want to continue?");

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String address = name.split(" ")[1];

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
        });

        listDevices();
    }

    @Override
    protected void onResume() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mReceiver, filter);

        super.onResume();
    }

    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if (btAdapter == null) {
            errorExit("Fatal Error", "Bluetooth not support");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth ON...");
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
                mArrayAdapter.add(device.getName() + " " + device.getAddress());
            }
        }
    }


    private void errorExit(String title, String message) {
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }

    public void discover(View view) {


        if (btAdapter.isDiscovering()) {
            btAdapter.cancelDiscovery();
        }
        btAdapter.startDiscovery();

    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);
        btAdapter.cancelDiscovery();

        super.onDestroy();
    }
}
