# Creating a calculator

In this chapter 2 of the tutorial we'll extend our language so that it
can actually do something useful. We'll implement a simple calculator!
We could of course do that by writing a simple parser by hand, but
because the grammar for these expressions is so simple it's a good way
to introduce you to ANTLR.

## Our new language features

So far the only thing our language can do is evaluate and print a
single number. We will now extend it with the basic arithmetic
operators, addition, subtraction, multiplication and division. We'll
still just support a single expression. So a valid Toyl program
execution can now look like this:

```
14 * 4 + 7 - 21
result = 42
```

### Generating a parser using ANTLR

[ANTLR](https://www.antlr.org/) (ANother Tool for Language
Recognition) is a parser generator. It makes the creation of our
parser a whole lot simpler.

To get started using ANTLR we will use the ANTLR tool to generate our
parser from a grammar file.

## Our first Grammar

The convention for ANTLR grammars is to use a `.g4` file
extension. We'll place the grammar file together with parser code that
we'll generate from the grammer, so let's create the directory for
that parser package:

```
mkdir language/src/main/java/toyl/parser
```

Now we can create `language/src/main/java/toyl/parser/Toyl.g4`:

```
grammar Toyl;

program: expr EOF;

expr
    : LITERAL_NUMBER                        # exprLiteralNumber
    | expr binaryOp=('+'|'-'|'*'|'/') expr  # exprBinaryOp
    ;

LITERAL_NUMBER: [0-9]+;
WS    : [ \t\r\n]+ -> skip ;
```

Let's spend a little bit of time breaking this down. The declaration
starts with naming the grammar. This will be used as a class prefix
for all the generated classes, so we'll end up with classes like
`ToylLexer` and `ToylParser`. Next comes our first rule named
`program`. This states that a program consists of a single `expr`
followed by End-Of-File which is a special built-in token.

At the bottom of the grammar are the other tokens. We only have two,
`LITERAL_NUMBER` which consists of one or more digits, and the `WS`
token which captures whitespace. Notice the ` -> skip` which tells
ANTLR to simply discard whitespace.

The `expr` rule states that an expr can either be a literal number, or
two expressions combined using a binary operator, which can be one of
the alternatives separated by `|`. Also note that we've used a label
for the operator alternatives.

## Generating the parser 

To generate a grammar we need to run ANTLR with this grammar as
input. We're going to tell ANTLR to generate a Visitor based parser
(and to not generate the default Listener variant). We also need to
tell it where to generate the code. So the command will look like this
(assuming we execute in the `language` module root):

```
java -Xmx500m -cp target/antlr-4.9.2-complete.jar org.antlr.v4.Tool -package toyl.parser -no-listener -visitor src/main/java/toyl/parser/Toyl.g4
```

But wait, first we need to download the ANTLR complete jar. To make
this easier I've wrapped this in a little shell script called
`generate_parser.sh` which downloads the jar file on demand:

```bash
#!/bin/bash
ANTLR=target/antlr-4.9.2-complete.jar
if [ ! -f "$ANTLR" ]; then
  echo "ANTLR jar not found, downloading..."
  curl --output $ANTLR https://www.antlr.org/download/antlr-4.9.2-complete.jar
fi
java -Xmx500m -cp target/antlr-4.9.2-complete.jar org.antlr.v4.Tool -package toyl.parser -no-listener -visitor src/main/java/toyl/parser/Toyl.g4
```

Run that script and you should see a bunch of files being generated
into the `src/main/java/toyl/parser/` package. If you open up
`ToylVisitor.java` you can inspect the generated code. If you're very
sharp you may notice something odd. There's no errors. But there
should be an import error the line that reads:

```
import org.antlr.v4.runtime.tree.ParseTreeVisitor;
```

After all, we haven't yet updated our POM! The reason for this is that
the `truffle-dsl-processor` includes ANTLR. But I'd like that
dependency to be explicit so we'll add it to `language/pom.xml`:

```xml
    <dependency>
      <groupId>org.antlr</groupId>
      <artifactId>antlr4-runtime</artifactId>
      <version>4.9.2</version>
    </dependency>
```

### Sidenote: Adding ANTLR tooling to IntelliJ

I would recommend that you add the ANTLR plugin to your IntelliJ if
you haven't already got it. It makes working with ANTLR grammars a lot
easier. One of the coolest features it has is the ability to visually
explore the resulting parse tree for any expression you type in. Very
useful as the grammars become a little bit more complex.

## From parse tree to AST

Now we have an ANTLR parser and we can use it to produce a parse tree
from a source code file. Then we must turn the parse tree into an AST
which we can then execute.

Let's start by adding the code needed to parse the source code. We'll
replace the `parse` method in `ToylLanguage`:

```java
  @Override
  protected CallTarget parse(ParsingRequest request) throws IOException {
    var expr = this.parseExpr(request.getSource());
    var program = new ToylProgramNode(this, new FrameDescriptor(), expr);
    return Truffle.getRuntime().createCallTarget(program);
  }
```

So all we really did was push the problem into a new `parseExpr`
method, which is another method in `ToylLanguage` that must be added,
it looks like this:

```java
  private ToylNode parseExpr(Source source) throws IOException {
    var lexer = new ToylLexer(CharStreams.fromReader(source.getReader()));
    var parser = new ToylParser(new CommonTokenStream(lexer));
	var parseTreeVisitor = new ToylParseTreeVisitor();
    return parseTreeVisitor.visit(parser.program());
  }
```

