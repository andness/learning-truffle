## Specializing our variable references

As we implemented `ToylVarRefNode` you may have wondered if we
shouldn't use the Truffle specialization tools. I decided to skip it
initially to keep the class as simple as possible. But let's try to
adapt our code to use it. My first coverage of specialiation was a bit
superficial and more in the "just accept this code for now"
style. This time though I'd like to step through a few mistakes to try
to shed a bit more light on what actually goes on.

So the first thing we need to do is make our class abstract since the
Truffle code generation is going to add the `execute` methods. And
then we need to add the `@Specialization` magic. But how do we declare
the specializations? In this case there is no arithmetic overflow to
trigger the change. Instead, we know the type of the variable from the
frame (using for example `frame.isInt(slot)`. To make use of this fact
we need to use the `guards` parameter for `@Specialization`. 

So we need to do three things:
1. Make `ToylVarRefNode` be `abstract`
2. Add `@Specialization` to `ToylVarRefNode.executeInt`
3. Change the creation of `ToylVarRefNode` to use `ToylVarRefNodeGen.create` instead

The specialization to add in step 2 is this:

```java
@Specialization(guards = "frame.isInt(slot)")
```

If you try to build you will get an error:

> java: Error parsing expression 'frame.isInt(slot)': slot is not visible.

Ignore that for now, the build shold still produce a
`ToylVarRefNodeGen` class so that you can complete step 3.

Ok got that? Now, what about that error? You might have figured it out
already, our slot is `private` so the `ToylVarRefNodeGen` class won't
be able to access it. Let's make it `protected` instead. 


Now, I intentionally did not rename `executeInt`. If you look at the
`ToylAddNode` we renamed the methods from `executeInt` to
`addInt`. And if you look at the generated class it doesn't contain an
`executeInt` method. Try renaming it to `readInt` and rebuild to see
how that affects the `ToylVarRefNodeGen` class. You should see it
generate an `executeInt` method.

Now, to explore how this works we can add a test that does addition
with only integer variables for now:

```java
  @Test
  void testIntVariables() {
    var program = """
        var a = 2
        var b = 2
        a + b
        """;
    assertEquals(4, evalInt(program));
  }
```

And if you try to run that test it will crash:

> org.graalvm.polyglot.PolyglotException: com.oracle.truffle.api.dsl.UnsupportedSpecializationException: Unexpected values provided for ToylVarRefNodeGen@106cc338: [], []

It's a good exercise to try to step through the code to see where it
breaks. You can add a breakpoint to the where it crashes. If you do
you will find that the guard test fails. If you look at the value of
`slot` in the context of the debugger you will see that it has kind
`Illegal`. So it appears we haven't stored the right type. And indeed,
our implementation of `ToylAssignmentNode` just uses
`frame.setObject`. As a quickfix we can try to just fix that code to
use `setInt` instead, so replace `ToylAssignmentNode.executeGeneric`
with:

```java
  @Override
  public Object executeGeneric(VirtualFrame frame) {
    var value = this.expr.executeGeneric(frame);
    frame.setInt(this.slot, (Integer) value);
    return value;
  }
```

But now if you try to run the tests you should see the `testVariables`
test fail, which is not surprising as it uses a double and we just
changed our code to only support integers. So we get a class cast
exception trying to set our double variable:

> org.graalvm.polyglot.PolyglotException: java.lang.ClassCastException: class java.lang.Double cannot be cast to class java.lang.Integer (java.lang.Double and java.lang.Integer are in module java.base of loader 'bootstrap')


So we neeed to fix that. How do we ensure that we call the right
`frame.set(Int|Double)` method? Well, maybe we can try with
`instanceof` checks:

```java
  @Override
  public Object executeGeneric(VirtualFrame frame) {
    var value = this.expr.executeGeneric(frame);
    if (value instanceof Double) {
      frame.setDouble(this.slot, (Double) value);
    } else {
      frame.setInt(this.slot, (Integer) value);
    }
    return value;
  }
```

But it still fails. The error has changed though:

> org.graalvm.polyglot.PolyglotException: com.oracle.truffle.api.dsl.UnsupportedSpecializationException: Unexpected values provided for ToylVarRefNodeGen@1603cd68: [], []

This looks like the error we started with in the integer only case. If
you look at the stack trace we're crashing in:

> 	at toyl.ast.ToylVarRefNodeGen.executeAndSpecialize(ToylVarRefNodeGen.java:57)

So it appears Truffle no longer knows how to handle doubles in var
ref, which is perhaps not surprising as we've left `executeDouble`
untouched. Let's update that to use `@Specialization` too:

```java
  @Specialization(guards = "frame.isDouble(slot)")
  public double readDouble(VirtualFrame frame) {
    try {
      return frame.getDouble(this.slot);
    } catch (FrameSlotTypeException e) {
      throw new IllegalStateException(e);
    }
  }	
```

And it runs! Yay! But you may be less satisfied with our `instanceof`
checks in `ToylAssignmentNode`. Can we improve on that? 
