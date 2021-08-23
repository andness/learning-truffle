## Generating optimized code using the Truffle DSL

So we've seen (some of) the code needed to use an optimal
representation for our numbers. But now we're going to learn that
thanks to the Truffle DSL you don't need to write any of this code.

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
class called `ToylAddNodeGen`. It might be a good idea to take a look
at that code. You might recognize some of the structure. And you may
also be very very happy you didn't have to write that code by hand!
The code we looked at earlier was just a small piece of what Truffle
generates to fully implement this behaviour.


TODO rewrite from there...



### The specialized version of `ToylArithmeticOpNode`

To use the specialization machinery that Truffle offers we can no
longer use a single node for our arithmetic operators. We have to
create one node per operator. They are going to be quite simple
though, so not to worry!

Let's handle addition first. Here's the new `ToylAddNode.java`:

```java
package toyl.ast;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;

@NodeChild("left") @NodeChild("right")
public abstract class ToylAddNode extends ToylNode {
  @Specialization(rewriteOn = ArithmeticException.class)
  protected int addInts(int leftValue, int rightValue) {
    return Math.addExact(leftValue, rightValue);
  }

  @Specialization(replaces = "addInts")
  protected double addDoubles(double leftValue, double rightValue) {
    return leftValue + rightValue;
  }
}
```

First of all notice that the class is `abstract`. We're not going to
instantiate this class. Instead, the Truffle DSL (which is where the
`@NodeChild` and `@Specialization` annotations are coming from will
generate a class called `ToylAddNodeGen`. If you try to build the
project after adding this file (nevermind if it doesn't compile yet)
the code generator should kick in and generate this class in
`target/generated-sources/annotations/toyl/ast/ToylAddNodeGen.java`.

You should take a look. And you should be happy you didn't have to
write all that code by hand!

Let's study this a little closer to better understand the Truffle
annotations. The `@NodeChild` annotations add `left` and `right` child
nodes to the `ToylAddNode` class. And you can see the result at the
top of the generated class:

```java
    @Child private ToylNode left_;
    @Child private ToylNode right_;
```

And we also get a constructor. But look, it's private:

```java
    private ToylAddNodeGen(ToylNode left, ToylNode right) {
        this.left_ = left;
        this.right_ = right;
    }
```

We're also going to adapt our `ToylParseTreeVisitor` to create this
new `ToylAddNode`, and if you look towards the bottom of the generated
class you'll find the tool we'll use:

```java
    public static ToylAddNode create(ToylNode left, ToylNode right) {
        return new ToylAddNodeGen(left, right);
    }
```

So that's how the `@Child` annotations works. Now let's move on to the
`@Specialized` annotation. The result of this annotation is a lot more
involved, but let's look at `addInts` first: 

```java
  @Specialization(rewriteOn = ArithmeticException.class)
  protected int addInts(int leftValue, int rightValue) {
    return Math.addExact(leftValue, rightValue);
  }
```

The key here is that an `AritmeticException` will trigger a
rewrite. And if you look at the documentation for `Math.addExact` it
states 

> Throws: ArithmeticException – if the result overflows an int

which is exactly what we want.

But what actually happens on a rewrite? Well that's where our
`addDoubles` comes in:


```java
  @Specialization(replaces = "addInts")
  protected double addDoubles(double leftValue, double rightValue) {
    return leftValue + rightValue;
  }
```

This declares a specialization with `replaces = "addInts"`. So when a
rewrite is triggered this method will be the execute method for our
add node. To be able to try this out we'll need to temporarily reduce
the feature set for our language down to just addition. You then need
to delete `ToylArithmeticOpNode`. Lastly, we need to update
`visitArithmeticExpression` in `ToylParseTreeVisitor` to look like
this:

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
  protected int addInts(int leftValue, int rightValue) {
    System.out.println("addInts(" + leftValue + "," + rightValue + ")");
    return Math.addExact(leftValue, rightValue);
  }

  @Specialization(replaces = "addInts")
  protected double addDoubles(double leftValue, double rightValue) {
    System.out.println("addDoubles(" + leftValue + "," + rightValue + ")");
    return leftValue + rightValue;
  }
```

Now we can try a few examples, let's start with a plain integer
addition, here's `2 + 2`:

```
2+2
addInts(2,2)
result = 4
```

Ok good. But what if I go for overflow? Let's add 1 to the maximum `int`
value:

```
2147483647 + 1
addInts(2147483647,1)
Exception in thread "main" org.graalvm.polyglot.PolyglotException: com.oracle.truffle.api.dsl.UnsupportedSpecializationException: Unexpected values provided for ToylAddNodeGen@5136d012: [2147483647, 1], [Integer,Integer]
	at toyl.ast.ToylAddNodeGen.executeAndSpecialize(ToylAddNodeGen.java:200)
```

Wait what? Why did this happen? And what happens if you try to
calculate `3.14 + 1`?

> Exception in thread "main" org.graalvm.polyglot.PolyglotException: com.oracle.truffle.api.dsl.UnsupportedSpecializationException: Unexpected values provided for ToylAddNodeGen@44a7bfbc: [3.14, 1], [Double,Integer]

You might find the second error more expected. Looking at the
signatures for the methods we have one that takes two ints and one
that takes two doubles, so it seems we're missing methods for (int,
double) and (double, int). At least that's how I was thinking the
first time I ran into this. But that's not what is going on
here. After all, why did we get an error that clams that [Integer,
Integer] is unsupported when we have `addInts(int, int)`? So let's go
through what should happen when we try `2147483647 +1`. First our
`addInts` method is invoked (as we can see from the printout). And as
expected it should throw an `ArithmeticException` which have declared
our `rewriteOn` rule for. So, the next that should happen is that
Truffle should replace our call with a call to the `addDoubles`
method. But here's the problem. While Java can happily do implicit
conversions from int to double (AKA widening), Truffle will not. Not
unless we tell it to, anyway. This is where `@TypeSystem` comes in. We
need to tell Truffle that an int can be used as a double. Let's create
`src/main/java/toyl/ToylTypeSystem.java`:

```java
package toyl;

import com.oracle.truffle.api.dsl.ImplicitCast;
import com.oracle.truffle.api.dsl.TypeSystem;

@TypeSystem
public class ToylTypeSystem {
  @ImplicitCast
  public static double castIntToDouble(int value) {
    return value;
  }
}
```

And next we have to tell Truffle about our type system which we do by
adding it to our base node class:

```java
@TypeSystemReference(ToylTypeSystem.class)
public abstract class ToylNode extends Node {
...
```

If you try again now it should succeed, and we should see both methods
called:

```
2147483647 + 1
addInts(2147483647,1)
addDoubles(2.147483647E9,1.0)
result = 2.147483648E9
```

Great! Now we just need to implement the remaining arithmetic
operation nodes. I'll leave that as an exercise, and if you get stuck
you can always cheat and look at the final source code for this
chapter.
