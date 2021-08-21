# Introduction

If you've ever tried to implement your own programming language, or
you have tried to implement an existing languages you have probably
found that it is a quite involved task. Aside from the lexing and
parsing you need to decide if you want to write an interpreter or a
compiler. Interpreters are easier, and easiest to write is the AST
interpreter. But AST interpreters are slow. So if you want something
that has acceptable performance you have to either come up with some
form of bytecode and VM to interpret it, or you target an existing VM
like the JVM.

Truffle is a framework that promises both the implementation ease of
an AST interpreter and the performance of a JIT compiled bytecode
interpreter. That sounds too good to be true, but there are
implementations of existing languages that prove this, like
TruffleRuby which outperforms JRuby in many cases.

In my view the most fun part of designing a programming language is
exploring the design space of the language itself, and all the
complexities of lexers and parsers and byte codes and interpreters and
compilers and JIT and everything can just get a little too much. It's
easy to get stuck. But with a wonderful tool like ANTLR the complexity
of lexing and parsing vanishes, and now, with Truffle, the language
implementation part can potentially become a lot easier.

And that's not all. If you write your own language you also have to
build the whole toolchain. With Truffle you can get debugging support
almost for free, and you can even get programs in your language
compiled into native images!

## Overview of this tutorial

We'll start with setting up the tools and the development
environment. Then we'll implement the first version of our language
which is nothing more than a trival calculator. Thanks to the power of
ANTLR we don't have to slog through the toil of lexing and parsing
just to get to the fun part of seeing the language design come to
life. Now don't get me wrong, lexing and parsing can be a lot of fun
and is an interesting area of study by itself, but there are other
places you can learn about that.

This tutorial will instead focus on incrementally growing the language
feature by feature. We'll start with just simple arithmetic
expressions and then add variables, functions, conditionals, loops,
closures (and objects). This approach lends itself to a gradual
introduction to the Truffle Framework.

## Why this tutorial?

First and foremost because I want to learn Truffle myself and I was a
little frustrated by the existing material. If you go to the Truffle
website you'll find that they direct you to the simplelanguage Github
project. That's a very comprehensive coverage of a lot of Truffle
features, but to someone new to Truffle (and perhaps programming
language implementation in general) it's quite overwhelming. I wanted
to learn in a more bottom up fashion, understanding why each part is
done the way it is, and to do that the learning has to happen a lot
more incrementally.

I suppose I was also influenced by having seen the work done by Robert
Nystrom on "Crafting Interpreters"
(https://craftinginterpreters.com/). While I have no ambitions of
producing something as comprehensive as that I found his story of how
he developed the book to be very inspiring.
