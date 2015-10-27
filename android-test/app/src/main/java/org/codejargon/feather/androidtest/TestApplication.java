package org.codejargon.feather.androidtest;

import android.app.Application;

import org.codejargon.feather.Feather;

public class TestApplication extends Application {
    private Feather feather;

    @Override public void onCreate() {
        super.onCreate();
        // ...
        feather = Feather.with( /* modules if needed*/);
    }
	
	Feather feather() {
		return feather;
	}
}
