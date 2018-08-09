package com.example.kakao.mybtv02;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

/**
 * Created by kakao on 2018. 5. 17..
 */

// AS BECAUSE addPreferencesFromResource IS DEPRECATED IN ACTIVITY CLASS U NEED TO USE IT WITHIN FRAGMENT.

public class SetFrag extends PreferenceFragment {

    public static CheckBoxPreference cb_video;
    public static CheckBoxPreference cb_photolog;
    public static ListPreference list_period_setting;

    public boolean was_checked = false;
    //public boolean ret = false;
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.cam_settings);
        setOnPreferenceChange (findPreference("cam_period_setting"));

        cb_video = (CheckBoxPreference) getPreferenceManager().findPreference("check_box_preference_video");
        cb_photolog = (CheckBoxPreference) getPreferenceManager().findPreference("check_box_preference_photolog");
        list_period_setting = (ListPreference) getPreferenceManager().findPreference("cam_period_setting") ;

        cb_video.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Log.d("MyApp", "Pref " + preference.getKey() + " changed to " + newValue.toString());
                if(newValue.toString().equals("true")){
                    try {
                        BluetoothService.writeInJson(Constants.CAM_VIDEO, Constants.ON);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if(cb_photolog.isChecked()){
                        cb_photolog.setChecked(false);
                        was_checked = true;
                    }
                    cb_photolog.setEnabled(false);
                }

                else{
                    try {
                        BluetoothService.writeInJson(Constants.CAM_VIDEO, Constants.OFF);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (!cb_photolog.isEnabled()){
                        if(was_checked){
                            cb_photolog.setChecked(true);
                            was_checked = false;
                        }
                        cb_photolog.setEnabled(true);
                    }
                }
                return true;
            }
        });
        cb_photolog.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Log.d("MyApp", "Pref " + preference.getKey() + " changed to " + newValue.toString());

                if(newValue.toString().equals("true")){
                    try {
                        BluetoothService.writeInJson(Constants.CAM_PERIOD, Constants.ON);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                else{
                    try {
                        BluetoothService.writeInJson(Constants.CAM_PERIOD, Constants.OFF);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return true;
            }
        });
        list_period_setting.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Log.d("MyApp", "Pref " + preference.getKey() + " changed to " + newValue.toString());

                try {
                    BluetoothService.writeInJson(Constants.CAM_PERIOD, newValue.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            }
        });
    }


    private static Preference.OnPreferenceChangeListener onPreferenceChangeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (preference instanceof ListPreference) {
                String stringValue = newValue.toString();
                ListPreference listPreference = (ListPreference) preference;
            }
            else{
                Log.e("PeriodSettingError", "newValue is "+newValue.toString());
                return false;
            }
            return true;
        }
    };

    private void setOnPreferenceChange (Preference mPreference){
        mPreference.setOnPreferenceChangeListener(onPreferenceChangeListener);
        Log.e(camActivity.class.getSimpleName(), "its key is ------------------ : "+ mPreference.getKey());
        onPreferenceChangeListener.onPreferenceChange(mPreference, PreferenceManager.getDefaultSharedPreferences(mPreference.getContext()).getString(mPreference.getKey(), ""));
    }


}
