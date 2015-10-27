package org.codejargon.feather.androidtest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import javax.inject.Inject;

public class MainActivity extends AppCompatActivity {
    @Inject
    private A a;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((TestApplication) getApplication())
                .feather()
                .injectFields(this);
        a.b.c.get().foo();
    }


}
