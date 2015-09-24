package org.codejargon.feather;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class InstantiationComparison {
    private static final int warmup = 1000;
    private static final int iterations = 10000000;

    public static void main(String[] args) {
        Feather feather = Feather.with();
        Injector injector = Guice.createInjector();

        for (int i = 0; i < warmup; ++i) {
            NewAFactory.create();
            feather.instance(A.class);
            injector.getInstance(A.class);
        }

        StopWatch.millis("Plain new", () -> {
            for (int i = 0; i < iterations; ++i) {
                NewAFactory.create();
            }
        });
        StopWatch.millis("Guice", () -> {
            for (int i = 0; i < iterations; ++i) {
                injector.getInstance(A.class);
            }
        });
        StopWatch.millis("Feather", () -> {
            for (int i = 0; i < iterations; ++i) {
                feather.instance(A.class);
            }
        });
    }
}
