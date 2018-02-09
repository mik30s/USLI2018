package com.example.mosei.control;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import ioio.lib.api.Uart;
import android.os.Bundle;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
    private TextView packetCountText;

    private ListView packetListView;
    private ArrayAdapter<String> packetListAdapter;
    private String[] packetDataStrings = new String[8];
    private static Handler handler;

    private int packetCount = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Iconify.with(new FontAwesomeModule());

        setContentView(R.layout.activity_main);

        lockSwitch = findViewById(R.id.lockSwitch);
        lockSwitch.setChecked(true);
        solarSwitch = findViewById(R.id.solarSwitch);
        solarSwitch.setChecked(false);
        driveSwitch = findViewById(R.id.driveSwitch);
        driveSwitch.setChecked(false);
        driveSpeedSeek = findViewById(R.id.speedSeek);
        driveSpeedSeek.setEnabled(false);

        driveSpeedSeek.setProgress(0);
        errorText = findViewById(R.id.errorText);
        driveSpeedText = findViewById(R.id.speedText);
        packetListView = findViewById(R.id.packet_values_list);
        packetCountText = findViewById(R.id.packetCntText);

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
                    //errorText.setText("Got rover data!");
                    try {
                        JSONObject sensorDataJson = new JSONObject((String) msg.obj);
                        packetDataStrings[0] = "Gyro. X - "+sensorDataJson.getString("gx");
                        packetDataStrings[1] = "Gyro. X - "+sensorDataJson.getString("gy");
                        packetDataStrings[2] = "Gyro. X - "+sensorDataJson.getString("gz");
                        packetDataStrings[3] = "Accl. X - "+sensorDataJson.getString("ax");
                        packetDataStrings[4] = "Accl. Y - "+sensorDataJson.getString("ay");
                        packetDataStrings[5] = "Accl. Z - "+sensorDataJson.getString("az");
                        packetDataStrings[6] = "Sol. V - "+sensorDataJson.getString("sv");
                        packetDataStrings[7] = "Bat. V - "+sensorDataJson.getString("bv");
                        packetCountText.setText(packetCount + " p");
                    }
                    catch(Exception ex) {
                     //   Log.e("SENSOR_DATA", ex.getMessage());
                    }
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
                    driveSpeedSeek.setEnabled(b);
                    sendControlValues(
                        "{\"cmd\":\"" + ROVER_COMM_DRIVE+ "\"," +
                        "\"aux\":\"" + (b ? 1 : 0) + "\"," +
                        "\"speed\":\"" + driveSpeedSeek.getProgress()+"\"}"
                    );
                }
            });

            driveSpeedSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    try {
                        driveSpeedText.setText("x" + i);
                        //Thread.sleep(200);
                        sendControlValues(
                                "{" +
                                        "\"cmd\":\"" + ROVER_COMM_DRIVE + "\"," +
                                        "\"aux\": \"" + (b ? 1 : 0) + "\"," +
                                        "\"speed\":\"" + driveSpeedSeek.getProgress() +
                                        "\"" + "}"
                        );
                    } catch(Exception ex){
                        Log.e("","Interrupted sleep!");
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        @Override
        public void loop() throws ConnectionLostException {
            InputStreamReader ir = new InputStreamReader(istream);
            StringBuilder sb = new StringBuilder();
            sb.append("0");
            try(BufferedReader reader = new BufferedReader(ir)) {
                int r;
                while ((r = reader.read()) != -1) {
                    char c = (char) r;
                    if (c == '}') {
                        sb.append(c);
                        break;
                    }
                    sb.append(c);
                }
                Log.d("STR_BUILDER",sb.toString() );
                //Log.i("SENSOR_READ_ERROR", sb.toString());
                Message msg = handler.obtainMessage();
                msg.obj = sb.toString();
                msg.arg1 = 0;
                handler.dispatchMessage(msg);
            } catch(IOException ex) {
                Log.e("SENSOR_READ_ERROR", ex.getMessage());
            }
        }
    }

    @Override
    protected Looper createIOIOLooper() {
        Log.i("Control app", "called ioio thread!");
        return new Looper();
    }
}


































