package org.codejargon.feather;

import org.junit.Test;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FieldInjectionTest {
    @Test
    public void fieldsInjected() {
        Feather feather = Feather.with();
        Target target = new Target();
        feather.injectFields(target);
        assertNotNull(target.a);
        assertTrue(target.postConstructCalled);
    }


    public static class Target {
        @Inject
        private A a;
        
        private boolean postConstructCalled;
        
        @PostConstruct
        void postConstruct() {
        	postConstructCalled = true;
        }
    }

    public static class A {

    }
}
