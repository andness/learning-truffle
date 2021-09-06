# Source code information and debugging

As I wrote at the end of chapter 5 we don't yet support reading
multiple lines of code in our launcher. We also don't support reading
from a file which is going to be pretty useful as our programs
grow. Furthermore, if you make a syntax mistake we don't really handle
that and everything just crashes. Let's improve that!

## Support multiple lines and reading from file

We'll start by adding support for multiple lines and file
sources. We'll change our `Launcher` so that it accepts a filename but
will read from standard in in the absence of one. You should try
making this change yourself.

Hint: `org.graalvm.polyglot.Source.newBuilder()`

You should end up with something like this:

```java
  public static void main(String[] args) throws IOException {
    Source source = null;
    if (args.length == 0) {
      var lineReader = new BufferedReader(new InputStreamReader(System.in));
      source = Source.newBuilder(ToylLanguage.ID, lineReader, "stdin").build();
    } else {
      var file = new File(args[0]);
      source = Source.newBuilder(ToylLanguage.ID, file).build();
    }
    var context = Context.newBuilder(ToylLanguage.ID).build();
    Value result = context.eval(source);
    System.out.println("result = " + result);
  }
```

The `Source` class that comes with Truffle makes it quite easy to
handle this. One thing to note now is that if you try to run the
launcher with no file argument you can enter multiple statements, but
if you're using standard IntelliJ you can't send a EOF using the
standard Control-D, you have to use Command-D as Control-D is used to
run debug configurations.

## Handling syntax errors

If you try to run the following program:

```
var a = 33
var b * 4
```

You'll get this output:

```
line 2:6 mismatched input '*' expecting '='
Exception in thread "main" org.graalvm.polyglot.PolyglotException: java.lang.NullPointerException: Cannot invoke "org.antlr.v4.runtime.tree.ParseTree.accept(org.antlr.v4.runtime.tree.ParseTreeVisitor)" because "tree" is null
```

You can see there's some information about a syntax error being
printed, but then execution seemingly stumbles on and finally crashes
and burns when trying to build the AST. What is happening here is that
ANTLR ships with a default error strategy that will print all errors
it encounters but always try to continue. We need to replace it with
one that exits immediately. So doing that is not hard, we need to
disable the existing error listeners on our parser and lexer (if you
look at their interface I'm sure you'll figure out how) and then add a
custom one that will throw an error that is compatible with the
Truffle framework.

But before we start changing the code we'll add a test with a syntax
error in it to verify that it's not currently working:

```java
  @Test
  void testSyntaxError() {
    var program = "var a = * 3";
    var error = assertThrows(PolyglotException.class, () -> eval(program));
    assertTrue(error.getMessage().startsWith("Parse error"));
  }
```

Now we have a red test and we can write the code to turn it green.

If you look at how our code is organized we have our Launcher which
creates a Truffle context and calls `eval`. And then the Truffle
framework code is responsible for calling our `ToylLanguage.parse`
method. Handling the error seems to be the responsibility of our
Launcher, after all that's the right place to exit from the
program. We don't want to drop a `System.exit` call in the middle of
`parse`!

We've already handled a semantic error. For that we created a class
derived from `AbstractTruffleException`. We can do the same to handle
parser errors. Let's create
`src/main/java/toyl/errors/ToylParseError.java`:

```java
package toyl.errors;

import com.oracle.truffle.api.exception.AbstractTruffleException;

public final class ToylParseError extends AbstractTruffleException {
  private final int line;
  private final int column;

  public ToylParseError(int line, int column, String error) {
    super("Parse error on line %s, position %s: %s".formatted(line, column, error));
    this.line = line;
    this.column = column;
  }
}
```

Unlike our `ToylSemanticError` though this new class takes in a line
and a column in the constructor. We'll use this to give the source
location for the error. To do that though we'll need to hook into the
ANTLR error listener mechanism. The easiest way to do that is to
extend `BaseErrorListener`. Since this error listener is part of
parsing I'll add it as
`src/main/java/toyl/parser/ToylErrorListener.java`:

