#!/bin/bash
ANTLR=antlr-4.9.2-complete.jar
if [ ! -f "$ANTLR" ]; then
  echo "ANTLR jar not found, downloading..."
  curl --output $ANTLR https://www.antlr.org/download/antlr-4.9.2-complete.jar
fi
java -Xmx500m -cp antlr-4.9.2-complete.jar org.antlr.v4.Tool -package toyl.parser -no-listener -visitor src/main/java/toyl/language/parser/Toyl.g4
