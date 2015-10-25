package org.codejargon.feather;

import javax.inject.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Feather {
    private final Map<Key, Provider<?>> providers = new ConcurrentHashMap<>();
    private final Map<Key, Object> singletons = new ConcurrentHashMap<>();
    private final Map<Key, Provider<?>[]> paramProviders = new ConcurrentHashMap<>();
    private final Map<Class, InjectField[]> injectFields = new ConcurrentHashMap<>(0);

    /**
     * Constructs Feather with the provided configuration modules
     */
    public static Feather with(Object... modules) {
        return new Feather(Arrays.asList(modules));
    }

    /**
     * Constructs Feather with the provided configuration modules
     */
    public static Feather with(Iterable<?> modules) {
        return new Feather(modules);
    }

    private Feather(Iterable<?> modules) {
        providers.put(Key.of(Feather.class), new Provider() {
                    @Override
                    public Object get() {
                        return this;
                    }
                }
        );
        for (final Object module : modules) {
            if (module instanceof Class) {
                throw new FeatherException(String.format("%s provided as class instead of an instance.", ((Class) module).getName()));
            }
            for (Method providerMethod : providers(module.getClass())) {
                providerMethod(module, providerMethod);
            }
        }
    }

    /**
     * @return an instance of the requested type
     */
    public <T> T instance(Class<T> type) {
        return provider(Key.of(type), null).get();
    }

    /**
     * @return an instance of the requested key (type/qualifier annotation)
     */
    public <T> T instance(Key<T> key) {
        return provider(key, null).get();
    }

    /**
     * @return a provider of the requested type
     */
    public <T> Provider<T> provider(Class<T> type) {
        return provider(Key.of(type), null);
    }

    /**
     * @return a provider of the requested key (type, name/qualifier annotation)
     */
    public <T> Provider<T> provider(Key<T> key) {
        return provider(key, null);
    }

    /**
     * Injects fields to the target object (non-transitive)
     */
    public void injectFields(Object target) {
        if (!injectFields.containsKey(target.getClass())) {
            injectFields.put(target.getClass(), injectFields(target.getClass()));
        }
        for (InjectField f : injectFields.get(target.getClass())) {
            try {
                f.field.set(target, f.providerType ? provider(f.key) : instance(f.key));
            } catch (IllegalAccessException e) {
                throw new FeatherException(String.format("Can't inject to field %s in %s", f.field.getName(), target.getClass().getName()));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Provider<T> provider(final Key<T> key, Set<Key> depChain) {
        if (!providers.containsKey(key)) {
            final Constructor constructor = constructor(key);
            final Provider<?>[] paramProviders = providersForParams(key, constructor.getParameterTypes(), constructor.getGenericParameterTypes(), constructor.getParameterAnnotations(), depChain);
            providers.put(key, singletonProvider(key, key.type.getAnnotation(Singleton.class), new Provider() {
                        @Override
                        public Object get() {
                            try {
                                return constructor.newInstance(arguments(paramProviders));
                            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                                throw new FeatherException(String.format("Can't instantiate dependency %s", key.toString()), e);
                            }
                        }
                    })
            );
        }
        return (Provider<T>) providers.get(key);
    }

    private void providerMethod(final Object module, final Method m) {
        final Key key = Key.of(m.getReturnType(), qualifier(m.getAnnotations()));
        if (providers.containsKey(key)) {
            throw new FeatherException(String.format("Multiple providers for dependency %s, in module %s", key.toString(), module.getClass()));
        }
        Singleton singleton = m.getAnnotation(Singleton.class) != null ? m.getAnnotation(Singleton.class) : m.getReturnType().getAnnotation(Singleton.class);
        final Provider<?>[] paramProviders = providersForParams(
                key,
                m.getParameterTypes(),
                m.getGenericParameterTypes(),
                m.getParameterAnnotations(),
                Collections.singleton(key)
        );
        providers.put(key, singletonProvider(key, singleton, new Provider() {
                            @Override
                            public Object get() {
                                try {
                                    return m.invoke(module, arguments(paramProviders));
                                } catch (IllegalAccessException | InvocationTargetException e) {
                                    throw new FeatherException(String.format("Can't instantiate %s with provider", key.toString()), e);
                                }
                            }
                        }
                )
        );
    }

    @SuppressWarnings("unchecked")
    private <T> Provider<T> singletonProvider(final Key key, Singleton singleton, final Provider<T> provider) {
        return singleton != null ? new Provider<T>() {
            @Override
            public T get() {
                if (!singletons.containsKey(key)) {
                    synchronized (singletons) {
                        if (!singletons.containsKey(key)) {
                            singletons.put(key, provider.get());
                        }
                    }
                }
                return (T) singletons.get(key);
            }
        } : provider;
    }

    private Provider<?>[] providersForParams(
            final Key key,
            Class<?>[] parameterClasses,
            Type[] parameterTypes,
            Annotation[][] parameterAnnotations,
            final Set<Key> depChain
    ) {
        if (!paramProviders.containsKey(key)) {
            Provider<?>[] providers = new Provider<?>[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; ++i) {
                Type parameterType = parameterTypes[i];
                Class<?> parameterClass = parameterClasses[i];
                Annotation qualifier = qualifier(parameterAnnotations[i]);
                Class<?> providerType = Provider.class.equals(parameterClass) ?
                        (Class<?>) ((ParameterizedType) parameterType).getActualTypeArguments()[0] :
                        null;
                if (providerType == null) {
                    final Key newKey = Key.of(parameterClass, qualifier);
                    final Set<Key> dependencyChain = append(depChain, key);
                    circularCheck(newKey, dependencyChain);
                    providers[i] = new Provider() {
                        @Override
                        public Object get() {
                            return provider(newKey, dependencyChain).get();
                        }
                    };
                } else {
                    final Key newKey = Key.of(providerType, qualifier);
                    providers[i] = new Provider() {
                        @Override
                        public Object get() {
                            return provider(newKey, null);
                        }
                    };
                }
            }
            paramProviders.put(key, providers);
        }
        return paramProviders.get(key);
    }

    private static Object[] arguments(Provider<?>[] paramProviders) {
        Object[] args = new Object[paramProviders.length];
        for (int i = 0; i < paramProviders.length; ++i) {
            args[i] = paramProviders[i].get();
        }
        return args;
    }

    private static void circularCheck(Key key, Set<Key> depChain) {
        if (depChain != null && depChain.contains(key)) {
            throw new FeatherException(String.format("Circular dependency: %s", chainString(depChain, key)));
        }
    }

    private static Set<Key> append(Set<Key> set, Key newItem) {
        if (set != null && !set.isEmpty()) {
            Set<Key> appended = new LinkedHashSet<>(set);
            appended.add(newItem);
            return appended;
        } else {
            return Collections.singleton(newItem);
        }
    }

    private static InjectField[] injectFields(Class<?> target) {
        Set<Field> fields = fields(target);
        InjectField[] injectFields = new InjectField[fields.size()];
        int i = 0;
        for(Field f : fields) {
            Class<?> providerType = f.getType().equals(Provider.class) ?
                    (Class<?>) ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0] :
                    null;
            injectFields[i++] = new InjectField(
                    f,
                    providerType != null,
                    Key.of(providerType != null ? providerType : f.getType(), qualifier(f.getAnnotations()))
            );
        }
        return injectFields;
    }

    private static Set<Field> fields(Class<?> target) {
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

    private static String chainString(Set<Key> chain, Key lastKey) {
        StringBuilder chainString = new StringBuilder();
        for (Key key : chain) {
            chainString.append(key.toString()).append(" -> ");
        }
        chainString.append(lastKey.toString());
        return chainString.toString();
    }

    private static Constructor constructor(Key key) {
        Constructor inject = null;
        Constructor noarg = null;
        for (Constructor c : key.type.getDeclaredConstructors()) {
            if (c.isAnnotationPresent(Inject.class)) {
                if(inject == null) {
                    inject = c;
                } else {
                    throw new FeatherException(String.format("Dependency %s has more than one @Inject constructor", key.type));
                }
            } else if (c.getParameters().length == 0) {
                noarg = c;
            }
        }
        Constructor constructor = inject != null ? inject : noarg;
        if (constructor != null) {
            constructor.setAccessible(true);
            return constructor;
        } else {
            throw new FeatherException(String.format("Dependency %s doesn't have an @Inject constructor, no-arg constructor or a module provider", key.type.getName()));
        }
    }

    private static Set<Method> providers(Class<?> clazz) {
        Class<?> currentClass = clazz;
        Set<Method> providers = new HashSet<>();
        while (!currentClass.equals(Object.class)) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Provides.class) && !providerInSubClass(method, providers)) {
                    method.setAccessible(true);
                    providers.add(method);
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        return providers;
    }

    private static Annotation qualifier(Annotation[] annotations) {
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
