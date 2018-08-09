package com.example.kakao.mybtv02;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

public class BluetoothService extends Service {
    // Debugging
    private static final String TAG = "BluetoothService";
    private static boolean is_running = false;

    // Intent request code
    private final int REQUEST_CONNECT_DEVICE = 1;
    private final int REQUEST_ENABLE_BT = 2;

    // RFCOMM Protocol
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter btAdapter;

    private Activity mActivity;
    private itemHolder_capture m_itemHolderCapture;
    private itemHolder_photolog m_itemHolderPhotolog;

    private ConnectThread mConnectThread;
    private static ConnectedThread mConnectedThread;

    private String loading_mode = Constants.NOTHING;
    private BroadcastReceiver ModebroadcastReceiver;


    private int mState;

    private final int STATE_NONE = 0; // we're doing nothing
    private final int STATE_LISTEN = 1; // now listening for incoming connections
    private final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    private final int STATE_CONNECTED = 3; // now connected to a remote device

    public String EXTRA_DEVICE_ADDRESS = "device_address";

    // Constructors
    public BluetoothService(){
        btAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public BluetoothService(Activity ac) {
        mActivity = ac;
        btAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        is_running = true;
        if(mConnectedThread != null){
            Log.e("mConnected Thread", "free");
            mConnectedThread.cancel();
        }
        if(mConnectThread != null){
            Log.e("mConnect Thread", "free");
            mConnectThread.cancel();
        }

        ModebroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d("MODE BROADCAST RECEIVER", action);
                Log.d("loading mode is : ", intent.getStringExtra("LOADING_MODE"));
                loading_mode =  intent.getStringExtra("LOADING_MODE");
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("CHANGE_LOADING_MODE"); //Action is just a string used to identify the receiver as there can be many in your app so it helps deciding which receiver should receive the intent.
        registerReceiver(ModebroadcastReceiver, intentFilter);

        IntentFilter intentFilter_FunctionCall = new IntentFilter();
        intentFilter_FunctionCall.addAction("writeInJson");
        registerReceiver(blFunctionReceiver, intentFilter_FunctionCall);
    }

    public static boolean is_running(){
        return is_running;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub
        getDeviceInfo(intent);
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        Log.e("BLUETOOTH SERVICE", "on destroy");
        unregisterReceiver(ModebroadcastReceiver);
        unregisterReceiver(blFunctionReceiver);

        if(mConnectThread != null){
            mConnectThread.cancel();
        }
        if(mConnectedThread != null){
            mConnectedThread.cancel();
        }
        is_running = false;

        Intent bt_disconnected_intent = new Intent();
        bt_disconnected_intent.setAction("bt_connection");
        bt_disconnected_intent.putExtra("is_connected",Constants.OFF);
        sendBroadcast(bt_disconnected_intent);

        bt_disconnected_intent.setAction("bt_connection_main");
        bt_disconnected_intent.putExtra("is_connected",Constants.OFF);
        sendBroadcast(bt_disconnected_intent);
    }

    public boolean getDeviceState() {
        Log.i(TAG, "Check the Bluetooth support");
        if (btAdapter == null) {
            Log.d(TAG, "Bluetooth is not available");
            return false;

        } else {
            Log.d(TAG, "Bluetooth is available");
            return true;
        }
    }


    /**
     * Available device search
     */
    public void scanDevice() {
        Log.d(TAG, "Scan Device");
        Intent serverIntent = new Intent(mActivity, btActivity.class);
        mActivity.startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    }

    /**
     * after scanning and get device info
     *
     * @param data
     */
    public void getDeviceInfo(Intent data) {
        // Get the device MAC address
        String address = data.getExtras().getString(
                btActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        Log.d(TAG, "Get Device Info \n" + "address : " + address);

        connect(device);
    }

    // Bluetooth state set
    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
    }

    // Bluetooth state get
    public synchronized int getState() {
        return mState;
    }

    public synchronized void start() {
        Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread == null) {

        } else {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread == null) {

        } else {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
    }

    // ConnectThread to device
    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread == null) {

            } else {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread == null) {

        } else {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);

        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    // ConnectedThread
    public synchronized void connected(BluetoothSocket socket,
                                       BluetoothDevice device) {
        Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread == null) {

        } else {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread == null) {

        } else {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        setState(STATE_CONNECTED);
    }

