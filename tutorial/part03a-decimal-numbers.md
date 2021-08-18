# Decimal numbers

So far our language can only handle integers which means that `1/4`
produces 0. And we can't even enter decimal numbers. This cannot
stand! Let's see what we can do to remedy this. First of all, we'll
need to extend the grammar tosupport decimal numbers. Secondly, we
need to extend our interpreter to perform the calculations using
decimals instead of integers.

Let's start with the grammar. We need to change our definition of the
`LITERAL_NUMBER` token:

```
LITERAL_NUMBER: [0-9]('.'?[0-9]+)?;
```

And we we need to update the code to handle these decimals. This
should be a simple exercise for you, the reader. Modify the
interpreter to use `double` instead of `long`!

If you get this right, you should be able to reproduce this result:

```
1/4
result = 0.25
```

But you may also notice something. If you enter something trivial,
like `2+2` the result is now `2.0` instead of simply `2`. One way of
fixing this is to convert the result into a BigDecimal in
`ToylProgramNode.execute`. But if you attempt to do that and return
the `BigDecimal` you'll get this error:

> Exception in thread "main" org.graalvm.polyglot.PolyglotException: java.lang.AssertionError: Language toyl returned an invalid return value 1. Must be an interop value.

The problem is that Truffle doesn't support all possible values since
it also needs to ensure interop with other Truffle languages. We
haven't talked much about the polyglot aspect of Truffle, but there
you are.
