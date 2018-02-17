package com.example.mosei.control;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONObject;

public class IOIOUIHandler extends Handler {
    @Override
    public void handleMessage(Message msg) {
        if(msg.what==0){
            //errorText.setText("Got rover data!");
            try {
            }
            catch(Exception ex) {
                Log.e("SENSOR_DATA", ex.getMessage());
            }
        }
        super.handleMessage(msg);
    }
}