package toyl;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class Launcher {
  public static void main(String[] args) throws IOException {
    Source source = null;
    if (args.length == 0) {
      var lineReader = new BufferedReader(new InputStreamReader(System.in));
      source = Source.newBuilder(ToylLanguage.ID, lineReader, "stdin").build();
    } else {
      var file = new File(args[0]);
      source = Source.newBuilder(ToylLanguage.ID, file).build();
    }
    var context = Context.newBuilder(ToylLanguage.ID).build();
    try {
      Value result = context.eval(source);
      System.out.println("result = " + result);
    } catch (PolyglotException error) {
      if (error.isInternalError()) {
        error.printStackTrace();
      } else {
        System.err.println(error.getMessage());
      }
      System.exit(1);
    }
  }
}
