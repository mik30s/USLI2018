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
    private Button launchRoverButton;
    private final int ROVER_COMM_DE = 1; // unlock, orient and deploy rover
    private final int ROVER_COMM_DA = 2; // report data on values and sensors
    private final int ROVER_COMM_DS = 3; // deploy solar panels.

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        launchRoverButton = (Button)findViewById(R.id.launch_button);
    }

    class Looper extends BaseIOIOLooper
    {
        private Uart uart;
        private InputStream istream;
        private OutputStream ostream;
        private final int IOIO_RX_PIN = 35;
        private final int IOIO_TX_PIN = 34;
        private final static int BAUD_RATE = 57600;
        private final String ROVER_DATA_ERROR = "ROVER_DATA_ERROR";

        @Override
        protected void setup() throws ConnectionLostException
        {
            // initialize IOIO board uart.
            uart = ioio_.openUart(IOIO_RX_PIN,
                                  IOIO_TX_PIN,
                                  BAUD_RATE,
                                  Uart.Parity.NONE,
                                  Uart.StopBits.ONE);

            // setup input and output streams for ioio communication
            istream = uart.getInputStream();
            ostream = uart.getOutputStream();

            // add listener for rover deployment
            launchRoverButton.setOnClickListener(new View.OnClickListener()
            {
                public void onClick(View view)
                {
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("cmd", ROVER_COMM_DE);
                        ostream.write((jsonObject.toString() + "\n").getBytes());
                        Log.i(TAG, jsonObject.toString());
                    } catch(Exception ex) {
                        Log.e(TAG, ex.getMessage());
                    }
                }
            });
        }

        @Override
        public void loop() throws ConnectionLostException
        {
            JSONObject jsonObject = new JSONObject();
            try {
                // ask rover for sensor and other data values
                JSONObject commandJson = new JSONObject();
                commandJson.put("cmd", ROVER_COMM_DA);
                ostream.write((commandJson.toString() + "\n").getBytes());

                // wait a few seconds before reading response.
                Thread.sleep(2000);

                // read the data sent
                byte[] roverData = new byte[1024];

                istream.read(roverData);

                String dataStr = new String(roverData);
                Log.d("Data from rover", dataStr);

                // sleep again
                Thread.sleep(500);
            }
            catch(Exception ex){
                Log.e("ERROR CONTROL APP", ex.getMessage(), ex);
            }
        }
    }


    @Override
    protected Looper createIOIOLooper()
    {
        Log.i("Control app", "called ioio thread!");
        return new Looper();
    }
}


































