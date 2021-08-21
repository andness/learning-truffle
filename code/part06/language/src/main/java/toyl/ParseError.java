package toyl;

import com.oracle.truffle.api.exception.AbstractTruffleException;

class ParseError extends AbstractTruffleException {
  public ParseError(String message) {
    super(message);
  }


}
