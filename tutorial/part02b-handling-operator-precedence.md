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
