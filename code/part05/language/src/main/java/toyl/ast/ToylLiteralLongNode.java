package toyl.ast;

import com.oracle.truffle.api.frame.VirtualFrame;

import java.math.BigDecimal;

public class ToylLiteralLongNode extends ToylExpressionNode {
  private final long value;
  public ToylLiteralLongNode(long value) {
    this.value = value;
  }

  @Override
  public long executeLong(VirtualFrame frame) {
    return this.value;
  }

  @Override
  public BigDecimal executeNumber(VirtualFrame frame) {
    return new BigDecimal(this.value);
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    return this.value;
  }
}