```java
package toyl.parser;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import toyl.errors.ToylParseError;

public class ToylErrorListener extends BaseErrorListener {
  @Override
  public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
    throw new ToylParseError(line, charPositionInLine, msg);
  }
}
```

And to tie it all together you have to disable the default error
listeners on the lexer and parser and add in ours, so
`ToylLanguage.parseProgram` becomes:

```java
  private ToylNode parseProgram(FrameDescriptor frameDescriptor, Source source) throws IOException {
    var lexer = new ToylLexer(CharStreams.fromReader(source.getReader()));
    var parser = new ToylParser(new CommonTokenStream(lexer));
    lexer.removeErrorListeners();
    parser.removeErrorListeners();
    final ToylErrorListener errorListener = new ToylErrorListener();
    lexer.addErrorListener(errorListener);
    parser.addErrorListener(errorListener);
    var parseTreeVisitor = new ToylParseTreeVisitor(frameDescriptor);
    return parseTreeVisitor.visitProgram(parser.program());
  }
```

At this point our test should actually be green. But if you try to run
a program through the Launcher and enter a syntax error the result
isn't quite what we want:

```
var a = * 2
^D
Exception in thread "main" Parse error on line 1, position 8: extraneous input '*' expecting {'-', '(', LITERAL_NUMBER, NAME}
	at org.graalvm.sdk/org.graalvm.polyglot.Context.eval(Context.java:375)
	at toyl.Launcher.main(Launcher.java:25)
```

We need to catch the error in the Launcher and print the relevant
message and exit with a status code. To do that, replace the code that
calls `context.eval` with this:

```java
  try {
      Value result = context.eval(source);
      System.out.println("result = " + result);
    } catch (PolyglotException error) {
      if (error.isInternalError()) {
        error.printStackTrace();
      } else {
        System.err.println(error.getMessage());
      }
      System.exit(1);
    }
```	

And now we get the desired result:

```
var a = * 2
^D
Parse error on line 1, position 8: extraneous input '*' expecting {'-', '(', LITERAL_NUMBER, NAME}

Process finished with exit code 1
```

Having written that I figured it would be really nice if the error
output included the failing code line with the error and a little
arrow underneath to point at the problem, like this:

```
var b * 4
      ^
```

So I decided to add that. And it's something you can try doing too if
you want, otherwise you'll find it in the source code for this
chapter.

## Fixing a bug

While testing out my syntax error handling I accidentally ran into a
bug. You might already have noticed and perhaps even fixed this, but
either way, let's write a test to show the problem:

```java
  @Test
  void testUseOfUndeclaredVariable() {
    var program = "a * 1";
    var error = assertThrows(PolyglotException.class, () -> eval(program));
    assertThat(error.getMessage()).startsWith("Use of undeclared variable a");
  }
```

This will fail as follows:

```
java.lang.AssertionError: 
Expecting actual:
  "java.lang.NullPointerException: Cannot invoke "com.oracle.truffle.api.frame.FrameSlot.getIndex()" because "slot" is null"
to start with:
  "Unknown variable 'a'"
```

Oops. We need to fix our `visitVarRefNode` method, it currently just
looks up the slow with no checking on whether it exists or not:

```java
  @Override
  public ToylNode visitVarRefExpr(ToylParser.VarRefExprContext ctx) {
    final String name = ctx.NAME().getText();
    return ToylVarRefNodeGen.create(name, this.frameDescriptor.findFrameSlot(name));
  }
```

The fix is simple enough, we just need to check if `findFrameSlot`
actually finds a slot!

```java
  @Override
  public ToylNode visitVarRefExpr(ToylParser.VarRefExprContext ctx) {
    final String name = ctx.NAME().getText();
    var slot = this.frameDescriptor.findFrameSlot(name);
    if (slot == null) {
      throw new ToylSemanticError("Use of undeclared variable " + name);
    }
    return ToylVarRefNodeGen.create(name, this.frameDescriptor.findFrameSlot(name));
  }
```

Great. But wait a minute! The semantic error has no location
information! We'll fix that in the next section, and in the process
we'll learn a little about the Truffle Interop Library.
