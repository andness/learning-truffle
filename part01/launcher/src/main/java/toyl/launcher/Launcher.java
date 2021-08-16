package toyl.launcher;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import toyl.language.ToylLanguage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Launcher {
  public static void main(String[] args) throws IOException {
    var lineReader = new BufferedReader(new InputStreamReader(System.in));
    var context = Context.newBuilder(ToylLanguage.ID).build();
    System.out.println("waiting for input");
    Value result = context.eval(ToylLanguage.ID, lineReader.readLine());
    System.out.println("result = " + result);
  }
}
