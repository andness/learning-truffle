package toyl.ast;

import com.oracle.truffle.api.frame.VirtualFrame;

public class ToylLiteralNumberNode extends ToylNode {
  private final double value;
  public ToylLiteralNumberNode(String number) {
    this.value = Double.parseDouble(number);
  }

  @Override
  public double execute(VirtualFrame frame) {
    return this.value;
  }
}
