package com.example.kakao.mybtv02;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by kakao on 2018. 4. 13..
 */

public class item implements Parcelable {
    private String title;
    private byte[] capturedImageByte=null;

    public item(String title, byte[] capturedImage) {
        this.title = title;
        this.capturedImageByte = capturedImage;
    }

    protected item(Parcel in) {
        title = in.readString();
        capturedImageByte = in.createByteArray();
    }

    public static final Creator<item> CREATOR = new Creator<item>() {
        @Override
        public item createFromParcel(Parcel in) {
            return new item(in);
        }

        @Override
        public item[] newArray(int size) {
            return new item[size];
        }
    };

    public String getTitle(){
        return this.title;
    }

    public byte[] getCapturedImage(){
        return this.capturedImageByte;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(title);
        parcel.writeByteArray(capturedImageByte);
    }
}

