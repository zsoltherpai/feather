package org.codejargon.feather.androidtest;

import javax.inject.Inject;

public class A {
    final B b;

    @Inject
    public A(B b) {
        this.b = b;
    }
}
