package com.example.mosei.control;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;
import com.joanzapata.iconify.Iconify;

import ioio.lib.api.Uart;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import com.joanzapata.iconify.fonts.FontAwesomeModule;

import org.json.JSONObject;

import static android.content.ContentValues.TAG;

public class MainActivity extends IOIOActivity
{
    // Rover command values
    private final int ROVER_COMM_UNLOCK = 1; // unlock, orient and rover
    private final int ROVER_COMM_VALUES = 2; // report data on values and sensors
    private final int ROVER_COMM_SOLAR  = 3; // deploy solar panels.
    private final int ROVER_COMM_DRIVE  = 4; // start rover drive.
    private final boolean FOWARD = true;
    private final boolean BACKWARD = false;

    private CustomLooper looper;

    // UI references
    private Switch lockSwitch;
    private Switch driveSwitch;
    private Switch solarSwitch;
    private SeekBar driveSpeedSeek;
    private TextView driveSpeedText;
    private TextView errorText;
    private TextView packetCountText;
    private RadioGroup speedRadioGroup;
    private ToggleButton modeToggleButton;
    private ListView packetListView;
    private ToggleButton driveDirectionToggleButton;

    // ui/ioio thread handler
    private static IOIOUIHandler handler;

    private int currentDriveSpeed = 0;
    private boolean direction;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Iconify.with(new FontAwesomeModule());

        setContentView(R.layout.activity_main);

        errorText = findViewById(R.id.errorText);
        lockSwitch = findViewById(R.id.lockSwitch);
        solarSwitch = findViewById(R.id.solarSwitch);
        driveSwitch = findViewById(R.id.driveSwitch);
        driveSpeedSeek = findViewById(R.id.speedSeek);
        driveSpeedText = findViewById(R.id.speedText);
        packetCountText = findViewById(R.id.packetCntText);
        speedRadioGroup = findViewById(R.id.speedRadioGroup);
        modeToggleButton = findViewById(R.id.modeToggleButton);
        packetListView = findViewById(R.id.packet_values_list);
        driveDirectionToggleButton = findViewById(R.id.driveDirectionToggleButton);

        lockSwitch.setChecked(false);
        solarSwitch.setChecked(false);
        driveSwitch.setChecked(false);
        driveSpeedSeek.setEnabled(false);
        modeToggleButton.setChecked(false);
        driveDirectionToggleButton.setChecked(true);

        driveSpeedSeek.setProgress(0);

        handler = new IOIOUIHandler();
    }

    class CustomLooper extends BaseIOIOLooper
    {
        private Uart uart;
        private final int IOIO_RX_PIN = 35;
        private final int IOIO_TX_PIN = 34;
        private final static int BAUD_RATE = 57600;

        public void sendControlValues(Object ...params) {
            String jsonFmt = "{\"cmd\":\"%d\", \"aux\":\"%d\", \"speed\":\"%d\"}";

            try {
                String jsonString = String.format(jsonFmt, params);
                JSONObject cmdObject = new JSONObject(jsonString+"\n");
                Log.i("Sending json Event", cmdObject.toString());
                uart.getOutputStream().write(cmdObject.toString().getBytes());
            } catch(Exception ex){
                Log.e(TAG, ex.getMessage());
            }
        }

        public void initializeHandlers() {
            lockSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    sendControlValues(ROVER_COMM_UNLOCK, (b ? 1 : 0), 0);
                }
            });

            solarSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    sendControlValues (ROVER_COMM_SOLAR, (b ? 1 : 0), 0);
                }
            });

            driveSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    driveSpeedSeek.setEnabled(b);
                    int speed = direction ? currentDriveSpeed : -1*currentDriveSpeed;
                    sendControlValues (ROVER_COMM_DRIVE, (b ? 1 : 0), speed);
                }
            });

            speedRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup radioGroup, int i) {
                    currentDriveSpeed =
                    Integer.parseInt(""+((RadioButton)findViewById(i)).getText().charAt(1));
                    driveSpeedText.setText("x" + currentDriveSpeed);
                    int b = driveSwitch.isChecked() ? 1 : 0;
                    int speed = direction ? currentDriveSpeed : -1*currentDriveSpeed;
                    sendControlValues(ROVER_COMM_DRIVE, b, speed);
                }
            });

            driveDirectionToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    //compoundButton.setText(b ? "Foward":"Backward");
                    direction = b;
                    sendControlValues (
                        ROVER_COMM_DRIVE,
                        (driveSwitch.isChecked() ? 1 : 0),
                        (direction ? currentDriveSpeed : -1*currentDriveSpeed)
                    );
                }
            });

            modeToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (b) {
                        Runnable r = new Runnable() {
                            @Override
                            public void run() {
                                disableControls();

                                String jsonFmt = "{\"cmd\":\"%d\", \"aux\":\"%d\", \"speed\":\"%d\"}";
                                try {
                                    // unlock
                                    sendControlValues(ROVER_COMM_UNLOCK, 0, 1);
                                    Thread.currentThread().sleep(1000);
                                    // drive out slowly
                                    sendControlValues(ROVER_COMM_DRIVE, 1, 2);
                                    Thread.currentThread().sleep(5000);
                                    // move fast
                                    sendControlValues(ROVER_COMM_DRIVE, 1, 5);
                                    Thread.currentThread().sleep(8000);
                                    // stop moving
                                    sendControlValues( ROVER_COMM_DRIVE, 0, 1);
                                    Thread.currentThread().sleep(1000);
                                    // deploy solar panels
                                    sendControlValues(ROVER_COMM_SOLAR, 1, 1);
                                } catch (Exception ex) {
                                    Log.e(TAG, ex.getMessage());
                                }
                            }
                        };
                        Thread t = new Thread(r);
                        t.start();
                    }
                }
            });
        }


        @Override
        protected void setup() throws ConnectionLostException {
            // initialize IOIO board uart.
            uart = ioio_.openUart(IOIO_RX_PIN,
                    IOIO_TX_PIN,
                    BAUD_RATE,
                    Uart.Parity.NONE,
                    Uart.StopBits.ONE);
            initializeHandlers();
            Log.i(TAG,"Custom Looper setup() was called.");
        }

        @Override
        public void loop() throws ConnectionLostException {

        }
    }



    public void disableControls() {
//        driveSwitch.setEnabled(false);
//        solarSwitch.setEnabled(false);
//        lockSwitch.setEnabled(false);
//        driveDirectionToggleButton.setEnabled(false);
//        findViewById(R.id.speedRadioGroup).setEnabled(false);
    }

    @Override
    protected CustomLooper createIOIOLooper() {
        Log.i("Control app", "called ioio thread!");
        return new CustomLooper();
    }
}


































