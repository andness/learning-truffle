package toyl.ast;

import com.oracle.truffle.api.frame.VirtualFrame;

import java.math.BigDecimal;
import java.math.MathContext;

public class ToylLiteralIntNode extends ToylNode {
  private final int value;
  public ToylLiteralIntNode(int value) {
    this.value = value;
  }

  @Override
  public int executeInt(VirtualFrame frame) {
    return this.value;
  }

  @Override
  public BigDecimal executeNumber(VirtualFrame frame) {
    return new BigDecimal(this.value, MathContext.DECIMAL128);
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    return this.value;
  }
}
