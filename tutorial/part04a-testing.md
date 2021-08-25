# Testing

So far we've just entered programs by hand. That's great as it keeps
you from falling asleep reading my poor writing, but we're
professionals, and we know that automated testing really is the way to
go. So in this chapter we'll look at how to write some tests for our
language.

We could create AST nodes directly and execute them, but that's going
to get quite unwieldy and as we grow the AST we'll have to constantly
rewrite the tests. It seems that testing at the interface will be a
lot more practical, and natural interface is the program text. So
program text in, result out.

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

  private String eval(String program) {
    return context.eval("toyl", program);
  }

  @Test
  void testIntegerAddition() {
    assertEquals("4", eval("2+2"));
  }
}
```

And we should be able to run the test. You can try running it in maven
using `mvn test`, or you can try to run it IntelliJ. Either way, both
attempts are going to fail:

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

We should probably verify that decimals works too. Let's try a simple
test for this:

```java
  @Test
  void testDecimal() {
    assertEquals("1.5", eval("3.0/2"));
  }
```

But it fails. Seems we have a bug! Ok, you might have noticed it long
ago and maybe you even fixed it, in which case you've sorta ruined my
pedagogical point, but if you started with the code from part3c you
should get the same error:

> org.opentest4j.AssertionFailedError: 
> Expected :1.5
> Actual   :1

To debug this I put a breakpoint in `ToylProgramNode.execute`. And
looking at the AST we see one slightly surprising thing. The AST looks
like this:

```
ToylDivNodeGen {
  left: ToylLiteralLongNode(3),
  right: ToylLoteralLongNode(2)
}
```

So our `3.0` literal has been interpreted as the long
value 3. Hmmm. Truth be told, we haven't really defined this part of
the language semantics. I think it's reasonable that the expression
`3/2` produces `1.5`. So how do we ensure that? Guess we need to do
something about our division operator implementation.

Here's a straightforward solution, use modulus to verify that we can
continue with integer division:

```java
  @Specialization(rewriteOn = ArithmeticException.class)
  protected long divLongs(long leftValue, long rightValue) {
    var mod = leftValue % rightValue;
    if (mod == 0) {
      return leftValue / rightValue;
    } else {
      throw new ArithmeticException();
    }
  }
```

By simply throwing an `ArithmeticException` if the modulo is != 0 we
will convert to `BigDecimal`. If you rerun the test now it should
succeed. I also added a test that runs through all the operators:

```java
  @Test
  void testAllOps() {
    assertEquals("2.5", eval("(4-3+1)*5/4"));
  }
```

And in the source code for this chapter you'll find a few more tests.

But there's one really simple test that fails somewhat surprisingly
(or maybe you've noticed this hole already):

```java
  @Test
  void testNegativeLiteral() {
    assertEquals("-1", eval("-1"));
  }
```

> line 1:0 extraneous input '-' expecting {'(', LITERAL_NUMBER}
>
> org.opentest4j.AssertionFailedError: 
> Expected :-1
> Actual   :1

Hah. We haven't actually implemented negative literals!
