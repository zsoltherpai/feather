package org.codejargon.feather;

import org.junit.Test;

import javax.inject.Provider;

import static org.junit.Assert.assertNotNull;

public class DependencyTest {
    @Test
    public void dependencyInstance() {
        Feather feather = Feather.with();
        assertNotNull(feather.instance(Plain.class));
    }

    @Test
    public void provider() {
        Feather feather = Feather.with();
        Provider<Plain> plainProvider = feather.provider(Plain.class);
        assertNotNull(plainProvider.get());
    }

    @Test(expected = FeatherException.class)
    public void unknown() {
        Feather feather = Feather.with();
        feather.instance(Unknown.class);
    }

    public static class Plain {

    }

    public static class Unknown {
        public Unknown(String noSuitableConstructor) {

        }
    }
}


