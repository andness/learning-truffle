package toyl.ast;

import com.oracle.truffle.api.frame.VirtualFrame;

import java.math.BigDecimal;
import java.math.MathContext;

public class ToylLiteralNumberNode extends ToylNode {
  private final BigDecimal value;
  public ToylLiteralNumberNode(String number) {
    this.value = new BigDecimal(number, MathContext.UNLIMITED);
  }

  @Override
  public BigDecimal execute(VirtualFrame frame) {
    return this.value;
  }
}
