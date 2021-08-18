package toyl.ast;

import com.oracle.truffle.api.frame.VirtualFrame;

public class ToylLiteralNumberNode extends ToylNode {
  private final long value;
  public ToylLiteralNumberNode(String number) {
    this.value = Long.parseLong(number);
  }

  @Override
  public long execute(VirtualFrame frame) {
    return this.value;
  }
}
