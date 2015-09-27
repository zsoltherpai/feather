package org.codejargon.feather;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dagger.ObjectGraph;
import org.picocontainer.MutablePicoContainer;

public class BootstrapComparison {
    private static final int warmup = 1000;
    private static final int iterations = 100000;

    public static void main(String[] args) {
        for (int i = 0; i < warmup; ++i) {
            Feather feather = Feather.with();
            Injector injector = Guice.createInjector();
            MutablePicoContainer pico = InstantiationComparison.pico();
            ObjectGraph dagger = InstantiationComparison.dagger();
            NewAFactory.create();
            feather.instance(A.class);
            injector.getInstance(A.class);
            pico.getComponent(A.class);
            dagger.get(A.class);
        }

        StopWatch.millis("Plain new", () -> {
            for (int i = 0; i < iterations; ++i) {
                NewAFactory.create();
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
        StopWatch.millis("Dagger", () -> {
            for (int i = 0; i < iterations; ++i) {
                ObjectGraph dagger = InstantiationComparison.dagger();
                dagger.get(A.class);
            }
        });
        StopWatch.millis("PicoContainer", () -> {
            for (int i = 0; i < iterations; ++i) {
                MutablePicoContainer pico = InstantiationComparison.pico();
                pico.getComponent(A.class);
            }
        });
    }
}
