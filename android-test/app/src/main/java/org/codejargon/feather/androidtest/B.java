package org.codejargon.feather.androidtest;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class B {
    Provider<C> c;

    @Inject
    public B(Provider<C> c) {
        this.c = c;
    }
}
