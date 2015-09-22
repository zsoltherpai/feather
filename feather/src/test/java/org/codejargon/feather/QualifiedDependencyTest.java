package org.codejargon.feather;

import org.junit.Test;

import javax.inject.Inject;
import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.junit.Assert.assertEquals;

public class QualifiedDependencyTest {
    @Test
    public void qualifiedInstances() {
        Feather feather = Feather.with(new Module());
        assertEquals(FooA.class, feather.instance(Key.of(Foo.class, A.class)).getClass());
        assertEquals(FooB.class, feather.instance(Key.of(Foo.class, B.class)).getClass());
    }

    @Test
    public void injectedQualified() {
        Feather feather = Feather.with(new Module());
        Dummy dummy = feather.instance(Dummy.class);
        assertEquals(FooB.class, dummy.foo.getClass());
    }

    @Test
    public void fieldInjectedQualified() {
        Feather feather = Feather.with(new Module());
        DummyTestUnit dummy = new DummyTestUnit();
        feather.injectFields(dummy);
        assertEquals(FooA.class, dummy.foo.getClass());
    }


    interface Foo {

    }

    public static class FooA implements Foo {

    }

    public static class FooB implements Foo {

    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @interface A {

    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @interface B {

    }

    public static class Module {
        @Provides
        @A
        Foo a(FooA fooA) {
            return fooA;
        }

        @Provides
        @B
        Foo b(FooB fooB) {
            return fooB;
        }
    }

    public static class Dummy {
        private final Foo foo;

        @Inject
        public Dummy(@B Foo foo) {
            this.foo = foo;
        }
    }

    public static class DummyTestUnit {
        @Inject
        @A
        private Foo foo;
    }
}
