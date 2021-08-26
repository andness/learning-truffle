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
	| '-' expr                                #UnaryMinus
    | NAME                                    #VarRefExpr
    ;

LITERAL_NUMBER: [0-9]+('.'[0-9]+)?;
NAME: ([a-z]|[A-Z])+;
WS    : [ \t\r\n]+ -> skip;
```

Ok, time to regenerate our parser and get to work on adapting our
AST. So we're going to run into an immediate problem. Our top level
`visitProgram` method just expects there to be a single `expr` but it
should now handle a list of `statement`. So we'll update the
`visitProgram` method:

```java
  @Override
  public ToylNode visitProgram(ToylParser.ProgramContext ctx) {
    return new ToylProgramNode(ctx.statement().stream().map(this::visit).toList());
  }
```

And we need to make the corresponding change to `ToylProgramNode`:

```java
public class ToylProgramNode extends ToylNode {

  private final List<ToylNode> statements = new ArrayList<>();

  public ToylProgramNode(List<ToylNode> statements) {
    this.statements.addAll(statements);
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    Object result = null;
    for (ToylNode statement : statements) {
      result = statement.executeGeneric(frame);
    }
    return result != null ? result.toString() : null;
  }

}
```

Note that we changed it to inherit from `ToylNode`! Your IDE should
now complain about the class not implementing `executeLong`. We'll fix
that in a second, but first we'll update `ToylLanguage`. We're going
to introduce a new node to represent the root node so create
`src/main/java/toyl/ToylRootNode.java`:

```java
package toyl.ast;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;

public class ToylRootNode extends RootNode {

  private final ToylNode program;

  public ToylRootNode(TruffleLanguage<?> language, FrameDescriptor frameDescriptor, ToylNode program) {
    super(language, frameDescriptor);
    this.program = program;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return program.executeGeneric(frame);
  }
}
```

Next create an instance of this where we currently create
`ToylProgramNode` in `ToylLanguage`:

```java
  @Override
  protected CallTarget parse(ParsingRequest request) throws IOException {
    final FrameDescriptor frameDescriptor = new FrameDescriptor();
    var program = this.parseProgram(frameDescriptor, request.getSource());
    var rootNode = new ToylRootNode(this, frameDescriptor, program);
    return Truffle.getRuntime().createCallTarget(rootNode);
  }

  private ToylNode parseProgram(FrameDescriptor frameDescriptor, Source source) throws IOException {
    var lexer = new ToylLexer(CharStreams.fromReader(source.getReader()));
    var parser = new ToylParser(new CommonTokenStream(lexer));
    var parseTreeVisitor = new ToylParseTreeVisitor(frameDescriptor);
    return parseTreeVisitor.visitProgram(parser.program());
  }
```

Phew. Nearly there. I mentioned we also needed to fix the complaints
in `ToylProgramNode` about it not implementing `executeLong`. We
actually want to move these methods from `ToylNode` and down one
level. To do this we'll introduce a new `ToylExpression` node which
will be the parent for all expressions and looks like this:

```java
package toyl.ast;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import java.math.BigDecimal;

