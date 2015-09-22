package org.codejargon.feather;

import javax.inject.Inject;

public class B {
    private final C c;

    @Inject
    public B(C c) {
        this.c = c;
    }
}
