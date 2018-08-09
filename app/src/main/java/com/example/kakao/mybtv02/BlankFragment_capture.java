package com.example.kakao.mybtv02;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;


public class BlankFragment_capture extends Fragment {

    private itemHolder_capture capture_itemHolderCapture;
    private ArrayList<item> capture_list_item;
    private ListView capture_listview;
    private TextView capture_textview;

    private BroadcastReceiver capture_broadcastReceiver;
    private Context _context;

    private CheckUpdatedTask task;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = null; // Fragment가 보여줄 View 객체를 참조할 참조변수
        view = inflater.inflate(R.layout.fragment_blank_fragment_capture, container, false);
        capture_textview = (TextView) view.findViewById(R.id.capture_feed_textview);
        capture_listview = (ListView) view.findViewById(R.id.capture_feed_listview);

        capture_broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("On receive ! ", "capture_broadcastReceiver");
                updateUI(intent);
            }
        };
        _context.registerReceiver(capture_broadcastReceiver, new IntentFilter(Intent.ACTION_SEND));

        return view;
    }

    public void updateUI(Intent intent){
        Bundle bundle = intent.getExtras();
        if(bundle != null){
            Log.d("starting_capture is ", String.valueOf(bundle.getBoolean("STARTING_CAPTURE")));
            Log.d("capture_done is ", String.valueOf(bundle.getBoolean("CAPTURE_DONE")));

            if (bundle.getBoolean("STARTING_CAPTURE")) {
                task = new CheckUpdatedTask();
                task.execute();
            }
            else{
                if(bundle.getBoolean("CAPTURE_DONE")){
                    task.setUpdated(true);
                    MyImageAdapter_capture capture_ImageAdapter;
                    capture_itemHolderCapture = itemHolder_capture.getInstance();
                    capture_list_item = capture_itemHolderCapture.getList_item();
                    Log.d("list number : ", String.valueOf(capture_list_item.size()));

                    if(capture_list_item != null){
                        capture_textview.setText("image");
                        capture_ImageAdapter = new MyImageAdapter_capture(getActivity(), capture_list_item);
                        capture_listview.setAdapter(capture_ImageAdapter);
                    }
                    else{
                        capture_textview.setText("no such image");
                    }
                }
                else{//사진 받기 실패
                    task.setUpdated(true);
                    Log.d("CAPTURE DONE", "false");
                    Toast.makeText(getActivity(), "사진을 받을 수 없습니다", Toast.LENGTH_SHORT).show();
                }
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
        _context.registerReceiver(capture_broadcastReceiver, new IntentFilter(Intent.ACTION_SEND));
    }

    @Override
    public void onStop(){
        super.onStop();
        _context.unregisterReceiver(capture_broadcastReceiver);
    }
}