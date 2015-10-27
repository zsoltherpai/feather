package org.codejargon.feather.androidtest;

import android.test.ActivityInstrumentationTestCase2;

public class ActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {
    public ActivityTest() {
        super(MainActivity.class);
    }

    public void testInjection() {
        getActivity();
    }


}
