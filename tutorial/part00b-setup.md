# Setting up the environment

Truffle is a Java project so we need to set up a good Java development
environment. To follow along you'll need:

- An IDE. I'll use IntelliJ so all configuration specifics will be for IntelliJ.
- Maven
- GraalVM (a special version of the JDK) 
- git

You can find a free IntelliJ download here:
https://www.jetbrains.com/idea/download/ (pick the Community Edition)

Maven installation instructions are here:
https://maven.apache.org/install.html

And you can find GraalVM here: https://www.graalvm.org/downloads/
(again, pick the Community Edition). You should choose the one based
on JDK 16 as that what the rest of the tutorial assumes.

At the time of writing this tutorial, GraalVM is at version 21.2 but
my working assumption is that you'll be able to use future versions as
they are released.

There's a git repository containing the code for this entire tutorial
at https://github.com/andness/learning-truffle which you can clone if
you want to. You'll be recreating all the code that's in there though
so it's not necessary.

Now we're ready to get going. I suggest you create yourself a git
repository to hold the code you'll write. Let's go!
