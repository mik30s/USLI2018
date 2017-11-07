package com.example.mosei.control;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import ioio.lib.util.IOIOConnectionRegistry;
import ioio.lib.util.android.IOIOActivity;

public class MainActivity extends AppCompatActivity
{
    static {
        IOIOConnectionRegistry
                .addBootstraps(new String[] {
                        "ioio.lib.impl.SocketIOIOConnectionBootstrap",
                        "ioio.lib.android.accessory.AccessoryConnectionBootstrap",
                        "ioio.lib.android.bluetooth.BluetoothIOIOConnectionBootstrap",
                        "ioio.lib.android.device.DeviceConnectionBootstrap"});
    }

    ViewPager viewPager;

    private BottomNavigationView.OnNavigationItemSelectedListener
    onNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener()
    {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_tracking:
                    viewPager.setCurrentItem(AppFragmentPagerAdapter.TRACKING_FRAGMENT);
                    return true;
                case R.id.navigation_radio:
                    viewPager.setCurrentItem(AppFragmentPagerAdapter.RADIO_FRAGMENT);
                    return true;
                case R.id.navigation_sensors:
                    viewPager.setCurrentItem(AppFragmentPagerAdapter.SENSORS_FRAGMENT);
                    return true;
                default:
                    viewPager.setCurrentItem(AppFragmentPagerAdapter.TRACKING_FRAGMENT);
                    return true;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(new AppFragmentPagerAdapter(getSupportFragmentManager()));

        // add bottom navigation.
        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener);

        new CommuncationThread(this).start();
    }
}
