package com.example.kakao.mybtv02;

import android.app.Activity;
import android.app.Notification;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Message;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class btActivity extends AppCompatActivity {

    static boolean active = false; // check whether this activity is active or not
    private long mLastClickTime = 0;

    @Override
    public void onStart() {
        super.onStart();
        active = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        active = false;
    }

    private final String TAG = btActivity.class.getSimpleName();

    // Return Intent extra
    public static String EXTRA_DEVICE_ADDRESS = "device_address";


    bt_ListPairedDevicesThread listThread;
    bt_DiscoverThread discoverThread;

    private ListView btDiscoveredListView;
    private ArrayAdapter<String> btDiscoveredArrayAdapter;

    private ListView btPairedListView;
    private ArrayAdapter<String> btPairedArrayAdapter;

    // BT settings
    private BluetoothDevice device;
    private BluetoothAdapter btAdapter;
    private Set<BluetoothDevice> btPairedDevices;

    ReceiverManager receiverManager = new ReceiverManager(this);

    public CheckBTConnectingTask btConnectingTask;



    public synchronized BluetoothDevice getBTConnection() {
        if (device == null){
            // raise message
            return null;
        }
        return device;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (btAdapter != null) {
            btAdapter.cancelDiscovery();
        }
        receiverManager.unregisterReceiver_all();
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();

        if (btAdapter != null) {
            btAdapter.cancelDiscovery();
        }
        receiverManager.unregisterReceiver_all();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bt);


        btAdapter = BluetoothAdapter.getDefaultAdapter();


        BroadcastReceiver bt_connect_status_broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = String.valueOf(intent.getAction());
                Log.d("bt ACT: show me action", action);
                if(action.equals("bt_connection")){
                    Log.d("value of key : ", intent.getExtras().getString("is_connected"));
                    String ConnectionStatus = intent.getExtras().getString("is_connected");
                    updateStatus(ConnectionStatus);
                }

                Log.d("On receive ! ", "what is what");
            }
        };
        receiverManager.registerReceiver(bt_connect_status_broadcastReceiver, new IntentFilter("bt_connection"));

        btDiscoveredArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        btPairedArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);


        btDiscoveredListView = (ListView) findViewById(R.id.scan_listview);
        btDiscoveredListView.setAdapter(btDiscoveredArrayAdapter);
        btDiscoveredListView.setOnItemClickListener(btDeviceClickListener);

        btPairedListView = (ListView) findViewById(R.id.paired_listview);
        btPairedListView.setAdapter(btPairedArrayAdapter);
        btPairedListView.setOnItemClickListener(btDeviceClickListener);


        listThread = new bt_ListPairedDevicesThread();
        discoverThread = new bt_DiscoverThread();
        listThread.start();
        discoverThread.start();
    }

    private void updateStatus(String tf){
        btConnectingTask.setConnectedStatus(tf);
    }

    ////////////////////////////////////////////////////////////////////////

    private AdapterView.OnItemClickListener btDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000){
                Log.d("clocktime", String.valueOf(SystemClock.elapsedRealtime()));
                return;
            }


            if (!btAdapter.isEnabled()) {
                Toast.makeText(getBaseContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
                return;
            }
            btAdapter.cancelDiscovery();

            if(discoverThread != null) {
                discoverThread.cancel();
            }

            if(listThread != null){
                listThread.cancel();
            }

            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            final String address = info.substring(info.length() - 17);
            final String name = info.substring(0, info.length() - 17);

            // Spawn a new thread to avoid blocking the GUI one
            Intent intent =new Intent("com.example.kakao.mybtv02.BluetoothService");
            intent.setPackage("com.example.kakao.mybtv02");
            Bundle address_bundle = new Bundle();
            address_bundle.putSerializable(EXTRA_DEVICE_ADDRESS, address);
            intent.putExtras(address_bundle);


            btConnectingTask = new CheckBTConnectingTask();
            btConnectingTask.execute();
            mLastClickTime = SystemClock.elapsedRealtime();

            startService(intent);

        }
    };

    /******************
     *private threads
     ******************/

    private class bt_DiscoverThread extends Thread {
        // Check if the device is already discovering
        public void run(){
            // Check if the device is already discovering
            if (btAdapter.isDiscovering()) {
                btAdapter.cancelDiscovery();
                Log.d("discover thread : ", "Discovery stopped");
            } else {
                if (btAdapter.isEnabled()) {
                    btDiscoveredArrayAdapter.clear(); // clear items
                    btAdapter.startDiscovery();
                    Log.d("discover thread : ", "Discovery start");
                    receiverManager.registerReceiver(blReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
                } else {
                    Log.d("discover thread : ", "Bluetooth not on");
                }
            }

        }

        public void cancel(){
            Log.d(TAG, "discover thread terminated");
            if (btAdapter.isDiscovering()) {
                btAdapter.cancelDiscovery();
            }
        }
    }

    private class bt_ListPairedDevicesThread extends Thread {
        public void run(){
            btPairedDevices = btAdapter.getBondedDevices();
            if (btAdapter.isEnabled()) {
                // put it's one to the adapter
                for (BluetoothDevice device : btPairedDevices)
                    btPairedArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            } else
                Log.e(TAG, "Bluetooth is currently OFF");
        }

        public void cancel(){
            Log.d(TAG, "list thread terminated");
        }
    }

    /*********************************************
     *  Broadcast Receiver                       *
     *********************************************/

    final BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add the name to the list
                Log.d(TAG, "detect " + device.getName() + "\t" + device.getAddress());
                btDiscoveredArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                btDiscoveredArrayAdapter.notifyDataSetChanged();
            }
        }
    };

    private class CheckBTConnectingTask extends AsyncTask<Void, Void, Void> {
        private String isConnected = Constants.NOTHING;

        public void CheckBTConnectingTask(){
            Log.d("CheckUpdatedTask", "created");
        }

        public void setConnectedStatus(String str){
            Log.d("in setConnectedStatus", str);
            isConnected = str;
            Log.d("isConnected", isConnected);
        }

        ProgressDialog asyncDialog = new ProgressDialog(btActivity.this);
        @Override
        protected void onPreExecute() {

            asyncDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            asyncDialog.setMessage("기기 연결중입니다..");
            Log.d("CheckBTConnectingTask", "on pre execute");
            Log.d("CheckBTConnectingTask", isConnected);
            // show dialog
            asyncDialog.show();
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            asyncDialog.setCanceledOnTouchOutside(false);
            Log.d("Background isConnected", isConnected);
            while(isConnected.equals(Constants.NOTHING)){
                Log.d("CheckBTConnectingTask", "while loop in dialog" + isConnected);
                try {
                    Thread.sleep(500);
                }
                catch(Exception e){
                    Log.e("doInBackground", "cannot sleep ! ");
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {

            Log.d("CheckBTConnectingTask", "dialog dismiss");
            asyncDialog.dismiss();
            Log.d("on post",isConnected);
            if(isConnected.equals(Constants.OFF)){

                //Toast.makeText(getApplicationContext(), "연결되었습니다",Toast.LENGTH_SHORT).show();
            }
            else if(isConnected.equals(Constants.ON)){
                Toast.makeText(getApplicationContext(), "성공메시지를 파이로 보냅니다" ,Toast.LENGTH_SHORT).show();;
            }
            super.onPostExecute(result);
        }
    }
}