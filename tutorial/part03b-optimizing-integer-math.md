# Optimizing integer math

Our calculator now works with all numbers, but it's inefficient. If we
have two numbers that are both integers we can obviously make the
interpreter code a lot more efficient by using the java built-in
`long` type for the calculations.

To do this we need to know what kind of number we are dealing with. We
can detect this already in our parser, and based on whether the number
fits in a `long` or not we can create two different types of literal
nodes, one for the `long` case and one for the `BigDecimal` case. For
literals we can detect what kind of number we have during parsing, so
we need to introduce a new `ToylLiteralLongNode`. We also need
multiple variants of our `execute` method, one using `long` as the
representation and the other using `BigDecimal`, so let's start by
modifying `ToylNode`:

```java
public abstract class ToylNode extends Node {
  public abstract long executeLong(VirtualFrame frame) throws UnexpectedResultException;
  public abstract BigDecimal executeNumber(VirtualFrame frame);
  public abstract Object executeGeneric(VirtualFrame frame);
}
```

The idea is this: We'll optimistically try using `executeLong`, but
catch the `UnexpectedResultException` in case the operation can no
longer be represented as a long. When that happens we'll fall back to
using `executeNumber`. And `executeGeneric` is the method we'll call
from the "outside" where we're not aware of the underlying type.

We can start by implementing the literal values as these are
straightforward, so add
`language/src/main/java/ast/ToylLiteralLongNode.java`:

```java
package toyl.ast;

import com.oracle.truffle.api.frame.VirtualFrame;

import java.math.BigDecimal;

public class ToylLiteralLongNode extends ToylNode {
  private final long value;

  public ToylLiteralLongNode(int value) {
    this.value = value;
  }

  public long executeLong(VirtualFrame frame) {
    return this.value;
  }

  @Override
  public BigDecimal executeNumber(VirtualFrame frame) {
    return new BigDecimal(this.value);
  }
}
```

And we also need to adapt our `ToylLiteralNumberNode`:

```java
package toyl.ast;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import java.math.BigDecimal;

public class ToylLiteralNumberNode extends ToylNode {
  private final BigDecimal value;
  public ToylLiteralNumberNode(BigDecimal number) {
    this.value = number;
  }

  @Override
  public long executeLong(VirtualFrame frame) throws UnexpectedResultException {
    throw new UnexpectedResultException(value);
  }

  @Override
  public BigDecimal executeNumber(VirtualFrame frame) {
    return this.value;
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    return this.value;
  }
}

```

Next we need to fix our arithmetic operations. As the code is going to
get quite complicated we'll need to split the arithmetic operators
into separate classes. So let's create
`language/src/main/java/ast/ToylAddNode.java`, for now we'll just put
in empty implementations of the `execute` methods:

```java
package toyl.ast;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import java.math.BigDecimal;

public class ToylAddNode extends ToylNode {
  private ToylNode left; 
  private ToylNode right;

  public ToylAddNode(ToylNode left, ToylNode right) {
    this.left = left;
    this.right = right;
  }

  @Override
  public long executeLong(VirtualFrame frame) throws UnexpectedResultException {
    return 0;
  }

  @Override
  public BigDecimal executeNumber(VirtualFrame frame) {
    return null;
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    return null;
  }
}
```

Ok. Now the fun starts. What we want to achieve is that GraalVM can
pick the efficient `long` representation for a node and keep using
that. Imagine you had a loop like this:

```
var i = 0
while (i < 100000) {
  i = i + 1
}
```

That addition node which handles `i + 1` will be repeated many times
over, and it will never outgrow the `long` representation. So we want
to let GraalVM JIT compile this addition node into the most efficient
code (using `long`) and keep using that representation until this
assumption is invalidated.

So that means our node can be in 3 states: 

1. Unset (we don't know yet)
2. Using long
3. Using number

We'll store this as an enum on the class:

```java
  private enum SpecializationState {
    LONG, 
    NUMBER
  }

  private SpecializationState specializationState = SpecializationState.LONG;
```

Now, let's do the easy part first, `executeNumber`:

```java
  @Override
  public BigDecimal executeNumber(VirtualFrame frame) {
    return this.left.executeNumber(frame).add(this.right.executeNumber(frame));
  }
```

Since BigDecimal our "final" state we don't need to do anything
more. 

Now, let's look at `executeGeneric`. Remember this is the method that
is called by `ToylProgramNode.execute` and is such the entrypoint into
our add node.

```java
  @Override
  public Object executeGeneric(VirtualFrame frame) {
    if (this.specializationState == SpecializationState.NUMBER) {
      return this.executeNumber(frame);
    }
    try {
      var value = this.executeLong(frame);
      this.specializationState = SpecializationState.LONG;
      return value;
    } catch (UnexpectedResultException e) {
      this.specializationState = SpecializationState.NUMBER;
      return e.getResult();
    }
  }
```

So we can see how we immediately use the slow path if we've entered
the `NUMBER` state, otherwise we attempt to use the `executeLong`
method and catch in case it goes wrong. Also notice how
`UnexpectedResultException` isn't just used to signal that our
assumption failed, but it also transports the actual value so that we
don't have to recalculate it.

So now the last remaining bit is the `executeLong` method:

```java
  @Override
  public long executeLong(VirtualFrame frame) throws UnexpectedResultException {
    long leftVal;
    try {
      leftVal = this.left.executeLong(frame);
    } catch (UnexpectedResultException e) {
      specializationState = SpecializationState.NUMBER;
      BigDecimal leftDecimal = (BigDecimal) e.getResult();
      throw new UnexpectedResultException(leftDecimal.add(this.right.executeNumber(frame)));
    }
    long rightVal;
    try {
      rightVal = this.right.executeLong(frame);
    } catch (UnexpectedResultException e) {
      specializationState = SpecializationState.NUMBER;
      BigDecimal rightDecimal = (BigDecimal) e.getResult();
      throw new UnexpectedResultException(rightDecimal.add(this.right.executeNumber(frame)));
    }
    try {
      return Math.addExact(leftVal, rightVal);
    } catch (ArithmeticException e) {
      specializationState = SpecializationState.NUMBER;
      throw new UnexpectedResultException(new BigDecimal(leftVal).add(new BigDecimal(rightVal)));
    }
  }
```

Phew. Not the prettiest piece of code. Since we want to get the result
from `UnexpectedResultException` in the case of either the left or
right node throwing we have to handle each of them separately. And if
we're able to get `long` values back from both left and right we rely
on `Math.addExact` to catch the overflow through
`ArithmeticException`.

If you want to test this you will also have to cheat a bit and hack
the `visitArithmeticExpression` method to just ignore all operators
except `+`:

```java
  @Override
  public ToylNode visitArithmeticExpression(ToylParser.ArithmeticExpressionContext ctx) {
    return new ToylAddNode(this.visit(ctx.left), this.visit(ctx.right));
  }
```

This code should work, but it's missing several pieces needed to make
this actually work well when executed on GraalVM. And we have to go
through the same dance for the other arithmetic
operators. Man. Luckily this is exactly where the Truffle DSL comes
storming in and saves the day. Let's continue to see how the DSL
greatly simplifies handling this case.
