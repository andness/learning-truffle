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
