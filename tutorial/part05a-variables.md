# Variables

Finally it's time to start adding some more features to our
language. Variables are an essential part of any programming language
and their introduction will expose us to several new parts of the
Truffle framework. So far our language only supports a single
expression. We will now allow a program to consist of multiple
expressions. So the following will now be a valid Toyl program:

```
var pi = 3.14
var radius = 42
pi * radius * radius
```

The result of this calculation should be 5538.96, because executing a
Toyl program now will return the value of the last expression. Our
first task then is to update our grammar to allow for these new
constructs. This is a good time to try for yourself!

Here's my suggestion. Note that `var` is a statement. But we still
want to allow for top-level expressions, otherwise we can't return the
value from the last expression, so our program should consist of a
sequence of variable assignments or expressions, in other words our
`program` rule should change to this:

```
program: statement+ EOF;
```

And we also need to add the new `statement` rule which further
requires a new `assignment` rule like this:

```
statement: (expr|assignment);

assignment: 'var' NAME '=' expr;
```

As you can see this uses a new token called `NAME` which is
defined simply as:

```
NAME: ([a-z]|[A-Z])+;
```

Our names are going to be very simple to begin with!

The last missing piece now is the variable references which must be
added as an `expr` alternative:

```
    | NAME                                    #VarRefExpr
```

So, the entire grammar file now looks like this:

```
grammar Toyl;

program: statement+ EOF;

statement: (expr|assignment);

assignment: 'var' NAME '=' expr;

expr
    : LITERAL_NUMBER                          #LiteralNumber
    | left=expr binaryOp=('*'|'/') right=expr #ArithmeticExpression
    | left=expr binaryOp=('+'|'-') right=expr #ArithmeticExpression
    | '(' expr ')'                            #ParenthesizedExpr
    | NAME                                    #VarRefExpr
    ;

LITERAL_NUMBER: [0-9]+('.'[0-9]+)?;
NAME: ([a-z]|[A-Z])+;
WS    : [ \t\r\n]+ -> skip;
```

Ok, time to regenerate our parser and get to work on adapting our
AST. So we're going to run into an immediate problem. Our top level
`visitProgram` method just expects there to be a single `expr` but now
it can hit either `assignment` or `expr` and it needs to visit them in
order and produce a list.

One way to fix that is to represent the program with a node. But we
already have that, it's the `ToylProgramNode` which extends `RootNode`
and so cannot also be a `ToylNode`. Instead we can introduce a
`ToylStatementList` to hold the result of parsing the program rule. We
want to change our hierarchy so that the root node is now a
`ToylStatement` node with `ToylExpressionNode` inheriting it. The
quickest way to do that is to rename `ToylNode` to
`ToylExpressionNode` and then add `ToylStatementNode`. Lastly we'll
move the `executeGeneric` method up to `ToylStatementNode`.

That has to be the worst paragraph of prose ever written.

Let me just put the code here. First, `ToylStatementNode`:

```java
package toyl.ast;

import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import toyl.ToylTypeSystem;

@TypeSystemReference(ToylTypeSystem.class)
public abstract class ToylStatementNode extends Node {
  public abstract Object executeGeneric(VirtualFrame frame);
}
```

Next, `ToylExpressionNode` which now inherits it:

```java
package toyl.ast;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

public abstract class ToylExpressionNode extends ToylStatementNode {
  public abstract int executeInt(VirtualFrame frame) throws UnexpectedResultException;
  public abstract double executeDouble(VirtualFrame frame);
}
```

Remember to ensure that the operator and literal nodes all inherit
`ToylExpressionNode`. Now we can add our `ToylStatementListNode` which
will hold the result of parsing the `program` rule:

