grammar Toyl;

program: expr EOF;

expr
    : LITERAL_NUMBER
    | left=expr binaryOp=('+'|'-'|'*'|'/') right=expr
    ;

LITERAL_NUMBER: [0-9]+;
WS    : [ \t\r\n]+ -> skip ;