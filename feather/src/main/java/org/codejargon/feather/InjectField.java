package org.codejargon.feather;

import java.lang.reflect.Field;

class InjectField {
    final Field field;
    final boolean providerType;
    final Key key;

    InjectField(Field field, boolean providerType, Key key) {
        this.field = field;
        this.providerType = providerType;
        this.key = key;
    }
}
