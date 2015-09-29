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
                throw new FeatherException(String.format("Class %s provided as module instead of an instance.", ((Class) module).getName()));
            }
            for (Method providerMethod : Inspection.providers(module.getClass())) {
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
        for (Field f : Inspection.injectFields(target.getClass())) {
            Class<?> providerType = f.getType().equals(Provider.class) ?
                    (Class<?>) ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0] :
                    null;
            Key key = Key.of(providerType != null ? providerType : f.getType(), Inspection.qualifier(f.getAnnotations()));
            try {
                f.set(target, providerType != null ? provider(key) : instance(key));
            } catch (IllegalAccessException e) {
                throw new FeatherException(String.format("Can't inject field %s to an instance of %s", f.getName(), target.getClass().getName()));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Provider<T> provider(final Key<T> key, Set<Key> depChain) {
        if (!providers.containsKey(key)) {
            synchronized (providers) {
                if (!providers.containsKey(key)) {
                    final Constructor constructor = Inspection.constructor(key);
                    final Provider<?>[] paramProviders = providersForParams(key, constructor.getParameterTypes(), constructor.getGenericParameterTypes(), constructor.getParameterAnnotations(), depChain);
                    providers.put(key, singletonProvider(key, key.type.getAnnotation(Singleton.class), new Provider() {
                                @Override
                                public Object get() {
                                    try {
                                        return constructor.newInstance(arguments(paramProviders));
                                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                                        throw new FeatherException(String.format("Failed to instantiate dependency %s", key.toString()), e);
                                    }
                                }
                            })
                    );
                }
            }
        }
        return (Provider<T>) providers.get(key);
    }

    private Object[] arguments(Provider<?>[] paramProviders) {
        Object[] args = new Object[paramProviders.length];
        for (int i = 0; i < paramProviders.length; ++i) {
            args[i] = paramProviders[i].get();
        }
        return args;
    }

    private void circularCheck(Key key, Set<Key> depChain) {
        if (depChain != null && depChain.contains(key)) {
            throw new FeatherException(String.format("Circular dependency: %s", chainString(depChain, key)));
        }
    }

    private void providerMethod(final Object module, final Method m) {
        final Key key = Key.of(m.getReturnType(), Inspection.qualifier(m.getAnnotations()));
        if (providers.containsKey(key)) {
            throw new FeatherException(String.format("Multiple providers for dependency %s defined in module %s", key.toString(), module.getClass()));
        }
        Singleton singleton = m.getAnnotation(Singleton.class) != null ? m.getAnnotation(Singleton.class) : m.getReturnType().getAnnotation(Singleton.class);
        final Provider<?>[] paramProviders = providersForParams(key, m.getParameterTypes(), m.getGenericParameterTypes(), m.getParameterAnnotations(), Collections.singleton(key));
        providers.put(key, singletonProvider(key, singleton, new Provider() {
                            @Override
                            public Object get() {
                                try {
                                    return m.invoke(module, arguments(paramProviders));
                                } catch (IllegalAccessException | InvocationTargetException e) {
                                    throw new FeatherException(String.format("Failed to instantiate %s with provider", key.toString()), e);
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

    private Provider<?>[] providersForParams(final Key key, Class<?>[] parameterClasses, Type[] parameterTypes, Annotation[][] parameterAnnotations, final Set<Key> depChain) {
        if (!paramProviders.containsKey(key)) {
            synchronized (paramProviders) {
                if (!paramProviders.containsKey(key)) {
                    Provider<?>[] providers = new Provider<?>[parameterTypes.length];
                    for(int i = 0; i < parameterTypes.length; ++i) {
                        Type parameterType = parameterTypes[i];
                        Class<?> parameterClass = parameterClasses[i];
                        Annotation qualifier = Inspection.qualifier(parameterAnnotations[i]);
                        Class<?> providerType = Provider.class.equals(parameterClass) ?
                                (Class<?>)((ParameterizedType) parameterType).getActualTypeArguments()[0] :
                                null;
                        if(providerType == null) {
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
            }
        }
        return paramProviders.get(key);
    }

    private Set<Key> append(Set<Key> set, Key newItem) {
        if(set != null && !set.isEmpty()) {
            Set<Key> appended = new LinkedHashSet<>(set);
            appended.add(newItem);
            return appended;
        } else {
            return Collections.singleton(newItem);
        }
    }

    static String chainString(Set<Key> chain, Key lastKey) {
        List<Key> keys = new ArrayList<>(chain);
        keys.add(lastKey);
        StringBuilder stringBuilder = new StringBuilder();
        boolean first = true;
        for (Key key : keys) {
            if (first) {
                stringBuilder.append(key.toString());
                first = false;
            } else {
                stringBuilder.append(" -> ").append(key.toString());
            }
        }
        return stringBuilder.toString();
    }
}
