package org.codejargon.feather;

import javax.inject.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Feather {
    private final Map<Key, Provider<?>> providers = new ConcurrentHashMap<>();
    private final Map<Key, Object> singletons = new ConcurrentHashMap<>();
    private final Map<Key, List<Provider<?>>> paramProviders = new ConcurrentHashMap<>();

    public static Feather with(Object... modules) {
        return new Feather(Arrays.asList(modules));
    }

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
     * Returns an instance of the requested type
     */
    public <T> T instance(Class<T> type) {
        return provider(Key.of(type), null).get();
    }

    public <T> T instance(Key<T> key) {
        return provider(key, null).get();
    }

    public <T> Provider<T> provider(Class<T> type) {
        return provider(Key.of(type), null);
    }


    public <T> Provider<T> provider(Key<T> key) {
        return provider(key, null);
    }

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
                    circularCheck(key, depChain);
                    Set<Key> dependencyChain = depChain != null ? depChain : new LinkedHashSet<Key>();
                    final Constructor constructor = Inspection.constructor(key);
                    final List<Provider<?>> paramProviders = providersForParams(key, constructor.getParameters(), dependencyChain);
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
        circularCheck(key, depChain);
        return (Provider<T>) providers.get(key);
    }

    private Object[] arguments(List<Provider<?>> paramProviders) {
        Object[] args = new Object[paramProviders.size()];
        for (int i = 0; i < paramProviders.size(); ++i) {
            args[i] = paramProviders.get(i).get();
        }
        return args;
    }

    private void circularCheck(Key key, Set<Key> depChain) {
        if (depChain != null && depChain.contains(key)) {
            throw new FeatherException(String.format("Circular dependency: %s", Key.chainString(depChain, key)));
        }
    }

    private void providerMethod(final Object module, final Method m) {
        final Key key = Key.of(m.getReturnType(), Inspection.qualifier(m.getAnnotations()));
        if (providers.containsKey(key)) {
            throw new FeatherException(String.format("Multiple providers for dependency %s defined in module %s", key.toString(), module.getClass()));
        }
        Singleton singleton = m.getAnnotation(Singleton.class) != null ? m.getAnnotation(Singleton.class) : m.getReturnType().getAnnotation(Singleton.class);
        final List<Provider<?>> paramProviders = providersForParams(key, m.getParameters(), Collections.singleton(key));
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

    private List<Provider<?>> providersForParams(final Key key, Parameter[] parameters, final Set<Key> dependencyChain) {
        if (!paramProviders.containsKey(key)) {
            synchronized (paramProviders) {
                if (!paramProviders.containsKey(key)) {
                    List<Provider<?>> providers = new ArrayList<>(parameters.length);
                    for (final Parameter p : parameters) {
                        final Annotation qualifier = Inspection.qualifier(p.getAnnotations());
                        final Class<?> providerType = p.getType().equals(Provider.class) ?
                                (Class<?>) ((ParameterizedType) p.getParameterizedType()).getActualTypeArguments()[0] :
                                null;
                        providers.add(providerType != null ?
                                        new Provider() {
                                            @Override
                                            public Object get() {
                                                return provider(Key.of(providerType, qualifier), null);
                                            }
                                        } :
                                        new Provider() {
                                            @Override
                                            public Object get() {
                                                return provider(Key.of(p.getType(), qualifier), append(dependencyChain, key)).get();
                                            }
                                        }
                        );
                    }
                    paramProviders.put(key, providers);
                }
            }
        }
        return paramProviders.get(key);
    }

    private <T> Set<T> append(Set<T> set, T newItem) {
        LinkedHashSet<T> appended = new LinkedHashSet<>(set);
        appended.add(newItem);
        return appended;
    }
}
