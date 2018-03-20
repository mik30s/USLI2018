package com.example.mosei.control;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.view.Chart;
import lecho.lib.hellocharts.view.LineChartView;

import com.joanzapata.iconify.fonts.FontAwesomeModule;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    private TextView orientationText;
    private RadioGroup speedRadioGroup;
    private ToggleButton modeToggleButton;
    private ListView packetListView;
    private ToggleButton driveDirectionToggleButton;
    private JSONObject roverJson;
    private ArrayList<String> packetData;
    ArrayAdapter adapter;

    LineChartView chart;

    List<PointValue> valuesGX ;
    List<PointValue> valuesGY ;
    List<PointValue> valuesGZ ;
    List<PointValue> valuesAX ;
    List<PointValue> valuesAY ;
    List<PointValue> valuesAZ ;

    Line[] lines;
    List<Line> lineList = new ArrayList<Line>();

    // ui/ioio thread handler
    private static Handler handler;
    private int dataCount = 0;

    private int currentDriveSpeed = 0;
    private boolean direction;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Iconify.with(new FontAwesomeModule());

        setContentView(R.layout.activity_main);

        lockSwitch = findViewById(R.id.lockSwitch);
        solarSwitch = findViewById(R.id.solarSwitch);
        driveSwitch = findViewById(R.id.driveSwitch);
        driveSpeedSeek = findViewById(R.id.speedSeek);
        driveSpeedText = findViewById(R.id.speedText);
        orientationText = findViewById(R.id.orientationDirText);
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

        List<PointValue>[] values = new List[6];

        lines = new Line[]{
            new Line(valuesGX).setColor(Color.RED).setCubic(true),
            new Line(valuesGY).setColor(Color.YELLOW).setCubic(true),
            new Line(valuesGZ).setColor(Color.GREEN).setCubic(true),
            new Line(valuesAX).setColor(Color.MAGENTA).setCubic(true),
            new Line(valuesAY).setColor(Color.WHITE).setCubic(true),
            new Line(valuesAZ).setColor(Color.BLUE).setCubic(true)
        };


        handler = new Handler() {
            public void handleMessage(Message msg) {
                final int what = msg.what;
                switch(what) {
                    case 1:
                        updatePacketList();
                        updateGraph();
                        break;
                    default: break;
                }
            }
        };

        chart = findViewById(R.id.chart);
        packetData = new ArrayList<String>();
        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, packetData);
    }

    public void updatePacketList() {
        try {
            adapter.clear();
            adapter.add(String.valueOf("Accl. x: " + roverJson.getInt("ax")));
            adapter.add(String.valueOf("Accl. y: " + roverJson.getInt("ay")));
            adapter.add(String.valueOf("Accl. z: " + roverJson.getInt("az")));
            adapter.add(String.valueOf("Grav. x: " + roverJson.getInt("gx")));
            adapter.add(String.valueOf("Grav. y: " + roverJson.getInt("gy")));
            adapter.add(String.valueOf("Grav. z: " + roverJson.getInt("gz")));

            packetListView.setAdapter(adapter);

            if (roverJson.getInt("az") > 0) {
                orientationText.setText("{fa-arrow-up}");
            } else {
                orientationText.setText("{fa-arrow-down}");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void updateGraph() {
        try {
            valuesGX.add(new PointValue(dataCount, roverJson.getInt("gx")));
            valuesGY.add(new PointValue(dataCount, roverJson.getInt("gy")));
            valuesGZ.add(new PointValue(dataCount, roverJson.getInt("gz")));
            valuesAX.add(new PointValue(dataCount, roverJson.getInt("ax")));
            valuesAY.add(new PointValue(dataCount, roverJson.getInt("ay")));
            valuesAZ.add(new PointValue(dataCount, roverJson.getInt("az")));

            for (Line line : lines) {
                lineList.add(line);
            }

            LineChartData data = new LineChartData();
            data.setLines(lineList);

            chart.setLineChartData(data);
            dataCount++;

        } catch(Exception ex){}
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
            try {
                BufferedInputStream bfi = new BufferedInputStream(uart.getInputStream());
                String s = "";
                char c = (char) bfi.read();
                while (c != '\n') {
                    s += c;
                    c = (char) bfi.read();
                }
                roverJson = new JSONObject(s);
                Log.i(TAG, "From rover: " + s);

                handler.sendEmptyMessage(1);

            } catch (Exception ex) {
            }
        }
    }

    public void disableControls() {}

    @Override
    protected CustomLooper createIOIOLooper() {
        Log.i("Control app", "called ioio thread!");
        return new CustomLooper();
    }
}


































