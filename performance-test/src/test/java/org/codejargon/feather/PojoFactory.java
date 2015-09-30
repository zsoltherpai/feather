package org.codejargon.feather;

public class PojoFactory {
    public A create() {
        return new A(createB());
    }

    public B createB() {
        return new B(createC());
    }

    public C createC() {
        return new C(createD1(), createD2());
    }

    public D2 createD2() {
        return new D2(createE());
    }

    public E createE() {
        return new E();
    }

    public D1 createD1() {
        return new D1();
    }
}