```java
package toyl.ast;

import com.oracle.truffle.api.frame.VirtualFrame;

import java.util.ArrayList;
import java.util.List;

public class ToylStatementListNode extends ToylStatementNode {

  private final List<ToylStatementNode> statements = new ArrayList<>();

  public ToylStatementListNode(List<ToylStatementNode> statements) {
    this.statements.addAll(statements);
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    Object result = null;
    for (ToylStatementNode statement : statements) {
      result = statement.executeGeneric(frame);
    }
    return result;
  }

}
```

Note that this is where we implement the rule about the result of the
last statement also being the result of the program as a whole.

Ok, now we need to update the `ToylProgramNode`:

```java
package toyl.ast;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;

public class ToylProgramNode extends RootNode {

  private final ToylStatementListNode statements;

  public ToylProgramNode(TruffleLanguage<?> language, FrameDescriptor frameDescriptor, ToylStatementListNode statements) {
    super(language, frameDescriptor);
    this.statements = statements;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return statements.executeGeneric(frame);
  }
}
```

And we need to adjust the `ToylParseTreeVisitor.visitProgram` method:

```java
  @Override
  public ToylStatementListNode visitProgram(ToylParser.ProgramContext ctx) {
    return new ToylStatementListNode(ctx.statement().stream().map(this::visit).toList());
  }
```

And you will notice that all the other methods require some
adjustments too since we now need to cast the result of the recursive
`visit` calls into `ToylExpressionNode`. I'll add the code for
`visitArithmeticExpression` here, but leave the rest as an exercise:

```java
  @Override
  public ToylExpressionNode visitArithmeticExpression(ToylParser.ArithmeticExpressionContext ctx) {
    var left = (ToylExpressionNode) this.visit(ctx.left);
    var right = (ToylExpressionNode) this.visit(ctx.right);
    return switch (ctx.binaryOp.getText()) {
      case "+" -> ToylAddNodeGen.create(left, right);
      case "-" -> ToylSubNodeGen.create(left, right);
      case "/" -> ToylDivNodeGen.create(left, right);
      case "*" -> ToylMulNodeGen.create(left, right);
      default -> throw new IllegalStateException("Unexpected arithmetic operator: " + ctx.binaryOp.getText());
    };
  }
```

And if you managed to get through all those changes correctly you may
have noticed something odd. The `ToylProgramNode` and
`ToylStatementListNode` classes. So far node classes have had a pretty
straightforward relationship with the grammar file, and that makes it
a lot easier to understand what's going on. Not so with these two. In
my opinion this is due to a mistake made in the very first part where
decided to call the root node `ToylProgramNode`. So I suggest that we
fix this now and rename `ToylProgramNode` to `ToylRootNode`, and then
let `ToylStatementListNode` become `ToylProgramNode`. Two quick
Shift-F6 invocations and we should be good (that's the Rename
refactoring if you're not yet quite up to speed on IntelliJ
shortcuts). (But you really should be, the refactoring tools are
fantastic).

So if you got that done correctly you can update `ToylLanguage`. Try
fixing it, there aren't too many changes needed. Here's what I ended
up with (notice that I decided to rename `parseExpr` to `parseProgram`
to better match the new structure):

```java
package toyl;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.source.Source;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import toyl.ast.ToylRootNode;
import toyl.ast.ToylProgramNode;
import toyl.parser.ToylLexer;
import toyl.parser.ToylParseTreeVisitor;
import toyl.parser.ToylParser;

import java.io.IOException;

@TruffleLanguage.Registration(
    id = ToylLanguage.ID,
    name = "Toyl", defaultMimeType = ToylLanguage.MIME_TYPE,
    characterMimeTypes = ToylLanguage.MIME_TYPE,
    contextPolicy = TruffleLanguage.ContextPolicy.SHARED)
public class ToylLanguage extends TruffleLanguage<ToylContext> {

  public static final String ID = "toyl";
  public static final String MIME_TYPE = "application/x-toyl";

  @Override
  protected ToylContext createContext(Env env) {
    return new ToylContext();
  }

  @Override
  protected CallTarget parse(ParsingRequest request) throws IOException {
    var statements = this.parseProgram(request.getSource());
    var program = new ToylRootNode(this, new FrameDescriptor(), statements);
    return Truffle.getRuntime().createCallTarget(program);
  }

  private ToylProgramNode parseProgram(Source source) throws IOException {
    var lexer = new ToylLexer(CharStreams.fromReader(source.getReader()));
    var parser = new ToylParser(new CommonTokenStream(lexer));
    var parseTreeVisitor = new ToylParseTreeVisitor();
    return parseTreeVisitor.visitProgram(parser.program());
  }

}
```

