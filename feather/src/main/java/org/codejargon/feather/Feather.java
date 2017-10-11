package org.codejargon.feather;

import javax.inject.Provider;
import javax.inject.Singleton;
import javax.inject.Inject;
import javax.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 * @author edwin.njeru
 *
 */
public class Feather {
    private final Map<Key, Provider<?>> providers = new ConcurrentHashMap<>();
    private final Map<Key, Object> singletons = new ConcurrentHashMap<>();
    //private final Map<Class, Object[][]> injectFields = new ConcurrentHashMap<>(0);

    /**
     * Constructs Feather with configuration modules
     * 
     * @param varargs of modules classes
     * 
     * @return an instance of Feather
     */
    public static Feather with(Object... modules) {
        return new Feather(Arrays.asList(modules));
    }

    /**
     * Constructs Feather with configuration modules
     * 
     * @param An {@code Iterable<?>} collection of configuration modules
     * @return
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
     * @return an instance of type
     */
    public <T> T instance(Class<T> type) {
        return provider(Key.of(type), null).get();
    }

    /**
     * @return instance specified by key (type and qualifier)
     */
    public <T> T instance(Key<T> key) {
        return provider(key, null).get();
    }

    /**
     * @return provider of type
     */
    public <T> Provider<T> provider(Class<T> type) {
        return provider(Key.of(type), null);
    }

    /**
     * @return provider of key (type, qualifier)
     */
    public <T> Provider<T> provider(Key<T> key) {
        return provider(key, null);
    }

