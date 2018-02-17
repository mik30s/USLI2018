package com.example.mosei.control;

import android.util.Log;
import ioio.lib.api.Uart;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.OutputStream;
import ioio.lib.util.BaseIOIOLooper;
import static android.content.ContentValues.TAG;
import ioio.lib.api.exception.ConnectionLostException;

class CustomLooper extends BaseIOIOLooper
{
    private Uart uart;
    private InputStream istream;
    private OutputStream ostream;
    private final int IOIO_RX_PIN = 35;
    private final int IOIO_TX_PIN = 34;
    private final static int BAUD_RATE = 57600;

    public void sendControlValues(String jsonString) {
        String jsonFmt = "{\"cmd\":\"%d\", \"aux\":\"%d\", \"speed\":\"%d\"}";

        try {
            JSONObject cmdObject = new JSONObject(jsonString+"\n");
            Log.i("Sending json Event", cmdObject.toString());
            ostream.write(cmdObject.toString().getBytes());
        } catch(Exception ex){
            Log.e(TAG, ex.getMessage());
        }
    }

    @Override
    protected void setup() throws ConnectionLostException {
        // initialize IOIO board uart.
        uart = ioio_.openUart(IOIO_RX_PIN,
                IOIO_TX_PIN,
                BAUD_RATE,
                Uart.Parity.NONE,
                Uart.StopBits.ONE);

        // setup input and output streams for ioio communication
        istream = uart.getInputStream();
        ostream = uart.getOutputStream();
    }

    @Override
    public void loop() throws ConnectionLostException {

    }
}
