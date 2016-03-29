package org.codejargon.feather;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.junit.Test;

public class TransitiveFieldsDependencyTest {
    @Test
    public void transitiveFields() {
        Feather feather = Feather.withAutoInjectFields();
        A a = feather.instance(A.class);
        assertNotNull(a.b.c);
        assertTrue(a.b.c.postConstructRan);
    }

    public static class A {
    	@Inject private B b;
    	
    }

    public static class B {
    	@Inject private C c;
    }

    public static class C {
    	boolean postConstructRan;
    	
    	@PostConstruct
    	void postConstruct() {
    		postConstructRan = true;
    	}
    }
}
