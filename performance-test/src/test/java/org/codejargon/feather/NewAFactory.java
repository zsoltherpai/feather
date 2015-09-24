package org.codejargon.feather;

public class NewAFactory {
    public static A create() {
        return new A(new B(new C(new D1(), new D2(new E()))));
    }
}
