grammar Toyl;

program: expr EOF;

expr
    : LITERAL_NUMBER
    | left=expr binaryOp=('*'|'/') right=expr
    | left=expr binaryOp=('+'|'-') right=expr
    | parenthesizedExpr
    ;

parenthesizedExpr : '(' expr ')';

LITERAL_NUMBER: [0-9]+;
WS    : [ \t\r\n]+ -> skip ;