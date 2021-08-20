package toyl.ast;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;

public class ToylAssignmentNode extends ToylStatementNode {
  private final String name;
  private final FrameSlot slot;
  private final ToylExpressionNode expr;

  public ToylAssignmentNode(String name, FrameSlot slot, ToylExpressionNode expr) {
    this.name = name;
    this.slot = slot;
    this.expr = expr;
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    var value = this.expr.executeGeneric(frame);
    frame.setObject(this.slot, value);
    return value;
  }
}
