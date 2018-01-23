package com.example.mosei.control;

import android.util.Log;
import ioio.lib.api.Uart;
import android.os.Bundle;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.OutputStream;

import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import ioio.lib.util.BaseIOIOLooper;
import android.widget.CompoundButton;
import android.widget.TextView;

import ioio.lib.util.android.IOIOActivity;
import ioio.lib.api.exception.ConnectionLostException;

import static android.content.ContentValues.TAG;

public class MainActivity extends IOIOActivity
{
    // Rover command values
    private final int ROVER_COMM_UNLOCK = 1; // unlock, orient and rover
    private final int ROVER_COMM_VALUES = 2; // report data on values and sensors
    private final int ROVER_COMM_SOLAR  = 3; // deploy solar panels.
    private final int ROVER_COMM_DRIVE  = 4; // start rover drive.

    // UI references
    private Switch lockSwitch;
    private Switch driveSwitch;
    private TextView driveSpeedText;
    private SeekBar driveSpeedScroll;

    @Override

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lockSwitch = findViewById(R.id.lockSwitch);
        driveSwitch = findViewById(R.id.driveSwitch);
        driveSpeedScroll = findViewById(R.id.driveSpeedScroll);
        driveSpeedText = findViewById(R.id.driveSpeedText);
    }

    class Looper extends BaseIOIOLooper
    {
        private Uart uart;
        private InputStream istream;
        private OutputStream ostream;
        private final int IOIO_RX_PIN = 35;
        private final int IOIO_TX_PIN = 34;
        private final static int BAUD_RATE = 57600;

        void sendControlValues(String jsonString) {
            try {
                JSONObject cmdObject = new JSONObject(jsonString);
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

            lockSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    sendControlValues("{" +
                    "\"cmd\":\"" + ROVER_COMM_UNLOCK + "\"," +
                    "\"aux\":\"" + (b ? 1 : 0) + "\"," + "}"
                    );
                }
            });

            driveSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    sendControlValues(
            "{" +
                    "\"cmd\":\"" + ROVER_COMM_DRIVE+ "\"," +
                    "\"aux\":\"" + (b ? 1 : 0) + "\"," +
                    "\"speed\":\"" + driveSpeedScroll.getProgress()+"\"" +"}"
                    );
                }
            });

            driveSpeedScroll.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    driveSpeedText.setText(i+"");
                    sendControlValues(
                            "{" +
                                    "\"cmd\":\"" + ROVER_COMM_DRIVE+ "\"," +
                                    "\"aux\":\"" + (b ? 1 : 0) + "\"," +
                                    "\"speed\":\"" + driveSpeedScroll.getProgress()+"\"" +"}"
                    );
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });


        }

        @Override
        public void loop() throws ConnectionLostException {

        }
    }


    @Override
    protected Looper createIOIOLooper() {
        Log.i("Control app", "called ioio thread!");
        return new Looper();
    }
}


































