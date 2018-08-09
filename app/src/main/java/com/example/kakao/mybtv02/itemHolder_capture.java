package com.example.kakao.mybtv02;


import java.util.ArrayList;

public class itemHolder_capture {
    private ArrayList<item> list_item;
    private int holderCapacity = 10;

    public ArrayList<item> getList_item() {
        return list_item;
    }

    public void setList_item(ArrayList<item> list_item) {
        this.list_item = list_item;
    }

    public void append_item(item item1){
        list_item.add(0, item1);
        int holderSize = itemSize();
        if(holderSize > holderCapacity){
            list_item.remove(holderSize-1);
        }
    }

    private int itemSize(){
        return list_item.size();
    }

    public itemHolder_capture(){
        this.list_item = new ArrayList<item>();
    }

    private static final itemHolder_capture holder = new itemHolder_capture();

    public static itemHolder_capture getInstance(){
        return holder;
    }
}