package org.codejargon.feather;

import org.junit.Test;

import javax.inject.Provider;
import javax.inject.Singleton;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class SingletonTest {
    @Test
    public void nonSingleton() {
        Feather feather = Feather.with();
        assertNotEquals(feather.instance(Plain.class), feather.instance(Plain.class));
    }

    @Test
    public void singleton() {
        Feather feather = Feather.with();
        assertEquals(feather.instance(SingletonObj.class), feather.instance(SingletonObj.class));
    }

    @Test
    public void singletonThroughProvider() {
        Feather feather = Feather.with();
        Provider<SingletonObj> provider = feather.provider(SingletonObj.class);
        assertEquals(provider.get(), provider.get());
    }

    public static class Plain {

    }

    @Singleton
    public static class SingletonObj {

    }
}
