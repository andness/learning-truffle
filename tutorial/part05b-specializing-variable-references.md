## Specializing our variable references

As we implemented `ToylVarRefNode` you may have wondered if we
shouldn't use the Truffle specialization tools. I decided to skip it
initially to keep the class as simple as possible. But let's try to
adapt our code to use it. 

So the first thing we need to do is make our class abstract since the
Truffle code generation is going to add the `execute` methods for
us. And then we need to add the `@Specialization` magic. But how do we
declare the specializations? In this case there is no arithmetic
overflow to trigger the change. Instead, we know the type of the
variable from the frame (using for example `frame.isLong(slot)`. To
make use of this fact we need to use the `guards` parameter for
`@Specialization`.

So we need to do three things:
1. Make `ToylVarRefNode` be `abstract`
2. Add `@Specialization` to replace `ToylVarRefNode.executeLong` and
   `ToylVarRefNode.executeBigDecimal`
3. Update `ToylParseTreeVisitor` to use `ToylVarRefNodeGen.create`
   instead of `new ToylVarRefNode()`
   
Declaring the class abstract is simple, just do that. Next, add this
`@Specialization` to `executeLong`:

```java
@Specialization(guards = "frame.isLong(slot)")
```

If you try to build you will get an error:

> java: Error parsing expression 'frame.isLong(slot)': slot is not visible.

Why? Well, since `slot` is `private` it won't be visible in the
derived class that Truffle generates. So we need to change it to be
`protected`. Now you can update `ToylParseTreeVisitor` to use the
newly generated `ToylVarRefNodeGen.create`.

Now, I intentionally did not rename `executeLong` to allow us to
explore how the code generation works a bit more. If you look at the
generated class it doesn't contain an `executeLong` method. Try
renaming it to `readInt` and rebuild to see how that affects the
`ToylVarRefNodeGen` class. You should see it generate an `executeLong`
method, and a lot more! 

Now, to explore how this works we can add a test that does addition
with only integer variables for now:

```java
  @Test
  void testIntegerVariables() {
    var program = """
        var a = 2
        var b = 2
        a + b
        """;
    assertEquals("4", eval(program));
  }
```

And if you try to run that test it will crash:

> org.graalvm.polyglot.PolyglotException: com.oracle.truffle.api.dsl.UnsupportedSpecializationException: Unexpected values provided for ToylVarRefNodeGen@106cc338: [], []

It's a good exercise to try to step through the code to see where it
breaks. You can simply look at the stacktrace generated in the error
above and add a breakpoint to the where it crashes. If you do this and
run the test you will find that the guard test fails. If you inspect
the value of `slot` in the debugger you will see that it has kind
`Illegal`. So it appears we haven't stored the right type. And as you
might remember we were wondering about that call to `setObject` in
`ToylAssignmentNode`. As a temporary fix we can try to just fix that
code to use `setLong` instead, so replace
`ToylAssignmentNode.executeGeneric` with:

```java
  @Override
  public Object executeGeneric(VirtualFrame frame) {
    var value = this.expr.executeGeneric(frame);
    frame.setLong(this.slot, (Long) value);
    return value;
  }
```

But now if you try to run the tests you should see the `testVariables`
test fail, which is not surprising as it uses a decimal number and we
just changed our code to only support integers. So we get a class cast
exception trying to set our decimal value variable:

> org.graalvm.polyglot.PolyglotException: java.lang.ClassCastException: class java.lang.Double cannot be cast to class java.lang.Integer (java.lang.Double and java.lang.Integer are in module java.base of loader 'bootstrap')


So we neeed to fix that. How do we ensure that we call the right
`frame.set(Long|BigDecimal)` method? Well, maybe we can try with
`instanceof` checks:

```java
  @Override
  public Object executeGeneric(VirtualFrame frame) {
    var value = this.expr.executeGeneric(frame);
    if (value instanceof BigDecimal) {
      frame.setObject(this.slot, value);
    } else {
      frame.setLong(this.slot, (Long) value);
    }
    return value;
  }
```

But it still fails. The error has changed though:

> org.graalvm.polyglot.PolyglotException: com.oracle.truffle.api.dsl.UnsupportedSpecializationException: Unexpected values provided for ToylVarRefNodeGen@1603cd68: [], []

This looks like the error we started with in the integer only case. If
you look at the stack trace we're crashing in:

> 	at toyl.ast.ToylVarRefNodeGen.executeAndSpecialize(ToylVarRefNodeGen.java:57)

So it appears Truffle no longer knows how to handle a decimal in var
ref, which is perhaps not surprising as we've left `executeNumber`
untouched. Let's update that to use `@Specialization` too:

```java
  @Specialization(guards = "frame.isObject(slot)")
  public BigDecimal readBigDecimal(VirtualFrame frame) {
    try {
      return (BigDecimal) frame.getObject(this.slot);
    } catch (FrameSlotTypeException e) {
      throw new IllegalStateException(e);
    }
  }	
```

If you run the tests now they should be green! Fantastic. You can also
delete the `executeGeneric` method from `ToylVarRefNode` as that
should now be generated by Truffle and is thus overriden in the
generated class.

And it runs! Yay! But you may be less satisfied with our `instanceof`
checks in `ToylAssignmentNode`. Can we improve on that? 
