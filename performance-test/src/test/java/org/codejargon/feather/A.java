package org.codejargon.feather;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
@Scope("prototype")
public class A {
    private final B b;

    @Inject
    public A(B b) {
        this.b = b;
    }
}
