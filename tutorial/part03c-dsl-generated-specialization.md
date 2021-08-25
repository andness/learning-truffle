## Generating optimized code using the Truffle DSL

So we've seen (some of) the code needed to use an optimal
representation for our numbers. But now we're going to learn that
thanks to the Truffle DSL you don't need to write any of this code
yourself.

Actually, I'm just going to dump the new `ToylAddNode` with the magic
DSL annotations here and you'll see just how simple it is:

```java
package toyl.ast;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;

import java.math.BigDecimal;

@NodeChild("left")
@NodeChild("right")
public abstract class ToylAddNode extends ToylNode {
  @Specialization(rewriteOn = ArithmeticException.class)
  protected long addLongs(long leftValue, long rightValue) {
    return Math.addExact(leftValue, rightValue);
  }

  @Specialization(replaces = "addLongs")
  protected BigDecimal addNumbers(BigDecimal leftValue, BigDecimal rightValue) {
    return leftValue.add(rightValue);
  }
}
```

Not bad! Probably makes no sense yet, but we'll get to that. The point
is that this is a lot simpler than the code we wrote by hand. When you
compile this code the DSL code generator kicks in and generates a
class called `ToylAddNodeGen`. I think you should take a look at that
code. You might recognize some of the structure from the class we
wrote by hand in the previous section. And you may also be very very
happy you didn't have to write the full code by hand as the code we
looked at earlier was just a small piece of what Truffle generates to
fully implement this behaviour.

## Breaking it down

Let's look at the various annotations used here and what they
do. We'll start with the `@NodeChild` annotation. The javadoc for it
is quite clear:

> A NodeChild element defines an executable child for the enclosing Node. A Node contains multiple NodeChildren specified in linear execution order.

Also notice that the class is `abstract`. We're not going to create
instances of this class, instead we'll create instances of the
`ToylAddNoeGen` class, and truffle uses the annotations to build the
constructor for `ToylAddNodeGen`. The constructor is private, but
Truffle generates a `create` method with the same signature. We'll
adapt our tree visitor once we have the other classes defined.

The fact that the children defined this way are *executable* is
key. This means that Truffle can generate `execute` calls to them. You
can see this at the start of the generated `executeNumber` for
example:

```java
    @Override
    public BigDecimal executeNumber(VirtualFrame frameValue) {
        int state_0 = this.state_0_;
        Object leftValue_ = this.left_.executeGeneric(frameValue);
        Object rightValue_ = this.right_.executeGeneric(frameValue);
```		

You can also see how the code generation reacts if you try to add
another `@NodeChild`, for example `@NodeChild("foo")`. You'll get two
errors:

> part03c/language/src/main/java/toyl/ast/ToylAddNode.java:12:3
> java: Method signature (long, long) does not match to the expected signature: 
>       Object addLongs([VirtualFrame frame], Object left, Object right, Object foo)
> 
> part03c/language/src/main/java/toyl/ast/ToylAddNode.java:17:3
> java: Method signature (BigDecimal, BigDecimal) does not match to the expected signature: 
>       Object addNumbers([VirtualFrame frame], Object left, Object right, Object foo)

So you can see our method signatures (the ones with `@Specialization`
anyway) need to match our `@NodeChild annotations`. 

Now let's move on to the `@Specialized` annotation. The code that
result from this annotation is a lot more involved (as you know from
trying to write the same code yourself). Let's start with `addLongs`:

```java
  @Specialization(rewriteOn = ArithmeticException.class)
  protected long addLongs(long leftValue, long rightValue) {
    return Math.addExact(leftValue, rightValue);
  }
```

The key here is that an `AritmeticException` will trigger a
rewrite. And if you look at the documentation for `Math.addExact` it
states 

> Throws: ArithmeticException – if the result overflows a long

which is exactly what we want.

But what actually happens on a rewrite? Well that's where our
`addNumbers` comes in:

```java
  @Specialization(replaces = "addLongs")
  protected BigDecimal addNumbers(BigDecimal leftValue, BigDecimal rightValue) {
    return leftValue.add(rightValue);
  }
```

This declares a specialization with `replaces = "addLongs"`. So when a
rewrite is triggered the node goes into the "slow" state and from then
on will use the `BigDecimal` code. 

To be able to try this out we'll need to temporarily reduce the
feature set for our language down to just addition. We'll do that by
updating `visitArithmeticExpression` in `ToylParseTreeVisitor` to look
like this:

```java
  @Override
  public ToylNode visitArithmeticExpression(ToylParser.ArithmeticExpressionContext ctx) {
    return switch (ctx.binaryOp.getText()) {
      case "+" -> ToylAddNodeGen.create(this.visit(ctx.left), this.visit(ctx.right));
      default -> throw new IllegalStateException("Unexpected arithmetic operator: " + ctx.binaryOp.getText());
    };
  }
```	

And to help us see what happens we'll use the good old print
statement. We'll add one to each add method:

```java
  @Specialization(rewriteOn = ArithmeticException.class)
  protected long addLongs(long leftValue, long rightValue) {
    System.out.println("addLongs(" + leftValue + "," + rightValue + ")");
    return Math.addExact(leftValue, rightValue);
  }

  @Specialization(replaces = "addLongs")
  protected BigDecimal addNumbers(BigDecimal leftValue, BigDecimal rightValue) {
    System.out.println("addNumbers(" + leftValue + "," + rightValue + ")");
    return leftValue.add(rightValue);
  }
```

Now we can try a few examples, let's start with a plain integer
addition, here's `2 + 2`:

```
2+2
addLong(2,2)
result = 4
```

Ok good. But what if I go for overflow? Let's add 1 to the maximum `long`
value:

