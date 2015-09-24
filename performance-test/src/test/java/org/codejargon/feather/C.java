package org.codejargon.feather;

import javax.inject.Inject;

public class C {
    private final D1 d1;
    private final D2 d2;

    @Inject
    public C(D1 d1, D2 d2) {
        this.d1 = d1;
        this.d2 = d2;
    }
}
