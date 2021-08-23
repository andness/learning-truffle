## Premature optimization for fun and education

The idea is simple enough and is called speculative
optimization. Basically, we guess that most of the time the numbers
are going to be integers and we'll default to using regular integer
math, and the JIT compiler will generate machine code for that
case. But if we detect that the numbers won't fit into our integer
type we'll deoptimize and fall back to the slower version of the code
using doubles. While not precisely the same, you can read more about
how this is done in the V8 engine here:
https://mrale.ph/blog/2015/01/11/whats-up-with-monomorphism.html

Let's start with the simple stuff. We need to add a few more methods
to our base `ToylNode` class, and we need to implement these methods
in our AST nodes. Our `ToylLiteralNumberNode` will split into two, one
for integers and one for doubles, and our `ToylArithmeticOpNode` is
where we really start to see the Truffle specialization framework do
its work.

Let's start with our base node in `ToylNode.java`:

```java
package toyl.ast;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

public abstract class ToylNode extends Node {
  public abstract int executeInt(VirtualFrame frame) throws UnexpectedResultException;
  public abstract double executeDouble(VirtualFrame frame);
  public abstract Object executeGeneric(VirtualFrame frame);
}
```

And now for our `ToylLiteralIntNode.java`:

```java
package toyl.ast;

import com.oracle.truffle.api.frame.VirtualFrame;

public class ToylLiteralIntNode extends ToylNode {
  private final int value;
  public ToylLiteralIntNode(int value) {
    this.value = value;
  }

  @Override
  public int executeInt(VirtualFrame frame) {
    return this.value;
  }

  @Override
  public double executeDouble(VirtualFrame frame) {
    return this.value;
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    return this.value;
  }
}
```

And the `ToylLiteralDoubleNode.java`:

```java
package toyl.ast;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

public class ToylLiteralDoubleNode extends ToylNode {
  private final double value;
  public ToylLiteralDoubleNode(double value) {
    this.value = value;
  }

  @Override
  public int executeInt(VirtualFrame frame) throws UnexpectedResultException {
    throw new UnexpectedResultException(this.value);
  }

  @Override
  public double executeDouble(VirtualFrame frame) {
    return this.value;
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    return this.value;
  }
}
```

Cool. So now we need to fix the `visitLiteralNumber` in
`ToylParseTreeVisitor`. We want to detect if the number we're dealing
with can fit in an integer. To do this, we'll use the `BigDecimal`
class. I suggest you give it a shot. There's probably smarter ways of
doing this than the code I'm gonna show here, but anyway, here it is:

```java
  @Override
  public ToylNode visitLiteralNumber(ToylParser.LiteralNumberContext ctx) {
    var number = new BigDecimal(ctx.LITERAL_NUMBER().getText());
    if (number.scale() > 0 || number.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
      return new ToylLiteralDoubleNode(number.doubleValue());
    } else {
      return new ToylLiteralIntNode(number.intValue());
    }
  }
```

So now we have the literals sorted and we need to implement our
arithmetic operation node.

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
