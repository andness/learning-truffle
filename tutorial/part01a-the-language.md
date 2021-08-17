# The language

A language has a name. Ours is going to be called Toyl, which is short
for Toy Language. And also, even though we'll be standing on the
shoulders of giants, it's hard work implementing a new programming
language, so the play on the word toil is fun. At least I thought so
when I came up with it.

The first version of our language isn't going to be very impressive
though. A valid Toyl 0.1 program will consist of a single number, and
executing it will simply return that number.

What? That's ridiculous!

I suppose. But you see, there's quite a bit of code needed to get that
far, and it's going to be a lot easier to explain that code without
having to deal with a lot of details of the language in addition. It
let's us start by explaining some of the basic Truffle machinery
before we start digging into the details of Lexers and Parsers and
Abstract Syntax Trees.

So, our first valid Toyl program is going to look like this:

```
42
```

And the result of executing it should be this:

```
result=42
```

Ok. Knuckles cracked? Good! Let's go!

## Preparing the project

We'll organize the project into two main modules to begin with. One is
the language an interpreter itself which is where we'll do most of our
coding, and the other is the launcher which is used to execute
programs.

The first we'll create is a pom file for the parent project which
references both modules. Place the following in `pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>

<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>toyl</groupId>
  <artifactId>toyl-parent</artifactId>
  <version>1.0-SNAPSHOT</version>

  <properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <graalvm.version>21.2.0</graalvm.version>
    <maven.compiler.target>16</maven.compiler.target>
    <maven.compiler.source>16</maven.compiler.source>
  </properties>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>toyl</groupId>
        <artifactId>language</artifactId>
        <version>1.0-SNAPSHOT</version>
      </dependency>
      <dependency>
        <groupId>toyl</groupId>
        <artifactId>launcher</artifactId>
        <version>1.0-SNAPSHOT</version>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <packaging>pom</packaging>
  <modules>
    <module>language</module>
    <module>launcher</module>
  </modules>
</project>
```

This defines a top level project which has two modules, the `language`
and the `launcher` modules. Notice that we're targeting Java 16 which
at the time of this writing is the most recent Java release.

Now we need to create one subdirectory per module and initialize them,
cd into your project root directory (where you placed the above pom)
and type the following:

```
mkdir -p launcher/src/main/java
mkdir -p language/src/main/java
```

Then we just need to create the pom files for the modules themselves,
first create `launcher/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <parent>
    <groupId>toyl</groupId>
    <artifactId>toyl-parent</artifactId>
    <version>1.0-SNAPSHOT</version>
  </parent>
  <modelVersion>4.0.0</modelVersion>

  <artifactId>launcher</artifactId>

  <properties>
    <maven.compiler.source>16</maven.compiler.source>
    <maven.compiler.target>16</maven.compiler.target>
  </properties>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-shade-plugin</artifactId>
        <version>3.1.1</version>
        <executions>
          <execution>
            <phase>package</phase>
            <goals>
              <goal>shade</goal>
            </goals>
            <configuration>
              <finalName>launcher</finalName>
              <artifactSet>
                <excludes>
                  <exclude>junit:junit</exclude>
                  <exclude>com.oracle.truffle:truffle-api</exclude>
                  <exclude>com.oracle.truffle:truffle-dsl-processor</exclude>
                  <exclude>com.oracle.truffle:truffle-tck</exclude>
                  <exclude>org.graalvm:graal-sdk</exclude>
                </excludes>
              </artifactSet>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>

  <dependencies>
    <dependency>
      <groupId>org.graalvm.sdk</groupId>
      <artifactId>graal-sdk</artifactId>
      <version>${graalvm.version}</version>
    </dependency>
    <dependency>
      <groupId>toyl</groupId>
      <artifactId>language</artifactId>
    </dependency>
  </dependencies>

</project>
```

And last, add the following code to `language/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>toyl</groupId>
    <artifactId>toyl-parent</artifactId>
    <version>1.0-SNAPSHOT</version>
  </parent>

  <artifactId>language</artifactId>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-shade-plugin</artifactId>
        <version>3.1.1</version>
        <executions>
          <execution>
            <phase>package</phase>
            <goals>
              <goal>shade</goal>
            </goals>
            <configuration>
              <finalName>toyl</finalName>
              <artifactSet>
                <excludes>
                  <exclude>junit:junit</exclude>
                  <exclude>org.graalvm.truffle:truffle-api</exclude>
                  <exclude>org.graalvm.truffle:truffle-dsl-processor</exclude>
                  <exclude>org.graalvm.truffle:truffle-tck</exclude>
                  <exclude>org.graalvm:graal-sdk</exclude>
                </excludes>
              </artifactSet>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>

  <dependencies>
    <dependency>
      <groupId>org.graalvm.truffle</groupId>
      <artifactId>truffle-dsl-processor</artifactId>
      <version>${graalvm.version}</version>
      <scope>provided</scope>
    </dependency>
    <dependency>
      <groupId>org.graalvm.truffle</groupId>
      <artifactId>truffle-api</artifactId>
      <version>${graalvm.version}</version>
    </dependency>
    <dependency>
      <groupId>org.graalvm.truffle</groupId>
      <artifactId>truffle-tck</artifactId>
      <version>${graalvm.version}</version>
      <scope>provided</scope>
    </dependency>
  </dependencies>

</project>
```