Here we meet the ANTLR generated code. `ToylLexer` is the lexer
generated by ANTLR, and `ToylParser` is the parser. The result of
lexing and parsing is a parse tree. Since our root grammar rule is
called `program` there is a method on the parser called `program` too,
and we invoke it to get the root of the parse tree and pass to our
parse tree visitor, `ToylParseTreeVisitor`. This is a new class that
we need to write. The job of the parse tree visitor is to walk the
parse tree and convert it to an AST which we can then execute.

So now we need to create
`src/main/java/toyl/parser/ToylParseTreeVisitor.java`:

```java
package toyl.parser;

import toyl.ast.ToylArithmeticOpNode;
import toyl.ast.ToylLiteralNumberNode;
import toyl.ast.ToylNode;

public class ToylParseTreeVisitor extends ToylBaseVisitor<ToylNode> {
  @Override
  public ToylNode visitProgram(ToylParser.ProgramContext ctx) {
    return this.visitExpr(ctx.expr());
  }

  @Override
  public ToylNode visitExpr(ToylParser.ExprContext ctx) {
    if (ctx.LITERAL_NUMBER() != null) {
      return new ToylLiteralNumberNode(ctx.LITERAL_NUMBER().getText());
    }
    var left = this.visit(ctx.left);
    var right = this.visit(ctx.right);
    return new ToylArithmeticOpNode(left, right, ctx.binaryOp.getText());
  }
}
```

ANTLR has generated a `ToylBaseVisitor` for us which is a good
starting point for implementing our own visitor.  Our parse tree
visitor follows the structure of the grammar pretty closely. Since our
grammar starts with the `program` rule which consists of a single
`expr` we simply call `this.visitExpr(ctx.expr())` to visit that
subtree. Inside `visitExpr` we recursively visit the left and right
sub-trees. You can see how the `left` and `right` labels we added in
our grammar file are available as fields on the `ExprContext`
object. When we hit a `LITERAL_NUMBER` the recursion ends.

So our visitor is creating an AST which of course consists of AST
nodes, but we haven't yet defined them. Let's do that now. First,
we'll create an abstract base node that all our nodes inherit from in
`src/main/java/toyl/ast/ToylNode.java`:

```java
package toyl.ast;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public abstract class ToylNode extends Node {
  public abstract long execute(VirtualFrame frame);
}
```

And then we need a node to represent a literal number and a our
expressions. First, our literal number in
`src/main/java/toyl/ast/ToylLiteralNumberNode.java`:

```java
package toyl.ast;

import com.oracle.truffle.api.frame.VirtualFrame;

public class ToylLiteralNumberNode extends ToylNode {
  private final long value;
  public ToylLiteralNumberNode(String number) {
    this.value = Long.parseLong(number);
  }

  @Override
  public long execute(VirtualFrame frame) {
    return this.value;
  }
}
```

And then our expression node which we'll goes into
`src/main/java/toyl/ast/ToylArithmeticOpNode.java`:

```java
package toyl.ast;

import com.oracle.truffle.api.frame.VirtualFrame;

public class ToylArithmeticOpNode extends ToylNode {

  private final ToylNode left;
  private final ToylNode right;
  private final String binaryOp;

  public ToylArithmeticOpNode(ToylNode left, ToylNode right, String binaryOp) {
    this.left = left;
    this.right = right;
    this.binaryOp = binaryOp;
  }

  @Override
  public long execute(VirtualFrame frame) {
    var leftVal = this.left.execute(frame);
    var rightVal = this.right.execute(frame);
    return switch (this.binaryOp) {
      case "+" -> leftVal + rightVal;
      case "-" -> leftVal - rightVal;
      case "*" -> leftVal * rightVal;
      case "/" -> leftVal / rightVal;
      default -> throw new AssertionError("Unexpected operator " + this.binaryOp);
    };
  }
}
```

I think these classes are pretty self-explanatory. We execute the AST
by recursively calling `execute` on the nodes. Now there's just small
piece missing before we can execute our code. IntelliJ should be
complaining about the `new ToylProgramNode` in `ToylLanguage` because
we've changed the argument list for it. We need to update
`ToylProgramNode.java` to look like this:

```java
package toyl.ast;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;

public class ToylProgramNode extends RootNode {

  private final ToylNode expr;

  public ToylProgramNode(TruffleLanguage<?> language, FrameDescriptor frameDescriptor, ToylNode expr) {
    super(language, frameDescriptor);
    this.expr = expr;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return this.expr.execute(frame);
  }
}
```

Where we previously "cheated" by just passing in the entire program as
a string we're now passing in the AST root which is the root
expression we get from our parse tree visitor.

So now we're finally ready to test our wonderful little language:

```
42 + 42
result = 84
```

Awesome! Let's try that again with a bit more complexity:

```
1 + 2 * 3
result = 9
```

Wait a minute now... Shouldn't the result of that be 7? What's going
on? You probably have an idea. Our language is naively executing this
expression as `(1 + 3) * 2`, but it should instead do it as `1 + (3 *
2)`. This is the problem of [operator
precedence](https://en.wikipedia.org/wiki/Order_of_operations).

To handle this, we're going to modify our grammar slightly.
