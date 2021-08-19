package toyl.ast;

import com.oracle.truffle.api.frame.VirtualFrame;

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
  public double executeDouble(VirtualFrame frame) {
    return this.value;
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    return this.value;
  }
}
