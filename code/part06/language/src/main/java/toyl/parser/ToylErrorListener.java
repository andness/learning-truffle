package toyl.parser;

import com.oracle.truffle.api.source.Source;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import toyl.errors.ToylParseError;

public class ToylErrorListener extends BaseErrorListener {

  private final Source source;

  public ToylErrorListener(Source source) {
    this.source = source;
  }

  @Override
  public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
    throw new ToylParseError(source, line, charPositionInLine, msg);
  }
}