    /**
     * Injects fields to the target object
     * 
     * @param target Object
     */
    public void injectFields(Object target) {
        /*if (!injectFields.containsKey(target.getClass())) {
            injectFields.put(target.getClass(), injectFields(target.getClass()));
        }*/
        //for (Object[] f: injectFields.get(target.getClass())) {
        for (Object[] f: injectFields(target.getClass())) {
            Field field = (Field) f[0];
            Key key = (Key) f[2];
            try {
                field.set(target, (boolean) f[1] ? provider(key) : instance(key));
            } catch (Exception e) {
                throw new FeatherException(String.format("Can't inject field %s in %s", field.getName(), target.getClass().getName()));
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <T> Provider<T> provider(final Key<T> key, Set<Key> chain) {
        if (!providers.containsKey(key)) {
            @SuppressWarnings("rawtypes")
			final Constructor constructor = constructor(key);
            final Provider<?>[] paramProviders = paramProviders(key, constructor.getParameterTypes(), constructor.getGenericParameterTypes(), constructor.getParameterAnnotations(), chain);
            providers.put(key, singletonProvider(key, key.type.getAnnotation(Singleton.class), new Provider() {
                        @Override
                        public Object get() {
                            try {
                                return constructor.newInstance(params(paramProviders));
                            } catch (Exception e) {
                                throw new FeatherException(String.format("Can't instantiate %s", key.toString()), e);
                            }
                        }
                    })
            );
        }
        return (Provider<T>) providers.get(key);
    }

    @SuppressWarnings("unchecked")
	private void providerMethod(final Object module, final Method m) {
        final Key key = Key.of(m.getReturnType(), qualifier(m.getAnnotations()));
        if (providers.containsKey(key)) {
            throw new FeatherException(String.format("%s has multiple providers, module %s", key.toString(), module.getClass()));
        }
        Singleton singleton = m.getAnnotation(Singleton.class) != null ? m.getAnnotation(Singleton.class) : m.getReturnType().getAnnotation(Singleton.class);
        final Provider<?>[] paramProviders = paramProviders(
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
                                    return m.invoke(module, params(paramProviders));
                                } catch (Exception e) {
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

    private Provider<?>[] paramProviders(
            final Key key,
            Class<?>[] parameterClasses,
            Type[] parameterTypes,
            Annotation[][] annotations,
            final Set<Key> chain
    ) {
        Provider<?>[] providers = new Provider<?>[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; ++i) {
            Class<?> parameterClass = parameterClasses[i];
            Annotation qualifier = qualifier(annotations[i]);
            Class<?> providerType = Provider.class.equals(parameterClass) ?
                    (Class<?>) ((ParameterizedType) parameterTypes[i]).getActualTypeArguments()[0] :
                    null;
            if (providerType == null) {
                final Key newKey = Key.of(parameterClass, qualifier);
                final Set<Key> newChain = append(chain, key);
                if (newChain.contains(newKey)) {
                    throw new FeatherException(String.format("Circular dependency: %s", chain(newChain, newKey)));
                }
                providers[i] = new Provider() {
                    @Override
                    public Object get() {
                        return provider(newKey, newChain).get();
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
        return providers;
    }

    /**
     * @return an array of  fully-constructed and injected instances fetched from an
     * array of @param paramProviders, which are then used as parameters
     */
    private static Object[] params(Provider<?>[] paramProviders) {
        Object[] params = new Object[paramProviders.length];
        for (int i = 0; i < paramProviders.length; ++i) {
            params[i] = paramProviders[i].get();
        }
        return params;
    }

    /**
     * Appends a @{code Key} @param newKey to an existing {@code Set} @param set of
     * keys, and returns a {@code Set} of key containing the new {@code Key}.
     * If the {@code Set} of {@code Key} is null, a serializable immutable set containing only 
     * the @param newKey is returned 
     *  
     * @return Set<Key> append {@code Key} {@code Set}
     */
    @SuppressWarnings("rawtypes")
	private static Set<Key> append(Set<Key> set, Key newKey) {
        if (set != null && !set.isEmpty()) {
            Set<Key> appended = new LinkedHashSet<>(set);
            appended.add(newKey);
            return appended;
        } else {
            return Collections.singleton(newKey);
        }
    }

    /**
     * Creates a map represented as a two-dimensional array of {@code Inject} annotated
     * fields with their corresponding {@code Provider} types, or Parameterised types or
     * type arguments.
     * 
     * @param Class<?> target
     * @return two-dimensional Object[][] array
     */
    private static Object[][] injectFields(Class<?> target) {
        Set<Field> fields = fields(target);
        Object[][] fs = new Object[fields.size()][];
        int i = 0;
        for (Field f : fields) {
            Class<?> providerType = f.getType().equals(Provider.class) ?
                    (Class<?>) ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0] :
                    null;
            fs[i++] = new Object[]{
                    f,
                    providerType != null,
                    Key.of(providerType != null ? providerType : f.getType(), qualifier(f.getAnnotations()))
            };
        }
        return fs;
    }

    /**
     * Returns accessible set of fields ({@code Field}) in a given class type
     * which are annotated with the {@code Inject} {@code Annotation}
     * 
     * @param Class<?> type
     * @return Set<Field> fields
     */
    private static Set<Field> fields(Class<?> type) {
        Class<?> current = type;
        Set<Field> fields = new HashSet<>();
        while (!current.equals(Object.class)) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isAnnotationPresent(Inject.class)) {
                    field.setAccessible(true);
                    fields.add(field);
                }
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    /**
     * Returns a String representing a concatenation of Keys from a {@code Set}
     * of {@code Key} and appends the last {@code Key}
     * 
     * @param Set<Key> chain
     * @param {@code Key} lastKey
     * @return "->" concatenated string of {@code Key} names
     */
    @SuppressWarnings("rawtypes")
	private static String chain(Set<Key> chain, Key lastKey) {
        StringBuilder chainString = new StringBuilder();
        for (Key key : chain) {
            chainString.append(key.toString()).append(" -> ");
        }
        return chainString.append(lastKey.toString()).toString();
    }

    /**
     * Fetches a constructor which is annotated with {@code Inject} from a {@code Key}. 
     * If such a constructor does not exist, the noargs constructor is used instead.
     * If both do not exist and a module provider does not also exist then a 
     * {@code FeatherException} is thrown
     * 
     * @param {@code Key}
     * @return {@code Constructor}
     */
    @SuppressWarnings("rawtypes")
	private static Constructor constructor(Key key) {
        Constructor inject = null;
        Constructor noarg = null;
        for (Constructor c : key.type.getDeclaredConstructors()) {
            if (c.isAnnotationPresent(Inject.class)) {
                if (inject == null) {
                    inject = c;
                } else {
                    throw new FeatherException(String.format("%s has multiple @Inject constructors", key.type));
                }
            } else if (c.getParameterTypes().length == 0) {
                noarg = c;
            }
        }
        Constructor constructor = inject != null ? inject : noarg;
        if (constructor != null) {
            constructor.setAccessible(true);
            return constructor;
        } else {
            throw new FeatherException(String.format("%s doesn't have an @Inject or no-arg constructor, or a module provider", key.type.getName()));
        }
    }

    /**
     * Creates a hashSet of methods from a given type which are annotated with the
     * {@code Provides} annotation
     * 
     * @param Class<?> type
     * @return Set<Method> containing {@code Provides} annotation
     */
    private static Set<Method> providers(Class<?> type) {
        Class<?> current = type;
        Set<Method> providers = new HashSet<>();
        while (!current.equals(Object.class)) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Provides.class) && (type.equals(current) || !providerInSubClass(method, providers))) {
                    method.setAccessible(true);
                    providers.add(method);
                }
            }
            current = current.getSuperclass();
        }
        return providers;
    }

    /**
     * Fetches a {@code Qualifier} {@code Annotation} from an array of annotations.
     * If a {@code Qualifier} {@code Annotation} is not found in the array, a null pointer
     * is returned
     * 
     * @param annotations
     * @return {@code Qualifier} {@code Annotation}
     */
    private static Annotation qualifier(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().isAnnotationPresent(Qualifier.class)) {
                return annotation;
            }
        }
        return null;
    }

    /**
     * Checks is a Provider exists in a subclass of the current class we are currently
     * looping through
     * 
     * @param {@code Method}
     * @param Set<Method> of discoveredMethods
     * @return boolean whether or not  a provider exists in a subclass
     */
    private static boolean providerInSubClass(Method method, Set<Method> discoveredMethods) {
        for (Method discovered : discoveredMethods) {
            if (discovered.getName().equals(method.getName()) && Arrays.equals(method.getParameterTypes(), discovered.getParameterTypes())) {
                return true;
            }
        }
        return false;
    }
}
