# Less var, more assign

Currently you have to write `var` to introduce a variable and then
also to reassign it. I think this is a bit odd, and it would make more
sense if you only declare a variable once and then you can assign it
simply using `=`. So let's allow that. We'll start with adapting the
grammar:

```
statement: (expr|varDecl|assignment);

varDecl: 'var' NAME '=' expr;

assignment: NAME '=' expr;
```

So we're really just splitting the existing `assignment` rule into a
separate `varDecl` and `assignment` rule. Now we can run
`generate_parser.sh` and start updating `ToylParseTreeVisitor`. The
first thing we need to do is implement `visitVarDecl`:

```java
  @Override
  public ToylNode visitVarDecl(ToylParser.VarDeclContext ctx) {
    final String name = ctx.NAME().getText();
    if(this.frameDescriptor.findFrameSlot(name) != null) {
      throw new ToylSemanticError("Attempt to re-declare previously declared variable " + name);
    }
    var slot = this.frameDescriptor.addFrameSlot(name);
    return ToylVarDeclNodeGen.create(this.visit(ctx.expr()), name, slot);
  }
```

We'll also add a test for this particular case, and update the
existing tests that do redeclarations on purpose so they don't
fail. First the new test:


```java
  @Test
  void testRedeclaringVariableIsAnError() {
    var program = """
        var a = 1.5
		var a = 1
        """;
    assertThrows(ToylSemanticError.class, () -> eval(program));
  }
```

And you should also update `testReassign` and
`testReassignNumberToLong` to use the new assigment syntax. Once that
is done you can run the test. But it will probably crash like this:

> java.lang.IllegalAccessError: superclass access check failed: class toyl.errors.ToylSemanticError (in unnamed module @0x7637f22) cannot access class com.oracle.truffle.api.exception.AbstractTruffleException (in module org.graalvm.truffle) because module org.graalvm.truffle does not export com.oracle.truffle.api.exception to unnamed module @0x7637f22

Argh! This again! After much time on Google I've yet to find a
solution. In the end I decided that the problem might be the advice I
gave earlier about running on the GraalVM which means we have to
reconfigure the dependency order in IntelliJ to load from the jar
files first because the Truffle classes are both part of the packages
that ship with the GraalVM, and they are externally imported from the
dependencies we've declared in Maven.

Let's change this so that our "language" module uses a standared JVM
16, I recommend using OpenJDK. This will make Truffle print an
exception when we execute the tests because we're no longer executing
on GraalVM which means our programs will be executed in full
interpreter mode. That should not matter for running our tests though,
and we can leave the "launcher" module using GraalVM as before. To
suppress this error message, add the following to the JUnit Runner
template

```
-Dpolyglot.engine.WarnInterpreterOnly=false
```

With that done we can run the test again. And see it fail again:

> org.opentest4j.AssertionFailedError: Unexpected exception type thrown ==> expected: <toyl.errors.ToylSemanticError> but was: <org.graalvm.polyglot.PolyglotException>

When exceptions occur they are packaged up in a PolyglotException. For
now we'll simply assert on the error message instead. Change the
existing assertThrows line to this:

```java
    var error = assertThrows(PolyglotException.class, () -> eval(program));
    assertTrue(error.getMessage().startsWith("Attempt to redeclare previously declared variable"));
```

All our tests should now be green. Good stuff. But we can't really
test this redeclaration in our launcher because it only accepts a
single input line, so variables are kinda useless. In part 6 we'll fix
that and also see how we can add a bit of context to the error so that
we can tell where it happens.
