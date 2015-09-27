package org.codejargon.feather;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dagger.ObjectGraph;
import org.junit.Test;
import org.picocontainer.MutablePicoContainer;

/**
 Measures bootstrap cost of different DI tools.
 An iteration includes creating an injector and instantiating the dependency graph.
 */
public class BootstrapComparisonTest {
    private static final int warmup = 1000;
    private static final int iterations = 100000;

    @Test
    public void bootstrapSpeed() {
        for (int i = 0; i < warmup; ++i) {
            Feather feather = Feather.with();
            Injector injector = Guice.createInjector();
            MutablePicoContainer pico = InstantiationThroughputComparisonTest.pico();
            ObjectGraph dagger = InstantiationThroughputComparisonTest.dagger();
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
                ObjectGraph dagger = InstantiationThroughputComparisonTest.dagger();
                dagger.get(A.class);
            }
        });
        StopWatch.millis("PicoContainer", () -> {
            for (int i = 0; i < iterations; ++i) {
                MutablePicoContainer pico = InstantiationThroughputComparisonTest.pico();
                pico.getComponent(A.class);
            }
        });
    }
}
