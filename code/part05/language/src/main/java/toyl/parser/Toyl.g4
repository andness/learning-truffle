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