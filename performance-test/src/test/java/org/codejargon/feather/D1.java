package org.codejargon.feather;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
@Scope("prototype")
public class D1 {
    @Inject
    public D1(E e) {
    }
}
