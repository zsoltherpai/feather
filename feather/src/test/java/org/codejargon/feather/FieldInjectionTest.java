package org.codejargon.feather;

import org.junit.Test;

import javax.inject.Inject;

import static org.junit.Assert.assertNotNull;

public class FieldInjectionTest {
    @Test
    public void fieldsInjected() {
        Feather feather = Feather.with();
        Target target = new Target();
        feather.injectFields(target);
        assertNotNull(target.a);
    }


    public static class Target {
        @Inject
        private A a;
    }

    public static class A {

    }
}
