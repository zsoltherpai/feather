package org.codejargon.feather;

import org.junit.Test;

import javax.inject.Inject;

import static org.junit.Assert.assertNotNull;

public class TransitiveDependencyTest {
    @Test
    public void transitive() {
        Feather feather = Feather.with();
        A a = feather.instance(A.class);
        assertNotNull(a.b.c);
    }

    @Test(expected = FeatherException.class)
    public void circular() {
        Feather feather = Feather.with();
        feather.instance(Circle1.class);
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

    public static class Circle1 {
        private final Circle2 circle2;

        @Inject
        public Circle1(Circle2 circle2) {
            this.circle2 = circle2;
        }
    }

    public static class Circle2 {
        private final Circle1 circle1;

        @Inject
        public Circle2(Circle1 circle1) {
            this.circle1 = circle1;
        }
    }
}
