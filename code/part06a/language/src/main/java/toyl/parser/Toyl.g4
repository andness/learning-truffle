grammar Toyl;

program: statement+ EOF;

statement: (expr|varDecl|assignment);

varDecl: 'var' NAME '=' expr;

assignment: NAME '=' expr;

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
WS    : [ \t\r\n]+ -> skip ;