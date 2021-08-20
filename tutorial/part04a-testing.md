# Testing

So far we've just entered programs by hand. That's great as it keeps
you from falling asleep reading my poor writing, but we're
professionals, and we know that automated testing really is the way to
go. So in this chapter we'll look at how to write some tests for our
language.

We could create AST nodes directly and execute them, but that's going
to get quite unwieldy and as we grow the AST we'll have to constantly
rewrite the tests. It seems that testing at the interface will be a
lot more practical, i.e. program text in, result out. So let's try
that approach.

For our testing we'll use JUnit 5, so let's start by updating our
Maven deps. We're going to need the JUnit 5 dependencies, and we also
need to update the surefire plugin as the one that comes default with
Maven is ancient.

First, in our root pom, add two `properties`:

```xml
    <junit.jupiter.version>5.7.2</junit.jupiter.version>
    <maven.surefire.version>3.0.0-M5</maven.surefire.version>
```

And then in `pom.xml` we'll import the junit dependency BOM:

```xml
      <dependency>
        <groupId>org.junit</groupId>
        <artifactId>junit-bom</artifactId>
        <version>${junit.jupiter.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
```

And we also need to add the correct surefire version to our
plugins. We haven't needed a build section in our base pom until now,
so we need to add the entire thing:

```xml
  <build>
    <pluginManagement>
      <plugins>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-surefire-plugin</artifactId>
          <version>${maven.surefire.version}</version>
        </plugin>
      </plugins>
    </pluginManagement>
  </build>
```

Now, all that remains is adding JUnit to our `language` module pom, so
in `language/pom.xml`:

```xml
    <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter</artifactId>
      <scope>test</scope>
    </dependency>>
```	

Now, we're ready to write our first test. Let's create `language/src/test/java/toyl/ToylLanguageTest.java`:

```java
package toyl;

import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ToylLanguageTest {

  Context context;

  @BeforeEach
  void setUp() {
    context = Context.create();
  }

  @AfterEach
  void tearDown() {
    context.close();
  }

  private Object eval(String program) {
    return context.eval("toyl", program);
  }

  @Test
  void testIntegerAddition() {
    assertEquals(2 + 2, eval("2+2"));
  }
}
```

And we should be able to run the test. You can try running it in maven
using `mvn test`, or you can try to run it IntelliJ which is a lot
more convenient and what I'd recommend that you do while
developing. Either way, both attempts are going to fail:

```
java.lang.IllegalArgumentException: A language with id 'toyl' is not installed. Installed languages are: [js, llvm].
	at toyl.ToylLanguageTest.eval(ToylLanguageTest.java:25)
	at toyl.ToylLanguageTest.testIntegerAddition(ToylLanguageTest.java:30)
```

You may recognize that error. We ran into it early in part 1. The
problem is that we're missing the `-Dtruffle.class.path.append` option
to our test runner. We'll fix this in Maven first, and then we'll look
at how to fix it in IntelliJ because it's going to be a lot simpler to
run the tests from IntelliJ.

Here's what we need to add to `language/pom.xml` under the
`build/plugins` tag:

```xml
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <configuration>
          <argLine>-Dtruffle.class.path.append=${project.basedir}/target/classes</argLine>
        </configuration>
      </plugin>
```

This uses the `${project.basedir}` maven variable to point at where
the Toyl language classes are. It's the same thing we need to do in
our IntelliJ setup. But while our Launcher could be set up as a single
run configuration IntelliJ makes it really easy to quickly run a
particular test instead of always running the entire test suite, and
that's pretty useful for development. So we need to configure IntelliJ
so that no matter how you run the tests in the `language` module it
will add the truffle class path. To do this, open click on the run
dropdown and click "Edit Configurations", or optionally go via the
menu Run -> Edit Configurations. In the window that appears there will
be a "Edit configuration templates" towards the bottom left of the
window. Click this and pick JUnit. Now all you need to do is add the
correct truffle class path option. In this case the Maven variable
won't work, you need to use an IntelliJ variable, so add this:

```
-Dtruffle.class.path.append=$ProjectFileDir$/language/target/classes
```

Now you can run tests by clicking Control-Shift-R (or debug tests with
Control-Shift-D). Great!

And if you run the test... it fails. You should get something like:

> org.opentest4j.AssertionFailedError: expected: java.lang.Integer@30124529<4> but was: org.graalvm.polyglot.Value@68df9280<4>

Ok that's probably understandable. The return value from
`context.eval` returns a `Value` instance which obviously won't be
equal to the integer we expected. So we can fix this by extracting the
underlying value using the right `as*` method on `Value`. I decided to
add two eval overloads to my test class like this:

```java
  private int evalInt(String program) {
    return context.eval("toyl", program).asInt();
  }

  private double evalDouble(String program) {
    return context.eval("toyl", program).asDouble();
  }
```

And then the tests end up like this:

```java
  @Test
  void testIntegerAddition() {
    assertEquals(2 + 2, evalInt("2+2"));
  }

  @Test
  void testDoubleAddition() {
    assertEquals(2.0 + 2.0, evalDouble("2.0+2.0"));
  }

  @Test
  void testMixedAddition() {
    assertEquals(2 + 2.0, evalDouble("2+2.0"));
  }

  @Test
  void testIntegerOverflow() {
    assertEquals(Integer.MAX_VALUE + 1L, evalDouble("2147483647 + 1"));
  }
```

That's all I'm going to say about testing for now. Having automated
tests is really useful for quickly testing little program snippets so
we don't have to use the launcher and enter the program by hand each
time, and of course for protecting us from unexpected regressions as
we start adding more complexity to our language.
