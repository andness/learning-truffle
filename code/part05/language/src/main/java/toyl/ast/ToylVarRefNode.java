package toyl.ast;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;

public class ToylVarRefNode extends ToylExpressionNode {
  private final String name;
  private final FrameSlot slot;

  public ToylVarRefNode(String name, FrameSlot slot) {
    this.name = name;
    this.slot = slot;
  }

  @Override
  public int executeInt(VirtualFrame frame) {
    try {
      return frame.getInt(this.slot);
    } catch (FrameSlotTypeException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public double executeDouble(VirtualFrame frame) {
    try {
      return frame.getDouble(this.slot);
    } catch (FrameSlotTypeException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    return frame.getValue(this.slot);
  }
}
