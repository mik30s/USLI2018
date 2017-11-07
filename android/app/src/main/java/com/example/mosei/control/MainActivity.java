package com.example.mosei.control;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.design.widget.BottomNavigationView;

public class MainActivity extends AppCompatActivity
{
    ViewPager viewPager;

    private BottomNavigationView.OnNavigationItemSelectedListener
    onNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener()
    {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_radio:
                    viewPager.setCurrentItem(AppFragmentPagerAdapter.RADIO_FRAGMENT);
                    break;
                case R.id.navigation_sensors:
                    viewPager.setCurrentItem(AppFragmentPagerAdapter.SENSORS_FRAGMENT);
                    break;
                default:
                    viewPager.setCurrentItem(AppFragmentPagerAdapter.TRACKING_FRAGMENT);
                    break;
            }
            return true;
        }
    };

    /**
     * Called for each activity. Perform work on the first start of the activity.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(new AppFragmentPagerAdapter(getSupportFragmentManager()));

        // add bottom navigation.
        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener);

        startService(new Intent(this, CommuncationService.class));

        finish();
    }
}