```
9223372036854775807 + 1
addLongs(9223372036854775807,1)
Exception in thread "main" org.graalvm.polyglot.PolyglotException: com.oracle.truffle.api.dsl.UnsupportedSpecializationException: Unexpected values provided for ToylAddNodeGen@5136d012: [9223372036854775807, 1], [Long,Long]
```

Wait what? Why did this happen? And what happens if you try to
calculate `3.14 + 1`?

> Exception in thread "main" org.graalvm.polyglot.PolyglotException: com.oracle.truffle.api.dsl.UnsupportedSpecializationException: Unexpected values provided for ToylAddNodeGen@2525ff7e: [3.14, 1], [BigDecimal,Long]

You might find the second error more expected. Looking at the
signatures for the `add` methods we have one that takes two `longs`
and one that takes two `BigDecimal`s, so it seems we're missing
methods for `(long, BigDecimal)` and `(BigDecimal, long)`. At least
that's how I was thinking the first time I ran into this. But that's
not what is going on here. After all, why did we get an error that
clams that [Long, Long] is unsupported when we have `addLongs(long,
long)`? 

Let's go through what should happen when we try `9223372036854775807
+1`. First our `addLongs` method is invoked (as we can see from the
printout). And as expected it should throw an `ArithmeticException` ,
for which have declared our `rewriteOn` rule. So, the next that should
happen is that Truffle should replace our call with a call to the
`addNumbers` method. You can try to run this under the debugger and
make it break on `UnsupportedSpecializationException`.

If you do, you'll find that it breaks in a method called
`executeAndSpecialize` in the generated `ToylAddNodeGen` class. If you
read through that `method` you'll see that it modifies the `exclude`
and `state_0_` fields when hitting the ArithmeticException, and then
calls itself again. In this second call it falls through to the second
`if` block where it tests for whether the values are
`BigDecimal`s. And their not, they are `Long`s, so it falls through to
the exception. Here's a simplfied version of this last part just in
case you aren't quite following (I've removed the state mutations and
kept just the main flow):

```java
            if (leftValue instanceof BigDecimal) {
                BigDecimal leftValue_ = (BigDecimal) leftValue;
                if (rightValue instanceof BigDecimal) {
                    BigDecimal rightValue_ = (BigDecimal) rightValue;
                    return addNumbers(leftValue_, rightValue_);
                }
            }
            throw new UnsupportedSpecializationException(this, new Node[] {this.left_, this.right_}, leftValue, rightValue);
```

Here's the problem: Truffle doesn't know how to convert the values
into a `BigDecimal`. We need to tell Truffle how to do this, and for
this we'll use the `@TypeSystem` annotation. Let's create
`src/main/java/toyl/ToylTypeSystem.java`:

```java
package toyl;

import com.oracle.truffle.api.dsl.ImplicitCast;
import com.oracle.truffle.api.dsl.TypeSystem;

@TypeSystem
public class ToylTypeSystem {
  @ImplicitCast
  public static BigDecimal castLongToBigDecimal(long value) {
    return new BigDecimal(value);
  }
}
```

And next we have to ensure that the DSL uses this type system when
generating code, so we must add it to our base node class:

```java
@TypeSystemReference(ToylTypeSystem.class)
public abstract class ToylNode extends Node {
...
```

If you try again now it should succeed, and we should see both methods
called:

```
9223372036854775807 + 1
addLongs(9223372036854775807,1)
addNumbers(9223372036854775807,1)
result = 9223372036854775808
```

It's worth revisiting `ToylAddNodeGen`. The `executeAndSpecialize`
code that we looked at earlier which was handling the case of the
`ArithmeticException` now looks like this:

```java                int bigDecimalCast0;
  if ((bigDecimalCast0 = ToylTypeSystemGen.specializeImplicitBigDecimal(leftValue)) != 0) {
      BigDecimal leftValue_ = ToylTypeSystemGen.asImplicitBigDecimal(bigDecimalCast0, leftValue);
      int bigDecimalCast1;
      if ((bigDecimalCast1 = ToylTypeSystemGen.specializeImplicitBigDecimal(rightValue)) != 0) {
          BigDecimal rightValue_ = ToylTypeSystemGen.asImplicitBigDecimal(bigDecimalCast1, rightValue);
          this.exclude_ = exclude = exclude | 0b1 /* add-exclude addLongs(long, long) */;
          state_0 = state_0 & 0xfffffffe /* remove-state_0 addLongs(long, long) */;
          state_0 = (state_0 | (bigDecimalCast0 << 2) /* set-implicit-state_0 0:BigDecimal */);
          state_0 = (state_0 | (bigDecimalCast1 << 4) /* set-implicit-state_0 1:BigDecimal */);
          this.state_0_ = state_0 = state_0 | 0b10 /* add-state_0 addNumbers(BigDecimal, BigDecimal) */;
          lock.unlock();
          hasLock = false;
          return addNumbers(leftValue_, rightValue_);
      }
  }
```

This time I included all the gory details. But the important thing to
notice is of course that we have calls to `ToylTypeSystemGen` which
take care of casting our `Long` to a `BigDecimal`. This also fixed the
other failing case of `3.14 + 1` as we now detect that one the values
is a `BigDecimal` and therefore implicitly cast the `1` into a
`BigDecimal` before adding. Indeed, you can see that only `addNumbers`
is involved this time:

```java
3.14+1
addNumbers(3.14,1)
result = 4.14
```

Great! Now we just need to implement the remaining arithmetic
operation nodes. I'll leave that as an exercise, and if you get stuck
you can always cheat and look at the final source code for this
chapter.
