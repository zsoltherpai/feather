package org.codejargon.feather;

import javax.inject.Inject;

public class A {
    private final B b;

    @Inject
    public A(B b) {
        this.b = b;
    }
}
