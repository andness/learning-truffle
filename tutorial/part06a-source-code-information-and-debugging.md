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
it encounters but always try to continue. We need to replace that with
one that exits immediately. So doing that is not hard, we need to
disable the exiting error listeners on our parser and lexer (if you
look at their interface I'm sure you'll figure out how) and then add a
custom one that will throw an error that is compatible with the
Truffle framework.

If you look at how our code is organized we have our Launcher which
creates a Truffle context and calls `eval`. And then the Truffle
framework code is responsible for calling our `ToylLanguage.parse`
method. Handling the error seems to be the responsibility of our
Launcher, after all that's the right place to exit from the
program. We don't want to drop a `System.exit` call in the middle of
`parse`!

To handle errors we can start with `AbstractTruffleException` which
provides a good starting point. The javadoc even includes an example
of how to handle syntax errors using this class, so all we need to do
is follow that recipe. Give it a shot!

Ok you're probably able to adapt the example, but you may have some
questions about the resulting code. I sure did! What is
`@ExportLibrary`? And what is `@ExportMessage`? I tried reading the
javadoc for `@ExportLibrary` and my eyes glazed over pretty quickly

> Allows to export messages of Truffle libraries. The exported library
> value specifies the library class that is exported. If there are
> abstract methods specified by a library then those messages need to
> be implemented. A receiver may export multiple libraries at the same
> time, by specifying multiple export annotations. Subclasses of the
> receiver type inherit all exported messages and may also be exported
> again. In this case the subclass overrides the base class export.

Oh

I *think* this is something that isn't really important until we start
supporting interop with other GraalVM languages, but I honestly don't
understand. So I decided to strip away this stuff and keep it dead
simple until I understand why the extra stuff is needed. So here's my
`src/main/java/toyl/ToylParseError.java`:

```java
package toyl;

import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.interop.ExceptionType;

final class ToylParseError extends AbstractTruffleException {
  private final int line;
  private final int column;
  
  ToylParseError(int line, int column, String error) {
    super("Parse error on line %s, position %s: %s".formatted(line, column, error));
    this.line = line;
    this.column = column;
  }

  ExceptionType getExceptionType() {
    return ExceptionType.PARSE_ERROR;
  }
}
```

I also added a `ToylErrorListener` as a nested class in `ToylLanguage`:

```java
  private static class ToylErrorListener extends BaseErrorListener {
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
  private ToylProgramNode parseProgram(FrameDescriptor frameDescriptor, Source source) throws IOException {
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

But having written that I figured it would be really nice if the error
output included the code line with the error and a little arrow
underneath to point at the problem, like this:

```
var b * 4
      ^
```

So I decided to add that. And it's something you can try doing too if
you want, otherwise you'll find it in the source code for this chapter.

## Integrating with the IDE debugger

