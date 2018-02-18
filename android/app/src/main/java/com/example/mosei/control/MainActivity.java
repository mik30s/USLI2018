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
import java.util.ArrayList;
import java.util.List;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import ioio.lib.util.BaseIOIOLooper;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

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

    // List adapter
    private ArrayAdapter<String> packetListAdapter;
    private String[] packetDataStrings = new String[8];

    // ui/ioio thread handler
    private static IOIOUIHandler handler;

    private int packetCount = 0;
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
        driveDirectionToggleButton.setChecked(true);

        driveSpeedSeek.setProgress(0);

        handler = new IOIOUIHandler();
        initializeHandlers();
    }

    public void initializeHandlers() {
        lockSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                looper.sendControlValues ("{" +
                        "\"cmd\":\"" + ROVER_COMM_UNLOCK + "\"," +
                        "\"aux\":\"" + (b ? 1 : 0) + "\"" + "}"
                );
            }
        });

        solarSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                looper.sendControlValues ("{" +
                        "\"cmd\":\"" + ROVER_COMM_SOLAR + "\"," +
                        "\"aux\":\"" + (b ? 1 : 0) + "\"" + "}"
                );
            }
        });

        driveSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                driveSpeedSeek.setEnabled(b);
                looper.sendControlValues (
                        "{\"cmd\":\"" + ROVER_COMM_DRIVE+ "\"," +
                                "\"aux\":\"" + (b ? 1 : 0) + "\"," +
                                "\"speed\":\"" + (direction ? currentDriveSpeed : -1*currentDriveSpeed)+"\"}"
                );
            }
        });

        speedRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                currentDriveSpeed = Integer.parseInt(""+((RadioButton)findViewById(i)).getText().charAt(1));
                driveSpeedText.setText("x" + currentDriveSpeed);
                int b = driveSwitch.isChecked() ? 1 : 0;
                //Thread.sleep(200);
                looper.sendControlValues(
                        "{\"cmd\":\"" + ROVER_COMM_DRIVE + "\"," +
                                "\"aux\": \"" + b  + "\"," +
                                "\"speed\":\"" + (direction ? currentDriveSpeed : -1*currentDriveSpeed) +"\"}"
                );
            }
        });

        driveDirectionToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                //compoundButton.setText(b ? "Foward":"Backward");
                direction = b;
                looper.sendControlValues (
                        "{\"cmd\":\"" + ROVER_COMM_DRIVE + "\"," +
                                "\"aux\": \"" + (driveSwitch.isChecked() ? 1 : 0)  + "\"," +
                                "\"speed\":\"" + (direction ? currentDriveSpeed : -1*currentDriveSpeed)+"\"}"
                );
            }
        });

        modeToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                disableControls();
                String jsonFmt = "{\"cmd\":\"%d\", \"aux\":\"%d\", \"speed\":\"%d\"}";
                try{
                    // unlock
                    looper.sendControlValues(String.format(jsonFmt, ROVER_COMM_UNLOCK, 0, 1));
                    Thread.sleep(1000);
                    // drive out slowly
                    looper.sendControlValues(String.format(jsonFmt, ROVER_COMM_DRIVE, 1, 2));
                    Thread.sleep(5000);
                    // move fast
                    looper.sendControlValues(String.format(jsonFmt, ROVER_COMM_DRIVE, 1, 5));
                    Thread.sleep(8000);
                    // stop moving
                    looper.sendControlValues(String.format(jsonFmt, ROVER_COMM_DRIVE, 0, 1));
                    Thread.sleep(8000);
                    // deploy solar panels
                    looper.sendControlValues(String.format(jsonFmt, ROVER_COMM_SOLAR, 1, 1));
                } catch(Exception ex) {};
            }
        });
    }

    public void disableControls() {
        findViewById(R.id.driveSwitch).setEnabled(false);
        findViewById(R.id.solarSwitch).setEnabled(false);
        findViewById(R.id.lockSwitch).setEnabled(false);
        findViewById(R.id.driveSwitch).setEnabled(false);
        findViewById(R.id.driveSwitch).setEnabled(false);
        findViewById(R.id.driveDirectionToggleButton).setEnabled(false);
    }

    @Override
    protected CustomLooper createIOIOLooper() {
        Log.i("Control app", "called ioio thread!");
        looper = new CustomLooper();
        return looper;
    }
}


































