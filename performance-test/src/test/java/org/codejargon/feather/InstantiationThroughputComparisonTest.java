package org.codejargon.feather;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dagger.Module;
import dagger.ObjectGraph;
import org.junit.Test;
import org.picocontainer.DefaultPicoContainer;
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
        MutablePicoContainer pico = pico();
        ObjectGraph dagger = dagger();
        for (int i = 0; i < warmup; ++i) {
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

    public static ObjectGraph dagger() {
        return ObjectGraph.create(new DaggerModule());
    }

    public static MutablePicoContainer pico() {
        MutablePicoContainer pico = new DefaultPicoContainer();
        pico.addComponent(A.class);
        pico.addComponent(B.class);
        pico.addComponent(C.class);
        pico.addComponent(D1.class);
        pico.addComponent(D2.class);
        pico.addComponent(E.class);
        return pico;
    }

    @Module(injects = {A.class, B.class, C.class, D1.class, D2.class, E.class})
    public static class DaggerModule {

    }
}
