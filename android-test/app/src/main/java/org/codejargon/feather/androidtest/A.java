package org.codejargon.feather.androidtest;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class A {
    final B b;

    @Inject
    public A(B b) {
        this.b = b;
    }
}
