package toyl.ast;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;

import java.math.BigDecimal;

public abstract class ToylVarRefNode extends ToylExpressionNode {
  private final String name;
  protected FrameSlot slot;

  public ToylVarRefNode(String name, FrameSlot slot) {
    this.name = name;
    this.slot = slot;
  }

  @Specialization(guards = "frame.isLong(slot)")
  public long readLong(VirtualFrame frame) {
    try {
      return frame.getLong(this.slot);
    } catch (FrameSlotTypeException e) {
      throw new IllegalStateException(e);
    }
  }

  @Specialization(guards = "frame.isObject(slot)")
  public BigDecimal readBigDecimal(VirtualFrame frame) {
    try {
      return (BigDecimal) frame.getObject(this.slot);
    } catch (FrameSlotTypeException e) {
      throw new IllegalStateException(e);
    }
  }

}