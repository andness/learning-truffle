package toyl.ast;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import java.math.BigDecimal;
import java.math.MathContext;

public class ToylLiteralNumberNode extends ToylNode {
  private final BigDecimal value;
  public ToylLiteralNumberNode(BigDecimal value) {
    this.value = value;
  }

  @Override
  public int executeInt(VirtualFrame frame) throws UnexpectedResultException {
    throw new UnexpectedResultException(this.value);
  }

  @Override
  public BigDecimal executeNumber(VirtualFrame frame) {
    return this.value;
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    return this.value;
  }
}
