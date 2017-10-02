package org.codejargon.feather;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SelfProvidingFeatherTest {

    public class RequiresFeather {

        @Provides
        public Integer dependency(Feather feather) {
            return feather.hashCode();
        }
    }

    @Test
    public void testFeatherCanInjectItself() {
        final Feather feather = Feather.with(new RequiresFeather());

        assertEquals(Integer.valueOf(feather.hashCode()), feather.instance(Integer.class));
    }
}
