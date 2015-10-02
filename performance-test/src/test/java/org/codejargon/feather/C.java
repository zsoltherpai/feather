package org.codejargon.feather;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
@Scope("prototype")
public class C {
    private final D1 d1;
    private final D2 d2;

    @Inject
    public C(D1 d1, D2 d2) {
        this.d1 = d1;
        this.d2 = d2;
    }
}
