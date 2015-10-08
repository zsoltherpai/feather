####About Feather####
Feather is an ultra-lightweight dependency injection ([JSR-330](https://jcp.org/en/jsr/detail?id=330 "JSR-330"))
library for Java and Android. It delivers easy-to-use dependency injection functionality with high performance 
and - taken to the extreme - small footprint.

```xml
<dependency>
    <groupId>org.codejargon.feather</groupId>
    <artifactId>feather</artifactId>
    <version>0.8</version>
</dependency>
```
[Javadoc](http://zsoltherpai.github.io/feather/apidocs-0.8 "Javadoc") for Feather

#####Footprint, performance######
- compared to [Guice] (https://github.com/google/guice "Guice"): 1/40 the library size, ~10x startup speed
- compared to [Dagger](http://square.github.io/dagger): 1/4 the library size, ~2x startup speed

Note: startup means initializing the container / instantiating an object graph. The executable benchmark is published 
in 'performance-test' module.

#####Usage - code examples#####
######Create Feather (the injector)######
```java
Feather feather = Feather.with();
```
Typically an application needs a single Feather instance.

######Instantiating dependencies######
Dependencies having an @Inject constructor or a default constructor will be injected by Feather without the need for
any configuration. Eg:
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
A a = feather.instance(A.class);
```
Direct use of Feather should typically happen when bootstrapping an application.

######Provide additional dependencies to Feather######
When injecting an interface, a 3rd party class or an object needing configuration, Feather relies on configuration modules:
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

Setting up Feather with module(s):
```java
Feather feather = Feather.with(new MyModule());
```
The configured dependency will be available for injection:
```java
public class MyApp {
    @Inject 
    public MyApp(DataSource ds) {
        // return a DataSource instance
    }
}
```
Feather injects dependencies to @Provides methods aguments. This is particularly interesting for binding an implementation
to an interface:
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
Note that the @Provides method serves just as a binding declaration here, no manual instantiation needed
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
String greet = feather.instance(String.class, "greeting");
Foo foo = feather.instance(Key.of(Foo.class, SomeQualifier.class));
```
######Provider injection######
Feather can inject javax.inject.Provider as dependencies to facilitate lazy loading or circular dependencies:
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
Feather supports Constructor injection only when it injects/assembles the dependencies. It also does inject field when 
explicitly triggered on a target object - eg to facilitate testing. A simplified example with a junit test:
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

#####Android considerations#####
For best possible performance, try to make most dependencies immutable, as @Singleton.
```java
class ExampleActivity extends Activity {
    @Inject
    private Foo foo;
    @Inject
    private Bar bar;

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.simple_activity);
    Feather feather = // obtain a reference to feather
    feather.injectFields(this);
  }
}
```
#####How it works under the hood#####
Feather is based on optimized use of reflection to provide dependencies. No code generating, classpath scanning, proxying or anything
costly involved. A simple example with some explanation:
```java
class A {
    @Inject
    A(B b) {

    }
}

class B {

}
```
Without the use of Feather, class A could be instantiated with the following factory methods:
```java
A a() {
    return new A(b());
}

B b() {
    return new B();
}
```
Feather does something very similar, but avoids the need for writing such factory code. When an instance of A is requested,
Feather calls it's constructor with the necessary arguments - an instance of B in this case. The instance of B is created
the same way - a simple recursion.
Note: Most of the work is done only once per dependency type. There are slight alterations when @Provides, @Singleton,
and @Qualifier are involved.
