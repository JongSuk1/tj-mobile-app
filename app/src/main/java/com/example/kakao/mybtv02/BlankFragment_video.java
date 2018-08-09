package com.example.kakao.mybtv02;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;


public class BlankFragment_video extends Fragment {
    private itemHolder_capture video_itemHolder;
    private ArrayList<item> video_list_item;

    private ListView video_listview;
    private TextView video_textview;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = null; // Fragment가 보여줄 View 객체를 참조할 참조변수
        view = inflater.inflate(R.layout.fragment_blank_fragment_video, null);


        MyImageAdapter_capture video_ImageAdapter;
        video_itemHolder = itemHolder_capture.getInstance();
        video_list_item = video_itemHolder.getList_item();

        video_textview = (TextView) view.findViewById(R.id.video_feed_textview);
        video_listview = (ListView) view.findViewById(R.id.video_feed_listview);

        if (video_list_item != null) {
            video_textview.setText("image");
            video_ImageAdapter = new MyImageAdapter_capture(getActivity(), video_list_item);
            video_listview.setAdapter(video_ImageAdapter);
        } else {
            video_textview.setText("no such image");
        }

        return inflater.inflate(R.layout.fragment_blank_fragment_capture, container, false);
    }
}