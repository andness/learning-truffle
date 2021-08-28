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

And it runs! Yay! But you may wonder, shouldn't `ToylAssignmentNode`
be specialized too? Yes, it probably should.

## Specializing ToylAssignmentNode

Our `ToylAssignmentNode` has the `expr` child node, so we need to
replace it with a `@NodeChild` annotation. If we do that, declare the
class abstract, remove the constructor and the `expr` field, we get
warnings about `slot` and `name` "might not have been
initialized". Ignore that for now. We want to add a specialized method
for handling the long, so let's call it `assignLong`, and create it
with an empty `guard` for now (also, delete `executeGeneric`):

```java
  @Specialization(guards = "")
  public void assignLong(VirtualFrame frame, long value) {
    frame.setLong(slot, value);
  }
```

It will fail to compile, but it will produce a `ToylAssignmentNodeGen`
class. Inspecting that we can see that the `create` method isn't quite
as we expected. It only accepts a single parameter:

```java
    public static ToylAssignmentNode create(ToylNode expr) {
        return new ToylAssignmentNodeGen(expr);
    }
```

We want to get that `name` and `slot` passed in to. So we need to tell
Truffle to generate them, and we do this using the `@NodeField`
annotation. So update the class declaration to look like this:

```java
@NodeField(name = "name", type = String.class)
@NodeField(name = "slot", type = FrameSlot.class)
@NodeChild("expr")
public abstract class ToylAssignmentNode extends ToylNode {
...
```

Attempting to build now will generate a new error:

> java: Duplicate field name 'name'.

But if we remove the `name` and `slot` fields our `assignLong` method
no longer works since it uses `slot`. Hmmm. The solution is to create
abstract getters. To summarize, this is how our class should look now:

```java
@NodeField(name = "name", type = String.class)
@NodeField(name = "slot", type = FrameSlot.class)
@NodeChild("expr")
public abstract class ToylAssignmentNode extends ToylNode {

  abstract FrameSlot getSlot();
  abstract String getName();

  @Specialization(guards = "")
  public long assignLong(VirtualFrame frame, long value) {
    frame.setLong(getSlot(), value);
	return value;
  }
}
```

Now we just need to figure out how to define that guard. So we want to
trigger `assignLong` if our slot holds a long value. Let's remind
ourselves about how we create the `slot` in `visitAssignment`:

```java
var slot = this.frameDescriptor.findOrAddFrameSlot(name);
```

There's a second overload for that `findOrAddFrameSlot` method which
takes a `FrameSlotKind`. But we can't use that since we don't know the
runtime type of the value expression when building the tree (if we
were making a statically typed language we would!). So what this calls
ends up doing is calling `addFrameSlot(Object identifier)`, which as
we can see from the javadoc defaults to `FrameSlotKind.Illegal`.

So want the guard to test if our slot kind is `Long` or `Illegal`. But
how? Looking at `FrameSlot` we see at deprecated `getKind` method
which states that we should instead use
`FrameDescriptor.getFrameSlotKind(FrameSlot)`. So where do we get
`FrameDescriptor` from? Do we need to pass it into our
`ToylAssignmentNode` class? No, we don't, we can get it from
`VirtualFrame.getFrameDescriptor()`. So we end up with this method:

```java
  protected boolean isLongOrIllegal(VirtualFrame frame) {
    var kind = frame.getFrameDescriptor().getFrameSlotKind(getSlot());
    return kind == FrameSlotKind.Long || kind == FrameSlotKind.Illegal;
  }
```

And now we can specify the guard for `assignLong`:

```java
  @Specialization(guards = "isLongOrIllegal(frame)")
```

So is our test green? Not yet. We still need to implement the
BigDecimal case. But now we know what to do, add our `assignNumber`
method:

```java
  @Specialization(guards = "isObjectOrIllegal(frame)")
  public Object assignNumber(VirtualFrame frame, Object value) {
    frame.setObject(getSlot(), value);
    return value;
  }
```

And implement the guard:

```java
  protected boolean isObjectOrIllegal(VirtualFrame frame) {
    var kind = frame.getFrameDescriptor().getFrameSlotKind(getSlot());
    return kind == FrameSlotKind.Object || kind == FrameSlotKind.Illegal;
  }
```

And the test is green. Great stuff!

## Reassignment

What do we expect to happen if we write the following program:

```
var a = 1
var a = 1.5
a
```

Will it return 1? 1.5? Or crash? We haven't really defined is this
should be legal or not. But it seems like this should crash? The first
assign should create the slot for `a` and the second assign should
reuse the same slot. When the interpreter executes this it should
specialize to use `long` on the first assignment and thus set the slot
kind to `Long`. The next assignment should then trigger the
`isLongOrIllegal` guard as the slot is now `Long`, and that seems like
it should crash as we'll attempt to stuff a `BigDecimal` into a `Long`
slot. Let's add a test for that:

```java
  @Test
  void testReassign() {
    var program = """
        var a = 1
        var a = 1.5
        a
        """;
    System.out.println(eval(program));
  }
```

Since we expect this to crash we just print the result. But it doesn't
crash. It actually prints 1.5. Puzzling. An easy way to debug this is
to place a breakpoint inside the for loop in `ToylProgramNode`,
i.e. on this line:

```java
result = statement.executeGeneric(frame);
```

This way you can see what happens as you step through the first and
second assignment and eventually the var ref. Doing so reveals that
after the first statement is executed, the `FrameSlot` corresponding
to our `a` variable still has kind `Illegal`. Why? The reason is that
setting a slot does not affect its kind. We need to add that to our
`assignLong` method. So we'll update `assignLong`:

```java
  @Specialization(guards = "isLongOrIllegal(frame)")
  public long assignLong(VirtualFrame frame, long value) {
    frame.getFrameDescriptor().setFrameSlotKind(getSlot(), FrameSlotKind.Long);
    frame.setLong(getSlot(), value);
    return value;
  }
```

And we also have to do the same for `assignNumber`, but now we need to
use `FrameSlotKind.Object`. By doing that we get the expected crash.

But ... is that actually what we want? Thinking about it, it seems
silly. After all, we just want our program to be efficient. But it
should be perfectly ok to assign a decimal number to a variable since
Toyl really just one type: number. If the number is internally
represented by a `long` or a `BigDecimal` is just an optimization
detail.
