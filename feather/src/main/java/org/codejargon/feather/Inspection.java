package org.codejargon.feather;

import javax.inject.Inject;
import javax.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class Inspection {
    public static Set<Method> providers(Class<?> clazz) {
        Class<?> currentClass = clazz;
        Set<Method> providerMethods = new HashSet<>();
        while (!currentClass.equals(Object.class)) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Provides.class) && !providerInSubClass(method, providerMethods)) {
                    method.setAccessible(true);
                    providerMethods.add(method);
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        return providerMethods;
    }

    public static Constructor constructor(Key key) {
        Constructor inject = null;
        Constructor noarg = null;
        for (Constructor c : key.type.getDeclaredConstructors()) {
            if (c.isAnnotationPresent(Inject.class)) {
                if (inject != null) {
                    throw new FeatherException(String.format("Dependency %s has more than one @Inject constructor", key.type));
                } else {
                    c.setAccessible(true);
                    inject = c;
                }
            } else if (c.getParameters().length == 0) {
                c.setAccessible(true);
                noarg = c;
            }
        }
        Constructor constructor = inject != null ? inject : noarg;
        if (constructor != null) {
            constructor.setAccessible(true);
            return constructor;
        } else {
            throw new FeatherException(String.format("Dependency %s must have an @Inject constructor or a no-arg constructor or configured with @Provides in a module", key.type.getName()));
        }
    }

    public static Set<Field> injectFields(Class<?> target) {
        Class<?> currentClass = target;
        Set<Field> fields = new HashSet<>();
        while (!currentClass.equals(Object.class)) {
            for (Field field : currentClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(Inject.class)) {
                    field.setAccessible(true);
                    fields.add(field);
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        return fields;
    }

    public static Annotation qualifier(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().isAnnotationPresent(Qualifier.class)) {
                return annotation;
            }
        }
        return null;
    }

    private static boolean providerInSubClass(Method method, Set<Method> discoveredMethods) {
        for (Method discovered : discoveredMethods) {
            if (discovered.getName().equals(method.getName()) && Arrays.equals(method.getParameterTypes(), discovered.getParameterTypes())) {
                return true;
            }
        }
        return false;
    }


}
