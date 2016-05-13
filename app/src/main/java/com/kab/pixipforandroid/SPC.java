package com.kab.pixipforandroid;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Suxx on 07.05.2016.
 */
public class SPC {
    private static SPC SPC;
    public SharedPreferences mSettings;
    public static final String APP_PREFERENCES = "mySettingsPref";
    public static final String ROOM_NAME = "ROOM_NAME";
    public static final String ROOM_PIN = "ROOM_PIN";

    public static SPC getInstance(Context context) {
        if (SPC == null) {
            SPC = new SPC(context);
        }
        return SPC;
    }

    private SPC(Context context) {
        mSettings = context.getSharedPreferences(APP_PREFERENCES,Context.MODE_PRIVATE);
    }

    void setRoom(String value){
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putString(ROOM_NAME, value).apply();
    }

    void setPin(String value){
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putString(ROOM_PIN, value).apply();
    }

    String getRoom(){
        return mSettings.getString(ROOM_NAME, "omnimbus.meet@meeton.vc");
    }

    String getPin(){
        return mSettings.getString(ROOM_PIN, "1234");
    }
}
