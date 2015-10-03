package org.codejargon.feather;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
@Scope("prototype")
public class B {
    private final C c;

    @Inject
    public B(C c) {
        this.c = c;
    }
}
