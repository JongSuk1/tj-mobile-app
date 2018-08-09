package com.example.kakao.mybtv02;

import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.Toast;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager viewPager;

    private ImageButton mImageButton;
    private long mLastClickTime = 0;

    private boolean capture_done = false;
    private boolean btOn_done = false;

    private boolean capture_check_ready = false;
    private boolean btOn_check_ready = false;

    private Handler mHandler;

    private final int capture_function = 1;
    private final int btOn_function = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == capture_function) {
                    Log.d("cap", "got it");
                    if (msg.arg1 == 0) {
                        Log.d("cap arg1", "no such case");
                    }
                    else{
                        if(msg.arg2 == 1){
                            ackTask.setSentStatus(true);
                            Intent start_capture_intent = new Intent();
                            start_capture_intent.setAction(Intent.ACTION_SEND);
                            start_capture_intent.putExtra("STARTING_CAPTURE",true);
                            sendBroadcast(start_capture_intent);


                            mLastClickTime = SystemClock.elapsedRealtime();
                            Intent setModeIntent = new Intent("CHANGE_LOADING_MODE");
                            setModeIntent.putExtra("LOADING_MODE", Constants.CAM_CAPTURE);
                            sendBroadcast(setModeIntent);
                        }
                        else{
                            mImageButton.setEnabled(true);
                            ackTask.setSentStatus(true);
                            Log.e("ACK : ","CAM_CAPTURE is not return");
                            Toast.makeText(getApplicationContext(), "촬영 명령이 전송되지 않았습니다", Toast.LENGTH_SHORT).show();

                            Log.d("arg2", "is false");
                        }
                    }
                }
                else if(msg.what == btOn_function){
                    Log.d("btOn", "got it");

                    if(msg.arg1 ==0) {
                        Log.d("btOn arg1", "no such case");
                    }
                    else{
                        if(msg.arg2 == 1){

                        }
                        else {
                            Log.e("ACK :","BT_ON is not return");
                            Toast.makeText(getApplicationContext(), "BT 연결 안됨", Toast.LENGTH_SHORT).show();
                            Intent stopServiceIntent = new Intent("com.example.kakao.mybtv02.BluetoothService");
                            stopServiceIntent.setPackage(getApplicationContext().getPackageName());
                            if (BluetoothService.is_running())
                                stopService(stopServiceIntent);
                        }
                    }
                }

            }
        };



        mImageButton = (ImageButton) findViewById(R.id.imagebutton_capture);
        mImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000){
                    Log.d("clocktime", String.valueOf(SystemClock.elapsedRealtime()));
                    return;
                }
                mImageButton.setEnabled(false);

                if(BluetoothService.is_running()){

                    try {
                        Intent capture_write_intent = new Intent();
                        capture_write_intent.setAction("writeInJson");
                        capture_write_intent.putExtra("msg",Constants.CAM_CAPTURE);
                        capture_write_intent.putExtra("value",Constants.NOTHING);
                        capture_write_intent.putExtra("count",3);
                        capture_write_intent.putExtra("function_name","capture");
                        sendBroadcast(capture_write_intent);

                    } catch (Exception e) {
                        Log.d("Exception","raised while write capture json");
                        capture_done = false;
                    }
                }
                else{
                    Toast.makeText(getApplicationContext(), "블루투스가 연결되지 않았습니다", Toast.LENGTH_SHORT).show();
                }
                mImageButton.setEnabled(true);

            }
        });


        // Adding Toolbar to the activity
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        // Initializing the TabLayout
        tabLayout = (TabLayout) findViewById(R.id.tabLayout);

        tabLayout.addTab(tabLayout.newTab().setText("CAPTURE"));
        tabLayout.addTab(tabLayout.newTab().setText("PHOTOLOG"));
        tabLayout.addTab(tabLayout.newTab().setText("VIDEO"));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        // Initializing ViewPager
        viewPager = (ViewPager) findViewById(R.id.pager);

        // Creating TabPagerAdapter adapter
        TabPagerAdapter pagerAdapter = new TabPagerAdapter(getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(pagerAdapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

        // Set TabSelectedListener
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                viewPager.setCurrentItem(position);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(blEventReceiver);
        unregisterReceiver(writtenWellReceiver);
        Intent stopServiceIntent = new Intent("com.example.kakao.mybtv02.BluetoothService");
        stopServiceIntent.setPackage(getApplicationContext().getPackageName());
        if(BluetoothService.is_running())
            stopService(stopServiceIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //return super.onCreateOptionsMenu(menu);
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);
        return true;
    }
    //추가된 소스, ToolBar에 추가된 항목의 select 이벤트를 처리하는 함수
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //return super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.bt:
                // bt connection alarm
                IntentFilter bt_connection_filter = new IntentFilter();
                bt_connection_filter.addAction("bt_connection_main");
                registerReceiver(blEventReceiver, bt_connection_filter);

                IntentFilter written_well_connection_filter = new IntentFilter();
                written_well_connection_filter.addAction("btOn");
                written_well_connection_filter.addAction("capture");
                registerReceiver(writtenWellReceiver, written_well_connection_filter);

                // User chose the "Settings" item, show the app settings UI...
                if(BluetoothService.is_running()){
                    Toast.makeText(getApplicationContext(), "블루투스가 이미 연결되었습니다", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(getApplicationContext(), "블루투스 버튼 클릭됨", Toast.LENGTH_SHORT).show();
                    Intent btIntent = new Intent(getApplicationContext(), btActivity.class); // view.getContext() indicates MainActivity, this
                    startActivity(btIntent);
                }
                return true;
            case R.id.cam:
                Toast.makeText(getApplicationContext(), "카메라설정 버튼 클릭됨", Toast.LENGTH_SHORT).show();
                Intent camIntent = new Intent(getApplicationContext(), camActivity.class); // view.getContext() indicates MainActivity, this
                startActivity(camIntent);
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                Toast.makeText(getApplicationContext(), "나머지 버튼 클릭됨", Toast.LENGTH_LONG).show();
                return super.onOptionsItemSelected(item);
        }
    }

    final BroadcastReceiver blEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String isConnected = intent.getExtras().getString("is_connected");

            if (isConnected.equals(Constants.ON)) {
                Log.d("MAIN ACTIVITY : ", "bluetooth has connected");
                try {
                    BluetoothService.writeInJson(Constants.BT, Constants.ON);
                } catch (IOException e) {
                    Log.e("blEventReceiver","Exception raised while write BT_ON",e);
                }

                Toast.makeText(getApplicationContext(), "연결됨", Toast.LENGTH_SHORT).show();
            } else if (isConnected.equals(Constants.OFF)){
                Log.d("MAIN ACTIVITY : ", "bluetooth has disconnected");
                Toast.makeText(getApplicationContext(), "연결유실", Toast.LENGTH_SHORT).show();
                Intent stopServiceIntent = new Intent("com.example.kakao.mybtv02.BluetoothService");
                stopServiceIntent.setPackage(getApplicationContext().getPackageName());
                if (BluetoothService.is_running())
                    stopService(stopServiceIntent);
            }
        }
    };

    final BroadcastReceiver writtenWellReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(action.equals("capture")){
                capture_done = intent.getBooleanExtra("isWriteSuccessful", false);
                capture_check_ready = true;
                mHandler.obtainMessage(capture_function, capture_check_ready? 1:0, capture_done? 1:0).sendToTarget();
            }
            else if(action.equals("btOn")){
                btOn_done = intent.getBooleanExtra("isWriteSuccessful", false);
                btOn_check_ready = true;
                mHandler.obtainMessage(btOn_function, btOn_check_ready? 1:0, btOn_done? 1:0).sendToTarget();
            }
            else{
                Log.e("writtenWellReceiver", "no such action");
            }
        }
    };


}
