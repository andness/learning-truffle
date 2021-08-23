# Handling operator precedence

Operator precedence is very easy to handle in ANTLR. All we need to do
is split our rule in two and order them from highest to lowest
precedence. ANTLR simply picks the first matching rule. So we'll add
our `expr` rule to look like this:

```
expr
    : LITERAL_NUMBER
    | left=expr binaryOp=('*'|'/') right=expr
    | left=expr binaryOp=('+'|'-') right=expr
    ;
```

And then we have to run our `generate_grammar.sh` script. Actually, at
this point I can also mention that the ANTLR plugin for IntelliJ can
be configured so that you can regenerate the parser using the hotkey
Command-Shift-G. First you need to configure the ANTLR plugin using
Tools->Configure ANTLR, and then you can use the hotkey (or use
Tools->Generate ANTLR Recognizer).

Anyway, with the parser regenerated we shouldn't need to make any
changes to our code. Just launch Toyl and enter our previous test
example `1 + 2 * 3`:

```
1+2*3
result = 7
```

Success! 

It is perhaps worth also pointing out that ANTLR is left-associative
by default, so if you try `7 - 4 + 2` you should get 5 `(7 - 4) + 2`
and not `7 - (4 + 2)`.

But if you actually want to calculate `7 - (4 + 2)` you're out of
luck. To fix this, we need to introduce parenthesised expressions into
our grammar. This is a good excercise. Go on and try it, I'll wait! 

But if you don't want try I'll cover it below.

## Adding paranthesized expressions

This is requires a small modification to our grammar. We need to
introduce a `parenthesizedExpr` rule, and we need to add it as an
option to our `expr` rule. It should look like this:

```
expr
    : LITERAL_NUMBER
    | left=expr binaryOp=('*'|'/') right=expr
    | left=expr binaryOp=('+'|'-') right=expr
    | parenthesizedExpr
    ;

parenthesizedExpr : '(' expr ')';
```

This time we need to modify our AST creation. We just need to add the
following method to `ToylParseTreeVisitor`:

```java
  @Override
  public ToylNode visitParenthesizedExpr(ToylParser.ParenthesizedExprContext ctx) {
    return this.visitExpr(ctx.expr());
  }
```

and we also need to ensure that we visit the new paranthesized
expression in our `visitExpr` method, so add this second if:

```java
    if (ctx.parenthesizedExpr() != null) {
      return this.visitParenthesizedExpr(ctx.parenthesizedExpr());
    }
```

Great. Let's try that out:

```
7 - (4 + 2)
result = 1
```

Now at this point you may feel that the code is a little clunky. Why
do we need all those if tests in the `visitExpr` code? Wouldn't it be
nice if we could have separate rules and then just write one method
per rule? Maybe you imagine a grammar like this:

```
expr
    : LITERAL_NUMBER
    | arithmeticExpr
    | parenthesizedExpr
    ;

arithmeticExpr
    : left=expr binaryOp=('*'|'/') right=expr
    | left=expr binaryOp=('+'|'-') right=expr;

parenthesizedExpr : '(' expr ')';
```

You can try pasting that grammar into our grammar file and if you have
the IntelliJ ANTLR Plugin you should immediately see an error
indicator. The problem is that we have left recursion. I'm not going
to write more about this here as there is an excellent explanation of
this problem available at
[Stackoverflow](https://stackoverflow.com/questions/26460013/antlr4-mutually-left-recursive-error-when-parsing). But
I'll show you how we can simplify this code a bit.

## Simplifying the code using alternative labels

The code got a little clunky when we have to add all these if-tests
and if you're wrinkling your nose at it I agree. You can probably
imagine how this is going to grow as we add more language
constructs. The nice thing about the `BaseTreeVisitor` that ANTLR
generates is that we should really only have to implement the "leaf"
methods. We can clean up the code a lot by using labels for the `expr`
rule alternatives. To do this, we add a `#<name>` after each
alternative, like this:

```
expr
    : LITERAL_NUMBER                          #LiteralNumber
    | left=expr binaryOp=('*'|'/') right=expr #ArithmeticExpression
    | left=expr binaryOp=('+'|'-') right=expr #ArithmeticExpression
    | '(' expr ')'                            #ParenthesizedExpr
    ;
```

It's ok to use the same label multiple times for the rules that have
the same structure, and we make use of this to represent our
`ArithmeticExpression`.  Note that I've also deleted the
`parenthesizedExpr` and inlined it into the `expr` rule directly.

We can now simplify our `ToylParseTreeVisitor`. Here's the whole
thing:

```java
package toyl.parser;

import toyl.ast.ToylArithmeticOpNode;
import toyl.ast.ToylLiteralNumberNode;
import toyl.ast.ToylNode;

public class ToylParseTreeVisitor extends ToylBaseVisitor<ToylNode> {
  @Override
  public ToylNode visitProgram(ToylParser.ProgramContext ctx) {
    return this.visit(ctx.expr());
  }

  @Override
  public ToylNode visitParenthesizedExpr(ToylParser.ParenthesizedExprContext ctx) {
    return this.visit(ctx.expr());
  }

  @Override
  public ToylNode visitArithmeticExpression(ToylParser.ArithmeticExpressionContext ctx) {
    return new ToylArithmeticOpNode(this.visit(ctx.left), this.visit(ctx.right), ctx.binaryOp.getText());
  }

  @Override
  public ToylNode visitLiteralNumber(ToylParser.LiteralNumberContext ctx) {
    return new ToylLiteralNumberNode(ctx.LITERAL_NUMBER().getText());
  }
}
```

That's pretty clean!
