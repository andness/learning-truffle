package toyl.ast;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;

import java.math.BigDecimal;

public class ToylVarRefNode extends ToylExpressionNode {
  private final String name;
  private final FrameSlot slot;

  public ToylVarRefNode(String name, FrameSlot slot) {
    this.name = name;
    this.slot = slot;
  }

  @Override
  public long executeLong(VirtualFrame frame) {
    try {
      return frame.getLong(this.slot);
    } catch (FrameSlotTypeException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public BigDecimal executeNumber(VirtualFrame frame) {
    try {
      return (BigDecimal) frame.getObject(this.slot);
    } catch (FrameSlotTypeException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    return frame.getValue(this.slot);
  }
}