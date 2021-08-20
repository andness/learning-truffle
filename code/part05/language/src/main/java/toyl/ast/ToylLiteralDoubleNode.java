package toyl.ast;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

public class ToylLiteralDoubleNode extends ToylExpressionNode {
  private final double value;
  public ToylLiteralDoubleNode(double value) {
    this.value = value;
  }

  @Override
  public int executeInt(VirtualFrame frame) throws UnexpectedResultException {
    throw new UnexpectedResultException(this.value);
  }

  @Override
  public double executeDouble(VirtualFrame frame) {
    return this.value;
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    return this.value;
  }
}
