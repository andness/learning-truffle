package toyl.errors;

import com.oracle.truffle.api.exception.AbstractTruffleException;

public class ToylSemanticError extends AbstractTruffleException {
  public ToylSemanticError(String message) {
    super(message);
  }
}