## Implementing variables

With all that done we have the skeleton in place for handling
variables although we're not really doing anything with them yet. Our
existing tests should be green, but let's add a simple test for the
use of variables:

```java
  @Test
  void testVariables() {
    var program = """
        var pi = 3.14
        var r = 42
        pi * r * r
        """;
    assertEquals(5538.96, evalDouble(program));
  }

```

This test will fail since we haven't yet written any code to handle
variables. Let's do that!

You may have noticed that we forgot something when we did all the node
refactoring in the previous section. We didn't actually write code to
visit the new assignment and variable reference rules. Let's do
that. To perform variable assignment and referencing variables we will
finally make use of the `VirtualFrame` parameter that gets passed to
all the `execute*` methods. The javadoc for `VirtualFrame` starts with
this:

> Represents a frame containing values of local variables of the guest language

Just what we need! So to see how this must be put together we can
start with the variable reference node. We haven't created this node,
but let's try. Just create a class called `ToylVarRefNode` and make it
extend `ToylExpressionNode` and then add empty bodies for the abstract
methods. Now, if you try to implement `executeGeneric`, how do you do
that? Well, since `VirtualFrame` holds the local variables it sounds
like we should somehow look up the variables from there. So how? If
you look at the javadoc for `VirtualFrame` you'll see it extends
`Frame` which is where all the useful methods are defined. In there is
a bunch of `get*` methods. And as you can see, they all require a
`FrameSlot`. So it seems we need to have a `FrameSlot` available in
our `ToylVarRefNode` somehow. And since we're going to create our node
in `ToylParseTreeVisitor` it probably must come from there. But how do
we get a `FrameSlot`? Well, the javadoc for `FrameSlot` says:

> A slot in a `Frame` and `FrameDescriptor` that can store a value of a given type.

Right.

It would be nice if it said somewhere how to create a
`FrameSlot`. Scroll down a bit in `FrameSlot` and you'll find lots of
references to `FrameDescriptor.addFrameSlot` though, so that's a
clue. So if we have a `FrameDescriptor` it seems we can create
slots. And `FrameDescriptor` has a constructor ... so if we just
create a `FrameDescriptor` field in `ToylParseTreeVisitor` we should
be able to create a `FrameSlot`s. Now, it seems pretty reasonable that
we must create slots when we declare variables, and we must look them
up when we reference a variable. So let's try to implement
`visitAssignment`:

```java
  @Override
  public ToylStatementNode visitAssignment(ToylParser.AssignmentContext ctx) {
    final String name = ctx.NAME().getText();
    var slot = this.frameDescriptor.findOrAddFrameSlot(name);
    return new ToylAssignmentNode(name, slot, (ToylExpressionNode) this.visit(ctx));
  }
```

Ok, and that means we also need to create the `ToylAssignmentNode`:

```java
package toyl.ast;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;

public class ToylAssignmentNode extends ToylStatementNode {
  private final String name;
  private final FrameSlot slot;
  private final ToylExpressionNode expr;

  public ToylAssignmentNode(String name, FrameSlot slot, ToylExpressionNode expr) {
    this.name = name;
    this.slot = slot;
    this.expr = expr;
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    return null;
  }
}
```

So I left that `executeGeneric` method empty. What we need to do here
is of course evaluate the expression node and then assign the
resulting value to a local variable (and remember that's what the
frame is for):

