package org.codejargon.feather;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class BootstrapComparison {
    private static final int warmup = 1000;
    private static final int iterations = 100000;

    public static void main(String[] args) {
        for (int i = 0; i < warmup; ++i) {
            Feather feather = Feather.with();
            Injector injector = Guice.createInjector();
            plainNew();
            feather.instance(A.class);
            injector.getInstance(A.class);
        }

        StopWatch.millis("Plain new", () -> {
            for (int i = 0; i < iterations; ++i) {
                plainNew();
            }
        });
        StopWatch.millis("Guice", () -> {
            for (int i = 0; i < iterations; ++i) {
                Injector injector = Guice.createInjector();
                injector.getInstance(A.class);
            }
        });
        StopWatch.millis("Feather", () -> {
            for (int i = 0; i < iterations; ++i) {
                Feather feather = Feather.with();
                feather.instance(A.class);
            }
        });
    }

    private static A plainNew() {
        return new A(new B(new C()));
    }

}
