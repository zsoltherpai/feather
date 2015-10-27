package org.codejargon.feather.androidtest;

import org.codejargon.feather.androidtest.C;

import javax.inject.Inject;
import javax.inject.Provider;

public class B {
    Provider<C> c;

    @Inject
    public B(Provider<C> c) {
        this.c = c;
    }
}
