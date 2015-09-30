package org.codejargon.feather;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dagger.ObjectGraph;
import org.junit.Test;
import org.picocontainer.MutablePicoContainer;

/**
 Measures instantiation throughput of different DI tools.
 An iteration includes instantiating the dependency graph. (injector created only once)
 */
public class InstantiationThroughputComparisonTest {
    private static final int warmup = 1000;
    private static final int iterations = 10000000;

    @Test
    public void instantiationThroughput() {
        Feather feather = Feather.with();
        Injector injector = Guice.createInjector();
        MutablePicoContainer pico = BootstrapComparisonTest.pico();
        ObjectGraph dagger = BootstrapComparisonTest.dagger();
        PojoFactory pojo = new PojoFactory();
        for (int i = 0; i < warmup; ++i) {
            pojo.create();
            feather.instance(A.class);
            injector.getInstance(A.class);
            pico.getComponent(A.class);
            dagger.get(A.class);
        }

        StopWatch.millis("Plain new", () -> {
            for (int i = 0; i < iterations; ++i) {
                pojo.create();
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
        StopWatch.millis("Dagger", () -> {
            for (int i = 0; i < iterations; ++i) {
                dagger.get(A.class);
            }
        });
        StopWatch.millis("PicoContainer", () -> {
            for (int i = 0; i < iterations; ++i) {
                pico.getComponent(A.class);
            }
        });
    }
}
