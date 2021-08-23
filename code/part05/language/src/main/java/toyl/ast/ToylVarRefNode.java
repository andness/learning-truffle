package toyl.ast;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;

public abstract class ToylVarRefNode extends ToylExpressionNode {
  private final String name;
  protected final FrameSlot slot;

  public ToylVarRefNode(String name, FrameSlot slot) {
    this.name = name;
    this.slot = slot;
  }

  @Specialization(guards = "frame.isInt(slot)")
  public int readInt(VirtualFrame frame) {
    try {
      return frame.getInt(this.slot);
    } catch (FrameSlotTypeException e) {
      throw new IllegalStateException(e);
    }
  }

  @Specialization(guards = "frame.isDouble(slot)")
  public double readDouble(VirtualFrame frame) {
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
