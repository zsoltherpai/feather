package org.codejargon.feather;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class PojoProvidedThroughModuleTest {
    @Test(expected = FeatherException.class)
    public void pojoNotProvided() {
        Feather feather = Feather.with();
        feather.instance(Pojo.class);
    }

    @Test
    public void pojoProvided() {
        Feather feather = Feather.with(new Module());
        assertNotNull(feather.instance(Pojo.class));
    }

    public static class Module {
        @Provides
        Pojo pojo() {
            return new Pojo("foo");
        }
    }

    public static class Pojo {
        private final String foo;

        public Pojo(String foo) {
            this.foo = foo;
        }
    }
}
