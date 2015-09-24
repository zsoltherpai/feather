package org.codejargon.feather;

import org.junit.Test;

import javax.inject.Inject;
import javax.inject.Provider;

import static org.junit.Assert.assertNotNull;

public class TransitiveDependencyTest {
    @Test
    public void transitive() {
        Feather feather = Feather.with();
        A a = feather.instance(A.class);
        assertNotNull(a.b.c);
    }

    public static class A {
        private final B b;

        @Inject
        public A(B b) {
            this.b = b;
        }
    }

    public static class B {
        private final C c;

        @Inject
        public B(C c) {
            this.c = c;
        }
    }

    public static class C {

    }


}
