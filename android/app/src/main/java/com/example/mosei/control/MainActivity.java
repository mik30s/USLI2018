package com.example.mosei.control;

import ioio.lib.api.Uart;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.android.IOIOActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;

import static android.content.ContentValues.TAG;

public class MainActivity extends IOIOActivity {
    private Button payloadBtn;
    static final String STR_LAUNCH = "l";
    static final String STR_DATA_COMM = "dc";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        payloadBtn = (Button)findViewById(R.id.launch_button);
    }

    class Looper extends BaseIOIOLooper {
        private final static int BAUD_RATE = 57600;
        Uart uart;
        private InputStream istream;
        private OutputStream ostream;

        @Override
        protected void setup() throws ConnectionLostException {
            uart = ioio_.openUart(36, 34, BAUD_RATE, Uart.Parity.NONE, Uart.StopBits.ONE);
            istream =  uart.getInputStream();
            ostream = uart.getOutputStream();

            payloadBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("state",STR_LAUNCH);
                        jsonObject.put("data","");
                        ostream.write((jsonObject.toString() + "\n").getBytes());
                    } catch(Exception ex) {
                        Log.e(TAG, ex.getMessage());
                    }
                }
            }) ;
        }

        @Override
        public void loop() throws ConnectionLostException {
            JSONObject jsonObject = new JSONObject();
            try {
                byte[] roverData = new byte[77];
                int a = istream.read(roverData);
                String dataStr = new String(roverData);
                if (dataStr.charAt(0) == '{'){
                    dataStr = "{"+dataStr;
                }
                if (dataStr.charAt(dataStr.length()-1) == '{') {
                    dataStr = dataStr + "}";
                }
                Log.e(TAG, dataStr);
                Thread.sleep(1000);
            }
            catch(Exception ex){
                Log.e("ERROR CONTROL APP",ex.getMessage(), ex);
            }
        }
    }


    @Override
    protected Looper createIOIOLooper() {
        Log.i("Control app", "called ioio thread!");
        return new Looper();
    }
}


