With that done you can open IntelliJ and load the root
directory. IntelliJ should detect the `pom.xml` file there and set up
everything automatically.

Once the project has loaded, hit CMD-; or go to File -> Project
Structure. Make sure that you select the GraalVM SDK that you
previously installed as the "Project SDK". Now we're ready to write
some code. We're going to implement our launcher.

## The launcher

The launcher has a simple job: Accept a valid Toyl program, execute
it, and print the result. We'll start by simply reading one line from
stdin. Add the following to
`launcher/src/main/java/toyl/launcher/Launcher.java`:

```java
package toyl;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Launcher {
  private static final String TOYL = "toyl";

  public static void main(String[] args) throws IOException {
    var lineReader = new BufferedReader(new InputStreamReader(System.in));
    var context = Context.newBuilder(TOYL).build();
    System.out.println("waiting for input");
    Value result = context.eval(TOYL, lineReader.readLine());
    System.out.println("result = " + result);
  }
}
```

Hopefully this is fairly easy to understand. We create a line reader
using stdin as the source. We then create a `Context` object using a
constructor that accepts a list of allowed languages, to which we'll
only pass our own "toyl" language.

If you attempt to run this program (using IntelliJ you should be able
to just execute the main function) the program will print "waiting for
input" and then, well, wait for input. If you type in something, 42
for example, and hit enter, the program will crash with the following:

```
Exception in thread "main" java.lang.IllegalArgumentException: A language with id 'toyl' is not installed. Installed languages are: [js, llvm].
	at org.graalvm.truffle/com.oracle.truffle.polyglot.PolyglotEngineException.illegalArgument(PolyglotEngineException.java:125)
	at org.graalvm.truffle/com.oracle.truffle.polyglot.PolyglotEngineImpl.requirePublicLanguage(PolyglotEngineImpl.java:983)
	at org.graalvm.truffle/com.oracle.truffle.polyglot.PolyglotContextImpl.requirePublicLanguage(PolyglotContextImpl.java:1369)
	at org.graalvm.truffle/com.oracle.truffle.polyglot.PolyglotContextImpl.lookupLanguageContext(PolyglotContextImpl.java:1331)
	at org.graalvm.truffle/com.oracle.truffle.polyglot.PolyglotContextImpl.eval(PolyglotContextImpl.java:1340)
	at org.graalvm.truffle/com.oracle.truffle.polyglot.PolyglotContextDispatch.eval(PolyglotContextDispatch.java:62)
	at org.graalvm.sdk/org.graalvm.polyglot.Context.eval(Context.java:375)
	at org.graalvm.sdk/org.graalvm.polyglot.Context.eval(Context.java:401)
	at toyl.launcher.Launcher.main(Launcher.java:17)
```

That's a pretty understandable error message. The reason this fails is
that we haven't yet created, let alone registered, our language. So
let's do that!

## Registering our language

To register our language we need to define it, and this is where we'll
start using the Truffle Framework API. We'll need to create a few
classes. We need to describe our language to Truffle. To do this
create a class called `ToylLanguage`, along with a supporting class
called `ToylContext`. We also need our first AST Node, which is going
to simply be a `ToylProgramNode`. But before you start creating these
files, a small warning. If you've configured the project to use the
GraalVM JDK as I recommend you'll get a lot of red warnings in
IntelliJ when you paste in the code for these classes. Don't worry,
we'll solve that. But first, the contents of the classes:

First our empty `ToylContext` class, place this in
`language/src/main/java/toyl/language/ToylContext.java`:

```java
package toyl;

public class ToylContext {
}
```

And then we need our AST Node in
`language/src/main/java/toyl/language/ToylProgramNode.java`:

```java
package toyl.ast;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;

public class ToylProgramNode extends RootNode {

  private final long program;

  public ToylProgramNode(TruffleLanguage<?> language, FrameDescriptor frameDescriptor, String program) {
    super(language, frameDescriptor);
    this.program = Long.parseLong(program);
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return this.program;
  }
}
```

And lastly the class that will put it all together `language/src/main/java/toyl/language/ToylLanguage.java`:

```java
package toyl;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.FrameDescriptor;
import toyl.language.ast.ToylProgramNode;

@TruffleLanguage.Registration(
    id = ToylLanguage.ID,
    name = "Toyl", defaultMimeType = ToylLanguage.MIME_TYPE,
    characterMimeTypes = ToylLanguage.MIME_TYPE,
    contextPolicy = TruffleLanguage.ContextPolicy.SHARED)
public class ToylLanguage extends TruffleLanguage<ToylContext> {

  public static final String ID = "toyl";
  public static final String MIME_TYPE = "application/x-toyl";

  @Override
  protected ToylContext createContext(Env env) {
    return new ToylContext();
  }

  @Override
  protected CallTarget parse(ParsingRequest request) throws Exception {

    var source = request.getSource();
    ToylProgramNode program = new ToylProgramNode(this, new FrameDescriptor(), source.getCharacters().toString());

    return Truffle.getRuntime().createCallTarget(program);
  }

}
```

Ok, got that pasted? Remember all those red warnings I mentioned? If
you hover over some of them you should see a message along the lines
of:

> Package 'com.oracle.truffle.api' is declared in module 'org.graalvm.truffle', which does not export it to the unnamed module

If you didn't pay much attention to the whole JDK 9 release and all
the stuff about modules you probably have no clue. I certainly
didn't. So what's going on here? The referenced package
`com.oracle.truffle.api` is a dependency that we declared in Maven,
and now we can't use it? There's a good introduction to Java modules
available at https://www.oracle.com/corporate/features/understanding-java-9-modules.html. In there you'll find this info:

> An exports module directive specifies one of the module’s packages whose public types (and their nested public and protected types) should be accessible to code in all other modules. An exports…to directive enables you to specify in a comma-separated list precisely which module’s or modules’ code can access the exported package—this is known as a qualified export. 

So the problem here seems to be that the package has been exported
only to some named modules. You can see the module exports very easily
in IntelliJ by finding the `truffle-api` jar in the "External
Dependencies" section in the Project window. Expand it and you'll find
`module-info.class`, in which you'll find this line:

```
exports com.oracle.truffle.api;
```

So that definitely does not look like an `export ... to`
directive. What in the...?

The answer is in the Project SDK. In the same IntelliJ window you
should see a folder for the GraalVM SDK. If you expand that there will
be an entry towards the bottom called `org.graalvm.truffle`. And if
you open the `module-info.class` file under there you'll find this:

```
exports com.oracle.truffle.api to com.oracle.graal.graal_enterprise, com.oracle.truffle.regex, com.oracle.truffle.truffle_nfi, jdk.internal.vm.compiler, org.graalvm.locator, org.graalvm.nativeimage.builder;
```

Ahhhh! Get it? So the problem is that the SDK we're building on ships
with the same Truffle API packages, but they are only intended for
internal use. And it turns out it's pretty easy to explain this to
IntelliJ and get rid of those error messages. IntelliJ processes
dependencies top to bottom. So we just need to reorder them so that
the jar we got through Maven is preferred. To do that, go to "Project
Settings" again (CMD-;) and pick "Modules" in the left hand list. Then
pick our `language` module and go to the "Dependencies" tab. Now just
pick the entry for the SDK (probably just named "16" and press the
arrow down button until it is at the bottom of this list and press
Apply. Now all the warnings should disappear. Yay!

## Ok, let's run a program!

Maybe I should explain some of that code? Nah, let's get to the fun
stuff and we'll dig into the code afterwards. Let's try to
execute. Just hit Control-R or execute the launcher. Type in your
program (42 right?) and wait for the glorious result! 

No?

> A language with id 'toyl' is not installed. Installed languages are: [js, llvm].

But...

😭

We've forgotten one important little bit. IntelliJ is really good at
figuring out how to construct the command that gets executed when you
run the launcher, but it isn't psychic. For our language to be
registered our program must know about the code we've added in the
language module. This is achieved using
`-Dtruffle.class.path.append`. Normally this will refer to a jar file,
but since we want to make it easy to execute this from IntelliJ and
not rely on Maven (to speed up our development cycle) we simply point
it to classes, so add the following to the comand line in IntelliJ:
`-Dtruffle.class.path.append=language/target/classes`. You probably
need to click "Modify Options" and check the "Add VM Options" flag.

And now, finally, you should be able to run your first Toyl program in
all it's glory!

> waiting for input
> 42
> result = 42

And of course if you try to type in something that's not a number this
will all crash and burn.

## Let's talk about what we did

Let's start by looking at `ToylLanguage`. This class defines our
language. The `@TruffleLanguage.Registration` annotation defines some
basic metadata about the language, of which the most important thing
is the language id. In fact, you might notice that our launcher has a
hardcoded language id which we can replace with `ToylLanguage.ID`. Go
ahead and do that (the code in the repository already uses this).

As you can see we've extended `TruffleLanguage` and implemented the
`parse` function which receives a `ParsingRequest`. As we change to
actually reading files instead of just stdin this abstraction means we
don't have to change anything inside `ToylLanguage`, it will all be
handled in the `Launcher`.

For now we only have one node in our AST and it therefore has to
extend `RootNode`. You will soon see that the other AST nodes that we
create will extend the basic `Node` class instead.

Now it's time to dive into lexing and parsing and AST creation.
