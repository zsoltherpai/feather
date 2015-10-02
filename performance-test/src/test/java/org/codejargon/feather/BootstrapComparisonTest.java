package org.codejargon.feather;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dagger.Module;
import dagger.ObjectGraph;
import org.junit.Test;
import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.MutablePicoContainer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 Measures bootstrap cost of different DI tools.
 An iteration includes creating an injector and instantiating the dependency graph.
 */
public class BootstrapComparisonTest {
    private static final int warmup = 100;
    private static final int iterations = 10000;

    @Test
    public void bootstrapSpeed() {
        System.out.println(String.format("Bootstrapping DI containers %s times", iterations));
        for (int i = 0; i < warmup; ++i) {
            Feather.with().instance(A.class);
            Guice.createInjector().getInstance(A.class);
            pico().getComponent(A.class);
            dagger().get(A.class);
            new PojoFactory().create();
            spring().getBean(A.class);
        }

        StopWatch.millis("Plain new", () -> {
            for (int i = 0; i < iterations; ++i) {
                PojoFactory pojo = new PojoFactory();
                pojo.create();
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
                ObjectGraph dagger = dagger();
                dagger.get(A.class);
            }
        });
        StopWatch.millis("Spring", () -> {
            for (int i = 0; i < iterations; ++i) {
                ApplicationContext applicationContext = spring();
                applicationContext.getBean(A.class);
            }
        });
        StopWatch.millis("PicoContainer", () -> {
            for (int i = 0; i < iterations; ++i) {
                MutablePicoContainer pico = pico();
                pico.getComponent(A.class);
            }
        });
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

    public static ApplicationContext spring() {
        return new AnnotationConfigApplicationContext("org.codejargon.feather");
    }

    public static ObjectGraph dagger() {
        return ObjectGraph.create(new DaggerModule());
    }

    @Module(injects = {A.class})
    public static class DaggerModule {
        @dagger.Provides
        E e() {
            return new E();
        }
    }
}
