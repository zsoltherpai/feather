package org.codejargon.feather;

public class NewAFactory {
    public static A create() {
        return new A(createB());
    }

    public static B createB() {
        return new B(createC());
    }

    private static C createC() {
        return new C(createD1(), createD2());
    }

    private static D2 createD2() {
        return new D2(createE());
    }

    private static E createE() {
        return new E();
    }

    private static D1 createD1() {
        return new D1();
    }
}
