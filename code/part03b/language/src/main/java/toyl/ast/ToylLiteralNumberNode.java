package toyl.ast;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import java.math.BigDecimal;

public class ToylLiteralNumberNode extends ToylNode {
  private final BigDecimal value;
  public ToylLiteralNumberNode(BigDecimal number) {
    this.value = number;
  }

  @Override
  public long executeLong(VirtualFrame frame) throws UnexpectedResultException {
    throw new UnexpectedResultException(value);
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