```java
  @Override
  public Object executeGeneric(VirtualFrame frame) {
    var value = this.expr.executeGeneric(frame);
    frame.setObject(this.slot, value);
    return value;
  }
```

Hm. If you tried to write that yourself you probably puzzled a bit
about what `set*` method to call. How to know what type the result of
the expression is? We'll get back to that.

    TODO: Go back and replace this with the right code once I understand how or leave my exporation here for the reader to follow?

With assignment done we can tackle variable referencing. We need to
implement `visitVarRefExpr` and we need to construct a
`ToylVarRefNode` from there. You should definitely try to write these
two classes yourself. And then you can compare with my version below.

Here's `visitVarRefExpr`:

```java
  @Override
  public ToylStatementNode visitVarRefExpr(ToylParser.VarRefExprContext ctx) {
    final String name = ctx.NAME().getText();
    return new ToylVarRefNode(name, this.frameDescriptor.findFrameSlot(name));
  }
```

And here's `ToylVarRefNode`:

```java
package toyl.ast;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;

public class ToylVarRefNode extends ToylExpressionNode {
  private final String name;
  private final FrameSlot slot;

  public ToylVarRefNode(String name, FrameSlot slot) {
    this.name = name;
    this.slot = slot;
  }

  @Override
  public int executeInt(VirtualFrame frame) {
    try {
      return frame.getInt(this.slot);
    } catch (FrameSlotTypeException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public double executeDouble(VirtualFrame frame) {
    try {
      return frame.getDouble(this.slot);
    } catch (FrameSlotTypeException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    return frame.getValue(this.slot);
  }
}
```

Notice that Truffle throws a checked `FrameSlotTypeException` that we
have to catch. Currently our language is so simple that we can't
really mix up types, we only have two types, and Truffle can
implicitly cast our integers into doubles, but we still need to catch
and rethrow this exception.

If we try to run our test now you'll notice that it fails. And if
you're smarter than me you may have noticed something about the
`FrameDescriptor`. I wrote as if this was the first time we saw it and
I just created it inside my `ToylParseTreeVisitor`. But look at
`ToylLanguage.parse` and you'll notice another `new FrameDescriptor()`
call. So the parse tree and interpreter don't share the same
`FrameDescriptor`. How is the runtime going to know about the slots we
created at parse time then?

To fix this we need to create the `FrameDescriptor` in `ToylLanguage`
and make sure it is shared by the parser and the interpreter, so pass
it through `parseProgram` and into `ToylParseTreeVisitor` and make
sure you pass the same instance to `ToylRootNode`. Here's how the two
`parse` and `parseProgram` methods in `ToylLanguage` end up:

```java
  @Override
  protected CallTarget parse(ParsingRequest request) throws IOException {
    final FrameDescriptor frameDescriptor = new FrameDescriptor();
    var statements = this.parseProgram(frameDescriptor, request.getSource());
    var program = new ToylRootNode(this, frameDescriptor, statements);
    return Truffle.getRuntime().createCallTarget(program);
  }

  private ToylProgramNode parseProgram(FrameDescriptor frameDescriptor, Source source) throws IOException {
    var lexer = new ToylLexer(CharStreams.fromReader(source.getReader()));
    var parser = new ToylParser(new CommonTokenStream(lexer));
    var parseTreeVisitor = new ToylParseTreeVisitor(frameDescriptor);
    return parseTreeVisitor.visitProgram(parser.program());
  }
```

With that in place our test should succeed! Awesome! You may also
decide you want to play around with the calculator a bit, and if you
do you'll notice that it immediately prints out the result of the
first expression you enter. Since we now support executing sequences
of statements we need to update the launcher to handle this. That in
itself is pretty trivial, but while doing this we'll also look into
how we can add syntax error messages with correct line numbers. We'll
also look into how we annotate the AST with information about the
source to enable better runtime error messages and be able to step
through our code in the IntelliJ debugger. Onwards to chapter 6!
