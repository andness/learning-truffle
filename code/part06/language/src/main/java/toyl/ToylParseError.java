package toyl;

import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.source.Source;

final class ToylParseError extends AbstractTruffleException {
  private final Source source;
  private final int line;
  private final int column;
  private final String error;

  ToylParseError(Source source, int line, int column, String error) {
    super("Parse error on line %s, position %s: %s".formatted(line, column, error));
    this.source = source;
    this.line = line;
    this.column = column;
    this.error = error;
  }

  @Override
  public String getMessage() {
    var section = this.source.createSection(line);
    var messageBuilder = new StringBuilder();
    messageBuilder.append("Syntax error on line %s: %s%n".formatted(line, error));
    messageBuilder.append("%s%n".formatted(section.getCharacters()));
    messageBuilder.append((("%" + (column + 1) + "s") + "%n").formatted("^"));
    return messageBuilder.toString();
  }
}