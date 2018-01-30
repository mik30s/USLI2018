package com.example.mosei.control;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import ioio.lib.api.Uart;
import android.os.Bundle;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.OutputStream;

import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import ioio.lib.util.BaseIOIOLooper;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.joanzapata.iconify.Iconify;
import com.joanzapata.iconify.fonts.FontAwesomeModule;

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
    private Switch solarSwitch;
    private SeekBar driveSpeedSeek;
    private TextView driveSpeedText;
    private TextView errorText;

    private ListView packetListView;
    private ArrayAdapter<String> packetListAdapter;
    private String[] packetDataStrings = new String[8];
    private static Handler handler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Iconify.with(new FontAwesomeModule());

        setContentView(R.layout.activity_main);

        lockSwitch = findViewById(R.id.lockSwitch);
        lockSwitch.setChecked(false);
        solarSwitch = findViewById(R.id.solarSwitch);
        solarSwitch.setChecked(false);
        driveSwitch = findViewById(R.id.driveSwitch);
        driveSwitch.setChecked(false);
        driveSpeedSeek = findViewById(R.id.speedSeek);
        driveSpeedSeek.setEnabled(false);

        driveSpeedSeek.setProgress(1);
        errorText = findViewById(R.id.errorText);
        driveSpeedText = findViewById(R.id.speedText);
        packetListView = findViewById(R.id.packet_values_list);

        packetDataStrings[0] = "Gyro. X - 0.000";
        packetDataStrings[1] = "Gyro. Y - 0.000";
        packetDataStrings[2] = "Gyro. Z - 0.000";
        packetDataStrings[3] = "Accl. X - 0.000";
        packetDataStrings[4] = "Accl. Y - 0.000";
        packetDataStrings[5] = "Accl. Z - 0.000";
        packetDataStrings[6] = "Sol.  V - 0.000";
        packetDataStrings[7] = "Bat.  V - 0.000";

        packetListAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, packetDataStrings);
        packetListView.setAdapter(packetListAdapter);

         handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                if(msg.what==0){
                    errorText.setText((String)msg.obj);
                }
                super.handleMessage(msg);
            }
        };
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
                    "\"aux\":\"" + (b ? 1 : 0) + "\"" + "}"
                    );
                }
            });

            solarSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    sendControlValues("{" +
                            "\"cmd\":\"" + ROVER_COMM_SOLAR + "\"," +
                            "\"aux\":\"" + (b ? 1 : 0) + "\"" + "}"
                    );
                }
            });

            driveSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (b) {
                        driveSpeedSeek.setEnabled(true);
                    } else {
                        driveSpeedSeek.setEnabled(false);
                    }
                    sendControlValues(
                            "{" +
                                    "\"cmd\":\"" + ROVER_COMM_DRIVE+ "\"," +
                                    "\"aux\":\"" + (b ? 1 : 0) + "\"," +
                                    "\"speed\":\"" + driveSpeedSeek.getProgress()+"\"" +"}"
                    );
                }
            });

            driveSpeedSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    driveSpeedText.setText("x"+ i);
                    sendControlValues(
                            "{" +
                                    "\"cmd\":\"" + ROVER_COMM_DRIVE+ "\"," +
                                    "\"aux\":\"" + (b ? 1 : 0) + "\"," +
                                    "\"speed\":\"" + driveSpeedSeek.getProgress()+"\"" +"}"
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
            sendControlValues("{" + "\"cmd\":\"" + ROVER_COMM_VALUES + "\"" + "}");
            Message msg = handler.obtainMessage();
            msg.what = 0;
            msg.arg1 = 0;
            msg.obj = new String("Asking rover for data...");
            handler.sendMessage(msg);

            try {
                //Thread.sleep(3000);
                int length = istream.read();
                msg = handler.obtainMessage();
                msg.obj = new String("Got rover data");
                handler.sendMessage(msg);

            } catch(Exception ex) {
                Log.e(TAG, "Could not read sensor values from rover");
            }
        }
    }

    @Override
    protected Looper createIOIOLooper() {
        Log.i("Control app", "called ioio thread!");
        return new Looper();
    }
}


































