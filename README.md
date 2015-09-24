####About Feather####
Feather is an ultra-lightweight dependency injecton ([JSR-330](https://jcp.org/en/jsr/detail?id=330 "JSR-330")) library for Java and Android. It's aimed for projects needing the basics done simply rather than a kitchen sink of features.

#####Footprint, performance######
Comparing Feather to [Guice] (https://github.com/google/guice "Guice") - as a reference:
- the library itself weighs less than 3% of Guice
- no external dependencies
- based on a crude benchmark - bootstraps ~3x faster, instantiates dependencies ~40% faster
Note: not to downplay the mighty Guice at all, Guice has a much larger set of features.

#####How it works#####
Feather is based on reflection. In a typical scenario it inspects the constructor of the requested dependency (happens only once) and calls it with the necessary dependencies (a recursion). No classpath scanning, proxying or anything costly involved.

#####Usage - code examples#####
######Create the injector (Feather)######
Typically an application needs a single Feather instance (the JSR-330 Injector).
```java
Feather feather = Feather.with();
```

######Instantiating dependencies######
Dependencies having an @Inject constructor or a default constructor will be handled by Feather without the need of any configuration. Eg:
```java
public class A {
    @Inject
    public A(B b) {
        // ...
    }
}

public class B {
    @Inject
    public B(C c, D d) {
        // ...
    }
}

public class C {}

@Singleton
public class D {
    // something expensive or other reasons
}
```
Note: supports @Singleton on classes
Getting an instance from Feather. Direct use of Feather should typically be used only for bootstrapping an application:
```java
A instance = feather.instance(A.class);
```


######Provide additional dependencies to Feather######
When a dependency doesn't have a suitable constructor (@Inject annotated or noarg), or custom construction, Feather relies on configuration. This is done through @Provides annotated methods in configuration modules:
```java
public class MyModule {
    @Provides
    @Singleton 
    DataSource ds() {
        DataSource dataSource = // instantiate some DataSource
        return dataSource;
    }
    
    // ... other @Provides methods
}
```
Note: Feather supports @Singleton on @Provides annotated methods too
Initializing feather with any number modules:
```java
Feather feather = Feather.with(new MyModule());
```
The provided dependency will be available for injection:
```java
public class MyApp {
    @Inject 
    public MyApp(DataSource ds) {
        // ...
    }
}
```
Feather injects dependencies to @Provides methods aguments
```java
public interface Foo {}
public class FooBar implements Foo {
    @Inject
    public FooBar(X x, Y y, Z z) {
        // ...
    }
}

public class MyModule {
    @Provides
    Foo foo(FooBar fooBar) {
        return fooBar;
    }
}

// injecting the interface type will work:
public class A {
    @Inject
    public A(Foo foo) {
        // ...
    }
}
```
Note that the @Provides method serves just as a declaration, a binding here, no manual instantiation or argument passing needed
######Qualifiers######
Feather supports Qualifiers (Named or custom)
```java
public class MyModule {
    @Provides
    @Named("greeting")
    String greeting() {
        return "hi";
    }
        
    @Provides
    @SomeQualifier
    Foo some(FooSome fooSome) {
        return fooSome;
    };
}
```
Injecting:
```java
public class A {
    @Inject
    public A(@SomeQualifier Foo foo, @Named("greeting") String greet) {
        // ...
    }
}
```
Or instantiating programmaticaly:
```java
String some = feather.instance(String.class, "some");
Foo foo = feather.instance(Key.of(Foo.class, SomeQualifier.class));
```
######Provider injection######
Feather can inject javax.inject.Provider as dependencies
```java
public class A {
    @Inject
    public A(Provider<B> b) {
        // fetch a new instance when needed
        B b = b.get();
    }
}
```
Or programmatically:
```java
Provider<B> bProvider = feather.provider(B.class);
```
######Override modules######
```java
public class Module {
    @Provides
    DataSource dataSource() {
        // return a mysql datasource
    }
    
    // other @Provides methods
}

public class TestModule extends Module {
    @Override
    @Provides
    DataSource dataSource() {
        // return a h2 datasource
    }
}
```
######Field injection######
Feather supports Constructor injection only when it deals with the dependency graph. However it does inject fields when triggered manually. The reason for this is to facilitate @Inject in unit tests:
```java
public class AUnitTest {
    @Inject
    private Foo foo;
    @Inject
    private Bar bar;

    @Before
    public void setUp() {
        Feather feather = // configure a Feather instance
        feather.injectFields(this);
    }
}
```
######Method injection######
Not supported
