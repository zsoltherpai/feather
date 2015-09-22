package org.codejargon.feather;

import org.junit.Test;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;

public class ModuleOverrideTest {
    @Test
    public void dependencyOverridenByModule() {
        Feather feather = Feather.with(new PlainStubOverrideModule());
        assertEquals(PlainStub.class, feather.instance(Plain.class).getClass());
    }


    @Test
    public void moduleOverwrittenBySubClass() {
        assertEquals("foo", Feather.with(new FooModule()).instance(String.class));
        assertEquals("bar", Feather.with(new FooOverrideModule()).instance(String.class));
    }

    public static class Plain {
    }

    public static class PlainStub extends Plain {

    }

    public static class PlainStubOverrideModule {
        @Provides
        public Plain plain(PlainStub plainStub) {
            return plainStub;
        }

    }

    public static class FooModule {
        @Provides
        String foo() {
            return "foo";
        }
    }

    public static class FooOverrideModule extends FooModule {
        @Provides
        @Override
        String foo() {
            return "bar";
        }
    }




}