public abstract class ToylExpressionNode extends ToylNode {
  public abstract long executeLong(VirtualFrame frame) throws UnexpectedResultException;
  public abstract BigDecimal executeNumber(VirtualFrame frame);
}
```

Lastly, delete the same methods from the `ToylNode` class and update
all the expression classes to inherit from `ToylExpressionNode`
instead of `ToylNode`. This will break things in
`ToylParseTreeVisitor` so we need to make some casts there. First,
`visitArithmeticExpression`:

```java
  @Override
  public ToylNode visitArithmeticExpression(ToylParser.ArithmeticExpressionContext ctx) {
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

And as the final fix you need to add a cast in `visitUnaryMinus`. Ok,
that was admittedly a lot of refactoring needed just to get to state
where we can write the actual code for supporting variables, but we're
there!

## Implementing variables

What better way to start than by writing a failing test:

```java
  @Test
  void testVariables() {
    var program = """
        var pi = 3.14
        var r = 42
        pi * r * r
        """;
    assertEquals("5538.96", eval(program));
  }
```

This test will fail since we haven't yet written any code to handle
variables. We'll start by updating `ToylParseTreeVisitor` to handle
the two new rules `assignment` and `varRef`. To perform variable
assignment and referencing variables we will finally make use of the
`VirtualFrame` parameter that gets passed to all the `execute*`
methods. The javadoc for `VirtualFrame` starts with this:

> Represents a frame containing values of local variables of the guest language

Just what we need! We'll need two new node classes to handle these two
new rules, `ToylAssignmentNode` and `ToylVarRefNode`. But what fields
do the classes need? Looking at the javadoc for `VirtualFrame` we see
that it inherits a bunch of `get*` methods from `Frame`, all of which
requires a `FrameSlot`. A `FrameSlot` is described as:

> A slot in a `Frame` and `FrameDescriptor` that can store a value of a given type.

Let's try creating a skeleton `ToylAssignmentNode`:

```java
package toyl.ast;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;

public class ToylAssignmentNode extends ToylNode {
  private final String name;
  private final FrameSlot slot;
  private final ToylNode expr;

  public ToylAssignmentNode(String name, FrameSlot slot, ToylNode expr) {
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

That seems pretty reasonable, we'll keep track of the name of the
variable we're storing, the expression that produces a value, and the
slot we need to store the result in. So now we need to create this
node in our `ToylParseTreeVisitor`:

```java
  @Override
  public ToylNode visitAssignment(ToylParser.AssignmentContext ctx) {
    final String name = ctx.NAME().getText();
    var slot = this.frameDescriptor.findOrAddFrameSlot(name);
    return new ToylAssignmentNode(name, slot, this.visit(ctx.expr()));
  }
```

To get a slot we use a `frameDescriptor`. Where did that come from?
Well, we need to add it to our `ToylParseTreeVisitor` on creation, so
first add this code at the start of the class:

```java
  private FrameDescriptor frameDescriptor;

  public ToylParseTreeVisitor(FrameDescriptor frameDescriptor) {
    this.frameDescriptor = frameDescriptor;
  }
```

And then we need to create it in `ToylLanguage` and pass it into our
`new ToylParseTreeVisitor` call. Note that we're going to create the
`FrameDescriptor` in `parse` and pass it into `parseProgram`. This is
necessary because we want to use the `frameDescriptor` we're passing
to our `ToylRootNode`:

```java
  @Override
  protected CallTarget parse(ParsingRequest request) throws IOException {
    final FrameDescriptor frameDescriptor = new FrameDescriptor();
    var statements = this.parseProgram(frameDescriptor, request.getSource());
    var program = new ToylRootNode(this, frameDescriptor, statements);
    return Truffle.getRuntime().createCallTarget(program);
  }

  private ToylNode parseProgram(FrameDescriptor frameDescriptor, Source source) throws IOException {
    var lexer = new ToylLexer(CharStreams.fromReader(source.getReader()));
    var parser = new ToylParser(new CommonTokenStream(lexer));
    var parseTreeVisitor = new ToylParseTreeVisitor(frameDescriptor);
    return parseTreeVisitor.visitProgram(parser.program());
  }
```

Now we can go back to `ToylAssignmentNode` and complete the
`executeGeneric` method there:

```java
  @Override
  public Object executeGeneric(VirtualFrame frame) {
    var value = this.expr.executeGeneric(frame);
    frame.setObject(this.slot, value);
    return value;
  }
```

You're probably wondering if calling `setObject` is the right thing to
do here, after all, the result of the expression can be a `long` or a
`BigDecimal`. And you're right, this is not the final version. We'll
return to that. But first we'll turn our attention to handling
variable references. Let's start by adding support for it in
`ToylParseTreeVisitor`:

```java
  @Override
  public ToylNode visitVarRefExpr(ToylParser.VarRefExprContext ctx) {
    final String name = ctx.NAME().getText();
    return new ToylVarRefNode(name, this.frameDescriptor.findFrameSlot(name));
  }
```

You can see how we get the slot using the variable name with the
`findFrameSlot` method. Now we can create
`src/main/java/language/ast/ToylVarRefNode.java`:

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
  public long executeLong(VirtualFrame frame) {
    try {
      return frame.getLong(this.slot);
    } catch (FrameSlotTypeException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public BigDecimal executeNumber(VirtualFrame frame) {
    try {
      return (BigDecimal) frame.getObject(this.slot);
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

Run the tests now, and you should see them come out green. We have
variables! Yay!

Notice that Truffle throws a checked `FrameSlotTypeException` that we
have to catch. Currently our language is so simple that we can't
really mix up types, we only have two types, and Truffle can
implicitly cast our longs into BigDecimal, but we still need to catch
and rethrow this exception.

With that in place our test should succeed! Awesome! You may also
decide you want to play around with the calculator a bit using the
Launcher, and if you do you'll notice that it immediately prints out
the result of the first expression you enter. Since we now support
executing sequences of statements we need to update the launcher to
handle this. That in itself is pretty trivial, but while doing this
we'll also look into how we can add syntax error messages with correct
line numbers. We'll also look into how we annotate the AST with
information about the source to enable better runtime error messages
and be able to step through our code in the IntelliJ debugger. We'll
cover that in part 6. But first we're going specialize our variable
references.

