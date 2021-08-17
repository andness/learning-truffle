# Introduction

Hi and welcome to my tutorial on Truffle. Truffle is a really cool
framework for creating programming languages. Whether you want to
write your own, or you want to implement some existing language.

I'm a bit of a programming language geek, and I've had several stalled
attempts at implementing a programming language. There's a lot
involved of course, you have to figure out the design, write a lexer
and a parser, then perhaps try to compile it, or if you want a
slightly less daunting task you go for an interpreter. But it's a lot
of work.

In my view the most fun part of designing a programming language is
exploring the design space of the language itself, and all the
complexities of lexers and parsers and byte codes and interpreters and
compilers and JIT and ... aaaaaa. The thing is, today there are tools
that take much of the complexity out of this. With ANTLR as the parser
generator you can have a lexer and parser created from an easy to
understand grammar file. To implement your language your best bet then
is to write an interpreter. And that's where Truffle comes in. The
magic of truffle is that all you need to do is write the interpreter,
and then Truffle will take the work of turning that into a compiler
for you. It almost sounds to good to be true.

## Overview of this tutorial

We'll start with setting up the tools and the development
environment. With that in place we'll implement the first version of
our language which is nothing more than a trival calculator. Thanks to
the power of ANTLR and Truffle we don't have to slog through the toil
of lexing and parsing just to get to the fun part at the very
end. Instead we'll build the language feature by feature, adding
variables, functions, conditionals, loops, closures (and objects) in a
gradual way that that will gradually exposes you to Truffle feature
set.

## Why this tutorial?

First and foremost because I want to learn Truffle myself and I was a
little frustrated by the lack of tutorials. If you go to the Truffle
website you'll find that they direct you to the simplelanguage Github
project. That's a very comprehensive coverage of a lot of Truffle
features, but someone new to Truffle (and perhaps programming language
implementation in general) it's overwhelming. I wanted to learn in a
more bottom up fashion, understanding why each part is done the way it
is, and to do that the learning has to happen a lot more
incrementally.
