grammar Toyl;

program: expr EOF;

expr
    : LITERAL_NUMBER                          #LiteralNumber
    | left=expr binaryOp=('*'|'/') right=expr #ArithmeticExpression
    | left=expr binaryOp=('+'|'-') right=expr #ArithmeticExpression
    | '(' expr ')'                            #ParenthesizedExpr
    ;

LITERAL_NUMBER: [0-9]+;
WS    : [ \t\r\n]+ -> skip ;