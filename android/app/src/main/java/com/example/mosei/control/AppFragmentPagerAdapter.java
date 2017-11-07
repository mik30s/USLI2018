package com.example.mosei.control;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

public class AppFragmentPagerAdapter extends FragmentStatePagerAdapter
{
    public final static int FRAGMENT_COUNT = 2;
    public static final int RADIO_FRAGMENT = 0;
    public static final int SENSORS_FRAGMENT = 1;
    public static final int TRACKING_FRAGMENT = 2;

    /**
     * Constructor for adapter
     * @param fm
     */
    public AppFragmentPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    /**
     * Returns the number of fragments for this adpater
     * @return an integer representing the count
     */
    @Override
    public int getCount() { return FRAGMENT_COUNT;}

    /**
     * Returns a fragment for the current tab
     * @param position
     * @return a Fragment
     */
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
