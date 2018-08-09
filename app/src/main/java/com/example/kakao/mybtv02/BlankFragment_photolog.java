package com.example.kakao.mybtv02;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;


public class BlankFragment_photolog extends Fragment {

    private itemHolder_photolog photolog_itemHolderCapture;
    private ArrayList<item> photolog_list_item;
    private ListView photolog_listview;
    private TextView photolog_textview;

    BroadcastReceiver photolog_broadcastReceiver;
    Context _context;

    private CheckUpdatedTask task;
    private long mLastClickTime = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = null; // Fragment가 보여줄 View 객체를 참조할 참조변수
        view = inflater.inflate(R.layout.fragment_blank_fragment_photolog, container, false);

        final LinearLayout  ll = (LinearLayout) view.findViewById(R.id.photolog_linearLayout);
        ll.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000){
                    Log.d("clocktime", String.valueOf(SystemClock.elapsedRealtime()));
                    return true;
                }

                Log.d("linear view event", String.valueOf(event.getAction()));

                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                        // 이미지 뷰의 위치를 옮기기
                        if (BluetoothService.is_running()) {

                            try {
                                BluetoothService.writeInJson(Constants.LD_IMAGE, Constants.CAM_PERIOD);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            task = new CheckUpdatedTask();
                            task.execute();
                            mLastClickTime = SystemClock.elapsedRealtime();


                            Intent setModeIntent = new Intent("CHANGE_LOADING_MODE");
                            setModeIntent.putExtra("LOADING_MODE", Constants.CAM_PERIOD);
                            getActivity().sendBroadcast(setModeIntent);
                        } else {
                            Toast.makeText(getActivity(), "블루투스가 연결되지 않았습니다", Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
                return true;
            }
        });

        photolog_textview = (TextView) view.findViewById(R.id.photolog_feed_textview);
        photolog_listview = (ListView) view.findViewById(R.id.photolog_feed_listview);

        photolog_broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("On receive ! ", "photolog");
                updateUI(intent);
            }
        };
        _context.registerReceiver(photolog_broadcastReceiver, new IntentFilter("ACTION_SEND_PHOTOLOG"));

        return view;
    }


    public void updateUI(Intent intent){
        Bundle bundle = intent.getExtras();
        if(bundle != null) {
            Log.d("bundle is ", "not null");
            Log.d("get CAPTURE_DONE", String.valueOf(bundle.getBoolean("CAPTURE_DONE")));
            if (bundle.getBoolean("CAPTURE_DONE")) {
                task.setUpdated(true);
                MyImageAdapter_photolog photolog_ImageAdapter;
                photolog_itemHolderCapture = itemHolder_photolog.getInstance();
                photolog_list_item = photolog_itemHolderCapture.getList_item();
                Log.d("list number : ", String.valueOf(photolog_list_item.size()));

                if (photolog_list_item != null) {
                    photolog_textview.setText("refresh");
                    photolog_ImageAdapter = new MyImageAdapter_photolog(getActivity(), photolog_list_item);
                    photolog_listview.setAdapter(photolog_ImageAdapter);
                } else {
                    photolog_textview.setText("no such image");
                }
            }
            else {//사진 받기 실패
                task.setUpdated(true);
                Log.d("CAPTURE DONE", "false");
                Toast.makeText(getActivity(), "사진을 받을 수 없습니다", Toast.LENGTH_SHORT).show();
            }
        }
        else{
            Log.d("bundle is ", "null");
        }
    }

    private class CheckUpdatedTask extends AsyncTask<Void, Void, Void> {
        private boolean isUpdated;

        public void CheckUpdatedTask(){
            Log.d("CheckUpdatedTask", "created");
            isUpdated = false;
        }

        void setUpdated(boolean tf){
            isUpdated = tf;
        }


        ProgressDialog asyncDialog = new ProgressDialog(getActivity());

        @Override
        protected void onPreExecute() {
            asyncDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            asyncDialog.setMessage("로딩중입니다..");
            Log.d("CheckUpdatedTask", "on pre execute");

            // show dialog
            asyncDialog.show();
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            asyncDialog.setCanceledOnTouchOutside(false);

            while(! isUpdated){
                Log.d("CheckUpdatedTask", "while loop in dialog");

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
            Log.d("CheckUpdatedTask", "dialog dismiss");

            asyncDialog.dismiss();
            super.onPostExecute(result);
        }
    }

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        _context = context;
    }

    @Override
    public void onResume(){
        super.onResume();
        Log.d("CAPTURE FRAGMENT :", " RESUME");
        _context.registerReceiver(photolog_broadcastReceiver, new IntentFilter("ACTION_SEND_PHOTOLOG"));
    }

    @Override
    public void onStop(){
        super.onStop();
        _context.unregisterReceiver(photolog_broadcastReceiver);
    }
}