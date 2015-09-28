####About Feather####
Feather is an ultra-lightweight dependency injection ([JSR-330](https://jcp.org/en/jsr/detail?id=330 "JSR-330")) library for Java and Android. It's main goal is to deliver easy-to-use basic dependency injection functionality with high performance and - taken to the extreme - small footprint.

```xml
<dependency>
    <groupId>org.codejargon.feather</groupId>
    <artifactId>feather</artifactId>
    <version>0.6</version>
</dependency>
```

#####Footprint, performance######
Comparing Feather to Google [Guice] (https://github.com/google/guice "Guice") - as a reference:
- the library weighs less than 3% of Guice
- no external dependencies
- based on a crude benchmark: bootstraps ~10 times faster, instantiates dependencies ~2 times faster. check out the performance-test module for details

Note: not to downplay the mighty Guice at all, Guice has a much wider array of features.

#####How it works#####
Feather is based on reflection to inject dependencies. No code generating, classpath scanning, proxying or anything costly involved.

#####Usage - code examples#####
######Create the injector (Feather)######
```java
Feather feather = Feather.with();
```
Typically an application needs a single Feather instance (the JSR-330 Injector).

######Instantiating dependencies######
Dependencies having an @Inject constructor or a default constructor will be delivered by Feather without the need for any configuration. Eg:
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
    // something expensive or other reasons for being singleton
}
```
Note: supports @Singleton on classes  

Getting an instance of A from Feather.
```java
A instance = feather.instance(A.class);
```
Note: direct use of Feather should typically be used only for bootstrapping an application

######Provide additional dependencies to Feather######
When a dependency doesn't have a suitable (@Inject annotated or noarg) constructor , needs custom construction, Feather relies on configuration. This is done by configuration modules:
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

Bootstrapping feather with module(s):
```java
Feather feather = Feather.with(new MyModule());
```
The provided dependency will be available for injection:
```java
public class MyApp {
    @Inject 
    public MyApp(DataSource ds) {
        // return a DataSource instance
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

// injecting an instance of Foo interface will work using the MyModule above:
public class A {
    @Inject
    public A(Foo foo) {
        // ...
    }
}
```
Note that the @Provides method serves just as a binding declaration here, no manual instantiation or argument passing needed
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
Feather can inject javax.inject.Provider as dependencies to facilitate lazy loading or allow circular dependencies:
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
Feather supports Constructor injection only when it assembles the dependency graph. However injecting fields when triggered on a target object non-transitively - to facilitate testing (eg. junit tests)
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
Not supported. The need for it can be generally avoided by a Provider / solid design (favoring immutability, injection via constructor).
