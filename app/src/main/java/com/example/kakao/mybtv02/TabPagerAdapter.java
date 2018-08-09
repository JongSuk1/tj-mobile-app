package com.example.kakao.mybtv02;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.List;

/**
 * Created by kakao on 2018. 5. 17..
 */

public class TabPagerAdapter extends FragmentPagerAdapter {
    private int tabCount;
    public TabPagerAdapter(FragmentManager fm, int tabCount){
        super(fm);
        this.tabCount = tabCount;
    }
    @Override
    public Fragment getItem(int position) {

        // Returning the current tabs
        switch (position) {
            case 0:
                BlankFragment_capture tabFragment1 = new BlankFragment_capture();
                return tabFragment1;
            case 1:
                BlankFragment_photolog tabFragment2 = new BlankFragment_photolog();
                return tabFragment2;
            case 2:
                BlankFragment_video tabFragment3 = new BlankFragment_video();
                return tabFragment3;
            default:
                return null;
        }
    }
    @Override
    public int getCount() {
        return tabCount;
    }
}
