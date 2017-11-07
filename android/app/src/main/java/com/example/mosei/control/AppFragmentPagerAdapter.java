package com.example.mosei.control;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

public class AppFragmentPagerAdapter extends FragmentStatePagerAdapter {
    public final static int FRAGMENT_COUNT = 3;
    public static final int RADIO_FRAGMENT = 0;
    public static final int SENSORS_FRAGMENT = 1;
    public static final int TRACKING_FRAGMENT = 2;

    public AppFragmentPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public int getCount() { return FRAGMENT_COUNT;}

    @Override
    public Fragment getItem(int position) {
        Fragment fragment = null;
        switch(position) {
            case RADIO_FRAGMENT:
                fragment = new RadioFragment();
                break;
            case SENSORS_FRAGMENT:
                fragment = new SensorFragment();
                break;
            case TRACKING_FRAGMENT:
                fragment = new TrackingFragment();
                break;
        }
        return fragment;
    }
}
