package org.codejargon.feather;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
@Scope("prototype")
public class D2 {
    private final E e;

    @Inject
    public D2(E e) {
        this.e = e;
    }
}