    // thread stop
    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_NONE);
    }

    public void write(byte[] out) { // Create temporary object
        ConnectedThread r; // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED)
                return;
            r = mConnectedThread;
        } // Perform the write unsynchronized r.write(out); }
    }

    private void connectionFailed() {
        Intent bt_disconnected_intent = new Intent();
        bt_disconnected_intent.setAction("bt_connection");
        bt_disconnected_intent.putExtra("is_connected",Constants.OFF);
        sendBroadcast(bt_disconnected_intent);

        bt_disconnected_intent.setAction("bt_connection_main");
        bt_disconnected_intent.putExtra("is_connected",Constants.OFF);
        sendBroadcast(bt_disconnected_intent);

        setState(STATE_LISTEN);
    }

    private void connectionLost() {
        setState(STATE_LISTEN);
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            btAdapter.cancelDiscovery();

            try {
                mmSocket.connect();
                Log.d(TAG, "Connect Success");
            } catch (IOException e) {
                if(mConnectedThread != null){
                    mConnectedThread.cancel();
                }
                if(mConnectThread != null){
                    mConnectThread.cancel();
                }
                connectionFailed();
                Log.d(TAG, "Connect Fail");
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG,
                            "unable to close() socket during connection failure",
                            e2);
                }
                return;
            }
            synchronized (BluetoothService.this) {
                mConnectThread = null;
            }

            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public InputStream getMmInStream(){
            return mmInStream;
        }
        public OutputStream getMmOutStream(){
            return mmOutStream;
        }

        public void run() {
            if(is_running != true){
                is_running = true;
            }
            Log.i(TAG, "BEGIN mConnectedThread");

            Intent bt_connected_intent = new Intent();
            bt_connected_intent.setAction("bt_connection");
            bt_connected_intent.putExtra("is_connected",Constants.ON);
            sendBroadcast(bt_connected_intent);

            bt_connected_intent.setAction("bt_connection_main");
            bt_connected_intent.putExtra("is_connected",Constants.ON);
            sendBroadcast(bt_connected_intent);


            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            try {
                //blocking
                readImages();
            } catch (Exception e) {
                Log.e(TAG, "disconnected", e);
                connectionLost();
            }
        }

        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    //
    //
    // send and load data
    //
    //
    //


    /* Call this from the main activity to send data to the remote device */
    public static void writeInJson(String msg, String value) throws IOException {
        InputStream mmInStream;
        OutputStream mmOutStream;

        if(mConnectedThread == null){
            Log.e(TAG, "mConnectedThread is null");
            return;
        }
        else{
            mmInStream = mConnectedThread.getMmInStream();
            mmOutStream= mConnectedThread.getMmOutStream();
        }

        String jsonStr = String.format("{\"msg\" : \"%s\", \"value\" : \"%s\"}", msg, value);
        Log.d("jsonStr :",jsonStr);

        byte[] bytes = jsonStr.getBytes();           //converts entered String into bytes
        try {
            mmOutStream.write(bytes);
        } catch (IOException e) {
            Log.e(TAG, "stream write failed", e);
            return;
        }

        Log.d("byte", "write json successfully");
        return;
    }



    public void readImages() {
        Log.d("Loading mode is ", loading_mode);

        InputStream mmInStream;
        int imageIndex = 0;
        int imageLength = 0;
        int titleLength = 0;
        long starttime = SystemClock.elapsedRealtime();
        boolean checkerror = false;

        byte[] byte_tank = new byte[200000];
        byte[] byte_titleLength = new byte[4];
        byte[] byte_imageLength = new byte[4];

        if(mConnectedThread == null){
            Log.e(TAG, "mConnectedThread is null");
            return;
        }
        else{
            mmInStream = mConnectedThread.getMmInStream();
        }

        // Keep listening to the InputStream until an exception occurs
        try {
            while(mmInStream.available() < 4){
                SystemClock.sleep(500);
            }
            mmInStream.read(byte_titleLength, 0, 4);

            Log.d("byte", String.valueOf(ByteBuffer.wrap(byte_titleLength).getInt()));
            starttime = SystemClock.elapsedRealtime();

            titleLength = ByteBuffer.wrap(byte_titleLength).getInt();
            if (titleLength == Constants.TERMINATE_CODE) {// message for termination
                Log.d(TAG, "got termination codon");
                publishResults("CAPTURE_DONE", true);
                return;
            }
            Log.d(TAG, "title length : " + String.valueOf(titleLength));

            byte[] titleBuf = new byte[titleLength];
            while(mmInStream.available() < titleLength){
                SystemClock.sleep(500);
            }
            if (mmInStream.read(titleBuf, 0, titleLength) < 0) {
                Log.e("2CONNECTED THREAD", "reading error");
                publishResults("CAPTURE_DONE", false);
                return;
            }
            String s_title = new String(titleBuf);
            Log.d("image title : ",s_title);


            while(mmInStream.available() < 4){
                SystemClock.sleep(500);
            }
            if (mmInStream.read(byte_imageLength, 0, 4) < 0) {
                Log.e("3CONNECTED THREAD", "reading error");
                publishResults("CAPTURE_DONE", false);
                return;
            }

            imageLength = ByteBuffer.wrap(byte_imageLength).getInt();
            Log.d(TAG, "image length : " + String.valueOf(imageLength));

            int tempBufSize;
            while (imageIndex < imageLength) {
                if (2000 < imageLength - imageIndex) {
                    tempBufSize = 2000;
                } else {
                    tempBufSize = imageLength - imageIndex;
                }

                Log.d("tempBufsize", String.valueOf(tempBufSize));

                byte[] curBuf = new byte[tempBufSize];
                while (mmInStream.available() < tempBufSize) {
                    SystemClock.sleep(500);
                    /*
                    if (SystemClock.elapsedRealtime() - starttime > 15000) {
                        checkerror = true;
                        break;
                    }*/
                    Log.d("2---------", "inavailable");
                }
                if (checkerror) {
                    Log.e("CONNECTED THREAD", "reading error");
                    publishResults("CAPTURE_DONE", false);
                    return;
                }

                if (mmInStream.read(curBuf, 0, tempBufSize) < 0) {
                    Log.e("4CONNECTED THREAD", "reading error");
                    publishResults("CAPTURE_DONE", false);
                    return;
                }
                System.arraycopy(curBuf, 0, byte_tank, imageIndex, tempBufSize);
                imageIndex += tempBufSize;
            }
            byte[] b_img = Arrays.copyOfRange(byte_tank, 0, imageIndex);

            Log.d(TAG, "title name : " + s_title);
            Log.d(TAG, "image size : " + String.valueOf(b_img.length));
            Log.d("mode is ---------", loading_mode);

            item newImgItem = new item(s_title, b_img);

            if (loading_mode.equals(Constants.CAM_CAPTURE)) {
                m_itemHolderCapture = itemHolder_capture.getInstance();
                m_itemHolderCapture.append_item(newImgItem);
            } else if (loading_mode.equals(Constants.CAM_PERIOD)) {
                m_itemHolderPhotolog = itemHolder_photolog.getInstance();
                m_itemHolderPhotolog.append_item(newImgItem);
            } else {
                Log.e("NOTHING", "Loading mode not updated");
            }
            Log.d(TAG, "final image size : " + String.valueOf(imageIndex));

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("CONNECTED THREAD", "reading error");
            publishResults("CAPTURE_DONE", false);
            return;
        }
        Log.d(TAG, "Reading image done! ");
    }

    private void publishResults(String mode, boolean arg_tf){
        if(mode.equals("STARTING_CAPTURE")){
            Intent start_capture_intent = new Intent();
            start_capture_intent.setAction(Intent.ACTION_SEND);
            start_capture_intent.putExtra("STARTING_CAPTURE",arg_tf);
            sendBroadcast(start_capture_intent);
        }
        if(mode.equals("CAPTURE_DONE")){
            if(loading_mode.equals(Constants.CAM_CAPTURE)){
                Intent capture_intent = new Intent();
                capture_intent.setAction(Intent.ACTION_SEND);
                capture_intent.putExtra("CAPTURE_DONE",arg_tf);
                sendBroadcast(capture_intent);
            }
            else if(loading_mode.equals(Constants.CAM_PERIOD)){
                Intent capture_intent = new Intent();
                capture_intent.setAction("ACTION_SEND_PHOTOLOG");
                capture_intent.putExtra("CAPTURE_DONE",arg_tf);
                sendBroadcast(capture_intent);
            }
            else{
                Log.e("loading mode", "set wrong loading mode !");
            }
        }
    }

    final BroadcastReceiver blFunctionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d("blFunctionReceiver",action);
            if(action.equals("writeInJson")){
                String msg = intent.getStringExtra("msg");
                String value = intent.getStringExtra("value");
                int count = intent.getIntExtra("count",3);
                Log.d("arg info : ", msg + " " + value + String.valueOf(count));

                boolean isWriteSuccessful = false;
                try{
                    isWriteSuccessful = writeInJson(msg, value, count);
                    Log.d("is successful?", String.valueOf(isWriteSuccessful));
                }
                catch(Exception e){
                    Log.e("isWriteSuccessful", "failed");
                }

                String function_name = intent.getStringExtra("function_name");
                Intent writeInJson_intent = new Intent();
                writeInJson_intent.setAction(function_name);
                writeInJson_intent.putExtra("isWriteSuccessful",isWriteSuccessful);
                sendBroadcast(writeInJson_intent);
            }
            else{
                Log.d("action is ", action);
            }
        }
    };
}
